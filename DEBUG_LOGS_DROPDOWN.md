# Debug: Logs Dropdown Not Showing

## Quick Fix - Try These First

### 1. Hard Refresh Browser
```
Windows/Linux: Ctrl + Shift + R
Mac: Cmd + Shift + R
```

### 2. Clear Browser Storage
```
1. Open browser DevTools (F12)
2. Go to "Application" or "Storage" tab
3. Clear all: localStorage, sessionStorage, cookies
4. Refresh page
```

### 3. Login as Correct User
```
Username: freshuser
Password: FreshPass123
```

This user has 3 logbooks created.

---

## Detailed Debugging Steps

### Step 1: Open Browser Console

**Open Developer Tools:**
- Press F12 or Right-click → Inspect
- Go to "Console" tab

**Look for errors:**
- Any red errors?
- Any 401 Unauthorized errors?
- Any 404 Not Found errors?

---

### Step 2: Check Network Requests

**In DevTools:**
1. Go to "Network" tab
2. Refresh the page (F5)
3. Look for API calls

**Check these requests:**

**Login Request:**
```
POST http://localhost/api/auth/login
Status: Should be 200
Response: Should have "token"
```

**Logs Request:**
```
GET http://localhost/api/logs
Status: Should be 200
Response: Should have array of logs
```

**Common Issues:**

| Status | Problem | Solution |
|--------|---------|----------|
| 401 | Not authenticated | Login again |
| 403 | No permission | Check user has access |
| 404 | Wrong URL | Check environment config |
| CORS error | Cross-origin issue | Check CORS settings |

---

### Step 3: Check LocalStorage

**In DevTools:**
1. Go to "Application" → "Local Storage" → "http://localhost"
2. Look for these keys:

**Should see:**
- `auth-token`: JWT token (long string starting with "eyJ...")
- `auth-user`: User object (JSON)

**If missing:**
- Not logged in! Go to login page
- Login with: freshuser / FreshPass123

---

### Step 4: Manual API Test in Browser Console

**Paste this in browser console:**

```javascript
// Check if logged in
console.log('Token:', localStorage.getItem('auth-token'));

// Test API call
fetch('http://localhost/api/logs', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('auth-token')
  }
})
.then(r => r.json())
.then(data => console.log('Logs:', data))
.catch(err => console.error('Error:', err));
```

**Expected output:**
```json
Logs: [
  {id: 1, name: "My First Logbook", ...},
  {id: 2, name: "Field Day 2025", ...},
  {id: 3, name: "POTA Activations", ...}
]
```

---

### Step 5: Check Which User is Logged In

**In browser console:**
```javascript
const user = JSON.parse(localStorage.getItem('auth-user'));
console.log('Logged in as:', user.username);
```

**If user is NOT "freshuser":**
- That user might not have any logs!
- Logout and login as: freshuser / FreshPass123

---

## Common Problems & Solutions

### Problem 1: "No logs in dropdown"

**Solution:**
1. Check you're logged in as **freshuser**
2. Hard refresh (Ctrl+Shift+R)
3. Clear browser cache
4. Login again

### Problem 2: "401 Unauthorized when calling /api/logs"

**Solution:**
1. Token expired - login again
2. Token missing - check localStorage
3. Clear cache and login

### Problem 3: "CORS error"

**Solution:**
```bash
# Restart backend
docker-compose restart backend
```

### Problem 4: "404 Not Found"

**Solution:**
- Frontend calling wrong URL
- Check: Should call `/api/logs` not `http://localhost:8080/api/logs`
- Nginx proxy should handle it

### Problem 5: "Frontend shows old version"

**Solution:**
```bash
# Clear browser cache completely
# Or use Incognito/Private mode
```

---

## Test Users & Their Logs

| Username | Password | Logs Count |
|----------|----------|------------|
| freshuser | FreshPass123 | 3 logs |
| newoperator | (check DB) | 0 logs |
| testuser | (check DB) | 0 logs |

**Only freshuser has logs!**

---

## Verify Backend Directly

**Test API without browser:**

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"freshuser","password":"FreshPass123"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")

# Get logs
curl -s -X GET http://localhost:8080/api/logs \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**Should return 3 logs!**

---

## Quick Test Script

**Copy and paste in browser console after logging in:**

```javascript
(async () => {
  console.log('=== LOGS DROPDOWN DEBUG ===');

  // Check auth
  const token = localStorage.getItem('auth-token');
  const user = localStorage.getItem('auth-user');

  console.log('1. Authentication:');
  console.log('   Token exists:', !!token);
  console.log('   User:', user ? JSON.parse(user).username : 'NOT LOGGED IN');

  if (!token) {
    console.error('   ❌ NOT LOGGED IN! Please login first.');
    return;
  }

  // Test API
  console.log('\n2. Testing API:');
  try {
    const response = await fetch('/api/logs', {
      headers: { 'Authorization': 'Bearer ' + token }
    });

    console.log('   Status:', response.status, response.statusText);

    if (response.ok) {
      const logs = await response.json();
      console.log('   ✅ API works! Logs count:', logs.length);
      console.log('   Logs:', logs.map(l => l.name));

      if (logs.length === 0) {
        console.warn('   ⚠️  No logs for this user. Try freshuser/FreshPass123');
      }
    } else {
      console.error('   ❌ API error:', response.status);
    }
  } catch (err) {
    console.error('   ❌ Network error:', err);
  }

  console.log('\n=== END DEBUG ===');
})();
```

---

## Solution Steps

1. **Open http://localhost in browser**
2. **Clear all browser cache** (Ctrl+Shift+Del)
3. **Close and reopen browser** (or use Incognito)
4. **Login as freshuser / FreshPass123**
5. **Check dropdown for 3 logs**

---

## Still Not Working?

**Check browser console and send:**
1. Screenshot of Console tab (errors)
2. Screenshot of Network tab (API calls)
3. Output of the test script above

**Or try different browser:**
- Chrome Incognito
- Firefox Private
- Different browser entirely
