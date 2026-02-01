# Contributing to OneAccess

First off, thank you for considering contributing to OneAccess! 🎉

## Code of Conduct

This project and everyone participating in it is governed by our commitment to creating a welcoming and inclusive environment. Be respectful, be collaborative, and be excellent to each other.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the behavior
- **Expected behavior**
- **Actual behavior**
- **Screenshots** if applicable
- **Environment details** (OS, Android version, Python version)

### Suggesting Features

Feature suggestions are welcome! Please provide:

- **Clear use case** - Why is this feature needed?
- **Proposed solution** - How should it work?
- **Alternatives considered** - What other approaches did you think about?

### Pull Requests

1. **Fork** the repo and create your branch from `main`
2. **Make your changes** with clear, descriptive commits
3. **Test** your changes thoroughly
4. **Update documentation** if needed
5. **Submit** a pull request

#### Pull Request Guidelines

- Follow the existing code style
- Write meaningful commit messages
- Add tests for new features
- Update README.md if needed
- Keep PRs focused - one feature/fix per PR

## Development Setup

### Backend Development

```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python -m flask run --debug
```

### Android Development

```bash
cd android
./gradlew clean assembleDebug
./gradlew test
```

## Code Style

### Python
- Follow PEP 8
- Use type hints where applicable
- Write docstrings for functions/classes
- Keep functions focused and small

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Prefer immutability (`val` over `var`)
- Write KDoc for public APIs

## Testing

### Backend Tests
```bash
cd backend
pytest
```

### Android Tests
```bash
cd android
./gradlew test
./gradlew connectedAndroidTest
```

## Commit Messages

Write clear, descriptive commit messages:

```
Add user profile screen

- Created ProfileScreen composable
- Integrated with user API
- Added profile image upload
- Includes unit tests
```

Format: `<type>: <subject>`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance tasks

## Questions?

Feel free to open a discussion or reach out to the maintainers!

Thank you for contributing! 🚀
