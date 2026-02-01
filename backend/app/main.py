from __future__ import annotations

import os
import time
from datetime import datetime

import jwt
from flask import Flask, jsonify, request

from .security import SigningKeys, issue_access_jwt, load_or_create_keys, verify_access_jwt
from .store import InMemoryStore, User


DATA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".data"))
APP_AUTH_SECRET = os.environ.get("ONEACCESS_APP_AUTH_SECRET", "dev-only-change-me")
TOKEN_TTL_SECONDS = int(os.environ.get("ONEACCESS_TOKEN_TTL_SECONDS", "20"))

app = Flask(__name__)
store = InMemoryStore()
signing_keys: SigningKeys = load_or_create_keys(DATA_DIR)


def _json_error(message: str, status: int):
    return jsonify({"error": message}), status


def _require_json() -> dict:
    if not request.is_json:
        raise ValueError("Expected application/json")
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        raise ValueError("Invalid JSON body")
    return data


def _issue_app_session(*, user: User) -> str:
    now = int(time.time())
    payload = {"sub": user.user_id, "email": user.email, "cid": user.company_id, "iat": now, "exp": now + 3600}
    return jwt.encode(payload, APP_AUTH_SECRET, algorithm="HS256")


def _get_user_from_bearer() -> User:
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise PermissionError("Missing Bearer token")
    token = auth.removeprefix("Bearer ").strip()
    try:
        payload = jwt.decode(token, APP_AUTH_SECRET, algorithms=["HS256"])
    except jwt.PyJWTError as e:
        raise PermissionError(f"Invalid session token: {e}") from e
    user_id = payload.get("sub")
    if not user_id or user_id not in store.users_by_id:
        raise PermissionError("Unknown user")
    user = store.users_by_id[user_id]
    if not user.active:
        raise PermissionError("User inactive")
    return user


def _format_duration(seconds: int) -> str:
    """Format duration in seconds to human-readable format"""
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60
    
    if hours > 0:
        return f"{hours}h {minutes}m {secs}s"
    elif minutes > 0:
        return f"{minutes}m {secs}s"
    else:
        return f"{secs}s"


@app.get("/.well-known/jwks.json")
def jwks():
    return jsonify({"keys": [signing_keys.public_jwk()]})


@app.post("/auth/login")
def login():
    try:
        data = _require_json()
        email = str(data.get("email", "")).lower().strip()
        if not email:
            return _json_error("Missing email", 400)
        user = store.users_by_email.get(email)
        if not user or not user.active:
            return _json_error("Not allowed", 403)
        return jsonify({"accessToken": _issue_app_session(user=user)})
    except ValueError as e:
        return _json_error(str(e), 400)


@app.post("/qr/token")
def qr_token():
    try:
        user = _get_user_from_bearer()
        data = _require_json()
        gate_id = str(data.get("gateId", "")).strip()
        reader_nonce = str(data.get("readerNonce", "")).strip()
        device_id = data.get("deviceId")
        device_id = str(device_id).strip() if device_id else None

        if not gate_id:
            return _json_error("Missing gateId", 400)
        if not reader_nonce or not (8 <= len(reader_nonce) <= 64):
            return _json_error("Invalid readerNonce", 400)

        gate = store.gates.get(gate_id)
        if not gate:
            return _json_error("Unknown gateId", 404)

        if gate.kind == "BUILDING" and gate.company_id != user.company_id:
            return _json_error("Not allowed for this building", 403)
        if device_id and device_id in store.revoked_devices:
            return _json_error("Device revoked", 403)

        # Check if user has delegated access for this gate
        delegated_by = None
        if gate.kind == "BUILDING" and gate.company_id != user.company_id:
            # Check for active delegations
            delegations = store.get_active_delegations_for_user(user.user_id)
            for delegation in delegations:
                if gate_id in delegation.gate_ids:
                    delegated_by = delegation.delegator_id
                    break
            if not delegated_by:
                return _json_error("Not allowed for this building", 403)

        now = int(time.time())
        exp = now + TOKEN_TTL_SECONDS
        claims = {
            "v": 1,
            "sub": user.user_id,
            "cid": user.company_id,
            "gid": gate_id,
            "rnonce": reader_nonce,
            "did": device_id or "UNKNOWN_DEVICE",
            "jti": f"{user.user_id}:{reader_nonce}:{now}",
        }
        
        if delegated_by:
            claims["delegated_by"] = delegated_by
            
        token = issue_access_jwt(keys=signing_keys, claims=claims, ttl_seconds=TOKEN_TTL_SECONDS)
        return jsonify({"token": token, "expEpochSeconds": exp})
    except PermissionError as e:
        return _json_error(str(e), 401)
    except ValueError as e:
        return _json_error(str(e), 400)


