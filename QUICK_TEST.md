# Quick Testing Guide - Time Tracking Feature

## Option 1: Automated Test (Recommended)

### Start Backend:
```powershell
cd backend
python -m pip install -r requirements.txt
$env:FLASK_APP='app.main:app'
python -m flask run --host 127.0.0.1 --port 8000
```

### Run Test (in new terminal):
```powershell
cd backend
.\scripts\test_time_tracking.ps1
```

**What the test does:**
- ✅ Login as Alice
- ✅ Simulate tap IN at BLD_ACME (entry)
- ✅ Check active session
- ✅ Wait 5 seconds
- ✅ Simulate tap OUT at BLD_ACME (exit)
- ✅ Verify session ended with duration
- ✅ Show statistics

---

## Option 2: Android App Test

### Prerequisites:
1. Backend running (see above)
2. Android Studio installed
3. Emulator or phone ready

### Steps:
1. Open `android/` folder in Android Studio
2. Run the app (emulator: use `http://10.0.2.2:8000`)
3. Sign in as `alice@acme.com`
4. Navigate to **"Time"** tab
5. You'll see "No active session" initially

**To test entry/exit:**
- Use manual API calls (see below) to simulate taps
- The Time tab will auto-update every 5 seconds
- Watch the duration counter increase

---

## Option 3: Manual API Calls

### 1. Login
```powershell
$login = (curl.exe -s -X POST http://127.0.0.1:8000/auth/login -H "Content-Type: application/json" -d '{"email":"alice@acme.com"}' | ConvertFrom-Json)
$token = $login.accessToken
```

### 2. Entry - Get Token
```powershell
$entry = (curl.exe -s -X POST http://127.0.0.1:8000/qr/token -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d '{"gateId":"BLD_ACME","readerNonce":"ENTRY123","deviceId":"D1"}' | ConvertFrom-Json)
```

### 3. Entry - Verify (starts session)
```powershell
curl.exe -X POST http://127.0.0.1:8000/access/verify -H "Content-Type: application/json" -d "{`"readerId`":`"R1`",`"gateId`":`"BLD_ACME`",`"token`":`"$($entry.token)`",`"doorOpened`":true,`"direction`":`"ENTRY`"}"
```

### 4. Check Active Session
```powershell
curl.exe http://127.0.0.1:8000/time/current -H "Authorization: Bearer $token"
```

### 5. Wait (simulate time inside)
```powershell
Start-Sleep -Seconds 10
```

### 6. Exit - Get Token
```powershell
$exit = (curl.exe -s -X POST http://127.0.0.1:8000/qr/token -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d '{"gateId":"BLD_ACME","readerNonce":"EXIT456","deviceId":"D1"}' | ConvertFrom-Json)
```

### 7. Exit - Verify (ends session)
```powershell
curl.exe -X POST http://127.0.0.1:8000/access/verify -H "Content-Type: application/json" -d "{`"readerId`":`"R1`",`"gateId`":`"BLD_ACME`",`"token`":`"$($exit.token)`",`"doorOpened`":true,`"direction`":`"EXIT`"}"
```

### 8. View Summary
```powershell
curl.exe http://127.0.0.1:8000/time/summary -H "Authorization: Bearer $token"
```

---

## Expected Results

### After ENTRY:
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_STARTED",
    "sessionId": "SES_XXXXXXXXXXXX"
  }
}
```

### After EXIT:
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_ENDED",
    "sessionId": "SES_XXXXXXXXXXXX",
    "entryTime": "2026-02-01T10:30:00.123456",
    "exitTime": "2026-02-01T10:30:10.123456",
    "durationSeconds": 10,
    "durationFormatted": "10s"
  }
}
```

---

## Demo Users

| Email | Company | Can Access |
|-------|---------|------------|
| alice@acme.com | ACME | MAIN_GATE, BLD_ACME |
| bob@globex.com | GLOBEX | MAIN_GATE, BLD_GLOBEX |

---

## Troubleshooting

### Backend not starting?
```powershell
# Check Python version
python --version  # Should be 3.12+

# Reinstall dependencies
cd backend
python -m pip install --upgrade -r requirements.txt
```

### Port 8000 already in use?
```powershell
# Change port
python -m flask run --host 127.0.0.1 --port 8001

# Update backend URL in app to http://10.0.2.2:8001
```

### Time tracking not working?
- ✓ Make sure `direction` is set to "ENTRY" or "EXIT"
- ✓ Only BUILDING gates track time (BLD_ACME, BLD_GLOBEX)
- ✓ MAIN_GATE does NOT track time
- ✓ Door must actually open (`doorOpened: true`)

---

## Next Steps

After testing, explore:
- View session history: `GET /time/sessions`
- Check all statistics: `GET /time/summary`
- See real-time updates in Android app's Time tab
- Test with different users (alice vs bob)
- Test cross-building access (should be blocked)

For full documentation, see `TIME_TRACKING.md`
