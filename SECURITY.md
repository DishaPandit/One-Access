# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of OneAccess seriously. If you discover a security vulnerability, please follow these steps:

### 1. **DO NOT** Open a Public Issue

Please do not disclose the vulnerability publicly until it has been addressed.

### 2. Report Privately

Send details to: **security@oneaccess.example.com** (or open a private security advisory on GitHub)

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### 3. What to Expect

- **Acknowledgment:** Within 48 hours
- **Initial Assessment:** Within 1 week
- **Status Updates:** Every 2 weeks
- **Fix Timeline:** 30-90 days depending on severity

### 4. Disclosure Policy

Once fixed:
1. We'll release a patch
2. Credit will be given (if desired)
3. Public disclosure after users have time to update (typically 2 weeks)

## Security Best Practices

### For Users

1. **Keep Updated** - Always use the latest version
2. **Secure Tokens** - Don't share JWT tokens
3. **Backend URL** - Use HTTPS in production
4. **Device Security** - Use screen lock and encryption

### For Developers

1. **Dependencies** - Regularly update dependencies
2. **Secrets** - Never commit secrets to git
3. **Input Validation** - Validate all user inputs
4. **Auth Tokens** - Use short expiry times
5. **HTTPS Only** - Always use TLS in production

## Known Security Considerations

### Current Implementation

- **In-Memory Storage:** Data is not persisted (suitable for demo/development)
- **Demo Authentication:** Simplified auth for testing (production should use real auth)
- **Token Expiry:** Default 20 seconds (configurable)
- **No Rate Limiting:** Should be implemented for production

### Production Recommendations

1. **Database:** Use PostgreSQL with encrypted connections
2. **Secrets Management:** Use environment variables or secret managers
3. **Rate Limiting:** Implement per-user and per-IP limits
4. **Logging:** Enable comprehensive audit logging
5. **HTTPS:** Enforce TLS 1.3+ for all communications
6. **Token Rotation:** Implement refresh tokens
7. **MFA:** Add multi-factor authentication
8. **Biometrics:** Require fingerprint/face before access

## Security Checklist for Production

- [ ] Use real authentication provider (OAuth2, SAML, etc.)
- [ ] Enable HTTPS with valid certificates
- [ ] Implement rate limiting
- [ ] Use persistent database with backups
- [ ] Enable audit logging
- [ ] Set up monitoring and alerts
- [ ] Implement token refresh mechanism
- [ ] Add biometric authentication
- [ ] Use secret management system
- [ ] Regular security audits
- [ ] Penetration testing
- [ ] Dependency scanning (Dependabot, Snyk)

## Cryptography

- **JWT Signing:** HMAC-SHA256
- **Token Transport:** HTTPS required
- **Secret Storage:** Environment variables (production should use KMS)

## Third-Party Dependencies

We regularly monitor and update dependencies. Known vulnerabilities are addressed promptly.

Run security scans:

```bash
# Python
pip install safety
safety check -r backend/requirements.txt

# Android
./gradlew dependencyCheckAnalyze
```

## Contact

For security concerns: **security@oneaccess.example.com**

For general questions: Open a discussion on GitHub

---

**Thank you for helping keep OneAccess secure!** 🔒