@app.post("/access/verify")
def verify():
    try:
        data = _require_json()
        reader_id = str(data.get("readerId", "")).strip()
        gate_id = str(data.get("gateId", "")).strip()
        token = str(data.get("token", "")).strip()
        door_opened = bool(data.get("doorOpened", False))
        direction = str(data.get("direction", "ENTRY")).strip().upper()  # "ENTRY" or "EXIT"

        if not reader_id or not gate_id or not token:
            return _json_error("Missing readerId/gateId/token", 400)

        gate = store.gates.get(gate_id)
        if not gate:
            return _json_error("Unknown gateId", 404)

        try:
            payload = verify_access_jwt(token=token, public_key=signing_keys.public_key)
        except Exception:
            store.record(user_id=None, company_id=None, gate_id=gate_id, reader_id=reader_id, 
                        decision="DENY", reason="Invalid token", door_status="UNKNOWN")
            return jsonify({"decision": "DENY", "reason": "INVALID_TOKEN"})

        user_id = payload.get("sub")
        token_cid = payload.get("cid")
        token_gid = payload.get("gid")
        delegated_by = payload.get("delegated_by")
        visitor_pass_id = payload.get("visitor_pass_id")
        user = store.users_by_id.get(user_id) if user_id else None

        # Handle visitor passes
        if visitor_pass_id:
            visitor_pass = store.get_visitor_pass(visitor_pass_id)
            if not visitor_pass or not visitor_pass.active:
                store.record(user_id=None, company_id=None, gate_id=gate_id, reader_id=reader_id,
                           decision="DENY", reason="Invalid visitor pass", door_status="UNKNOWN")
                return jsonify({"decision": "DENY", "reason": "INVALID_VISITOR_PASS"})
            
            if visitor_pass.used_count >= visitor_pass.max_uses:
                store.record(user_id=None, company_id=visitor_pass.host_company_id, gate_id=gate_id, reader_id=reader_id,
                           decision="DENY", reason="Visitor pass usage exceeded", door_status="UNKNOWN", 
                           visitor_pass_id=visitor_pass_id)
                return jsonify({"decision": "DENY", "reason": "USAGE_EXCEEDED"})

        if not user or not user.active:
            door_status = "OPENED" if door_opened else "UNKNOWN"
            store.record(user_id=user_id, company_id=token_cid, gate_id=gate_id, reader_id=reader_id, 
                        decision="DENY", reason="Unknown/inactive user", door_status=door_status)
            return jsonify({"decision": "DENY", "reason": "USER_INACTIVE"})

        if token_gid != gate_id:
            door_status = "OPENED" if door_opened else "UNKNOWN"
            store.record(user_id=user.user_id, company_id=user.company_id, gate_id=gate_id, reader_id=reader_id, 
                        decision="DENY", reason="Token gate mismatch", door_status=door_status)
            return jsonify({"decision": "DENY", "reason": "GATE_MISMATCH"})

        # Check building access (including delegations)
        has_access = False
        if gate.kind == "MAIN":
            has_access = True
        elif gate.kind == "BUILDING":
            # Check direct access
            if gate.company_id == user.company_id:
                has_access = True
            # Check delegated access
            elif delegated_by:
                delegations = store.get_active_delegations_for_user(user.user_id)
                for delegation in delegations:
                    if gate_id in delegation.gate_ids:
                        has_access = True
                        break

        if not has_access:
            door_status = "OPENED" if door_opened else "UNKNOWN"
            store.record(user_id=user.user_id, company_id=user.company_id, gate_id=gate_id, reader_id=reader_id, 
                        decision="DENY", reason="Not allowed for building", door_status=door_status,
                        delegated_by=delegated_by, visitor_pass_id=visitor_pass_id)
            return jsonify({"decision": "DENY", "reason": "NOT_ALLOWED"})

        # Access granted - record with door status
        door_status = "OPENED" if door_opened else "FAILED"
        store.record(user_id=user.user_id, company_id=user.company_id, gate_id=gate_id, reader_id=reader_id, 
                    decision="ALLOW", reason="OK", door_status=door_status,
                    delegated_by=delegated_by, visitor_pass_id=visitor_pass_id)
        
        # Increment visitor pass usage if applicable
        if visitor_pass_id:
            visitor_pass = store.get_visitor_pass(visitor_pass_id)
            if visitor_pass:
                visitor_pass.used_count += 1
        
        # Time tracking for building gates (if door actually opened)
        session_info = None
        if door_opened and gate.kind == "BUILDING":
            if direction == "ENTRY":
                session_id = store.start_time_session(user.user_id, user.company_id, gate_id)
                session_info = {"action": "SESSION_STARTED", "sessionId": session_id}
            elif direction == "EXIT":
                session = store.end_time_session(user.user_id, gate_id)
                if session:
                    session_info = {
                        "action": "SESSION_ENDED",
                        "sessionId": session.session_id,
                        "entryTime": session.entry_time.isoformat(),
                        "exitTime": session.exit_time.isoformat() if session.exit_time else None,
                        "durationSeconds": session.duration_seconds,
                        "durationFormatted": _format_duration(session.duration_seconds) if session.duration_seconds else None
                    }
        
        response = {"decision": "ALLOW", "reason": "OK"}
        if session_info:
            response["timeTracking"] = session_info
        
        return jsonify(response)
    except ValueError as e:
        return _json_error(str(e), 400)


