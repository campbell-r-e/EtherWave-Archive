# Rig Control Service - API Quick Reference

## WebSocket Endpoints

```
ws://<host>:8081/ws/rig/command?clientName=<name>  (Bidirectional)
ws://<host>:8081/ws/rig/status                     (Receive-only)
ws://<host>:8081/ws/rig/events                     (Receive-only)
```

## Quick Start

### 1. Connect to All Three Channels

```javascript
const commandWs = new WebSocket('ws://localhost:8081/ws/rig/command?clientName=MyApp');
const statusWs = new WebSocket('ws://localhost:8081/ws/rig/status');
const eventsWs = new WebSocket('ws://localhost:8081/ws/rig/events');
```

### 2. Send a Command

```javascript
const request = {
    id: 'req-001',
    command: 'setFrequency',
    params: { hz: 14250000 }
};

commandWs.send(JSON.stringify(request));
```

### 3. Receive Response

```javascript
commandWs.onmessage = (event) => {
    const response = JSON.parse(event.data);
    // { id: 'req-001', success: true, result: {...}, message: '...' }
};
```

### 4. Receive Status Updates

```javascript
statusWs.onmessage = (event) => {
    const status = JSON.parse(event.data);
    // { frequency: 14250000, mode: 'USB', ptt: false, sMeter: 5, connected: true }
};
```

### 5. Receive Events

```javascript
eventsWs.onmessage = (event) => {
    const rigEvent = JSON.parse(event.data);
    // { timestamp: '...', eventType: '...', clientId: '...', message: '...' }
};
```

## Command Reference

### Get Frequency

**Request:**
```json
{
    "id": "req-001",
    "command": "getFrequency",
    "params": {}
}
```

**Response:**
```json
{
    "id": "req-001",
    "success": true,
    "result": { "frequency": 14250000 },
    "message": "Current frequency: 14250000 Hz"
}
```

### Set Frequency

**Request:**
```json
{
    "id": "req-002",
    "command": "setFrequency",
    "params": { "hz": 14250000 }
}
```

**Response:**
```json
{
    "id": "req-002",
    "success": true,
    "result": { "frequency": 14250000 },
    "message": "Frequency set to 14250000 Hz"
}
```

### Get Mode

**Request:**
```json
{
    "id": "req-003",
    "command": "getMode",
    "params": {}
}
```

**Response:**
```json
{
    "id": "req-003",
    "success": true,
    "result": { "mode": "USB", "bandwidth": 2400 },
    "message": "Current mode: USB, bandwidth: 2400 Hz"
}
```

### Set Mode

**Request:**
```json
{
    "id": "req-004",
    "command": "setMode",
    "params": { "mode": "USB", "bandwidth": 2400 }
}
```

**Response:**
```json
{
    "id": "req-004",
    "success": true,
    "result": { "mode": "USB", "bandwidth": 2400 },
    "message": "Mode set to USB"
}
```

**Supported Modes:** USB, LSB, CW, FM, AM, RTTY (depends on radio)

### Get PTT Status

**Request:**
```json
{
    "id": "req-005",
    "command": "getPTT",
    "params": {}
}
```

**Response:**
```json
{
    "id": "req-005",
    "success": true,
    "result": { "ptt": false },
    "message": "PTT is OFF"
}
```

### Set PTT (Transmit)

**Request:**
```json
{
    "id": "req-006",
    "command": "setPTT",
    "params": { "enable": true }
}
```

**Response (Success):**
```json
{
    "id": "req-006",
    "success": true,
    "result": { "ptt": true },
    "message": "PTT activated"
}
```

**Response (Denied):**
```json
{
    "id": "req-006",
    "success": false,
    "result": {},
    "message": "PTT denied: currently held by 'OtherClient'"
}
```

⚠️ **PTT is exclusive** - only one client can transmit at a time

### Get S-Meter

**Request:**
```json
{
    "id": "req-007",
    "command": "getSMeter",
    "params": {}
}
```

**Response:**
```json
{
    "id": "req-007",
    "success": true,
    "result": { "sMeter": 5 },
    "message": "S-meter: 5"
}
```

### Get All Status

**Request:**
```json
{
    "id": "req-008",
    "command": "getStatus",
    "params": {}
}
```

**Response:**
```json
{
    "id": "req-008",
    "success": true,
    "result": {
        "status": {
            "frequency": 14250000,
            "mode": "USB",
            "ptt": false,
            "sMeter": 5,
            "connected": true
        }
    },
    "message": "Status retrieved"
}
```

