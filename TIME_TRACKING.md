# Time Tracking Feature

Track how long employees spend inside building premises by recording entry and exit events.

## Overview

The time tracking feature automatically:
- **Starts a session** when someone taps IN at a building gate
- **Ends the session** when they tap OUT
- **Calculates total duration** spent inside
- **Maintains history** of all sessions
- **Provides statistics** (total time, average time, today's time, etc.)

## How It Works

### 1. Entry Event
When a person successfully accesses a BUILDING gate with `direction=ENTRY`:
- A new time session is created
- Entry time is recorded
- Session status = `ACTIVE`

### 2. Exit Event
When the same person successfully accesses a BUILDING gate with `direction=EXIT`:
- The active session is completed
- Exit time is recorded
- Duration is calculated
- Session status = `COMPLETED`

### 3. Edge Cases
- **Multiple entries without exit**: New entry auto-completes the previous session
- **MAIN_GATE access**: Not tracked (only BUILDING gates track time)
- **Failed access**: Does not affect time tracking

## API Endpoints

### Get Current Active Session
```http
GET /time/current
Authorization: Bearer <accessToken>
```

**Response (with active session):**
```json
{
  "activeSession": {
    "sessionId": "SES_ABC123DEF456",
    "userId": "U_ALICE",
    "companyId": "ACME",
    "gateIdEntry": "BLD_ACME",
    "entryTime": "2026-02-01T10:30:00.123456",
    "currentDurationSeconds": 3725,
    "currentDurationFormatted": "1h 2m 5s",
    "status": "ACTIVE"
  }
}
```

**Response (no active session):**
```json
{
  "activeSession": null
}
```

### Get Session History
```http
GET /time/sessions?limit=50
Authorization: Bearer <accessToken>
```

**Response:**
```json
{
  "sessions": [
    {
      "sessionId": "SES_ABC123DEF456",
      "userId": "U_ALICE",
      "companyId": "ACME",
      "gateIdEntry": "BLD_ACME",
      "entryTime": "2026-02-01T08:30:00.123456",
      "exitTime": "2026-02-01T17:45:00.123456",
      "durationSeconds": 33300,
      "durationFormatted": "9h 15m 0s",
      "status": "COMPLETED"
    }
  ]
}
```

### Get Time Summary Statistics
```http
GET /time/summary
Authorization: Bearer <accessToken>
```

**Response:**
```json
{
  "summary": {
    "totalSessions": 45,
    "totalTimeSeconds": 1350000,
    "totalTimeFormatted": "375h 0m 0s",
    "averageTimeSeconds": 30000,
    "averageTimeFormatted": "8h 20m 0s",
    "todaySessions": 1,
    "todayTimeSeconds": 33300,
    "todayTimeFormatted": "9h 15m 0s",
    "hasActiveSession": false
  }
}
```

### Verify Access with Direction
```http
POST /access/verify
Content-Type: application/json

{
  "readerId": "READER_1",
  "gateId": "BLD_ACME",
  "token": "eyJhbGc...",
  "doorOpened": true,
  "direction": "ENTRY"  // or "EXIT"
}
```

**Response (with time tracking):**
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_STARTED",
    "sessionId": "SES_ABC123DEF456"
  }
}
```

**Response (exit with time tracking):**
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_ENDED",
    "sessionId": "SES_ABC123DEF456",
    "entryTime": "2026-02-01T10:30:00.123456",
    "exitTime": "2026-02-01T17:45:00.123456",
    "durationSeconds": 26100,
    "durationFormatted": "7h 15m 0s"
  }
}
```

## Data Model

### TimeSession
```python
@dataclass
class TimeSession:
    session_id: str                    # Unique session ID (SES_...)
    user_id: str                       # User who entered
    company_id: str                    # User's company
    gate_id_entry: str                 # Gate used for entry
    entry_time: datetime               # When they entered
    exit_time: datetime | None         # When they exited (None if active)
    duration_seconds: int | None       # Total time inside (None if active)
    status: str                        # "ACTIVE" or "COMPLETED"
```