@app.post("/visitor/token")
def visitor_token():
    try:
        data = _require_json()
        pass_id = str(data.get("passId", "")).strip()
        gate_id = str(data.get("gateId", "")).strip()
        reader_nonce = str(data.get("readerNonce", "")).strip()

        if not pass_id or not gate_id:
            return _json_error("Missing passId or gateId", 400)
        if not reader_nonce or not (8 <= len(reader_nonce) <= 64):
            return _json_error("Invalid readerNonce", 400)

        visitor_pass = store.get_visitor_pass(pass_id)
        if not visitor_pass or not visitor_pass.active:
            return _json_error("Invalid visitor pass", 404)

        if visitor_pass.valid_until < datetime.utcnow():
            return _json_error("Visitor pass expired", 403)

        if visitor_pass.used_count >= visitor_pass.max_uses:
            return _json_error("Visitor pass usage exceeded", 403)

        if gate_id not in visitor_pass.gate_ids:
            return _json_error("Gate not authorized for this visitor pass", 403)

        gate = store.gates.get(gate_id)
        if not gate:
            return _json_error("Unknown gateId", 404)

        now = int(time.time())
        exp = now + TOKEN_TTL_SECONDS
        claims = {
            "v": 1,
            "sub": f"VISITOR_{pass_id}",
            "cid": visitor_pass.host_company_id,
            "gid": gate_id,
            "rnonce": reader_nonce,
            "did": "VISITOR_DEVICE",
            "jti": f"visitor:{pass_id}:{reader_nonce}:{now}",
            "visitor_pass_id": pass_id,
        }

        token = issue_access_jwt(keys=signing_keys, claims=claims, ttl_seconds=TOKEN_TTL_SECONDS)
        return jsonify({
            "token": token, 
            "expEpochSeconds": exp,
            "visitorName": visitor_pass.visitor_name,
            "remainingUses": visitor_pass.max_uses - visitor_pass.used_count
        })
    except ValueError as e:
        return _json_error(str(e), 400)


