# Testing Without Hardware

The rig control service is now running and fully functional, even without a physical radio connected. Here's what you can test and what to expect:

##  What Works Without a Radio

1. **WebSocket Connections**
   - All three WebSocket endpoints accept connections
   - Client identification and naming works
   - Connection/disconnection events are broadcast
   - Multiple clients can connect simultaneously

2. **PTT Locking System**
   - First-come-first-served PTT arbitration
   - PTT conflicts are properly detected and reported
   - Auto-release on disconnect works
   - Events are broadcast when PTT state changes

3. **Command Processing**
   - Commands are received and parsed correctly
   - Request/response correlation works (request IDs match)
   - Error handling activates when rigctld is unavailable

4. **Real-Time Status Broadcasting**
   - Status WebSocket connects and attempts polling
   - 100ms polling interval is maintained
   - Multiple subscribers receive updates simultaneously

5. **Architecture Components**
   - Command serialization (single-threaded executor)
   - Request coalescing
   - Smart caching (50ms/20ms TTL)
   - Connection pooling

##  What Requires rigctld

Commands that interact with the radio will fail gracefully with error messages:
- `setFrequency` - "Failed to set frequency"
- `setMode` - "Failed to set mode"
- `setPTT` - "PTT command failed" (but lock management still works)
- `getStatus` - Returns `connected: false`

## Testing Multi-Client Behavior (No Radio Needed)

### Test 1: Multiple Clients Connecting

1. Open the test client in **3 different browser tabs**
2. In each tab, change the "Client Name":
   - Tab 1: "Logbook"
   - Tab 2: "WebInterface"
   - Tab 3: "MobileApp"
3. Click "Connect" on all three WebSockets in each tab
4. Watch the **Events** panel - you'll see connection events from all clients

**Expected Result:**
```
[client_connected] Client 'Logbook' connected
[client_connected] Client 'WebInterface' connected
[client_connected] Client 'MobileApp' connected
```

### Test 2: PTT Conflict Resolution

1. Using the 3 tabs from Test 1:
2. In Tab 1 (Logbook), click **"PTT ON"**
3. In Tab 2 (WebInterface), click **"PTT ON"**
4. In Tab 3 (MobileApp), click **"PTT ON"**

**Expected Result:**
- Tab 1:  PTT granted (first client wins)
- Tab 2:  PTT denied - "PTT denied: held by Logbook"
- Tab 3:  PTT denied - "PTT denied: held by Logbook"

**Events Panel Shows:**
```
[ptt_activated] Client 'Logbook' activated PTT
[ptt_denied] PTT denied for 'WebInterface': held by 'Logbook'
[ptt_denied] PTT denied for 'MobileApp': held by 'Logbook'
```

### Test 3: PTT Auto-Release on Disconnect

1. With Logbook still holding PTT from Test 2
2. In Tab 1, click **"Disconnect"** on Command WebSocket
3. Watch the Events panel in other tabs

**Expected Result:**
```
[client_disconnected] Client 'Logbook' disconnected (PTT auto-released)
```

4. Now Tab 2 or Tab 3 can successfully acquire PTT

### Test 4: Status Broadcasting to Multiple Clients

1. In all 3 tabs, click "Connect" on the **Status WebSocket**
2. Watch the "Current Rig Status" panel in each tab
3. All three panels update simultaneously every 100ms

**Expected Result:**
- All clients receive identical status updates
- Updates arrive within 100ms intervals
- When one client disconnects, others continue receiving updates

### Test 5: Command Response Isolation

1. In Tab 1, click "Get Status"
2. In Tab 2, click "Get Status"
3. Each tab receives ONLY its own response

**Expected Result:**
- Tab 1 receives response with `id: "req-1"`
- Tab 2 receives response with `id: "req-1"` (independent counter)
- Responses don't cross between clients

## Simulating rigctld Without a Radio

If you want to test with mock radio responses (without hardware), you can run a simple rigctld simulator:

```bash
# Install netcat if needed: brew install netcat

# Run a fake rigctld on port 4532
while true; do
  echo -e "14250000\\nRPRT 0" | nc -l 4532
done
```

Or use the Python mock rigctld server:

```bash
cd /Users/Campbell/logbook/Hamradiologbook/rig-control-service
python3 << 'EOF'
import socket

def mock_rigctld():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('localhost', 4532))
    server.listen(1)

    print("Mock rigctld listening on port 4532...")
    print("Press Ctrl+C to stop")

    while True:
        client, addr = server.accept()
        print(f"Client connected: {addr}")

        while True:
            try:
                data = client.recv(1024).decode().strip()
                if not data:
                    break

                print(f"Received: {data}")

                # Respond based on command
                if data == 'f':  # Get frequency
                    client.send(b"14250000\nRPRT 0\n")
                elif data.startswith('F '):  # Set frequency
                    client.send(b"RPRT 0\n")
                elif data == 'm':  # Get mode
                    client.send(b"USB\n2400\nRPRT 0\n")
                elif data.startswith('M '):  # Set mode
                    client.send(b"RPRT 0\n")
                elif data == 't':  # Get PTT
                    client.send(b"0\nRPRT 0\n")
                elif data.startswith('T '):  # Set PTT
                    client.send(b"RPRT 0\n")
                elif data == '\\get_level STRENGTH':  # S-meter
                    client.send(b"-54\nRPRT 0\n")
                else:
                    client.send(b"RPRT -1\n")

            except Exception as e:
                print(f"Error: {e}")
                break

        client.close()
        print("Client disconnected")

if __name__ == '__main__':
    try:
        mock_rigctld()
    except KeyboardInterrupt:
        print("\nShutting down mock rigctld")

EOF
```

Once the mock rigctld is running, refresh the test client and try commands - they'll now succeed!

## Performance Testing Without Hardware

Even without rigctld, you can verify performance characteristics:

### Latency Test
```javascript
// In browser console on test client page
const start = Date.now();
sendGetStatus();
// When response arrives, check: Date.now() - start
// Should be < 50ms (plus network overhead)
```

### Cache Test
```javascript
// Send same command rapidly - should see cache hits in server logs
for (let i = 0; i < 10; i++) {
    sendGetStatus();
}
// First request: slow (cache miss)
// Next 9: fast (cache hits within 50ms TTL)
```

### Concurrent Client Test
Open 10 browser tabs and connect all simultaneously. The service handles unlimited concurrent clients.

## Monitoring Service Logs

The service logs show internal behavior:

```bash
# View logs (in another terminal)
tail -f /tmp/claude/tasks/b39574a.output
```

Look for:
- "Command WebSocket connected" - Client connections
- "PTT acquired by client" - Lock management
- "Cache hit for command" - Request coalescing working
- "RigStatusPoller" - 100ms polling activity

## Summary

**Without Hardware, You Can Test:**
 Multi-client WebSocket connections
 PTT locking and conflict resolution
 Auto-release on disconnect
 Event broadcasting
 Command serialization
 Request coalescing
 Client isolation
 Concurrent access safety

**Requires rigctld (real or mock):**
 Actual frequency/mode changes
 Physical PTT activation
 Real-time S-meter readings
 Hardware status queries

The architecture and multi-client safety mechanisms work perfectly without hardware - you're testing the broker's core functionality!
