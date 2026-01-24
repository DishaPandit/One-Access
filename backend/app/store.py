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

