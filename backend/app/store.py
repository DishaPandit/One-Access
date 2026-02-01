from __future__ import annotations

import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta


@dataclass(frozen=True)
class User:
    user_id: str
    email: str
    company_id: str
    active: bool = True


@dataclass(frozen=True)
class Gate:
    gate_id: str
    kind: str  # "MAIN" | "BUILDING"
    company_id: str | None  # only for building gates


@dataclass
class AuditEvent:
    ts: int
    user_id: str | None
    company_id: str | None
    gate_id: str
    reader_id: str
    decision: str
    reason: str
    door_status: str = "UNKNOWN"  # "OPENED", "FAILED", "UNKNOWN"
    delegated_by: str | None = None
    visitor_pass_id: str | None = None


@dataclass
class Delegation:
    delegation_id: str
    delegator_id: str  # Who is giving access
    delegatee_id: str  # Who is receiving access
    gate_ids: list[str]  # Which gates
    valid_until: datetime
    created_by: str
    active: bool = True
    created_at: datetime = None

    def __post_init__(self):
        if self.created_at is None:
            object.__setattr__(self, 'created_at', datetime.utcnow())


@dataclass
class VisitorPass:
    pass_id: str
    created_by: str  # Employee who created the pass
    visitor_name: str
    visitor_phone: str
    gate_ids: list[str]
    valid_until: datetime
    host_company_id: str
    active: bool = True
    created_at: datetime = None
    used_count: int = 0
    max_uses: int = 5

    def __post_init__(self):
        if self.created_at is None:
            object.__setattr__(self, 'created_at', datetime.utcnow())


@dataclass
class TimeSession:
    session_id: str
    user_id: str
    company_id: str
    gate_id_entry: str  # Which gate they entered through
    entry_time: datetime
    exit_time: datetime | None = None
    duration_seconds: int | None = None
    status: str = "ACTIVE"  # "ACTIVE" | "COMPLETED"

    def complete_session(self, gate_id_exit: str, exit_time: datetime) -> None:
        object.__setattr__(self, 'exit_time', exit_time)
        object.__setattr__(self, 'duration_seconds', int((exit_time - self.entry_time).total_seconds()))
        object.__setattr__(self, 'status', 'COMPLETED')