## Status Messages

Broadcast **every 100ms** on `/ws/rig/status`:

```json
{
    "frequency": 14250000,
    "mode": "USB",
    "ptt": false,
    "sMeter": 5,
    "connected": true
}
```

**Fields:**
- `frequency` (number) - Frequency in Hz
- `mode` (string) - Current mode (USB, LSB, CW, etc.)
- `ptt` (boolean) - Transmit status
- `sMeter` (number) - Signal strength (0-9+)
- `connected` (boolean) - rigctld connection status

## Event Messages

Broadcast in real-time on `/ws/rig/events`:

```json
{
    "timestamp": "2025-12-12T15:30:45.123",
    "eventType": "ptt_activated",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' activated PTT"
}
```

### Event Types

| Event Type | Description |
|------------|-------------|
| `client_connected` | Client connected to rig control |
| `client_disconnected` | Client disconnected (PTT auto-released if held) |
| `ptt_activated` | Client activated PTT (transmitting) |
| `ptt_released` | Client released PTT (receiving) |
| `ptt_denied` | PTT request denied (held by another client) |
| `error` | Error occurred |

## Error Responses

### Connection Failed

```json
{
    "id": "req-009",
    "success": false,
    "result": {},
    "message": "Failed to connect to rigctld: Connection refused"
}
```

### Unknown Command

```json
{
    "id": "req-010",
    "success": false,
    "result": {},
    "message": "Unknown command: invalidCommand"
}
```

### Missing Parameter

```json
{
    "id": "req-011",
    "success": false,
    "result": {},
    "message": "Missing required parameter: hz"
}
```

## Common Patterns

### Request/Response Pattern

```javascript
let requestId = 0;
const pendingRequests = new Map();

function sendCommand(command, params) {
    return new Promise((resolve, reject) => {
        const id = `req-${++requestId}`;

        pendingRequests.set(id, { resolve, reject });

        const request = { id, command, params };
        commandWs.send(JSON.stringify(request));

        setTimeout(() => {
            if (pendingRequests.has(id)) {
                pendingRequests.delete(id);
                reject(new Error('Timeout'));
            }
        }, 5000);
    });
}

commandWs.onmessage = (event) => {
    const response = JSON.parse(event.data);
    const pending = pendingRequests.get(response.id);

    if (pending) {
        pendingRequests.delete(response.id);

        if (response.success) {
            pending.resolve(response.result);
        } else {
            pending.reject(new Error(response.message));
        }
    }
};
```

### Usage

```javascript
// Set frequency
await sendCommand('setFrequency', { hz: 14250000 });

// Set mode
await sendCommand('setMode', { mode: 'USB', bandwidth: 2400 });

// Activate PTT
try {
    await sendCommand('setPTT', { enable: true });
    console.log('Transmitting');
} catch (error) {
    console.error('PTT denied:', error.message);
}

// Get status
const status = await sendCommand('getStatus', {});
console.log('Frequency:', status.status.frequency);
```

## Frequency Conversion

### MHz to Hz

```javascript
const mhz = 14.250;
const hz = mhz * 1000000; // 14250000
await sendCommand('setFrequency', { hz });
```

### Hz to MHz

```javascript
const hz = 14250000;
const mhz = hz / 1000000; // 14.250
console.log(`Frequency: ${mhz.toFixed(3)} MHz`);
```

## Complete Example

