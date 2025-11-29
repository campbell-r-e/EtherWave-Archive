# Registration & Login Guide

## ✅ Registration Successfully Tested!

Both registration and login are working correctly in the production environment.

---

## 🌐 Method 1: Web Interface (Recommended)

### Step-by-Step Registration

1. **Open the application in your browser:**
   ```
   http://localhost
   ```

2. **Click "Register here"** on the login page

3. **Fill in the registration form:**
   - **Username** (required): 3-50 characters, letters, numbers, underscores, hyphens
   - **Email** (required): Valid email address
   - **Password** (required): Minimum 8 characters
   - **Full Name** (optional): Your full name
   - **Callsign** (optional): Your amateur radio callsign (uppercase, e.g., W1ABC, K2XYZ)
   - **Grid Square** (optional): Your Maidenhead grid square (e.g., FN20)
   - **QRZ API Key** (optional): Your QRZ.com API key for callsign lookups

4. **Click "Register"**

5. **You're automatically logged in!** The system will:
   - Create your account
   - Generate a JWT authentication token
   - Redirect you to the main application
   - Assign you the ROLE_USER role

---

## 🔧 Method 2: API Testing (For Developers)

### Registration via API

**Endpoint:** `POST http://localhost:8080/api/auth/register`

**Minimal Registration (username, email, password only):**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "email": "your@email.com",
    "password": "YourPassword123"
  }'
```

**Full Registration (with all optional fields):**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "hamoperator",
    "email": "operator@hamradio.com",
    "password": "SecurePass123",
    "fullName": "John Operator",
    "callsign": "W1ABC",
    "gridSquare": "FN20xa",
    "qrzApiKey": "your-qrz-api-key"
  }'
```

**Successful Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 2,
  "username": "hamoperator",
  "email": "operator@hamradio.com",
  "callsign": "W1ABC",
  "fullName": "John Operator",
  "roles": ["ROLE_USER"]
}
```

**Error Responses:**
```json
// Username already taken
{"message": "Username is already taken"}

// Email already registered
{"message": "Email is already registered"}

// Callsign already registered
{"message": "Callsign is already registered"}

// Validation error
{
  "timestamp": "2025-11-29T21:19:04.873+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/auth/register"
}
```

---

### Login via API

**Endpoint:** `POST http://localhost:8080/api/auth/login`

**Login with Username:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "hamoperator",
    "password": "SecurePass123"
  }'
```

**Login with Email:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "operator@hamradio.com",
    "password": "SecurePass123"
  }'
```

**Successful Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 2,
  "username": "hamoperator",
  "email": "operator@hamradio.com",
  "callsign": "W1ABC",
  "fullName": "John Operator",
  "roles": ["ROLE_USER"]
}
```

---

## 📋 Field Requirements

### Username
- **Required**: Yes
- **Min Length**: 3 characters
- **Max Length**: 50 characters
- **Pattern**: Letters, numbers, underscores, hyphens only (`^[a-zA-Z0-9_-]+$`)
- **Examples**: `john_doe`, `ham-operator`, `W1ABC`

### Email
- **Required**: Yes
- **Format**: Valid email address
- **Max Length**: 100 characters
- **Examples**: `user@example.com`, `operator@hamradio.org`

### Password
- **Required**: Yes
- **Min Length**: 8 characters
- **Max Length**: 100 characters
- **Recommendations**: Use strong passwords with mixed case, numbers, and symbols
- **Examples**: `SecurePass123`, `MyP@ssw0rd!`

### Callsign (Optional)
- **Required**: No
- **Format**: Uppercase letters, numbers, and slashes only
- **Max Length**: 20 characters
- **Pattern**: `^[A-Z0-9/]+$`
- **Examples**: `W1ABC`, `K2XYZ`, `G4ABC/M`, `VE3/W1ABC`

### Full Name (Optional)
- **Required**: No
- **Max Length**: 100 characters
- **Examples**: `John Doe`, `Jane Operator`

### Grid Square (Optional)
- **Required**: No
- **Max Length**: 10 characters
- **Examples**: `FN20`, `FN20xa`, `JN58td`

### QRZ API Key (Optional)
- **Required**: No
- **Max Length**: 500 characters
- **Note**: Get your API key from https://www.qrz.com/

---

## 🔐 Using the JWT Token

After successful login/registration, use the token in API requests:

```bash
# Get your logs
curl -X GET http://localhost:8080/api/logs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"

# Create a new log
curl -X POST http://localhost:8080/api/logs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Field Day 2025",
    "description": "ARRL Field Day Contest",
    "contestCode": "ARRL-FD"
  }'
```

---

## 📊 Current Users in Database

To check registered users (for testing):

```bash
docker exec hamradio-postgres psql -U hamradio -d hamradio_logbook \
  -c "SELECT id, username, email, callsign FROM users;"
```

Current users:
```
 id |  username   |      email       | callsign
----+-------------+------------------+----------
  1 | testuser    | test@example.com | W1ABC
  2 | newoperator | new@hamradio.com |
```

---

## 🧪 Testing Registration Flow

### Test Case 1: Basic Registration
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testop1",
    "email": "testop1@hamradio.com",
    "password": "TestPass123"
  }'
```

### Test Case 2: Registration with Callsign
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testop2",
    "email": "testop2@hamradio.com",
    "password": "TestPass123",
    "callsign": "K2XYZ",
    "fullName": "Test Operator 2"
  }'
```

### Test Case 3: Login After Registration
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testop1",
    "password": "TestPass123"
  }'
```

### Test Case 4: Duplicate Username (Should Fail)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testop1",
    "email": "different@email.com",
    "password": "TestPass123"
  }'

# Expected: {"message": "Username is already taken"}
```

---

## ⚠️ Common Issues

### 1. JSON Parse Error
**Error:** `Unrecognized character escape '!' (code 33)`
**Cause:** Special characters in JSON not properly escaped
**Solution:** Use simple passwords for testing or escape special chars properly

### 2. 400 Bad Request
**Possible Causes:**
- Missing required fields (username, email, password)
- Username too short (< 3 chars) or too long (> 50 chars)
- Invalid email format
- Password too short (< 8 chars)
- Invalid callsign format (must be uppercase)

### 3. Username Already Taken
**Solution:** Choose a different username or delete the existing user from database

### 4. Email Already Registered
**Solution:** Use a different email address

---

## 🎯 Next Steps After Registration

1. **Login to the web interface** at http://localhost
2. **Create a logbook:**
   - Click "Create New Log"
   - Enter log details
   - Select contest type (optional)
3. **Start logging QSOs:**
   - Select your log
   - Click "Add QSO"
   - Enter contact details
4. **Explore features:**
   - QSO list and search
   - Map visualization
   - Export to ADIF/Cabrillo
   - Contest validation

---

## 📞 Support

**Web Interface:** http://localhost
**API Health:** http://localhost:8080/actuator/health
**Database:** PostgreSQL on localhost:5432

**View logs:**
```bash
docker-compose logs -f backend
```

---

**Happy logging! 73!** 📻
