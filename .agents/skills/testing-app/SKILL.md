# Testing the Spring Boot RealWorld App

## Prerequisites
- Java 17 installed
- No additional secrets required for local dev (dev-only defaults are baked in)

## Starting the App
```bash
# Default (no env vars needed)
./gradlew bootRun
# App starts on http://localhost:8080

# With custom JWT secret
JWT_SECRET="your-secret-here" ./gradlew bootRun
```

- Wait for `Started RealWorldApplication` in logs before making requests
- If port 8080 is already in use: `fuser -k 8080/tcp`
- The app uses SQLite (`dev.db`). Delete it to start fresh: `rm -f dev.db`

## Key API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/users` | No | Register user |
| POST | `/users/login` | No | Login |
| GET | `/user` | Token | Get current user |
| PUT | `/user` | Token | Update profile |
| GET | `/tags` | No | List tags (health check) |
| GET | `/articles` | No | List articles |
| POST | `/articles` | Token | Create article |

## Auth Token Format
- Header: `Authorization: Token <jwt>`
- Registration and login responses include `user.token` field
- Example: `curl -H "Authorization: Token eyJ..." http://localhost:8080/user`

## Request Body Format
The API uses root-wrapped JSON (Jackson UNWRAP_ROOT_VALUE):
```json
{"user": {"username": "test", "email": "test@example.com", "password": "pass123"}}
```

## Testing JWT Secret Externalization
To verify `JWT_SECRET` env var is actually being read:
1. Start app WITHOUT `JWT_SECRET` → register user → save token
2. Stop app, restart WITH a different `JWT_SECRET`
3. The old token should be rejected (HTTP 401) — this proves the env var is read
4. A new token from the custom-secret app should work (HTTP 200)

## Running Unit Tests
```bash
./gradlew test
```
- 2 pre-existing failures in `Java17PerformanceBenchmarkTest` (startup time & GC assertions) are flaky and unrelated to functional changes
- Test profile uses `application-test.properties` with its own JWT secret default

## Code Formatting
```bash
./gradlew spotlessJavaCheck   # Check
./gradlew spotlessJavaApply   # Auto-fix
```

## Devin Secrets Needed
- No secrets required for local development and testing
- `JWT_SECRET` env var is optional locally (dev-only default is used)
