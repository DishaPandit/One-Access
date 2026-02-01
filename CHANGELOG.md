# Changelog

All notable changes to OneAccess will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-01

### 🎉 Initial Release

#### Added
- **NFC Card Emulation (HCE)** - Phone acts as access card
- **QR Code Access** - Generate and scan QR codes for entry
- **Time Tracking System** - Automatic session tracking with live duration
- **Delegation Management** - Temporary access sharing
- **Visitor Pass System** - Time-limited guest access
- **JWT Authentication** - Secure token-based auth
- **Material 3 UI** - Modern, clean interface with Jetpack Compose
- **Backend API** - Flask-based REST API
- **Cloud Deployment** - Ready for Render.com deployment

#### Features
- Real-time session duration updates (1-second precision)
- Auto-refreshing QR codes (20-second expiry)
- Session history and statistics
- Multi-gate support
- Device ID tracking
- Access audit logging

#### Documentation
- Comprehensive README with features, setup, and usage
- Full API documentation
- Contributing guidelines
- Screenshots placeholders

---

## [Unreleased]

### Planned
- [ ] PostgreSQL database integration
- [ ] Redis caching layer
- [ ] Push notifications
- [ ] Biometric authentication
- [ ] Web admin dashboard
- [ ] iOS app
- [ ] Bluetooth fallback
- [ ] Offline mode
- [ ] Multi-language support
- [ ] Dark theme

---

## Version History

- **1.0.0** (2026-02-01) - Initial release

---

[Unreleased]: https://github.com/yourusername/oneaccess/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yourusername/oneaccess/releases/tag/v1.0.0