```javascript
class RigControl {
    constructor(host = 'localhost', port = 8081, clientName = 'MyApp') {
        this.requestId = 0;
        this.pending = new Map();

        this.commandWs = new WebSocket(
            `ws://${host}:${port}/ws/rig/command?clientName=${clientName}`
        );
        this.statusWs = new WebSocket(`ws://${host}:${port}/ws/rig/status`);
        this.eventsWs = new WebSocket(`ws://${host}:${port}/ws/rig/events`);

        this.commandWs.onmessage = (e) => {
            const res = JSON.parse(e.data);
            const p = this.pending.get(res.id);
            if (p) {
                this.pending.delete(res.id);
                res.success ? p.resolve(res.result) : p.reject(new Error(res.message));
            }
        };

        this.statusWs.onmessage = (e) => {
            this.onStatus(JSON.parse(e.data));
        };

        this.eventsWs.onmessage = (e) => {
            this.onEvent(JSON.parse(e.data));
        };
    }

    cmd(command, params) {
        return new Promise((resolve, reject) => {
            const id = `req-${++this.requestId}`;
            this.pending.set(id, { resolve, reject });
            this.commandWs.send(JSON.stringify({ id, command, params }));
            setTimeout(() => {
                if (this.pending.has(id)) {
                    this.pending.delete(id);
                    reject(new Error('Timeout'));
                }
            }, 5000);
        });
    }

    async setFreq(hz) { return await this.cmd('setFrequency', { hz }); }
    async getFreq() { return (await this.cmd('getFrequency', {})).frequency; }
    async setMode(mode, bw = 0) { return await this.cmd('setMode', { mode, bandwidth: bw }); }
    async setPTT(enable) { return await this.cmd('setPTT', { enable }); }
    async getStatus() { return (await this.cmd('getStatus', {})).status; }

    onStatus(status) { console.log('Status:', status); }
    onEvent(event) { console.log('Event:', event); }
}

// Usage
const rig = new RigControl('localhost', 8081, 'TestApp');

await rig.setFreq(14250000);
await rig.setMode('USB', 2400);
await rig.setPTT(true);
await new Promise(r => setTimeout(r, 5000));
await rig.setPTT(false);
```

## Tips

### 1. Always Use Request IDs

Make request IDs unique and sequential:

```javascript
const id = `req-${Date.now()}-${Math.random()}`;
// OR
const id = `req-${++requestIdCounter}`;
```

### 2. Set Timeouts

Always timeout requests (5 seconds recommended):

```javascript
setTimeout(() => {
    if (pendingRequests.has(id)) {
        pendingRequests.delete(id);
        reject(new Error('Request timeout'));
    }
}, 5000);
```

### 3. Handle PTT Denials

```javascript
try {
    await rig.setPTT(true);
} catch (error) {
    if (error.message.includes('denied')) {
        console.log('Someone else is transmitting');
    }
}
```

### 4. Use Status WebSocket for UI

Don't poll - subscribe to status updates:

```javascript
// Good
statusWs.onmessage = (e) => {
    const status = JSON.parse(e.data);
    updateUI(status);
};

// Bad
setInterval(async () => {
    const status = await getStatus(); // Wasteful!
}, 100);
```

### 5. Provide Client Name

```javascript
// Good
const ws = new WebSocket('ws://host:8081/ws/rig/command?clientName=ContestLogger');

// Bad (auto-assigned ID)
const ws = new WebSocket('ws://host:8081/ws/rig/command');
```

### 6. Clean Up on Exit

```javascript
window.addEventListener('beforeunload', async () => {
    await rig.setPTT(false); // Release PTT
    rig.commandWs.close();
    rig.statusWs.close();
    rig.eventsWs.close();
});
```

## Performance

| Metric | Value |
|--------|-------|
| Status Update Frequency | 100ms (10 Hz) |
| Command Latency (typical) | 10-30ms |
| Command Latency (target) | <50ms |
| Request Timeout | 5000ms |
| Status Message Size | ~100 bytes |
| Event Message Size | ~150 bytes |
| Network Usage per Client | ~1 KB/s |

## Testing

### Mock WebSocket for Testing

```javascript
class MockWebSocket {
    constructor(url) {
        this.url = url;
        setTimeout(() => this.onopen(), 10);
    }

    send(data) {
        const req = JSON.parse(data);
        setTimeout(() => {
            this.onmessage({
                data: JSON.stringify({
                    id: req.id,
                    success: true,
                    result: { frequency: 14250000 },
                    message: 'Mock response'
                })
            });
        }, 50);
    }

    close() { setTimeout(() => this.onclose(), 10); }
}
```

### Test Without Radio

Start mock rigctld:

```bash
python3 rig-control-service/scripts/mock-rigctld.py
```

Then connect normally. All commands will succeed with simulated responses.

## Documentation

- **User Guide:** `/docs/RIG_CONTROL_USER_GUIDE.md`
- **Developer Guide:** `/docs/RIG_CONTROL_DEVELOPER_GUIDE.md`
- **Quick Start:** `/RIG_CONTROL_QUICKSTART.md`
- **Integration:** `/RIG_CONTROL_INTEGRATION.md`

---

**Last Updated:** 2025-12-12
**API Version:** 1.0
**Service Version:** 1.0.0-SNAPSHOT
