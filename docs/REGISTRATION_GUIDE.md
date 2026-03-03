# Registration & Login Guide

---

## Method 1: Web Interface (Recommended)

### Step-by-Step Registration

1. **Open the application in your browser:**
   ```
   http://localhost
   ```

2. **Click "Register here"** on the login page

3. **Fill in the registration form:**
   - **Username** (required): 3-50 characters, letters, numbers, underscores, hyphens
   - **Password** (required): Minimum 8 characters
   - **Callsign** (optional): Your amateur radio callsign (uppercase, e.g., W1ABC)
   - **Grid Square** (optional): Your Maidenhead grid square (e.g., FN20)

4. **Click "Register"**

5. **You're automatically logged in.** The system will:
   - Create your account
   - Generate a JWT authentication token (24-hour expiry)
   - Redirect you to the main application
   - Assign you the `ROLE_USER` role

**Note:** No email address is required or collected. Login is by username only.

---

## Method 2: API (For Developers)

### Register

**Endpoint:** `POST http://localhost:8080/api/auth/register`

**Minimal Registration:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "YourPassword123"
  }'
```

**Registration with optional fields:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "hamoperator",
    "password": "SecurePass123",
    "callsign": "W1ABC",
    "gridSquare": "FN20xa"
  }'
```

**Successful Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "hamoperator",
  "callsign": "W1ABC",
  "roles": ["ROLE_USER"]
}
```

**Error Responses:**
```json
// Username already taken
{"message": "Username is already taken"}

// Callsign already registered
{"message": "Callsign is already registered"}

// Validation error
{"status": 400, "error": "Bad Request"}
```

---

### Login

**Endpoint:** `POST http://localhost:8080/api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "hamoperator",
    "password": "SecurePass123"
  }'
```

**Successful Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "hamoperator",
  "callsign": "W1ABC",
  "roles": ["ROLE_USER"]
}
```

**Error Response:**
```json
{"status": 401, "error": "Unauthorized"}
```

---

## Field Requirements

### Username (Required)
- **Min Length**: 3 characters
- **Max Length**: 50 characters
- **Pattern**: Letters, numbers, underscores, hyphens only (`^[a-zA-Z0-9_-]+$`)
- **Examples**: `john_doe`, `ham-operator`, `W1ABC`

### Password (Required)
- **Min Length**: 8 characters
- **Storage**: BCrypt hashed — never stored in plaintext
- **Recommendations**: Use mixed case, numbers, and symbols

### Callsign (Optional)
- **Format**: Uppercase letters, numbers, and slashes only
- **Max Length**: 20 characters
- **Examples**: `W1ABC`, `K2XYZ`, `G4ABC/M`

### Grid Square (Optional)
- **Format**: Maidenhead locator
- **Max Length**: 10 characters
- **Examples**: `FN20`, `FN20xa`, `JN58td`

---

## Using the JWT Token

After login or registration, attach the token to all API requests:

```bash
curl -X GET http://localhost:8080/api/logs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

Tokens expire after 24 hours. Log in again to get a new token.

---

## Check Registered Users (Admin)

```bash
docker exec hamradio-postgres psql -U hamradio -d hamradio_logbook \
  -c "SELECT id, username, callsign FROM users;"
```

---

## Common Issues

### 400 Bad Request
- Missing required fields (username or password)
- Username too short (< 3 chars) or too long (> 50 chars)
- Password too short (< 8 chars)
- Invalid callsign format (must be uppercase alphanumeric)

### 409 Conflict
- Username already taken — choose a different username
- Callsign already registered — callsigns must be unique in the system

### JSON Parse Error
Special characters in passwords can break shell quoting. Use single quotes around the JSON body or escape characters properly.

---

## Next Steps After Registration

1. **Login to the web interface** at http://localhost
2. **Create a logbook:** Click "New Personal Log" or "New Shared Log"
3. **Start logging QSOs:** Select your log, click "Add QSO"
4. **Explore features:** Map visualization, contest validation, export, rig control

---

**Health Check:** `http://localhost:8080/actuator/health`

**View backend logs:**
```bash
docker compose logs -f backend
```
