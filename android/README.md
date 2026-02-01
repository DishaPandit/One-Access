# One-Access Android (MVP)

Android app (Kotlin + Jetpack Compose) that behaves like an NFC card using **Host Card Emulation (HCE)**.

It talks to the local backend in `../backend`:
- `POST /auth/login` (demo login)
- `POST /hce/token` (issues short-lived signed token)
- **Time tracking**: View real-time duration inside buildings with automatic entry/exit tracking

## Prereqs

- Android Studio (latest stable)
- An Android phone with NFC (HCE-capable)

## Open & run

1) Start backend:

```powershell
cd backend
python -m pip install -r requirements.txt
$env:FLASK_APP='app.main:app'
python -m flask run --host 127.0.0.1 --port 8000
```

2) Open `android/` in Android Studio (File → Open).

3) Run the app:
- **Emulator**: UI will run fine (NFC tap cannot be tested on emulator).
- **Phone** (later): required to test actual NFC tap/HCE.

If you hit Gradle sync issues, use the included wrapper:

```bash
# From the android/ directory
./gradlew tasks        # macOS/Linux
gradlew.bat tasks      # Windows
```

## What you can do today (without a phone)

- Run the backend + smoke test:

```powershell
cd backend
$env:FLASK_APP='app.main:app'
python -m flask run --host 127.0.0.1 --port 8000
```

In another terminal:

```powershell
cd backend
.\scripts\smoke_test.ps1
```

- Open `android/` in Android Studio and run on an emulator to validate:
  - UI (minimalist card + settings)
  - Login flow (talks to backend)
  - Token fetch behavior (from UI sign-in)

## Backend URL notes

- **Android Emulator → laptop backend**: keep default `http://10.0.2.2:8000`
- **Real phone → laptop backend** (later): set backend URL to `http://<your-laptop-lan-ip>:8000`
  - Example: `http://192.168.1.10:8000`
  - Ensure Windows Firewall allows inbound on port 8000 (private network)

## NFC reader protocol (MVP)

The app registers AID `F0010203040506` and responds to:
- **SELECT AID**
- **GET TOKEN** (INS `0xCA`)

Reader sends APDU:
- `00 CA 00 00 <Lc> <data>`

Where `<data>` is ASCII:
- `GATE_ID|READER_NONCE`

Example payload:
- `MAIN_GATE|0123456789ABCDEF`

App replies with:
- ASCII JWT token + status word `9000`

If there’s an error (not logged in / no network / backend denied), app replies with status word `6985`.