## Testing

### Backend Test Script
```powershell
cd backend

# Start the backend first
$env:FLASK_APP='app.main:app'
python -m flask run --host 127.0.0.1 --port 8000

# In another terminal, run the test
.\scripts\test_time_tracking.ps1
```

The test script:
1. Logs in as Alice
2. Simulates ENTRY tap at BLD_ACME
3. Waits 5 seconds
4. Checks active session
5. Simulates EXIT tap
6. Verifies session ended
7. Shows session history and statistics

### Manual Testing with curl

**1. Login:**
```powershell
curl -X POST http://127.0.0.1:8000/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"alice@acme.com"}'
```

**2. Get token for ENTRY:**
```powershell
curl -X POST http://127.0.0.1:8000/qr/token `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <TOKEN>" `
  -d '{"gateId":"BLD_ACME","readerNonce":"ENTRY123","deviceId":"D1"}'
```

**3. Verify ENTRY:**
```powershell
curl -X POST http://127.0.0.1:8000/access/verify `
  -H "Content-Type: application/json" `
  -d '{"readerId":"R1","gateId":"BLD_ACME","token":"<JWT>","doorOpened":true,"direction":"ENTRY"}'
```

**4. Check current session:**
```powershell
curl http://127.0.0.1:8000/time/current `
  -H "Authorization: Bearer <TOKEN>"
```

**5. Simulate EXIT (after some time):**
```powershell
# Get new token
curl -X POST http://127.0.0.1:8000/qr/token `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <TOKEN>" `
  -d '{"gateId":"BLD_ACME","readerNonce":"EXIT123","deviceId":"D1"}'

# Verify EXIT
curl -X POST http://127.0.0.1:8000/access/verify `
  -H "Content-Type: application/json" `
  -d '{"readerId":"R1","gateId":"BLD_ACME","token":"<JWT>","doorOpened":true,"direction":"EXIT"}'
```

**6. View statistics:**
```powershell
curl http://127.0.0.1:8000/time/summary `
  -H "Authorization: Bearer <TOKEN>"
```

## Android App

The Android app includes a dedicated **Time Tracking** tab that shows:

### Features:
1. **Current Session Card** (real-time)
   - Shows if user is currently inside or outside
   - Entry gate and entry time
   - Live duration counter (updates every 5 seconds)
   - Highlighted when active

2. **Statistics Card**
   - Today's sessions and total time
   - All-time sessions, total time, and average time
   - Summary of time spent in building

3. **Auto-Refresh**
   - Updates every 5 seconds automatically
   - No manual refresh needed
   - Shows live duration while inside

### UI Location:
- Navigate to **"Time"** tab in the app
- Sign in to view your time tracking data
- Data persists across app sessions

## Use Cases

### 1. Work Hours Tracking
Track employee working hours automatically:
- Entry when arriving at office
- Exit when leaving
- Daily/weekly/monthly reports

### 2. Building Occupancy
Monitor who's currently inside:
- Query active sessions
- Count current occupants
- Emergency evacuation lists

### 3. Compliance & Auditing
Maintain records for compliance:
- Entry/exit timestamps
- Duration calculations
- Historical session data

### 4. Access Analytics
Analyze building usage patterns:
- Peak entry/exit times
- Average time spent
- Frequency of visits

## Notes

- **Automatic**: No manual action needed beyond normal tap-in/tap-out
- **Privacy**: Only tracks building access time, not location inside
- **Accurate**: Uses server timestamps to avoid client-side manipulation
- **Reliable**: Handles edge cases (multiple entries, missing exits)
- **Scalable**: In-memory for MVP, easily migrates to database

## Future Enhancements

Potential improvements:
- [ ] Export session history to CSV
- [ ] Weekly/monthly time reports
- [ ] Overtime alerts
- [ ] Multi-building tracking (track which building)
- [ ] Break time detection (short exits)
- [ ] Admin dashboard for all employees
- [ ] Notifications (reminder to tap out)
- [ ] Integration with HR/payroll systems