class InMemoryStore:
    """
    MVP in-memory store. Replace with Postgres later.
    """

    def __init__(self) -> None:
        self.users_by_email: dict[str, User] = {
            "alice@acme.com": User(user_id="U_ALICE", email="alice@acme.com", company_id="ACME"),
            "bob@globex.com": User(user_id="U_BOB", email="bob@globex.com", company_id="GLOBEX"),
        }
        self.users_by_id: dict[str, User] = {u.user_id: u for u in self.users_by_email.values()}

        self.gates: dict[str, Gate] = {
            "MAIN_GATE": Gate(gate_id="MAIN_GATE", kind="MAIN", company_id=None),
            "BLD_ACME": Gate(gate_id="BLD_ACME", kind="BUILDING", company_id="ACME"),
            "BLD_GLOBEX": Gate(gate_id="BLD_GLOBEX", kind="BUILDING", company_id="GLOBEX"),
        }

        self.audit: list[AuditEvent] = []
        self.revoked_devices: set[str] = set()
        
        # New collections for extended features
        self.delegations: dict[str, Delegation] = {}
        self.visitor_passes: dict[str, VisitorPass] = {}
        
        # Time tracking sessions
        self.time_sessions: dict[str, TimeSession] = {}  # session_id -> TimeSession
        self.active_sessions: dict[str, str] = {}  # user_id -> session_id (for quick lookup)

    def record(self, *, user_id: str | None, company_id: str | None, gate_id: str, reader_id: str, 
               decision: str, reason: str, door_status: str = "UNKNOWN", 
               delegated_by: str | None = None, visitor_pass_id: str | None = None) -> None:
        self.audit.append(
            AuditEvent(
                ts=int(time.time()),
                user_id=user_id,
                company_id=company_id,
                gate_id=gate_id,
                reader_id=reader_id,
                decision=decision,
                reason=reason,
                door_status=door_status,
                delegated_by=delegated_by,
                visitor_pass_id=visitor_pass_id,
            )
        )

    def create_delegation(self, delegator_id: str, delegatee_email: str, gate_ids: list[str], 
                         hours: int, created_by: str) -> str:
        """Create a new delegation"""
        delegation_id = f"DEL_{uuid.uuid4().hex[:8].upper()}"
        delegatee = self.users_by_email.get(delegatee_email)
        if not delegatee:
            raise ValueError(f"User not found: {delegatee_email}")
            
        valid_until = datetime.utcnow() + timedelta(hours=hours)
        delegation = Delegation(
            delegation_id=delegation_id,
            delegator_id=delegator_id,
            delegatee_id=delegatee.user_id,
            gate_ids=gate_ids,
            valid_until=valid_until,
            created_by=created_by
        )
        self.delegations[delegation_id] = delegation
        return delegation_id

    def create_visitor_pass(self, created_by: str, visitor_name: str, visitor_phone: str, 
                           gate_ids: list[str], hours: int, host_company_id: str) -> str:
        """Create a new visitor pass"""
        pass_id = f"VIS_{uuid.uuid4().hex[:8].upper()}"
        valid_until = datetime.utcnow() + timedelta(hours=hours)
        
        visitor_pass = VisitorPass(
            pass_id=pass_id,
            created_by=created_by,
            visitor_name=visitor_name,
            visitor_phone=visitor_phone,
            gate_ids=gate_ids,
            valid_until=valid_until,
            host_company_id=host_company_id
        )
        self.visitor_passes[pass_id] = visitor_pass
        return pass_id

    def get_active_delegations_for_user(self, user_id: str) -> list[Delegation]:
        """Get all active delegations where user is the delegatee"""
        now = datetime.utcnow()
        return [d for d in self.delegations.values() 
                if d.delegatee_id == user_id and d.active and d.valid_until > now]

    def get_visitor_pass(self, pass_id: str) -> VisitorPass | None:
        """Get visitor pass by ID"""
        return self.visitor_passes.get(pass_id)

    def start_time_session(self, user_id: str, company_id: str, gate_id: str) -> str:
        """Start a time tracking session for entry"""
        # Close any existing active session for this user (shouldn't happen, but handle it)
        if user_id in self.active_sessions:
            old_session_id = self.active_sessions[user_id]
            if old_session_id in self.time_sessions:
                old_session = self.time_sessions[old_session_id]
                if old_session.status == "ACTIVE":
                    # Auto-complete the old session
                    old_session.complete_session(gate_id, datetime.utcnow())
        
        session_id = f"SES_{uuid.uuid4().hex[:12].upper()}"
        session = TimeSession(
            session_id=session_id,
            user_id=user_id,
            company_id=company_id,
            gate_id_entry=gate_id,
            entry_time=datetime.utcnow(),
            status="ACTIVE"
        )
        self.time_sessions[session_id] = session
        self.active_sessions[user_id] = session_id
        return session_id

    def end_time_session(self, user_id: str, gate_id: str) -> TimeSession | None:
        """End the active time tracking session for exit"""
        if user_id not in self.active_sessions:
            return None
        
        session_id = self.active_sessions[user_id]
        session = self.time_sessions.get(session_id)
        if not session or session.status != "ACTIVE":
            return None
        
        session.complete_session(gate_id, datetime.utcnow())
        del self.active_sessions[user_id]
        return session

    def get_user_time_sessions(self, user_id: str, limit: int = 50) -> list[TimeSession]:
        """Get time sessions for a user, most recent first"""
        sessions = [s for s in self.time_sessions.values() if s.user_id == user_id]
        sessions.sort(key=lambda s: s.entry_time, reverse=True)
        return sessions[:limit]

    def get_active_session(self, user_id: str) -> TimeSession | None:
        """Get active session for a user"""
        if user_id not in self.active_sessions:
            return None
        session_id = self.active_sessions[user_id]
        return self.time_sessions.get(session_id)

