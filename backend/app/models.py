"""
Data models for the One-Access system.

In the MVP we keep the backend dependency-light (Flask) to run on Python 3.14
without compiling pydantic-core. We validate JSON manually in routes.
"""

from typing import Dict, List, Optional
from datetime import datetime, timedelta

class User:
    def __init__(self, id: str, email: str, company_id: str, name: str = ""):
        self.id = id
        self.email = email
        self.company_id = company_id
        self.name = name or email.split('@')[0].title()
        self.active = True

class Company:
    def __init__(self, id: str, name: str, building_ids: List[str]):
        self.id = id
        self.name = name
        self.building_ids = building_ids

class Gate:
    def __init__(self, id: str, name: str, type: str, building_id: Optional[str] = None):
        self.id = id
        self.name = name
        self.type = type  # "MAIN_GATE" or "BUILDING_GATE"
        self.building_id = building_id

class AccessEvent:
    def __init__(self, user_id: str, gate_id: str, result: str, reason: str, reader_id: str = "", device_id: str = "", 
                 door_status: str = "UNKNOWN", delegated_by: str = "", visitor_pass_id: str = ""):
        self.user_id = user_id
        self.gate_id = gate_id
        self.result = result  # "ALLOW" or "DENY"
        self.reason = reason
        self.reader_id = reader_id
        self.device_id = device_id
        self.door_status = door_status  # "OPENED", "FAILED", "UNKNOWN"
        self.delegated_by = delegated_by  # If access was delegated by another employee
        self.visitor_pass_id = visitor_pass_id  # If this was a visitor pass
        self.timestamp = datetime.utcnow().isoformat()

class Delegation:
    def __init__(self, id: str, delegator_id: str, delegatee_id: str, gate_ids: List[str], 
                 valid_until: datetime, created_by: str):
        self.id = id
        self.delegator_id = delegator_id  # Who is giving access
        self.delegatee_id = delegatee_id  # Who is receiving access
        self.gate_ids = gate_ids  # Which gates they can access
        self.valid_until = valid_until
        self.created_by = created_by
        self.active = True
        self.created_at = datetime.utcnow()

class VisitorPass:
    def __init__(self, id: str, created_by: str, visitor_name: str, visitor_phone: str, 
                 gate_ids: List[str], valid_until: datetime, host_company_id: str):
        self.id = id
        self.created_by = created_by  # Employee who created the pass
        self.visitor_name = visitor_name
        self.visitor_phone = visitor_phone
        self.gate_ids = gate_ids
        self.valid_until = valid_until
        self.host_company_id = host_company_id
        self.active = True
        self.created_at = datetime.utcnow()
        self.used_count = 0
        self.max_uses = 5  # Default limit
        
class TokenRequest:
    def __init__(self, gate_id: str, reader_nonce: str = "", device_id: str = ""):
        self.gate_id = gate_id
        self.reader_nonce = reader_nonce
        self.device_id = device_id

class VerifyRequest:
    def __init__(self, reader_id: str, gate_id: str, token: str, door_opened: bool = False):
        self.reader_id = reader_id
        self.gate_id = gate_id
        self.token = token
        self.door_opened = door_opened  # Reader reports if door actually opened