@app.post("/delegation/create")
def create_delegation():
    try:
        user = _get_user_from_bearer()
        data = _require_json()
        
        delegatee_email = str(data.get("delegateeEmail", "")).lower().strip()
        gate_ids = data.get("gateIds", [])
        hours = int(data.get("hours", 24))
        
        if not delegatee_email:
            return _json_error("Missing delegateeEmail", 400)
        if not gate_ids:
            return _json_error("Missing gateIds", 400)
        if hours < 1 or hours > 168:  # Max 1 week
            return _json_error("Hours must be between 1 and 168", 400)
            
        # Validate gate access
        for gate_id in gate_ids:
            gate = store.gates.get(gate_id)
            if not gate:
                return _json_error(f"Unknown gate: {gate_id}", 404)
            if gate.kind == "BUILDING" and gate.company_id != user.company_id:
                return _json_error(f"Not authorized for gate: {gate_id}", 403)
        
        delegation_id = store.create_delegation(
            delegator_id=user.user_id,
            delegatee_email=delegatee_email,
            gate_ids=gate_ids,
            hours=hours,
            created_by=user.user_id
        )
        
        return jsonify({"delegationId": delegation_id, "status": "created"})
    except PermissionError as e:
        return _json_error(str(e), 401)
    except ValueError as e:
        return _json_error(str(e), 400)


@app.post("/visitor/create")
def create_visitor_pass():
    try:
        user = _get_user_from_bearer()
        data = _require_json()
        
        visitor_name = str(data.get("visitorName", "")).strip()
        visitor_phone = str(data.get("visitorPhone", "")).strip()
        gate_ids = data.get("gateIds", [])
        hours = int(data.get("hours", 24))
        
        if not visitor_name:
            return _json_error("Missing visitorName", 400)
        if not visitor_phone:
            return _json_error("Missing visitorPhone", 400)
        if not gate_ids:
            return _json_error("Missing gateIds", 400)
        if hours < 1 or hours > 72:  # Max 3 days for visitors
            return _json_error("Hours must be between 1 and 72", 400)
            
        # Validate gate access
        for gate_id in gate_ids:
            gate = store.gates.get(gate_id)
            if not gate:
                return _json_error(f"Unknown gate: {gate_id}", 404)
            if gate.kind == "BUILDING" and gate.company_id != user.company_id:
                return _json_error(f"Not authorized for gate: {gate_id}", 403)
        
        pass_id = store.create_visitor_pass(
            created_by=user.user_id,
            visitor_name=visitor_name,
            visitor_phone=visitor_phone,
            gate_ids=gate_ids,
            hours=hours,
            host_company_id=user.company_id
        )
        
        return jsonify({"passId": pass_id, "status": "created"})
    except PermissionError as e:
        return _json_error(str(e), 401)
    except ValueError as e:
        return _json_error(str(e), 400)


@app.get("/delegation/list")
def list_delegations():
    try:
        user = _get_user_from_bearer()
        
        # Get delegations created by this user
        created_delegations = []
        for delegation in store.delegations.values():
            if delegation.delegator_id == user.user_id and delegation.active:
                delegatee = store.users_by_id.get(delegation.delegatee_id)
                created_delegations.append({
                    "delegationId": delegation.delegation_id,
                    "delegateeEmail": delegatee.email if delegatee else "Unknown",
                    "gateIds": delegation.gate_ids,
                    "validUntil": delegation.valid_until.isoformat(),
                    "createdAt": delegation.created_at.isoformat()
                })
        
        # Get delegations received by this user
        received_delegations = []
        for delegation in store.get_active_delegations_for_user(user.user_id):
            delegator = store.users_by_id.get(delegation.delegator_id)
            received_delegations.append({
                "delegationId": delegation.delegation_id,
                "delegatorEmail": delegator.email if delegator else "Unknown",
                "gateIds": delegation.gate_ids,
                "validUntil": delegation.valid_until.isoformat(),
                "createdAt": delegation.created_at.isoformat()
            })
        
        return jsonify({
            "created": created_delegations,
            "received": received_delegations
        })
    except PermissionError as e:
        return _json_error(str(e), 401)


@app.get("/visitor/list")
def list_visitor_passes():
    try:
        user = _get_user_from_bearer()
        
        visitor_passes = []
        for vpass in store.visitor_passes.values():
            if vpass.created_by == user.user_id and vpass.active:
                visitor_passes.append({
                    "passId": vpass.pass_id,
                    "visitorName": vpass.visitor_name,
                    "visitorPhone": vpass.visitor_phone,
                    "gateIds": vpass.gate_ids,
                    "validUntil": vpass.valid_until.isoformat(),
                    "createdAt": vpass.created_at.isoformat(),
                    "usedCount": vpass.used_count,
                    "maxUses": vpass.max_uses
                })
        
        return jsonify({"visitorPasses": visitor_passes})
    except PermissionError as e:
        return _json_error(str(e), 401)


