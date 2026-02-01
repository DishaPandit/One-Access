# OneAccess API Documentation

Complete API reference for the OneAccess backend.

**Base URL:** `https://oneaccess-backend.onrender.com` (Production)  
**Version:** 1.0  
**Authentication:** JWT Bearer Token

---

## Table of Contents

- [Authentication](#authentication)
- [Access Control](#access-control)
- [Time Tracking](#time-tracking)
- [Delegation](#delegation)
- [Visitor Management](#visitor-management)
- [Error Codes](#error-codes)

---

## Authentication

### Login

Authenticate a user and receive an access token.

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "email": "alice@acme.com"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "userId": "USR_ALICE",
  "companyId": "ACME",
  "email": "alice@acme.com"
}
```

**Errors:**
- `400 Bad Request` - Missing or invalid email
- `404 Not Found` - User not found

---

## Access Control

### Issue QR Token

Generate a short-lived access token for QR code entry.

**Endpoint:** `POST /qr/token`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "gateId": "BLD_ACME",
  "readerNonce": "READER_NONCE_123",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "expEpochSeconds": 1707890123
}
```

**Token Claims:**
```json
{
  "sub": "USR_ALICE",
  "gateId": "BLD_ACME",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1707890100,
  "exp": 1707890123
}
```

**Errors:**
- `401 Unauthorized` - Invalid or expired access token
- `403 Forbidden` - No access to specified gate
- `400 Bad Request` - Missing required fields

---

### Verify Access

Verify an access token at a door reader.

**Endpoint:** `POST /access/verify`

**Request:**
```json
{
  "readerId": "FRONT_DOOR",
  "gateId": "BLD_ACME",
  "token": "eyJ0eXAiOiJKV1Qi...",
  "doorOpened": true,
  "direction": "ENTRY"
}
```

**Parameters:**
- `readerId` (string, required) - Physical reader identifier
- `gateId` (string, required) - Gate/door identifier
- `token` (string, required) - Access token (JWT)
- `doorOpened` (boolean, optional) - Whether door actually opened
- `direction` (string, optional) - "ENTRY" or "EXIT" for buildings

**Response:** `200 OK`
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_STARTED",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**With EXIT:**
```json
{
  "decision": "ALLOW",
  "reason": "OK",
  "timeTracking": {
    "action": "SESSION_ENDED",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "entryTime": "2026-02-01T09:30:00",
    "exitTime": "2026-02-01T17:30:00",
    "durationSeconds": 28800,
    "durationFormatted": "8h 0m 0s"
  }
}
```

**Errors:**
- `400 Bad Request` - Invalid request format
- `401 Unauthorized` - Invalid token
- `403 Forbidden` - Access denied

---

## Time Tracking

### Get Current Session

Retrieve the user's active time tracking session.

**Endpoint:** `GET /time/current`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "entryTime": "2026-02-01T09:30:00",
  "currentDurationSeconds": 3600,
  "currentDurationFormatted": "1h 00m 00s",
  "gateIdEntry": "BLD_ACME"
}
```

**No Active Session:**
```json
{
  "hasActiveSession": false
}
```

**Errors:**
- `401 Unauthorized` - Invalid access token

---

### Get Session History

Retrieve all time tracking sessions for the user.

**Endpoint:** `GET /time/sessions`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "gateIdEntry": "BLD_ACME",
      "entryTime": "2026-02-01T09:30:00",
      "exitTime": "2026-02-01T17:30:00",
      "durationSeconds": 28800,
      "durationFormatted": "8h 0m 0s",
      "status": "COMPLETED"
    },
    {
      "sessionId": "650e8400-e29b-41d4-a716-446655440001",
      "gateIdEntry": "BLD_ACME",
      "entryTime": "2026-01-31T09:00:00",
      "exitTime": "2026-01-31T18:15:00",
      "durationSeconds": 33300,
      "durationFormatted": "9h 15m 0s",
      "status": "COMPLETED"
    }
  ],
  "total": 2
}
```

---

### Get Time Summary

Retrieve aggregated time tracking statistics.

**Endpoint:** `GET /time/summary`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "totalSessions": 45,
  "totalTimeFormatted": "360h 30m 15s",
  "averageTimeFormatted": "8h 00m 40s",
  "todaySessions": 1,
  "todayTimeFormatted": "3h 45m 20s",
  "hasActiveSession": true
}
```

---

## Delegation

### Create Delegation

Grant temporary access to another user.

**Endpoint:** `POST /delegation/create`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "recipientEmail": "bob@globex.com",
  "gateId": "BLD_ACME",
  "startsAt": "2026-02-05T09:00:00",
  "endsAt": "2026-02-05T18:00:00"
}
```

**Response:** `200 OK`
```json
{
  "delegationId": "DEL_123",
  "grantor": "alice@acme.com",
  "recipient": "bob@globex.com",
  "gateId": "BLD_ACME",
  "startsAt": "2026-02-05T09:00:00",
  "endsAt": "2026-02-05T18:00:00",
  "status": "ACTIVE"
}
```

**Errors:**
- `400 Bad Request` - Invalid dates or missing fields
- `404 Not Found` - Recipient user not found
- `403 Forbidden` - Grantor doesn't have access to gate

---

### List Delegations

Get all delegations (granted and received).

**Endpoint:** `GET /delegation/list`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "granted": [
    {
      "delegationId": "DEL_123",
      "recipient": "bob@globex.com",
      "gateId": "BLD_ACME",
      "startsAt": "2026-02-05T09:00:00",
      "endsAt": "2026-02-05T18:00:00",
      "status": "ACTIVE"
    }
  ],
  "received": []
}
```

---

### Revoke Delegation

Cancel a delegation before it expires.

**Endpoint:** `POST /delegation/revoke`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "delegationId": "DEL_123"
}
```

**Response:** `200 OK`
```json
{
  "message": "Delegation revoked",
  "delegationId": "DEL_123"
}
```

---

## Visitor Management

### Create Visitor Pass

Generate a temporary access pass for a visitor.

**Endpoint:** `POST /visitor/create`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "visitorName": "John Doe",
  "gateId": "BLD_ACME",
  "expiresAt": "2026-02-01T18:00:00",
  "maxUses": 2
}
```

**Response:** `200 OK`
```json
{
  "passId": "PASS_789",
  "visitorName": "John Doe",
  "gateId": "BLD_ACME",
  "createdBy": "alice@acme.com",
  "expiresAt": "2026-02-01T18:00:00",
  "maxUses": 2,
  "remainingUses": 2,
  "accessCode": "VP-BLD-ACME-789ABC"
}
```

**Errors:**
- `400 Bad Request` - Invalid expiry or missing fields
- `403 Forbidden` - No permission to create passes for gate

---

### List Visitor Passes

Get all visitor passes created by the user.

**Endpoint:** `GET /visitor/list`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "passes": [
    {
      "passId": "PASS_789",
      "visitorName": "John Doe",
      "gateId": "BLD_ACME",
      "expiresAt": "2026-02-01T18:00:00",
      "remainingUses": 1,
      "status": "ACTIVE"
    }
  ]
}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| `200` | Success |
| `400` | Bad Request - Invalid parameters |
| `401` | Unauthorized - Invalid/expired token |
| `403` | Forbidden - No permission |
| `404` | Not Found - Resource doesn't exist |
| `500` | Internal Server Error |

**Error Response Format:**
```json
{
  "error": "Description of the error"
}
```

---

## Rate Limiting

No rate limiting currently implemented. Production deployments should implement rate limiting based on IP address or user ID.

**Recommended:**
- 100 requests per minute per user
- 1000 requests per hour per IP

---

## Webhooks (Future)

Planned webhook support for:
- Access events (entry/exit)
- Session completion
- Delegation creation/revocation
- Visitor pass usage

---

## SDK Support

Official SDKs:
- ✅ Android (Kotlin) - Built-in
- 🔜 Python - Coming soon
- 🔜 JavaScript/TypeScript - Coming soon
- 🔜 Go - Coming soon

---

For questions or support, please open an issue on [GitHub](https://github.com/yourusername/oneaccess/issues).
