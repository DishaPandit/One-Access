# One-Access Backend (MVP)

Flask backend for an NFC (ISO-DEP) HCE access system:

- Issues **short-lived signed tokens** for a given `gateId`
- Verifies tokens for a reader / gate
- Enforces policy: **all employees** can access `MAIN_GATE`; **only same-company** can access building gates

## Prereqs

- Python 3.12+ (you have Python 3.14)

## Run locally

```bash
cd backend
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
set FLASK_APP=app.main
python -m flask run --port 8000
```

Open API docs: `http://127.0.0.1:8000/docs`

Note: MVP uses Flask (no Swagger UI). You can hit endpoints with curl/Postman.

## Smoke test

With the server running, in another PowerShell terminal:

```powershell
cd backend
.\scripts\smoke_test.ps1
```

## Quick test (no Android required)

1) Login as a demo user:

```bash
curl -X POST http://127.0.0.1:8000/auth/login -H "Content-Type: application/json" ^
  -d "{\"email\":\"alice@acme.com\"}"
```

2) Use returned `accessToken` to request an NFC token for a gate:

```bash
curl -X POST http://127.0.0.1:8000/hce/token -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <ACCESS_TOKEN>" ^
  -d "{\"gateId\":\"MAIN_GATE\",\"readerNonce\":\"0123456789ABCDEF\"}"
```

3) Simulate reader verification:

```bash
curl -X POST http://127.0.0.1:8000/access/verify -H "Content-Type: application/json" ^
  -d "{\"readerId\":\"READER_1\",\"gateId\":\"MAIN_GATE\",\"token\":\"<TOKEN_FROM_STEP_2>\"}"
```