@app.get("/audit")
def audit():
    limit_raw = request.args.get("limit", "50")
    try:
        limit = max(1, min(500, int(limit_raw)))
    except ValueError:
        limit = 50
    events = store.audit[-limit:]
    return jsonify(
        [
            {
                "ts": e.ts,
                "userId": e.user_id,
                "companyId": e.company_id,
                "gateId": e.gate_id,
                "readerId": e.reader_id,
                "decision": e.decision,
                "reason": e.reason,
                "doorStatus": e.door_status,
                "delegatedBy": e.delegated_by,
                "visitorPassId": e.visitor_pass_id,
            }
            for e in reversed(events)
        ]
    )


@app.get("/time/sessions")
def get_time_sessions():
    """Get time tracking sessions for the logged-in user"""
    try:
        user = _get_user_from_bearer()
        limit_raw = request.args.get("limit", "50")
        try:
            limit = max(1, min(100, int(limit_raw)))
        except ValueError:
            limit = 50
        
        sessions = store.get_user_time_sessions(user.user_id, limit)
        
        return jsonify({
            "sessions": [
                {
                    "sessionId": s.session_id,
                    "userId": s.user_id,
                    "companyId": s.company_id,
                    "gateIdEntry": s.gate_id_entry,
                    "entryTime": s.entry_time.isoformat(),
                    "exitTime": s.exit_time.isoformat() if s.exit_time else None,
                    "durationSeconds": s.duration_seconds,
                    "durationFormatted": _format_duration(s.duration_seconds) if s.duration_seconds else None,
                    "status": s.status
                }
                for s in sessions
            ]
        })
    except PermissionError as e:
        return _json_error(str(e), 401)


@app.get("/time/current")
def get_current_session():
    """Get the current active session for the logged-in user"""
    try:
        user = _get_user_from_bearer()
        session = store.get_active_session(user.user_id)
        
        if not session:
            return jsonify({"activeSession": None})
        
        # Calculate current duration for active session
        current_duration = int((datetime.utcnow() - session.entry_time).total_seconds())
        
        return jsonify({
            "activeSession": {
                "sessionId": session.session_id,
                "userId": session.user_id,
                "companyId": session.company_id,
                "gateIdEntry": session.gate_id_entry,
                "entryTime": session.entry_time.isoformat(),
                "currentDurationSeconds": current_duration,
                "currentDurationFormatted": _format_duration(current_duration),
                "status": session.status
            }
        })
    except PermissionError as e:
        return _json_error(str(e), 401)


@app.get("/time/summary")
def get_time_summary():
    """Get time tracking summary statistics for the logged-in user"""
    try:
        user = _get_user_from_bearer()
        sessions = store.get_user_time_sessions(user.user_id, limit=1000)
        
        completed_sessions = [s for s in sessions if s.status == "COMPLETED" and s.duration_seconds]
        
        total_time = sum(s.duration_seconds for s in completed_sessions)
        avg_time = total_time // len(completed_sessions) if completed_sessions else 0
        
        # Today's sessions
        today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        today_sessions = [s for s in completed_sessions if s.entry_time >= today_start]
        today_total = sum(s.duration_seconds for s in today_sessions)
        
        return jsonify({
            "summary": {
                "totalSessions": len(completed_sessions),
                "totalTimeSeconds": total_time,
                "totalTimeFormatted": _format_duration(total_time),
                "averageTimeSeconds": avg_time,
                "averageTimeFormatted": _format_duration(avg_time),
                "todaySessions": len(today_sessions),
                "todayTimeSeconds": today_total,
                "todayTimeFormatted": _format_duration(today_total),
                "hasActiveSession": user.user_id in store.active_sessions
            }
        })
    except PermissionError as e:
        return _json_error(str(e), 401)


def create_app() -> Flask:
    return app


if __name__ == "__main__":
    # For local development
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 8000)), debug=False)

