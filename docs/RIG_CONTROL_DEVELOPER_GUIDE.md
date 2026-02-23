# Rig Control Service - Developer Integration Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [WebSocket API Reference](#websocket-api-reference)
4. [Integration Examples](#integration-examples)
5. [Multi-Client Considerations](#multi-client-considerations)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)
8. [Code Examples](#code-examples)

## Introduction

The **Rig Control Service** is a multi-client WebSocket broker that allows multiple applications to control amateur radio equipment simultaneously without conflicts. This guide explains how to integrate your application with the service.

### Key Features

-  **Multi-Client Support** - Multiple apps control the same rig safely
-  **Real-Time Updates** - Status broadcasts every 100ms
-  **PTT Locking** - First-come-first-served exclusive transmission control
-  **Command Serialization** - No race conditions or conflicts
-  **Event Broadcasting** - All clients notified of changes
-  **Request Coalescing** - Optimized duplicate request handling
-  **Smart Caching** - Reduced rigctld load, improved performance

### Use Cases

- **Contest Logging Software** - Real-time frequency/mode tracking
- **Remote Control Applications** - Web-based rig control
- **Multi-Operator Stations** - Coordinated access to shared radios
- **Rig Monitoring Dashboards** - Real-time status displays
- **Automated Systems** - Frequency scanning, band monitoring, etc.

### Requirements

- **WebSocket Client** - Your application must support WebSocket connections
- **JSON Parsing** - Messages are JSON-formatted
- **Network Access** - Connect to rig control service (default: `ws://host:8081`)

## Architecture Overview

### System Components

```
Your Application
    │
    ├─ WebSocket 1: Command Channel (bidirectional)
    ├─ WebSocket 2: Status Channel (receive only)
    └─ WebSocket 3: Events Channel (receive only)
    ↓
Rig Control Service (Port 8081)
    │
    ├─ RigCommandHandler - Processes commands, returns responses
    ├─ RigStatusPoller - Broadcasts status every 100ms
    ├─ RigEventsHandler - Broadcasts events in real-time
    ├─ PTTLockManager - Manages exclusive PTT access
    └─ RigCommandDispatcher - Serializes commands, caches results
    ↓
rigctld (Hamlib)
    ↓
Radio Hardware
```

### Three WebSocket Architecture

The service uses **three separate WebSocket endpoints** for different purposes:

| Endpoint | Type | Purpose | Frequency |
|----------|------|---------|-----------|
| `/ws/rig/command` | Bidirectional | Send commands, receive responses | On-demand |
| `/ws/rig/status` | Receive-only | Real-time status updates | Every 100ms |
| `/ws/rig/events` | Receive-only | Event notifications | Real-time |

**Why Three Endpoints?**
- **Separation of Concerns** - Commands don't interfere with status/events
- **Performance** - Status/events broadcast independently
- **Reliability** - Failure in one channel doesn't affect others

## WebSocket API Reference

### 1. Command WebSocket

**Endpoint:** `ws://<host>:8081/ws/rig/command?clientName=<name>`

**Purpose:** Send commands to the rig and receive responses

#### Connection

```javascript
// Connect with optional client name
const commandWs = new WebSocket('ws://localhost:8081/ws/rig/command?clientName=MyApp');

commandWs.onopen = () => {
    console.log('Command channel connected');
};

commandWs.onmessage = (event) => {
    const response = JSON.parse(event.data);
    handleCommandResponse(response);
};
```

**Query Parameters:**
- `clientName` (optional) - Identifies your application
  - If omitted, auto-assigned ID will be used
  - Visible in events to other clients
  - Example: `clientName=ContestLogger`

#### Request Format

All commands follow this JSON structure:

```json
{
    "id": "unique-request-id",
    "command": "commandName",
    "params": {
        "param1": "value1",
        "param2": "value2"
    }
}
```

**Fields:**
- `id` (required, string) - Unique identifier for request/response matching
- `command` (required, string) - Command name (see below)
- `params` (required, object) - Command-specific parameters

#### Response Format

```json
{
    "id": "unique-request-id",
    "success": true,
    "result": {
        "key": "value"
    },
    "message": "Optional message"
}
```

**Fields:**
- `id` (string) - Matches request ID
- `success` (boolean) - `true` if command succeeded, `false` otherwise
- `result` (object) - Command-specific result data (if successful)
- `message` (string) - Human-readable message (success confirmation or error)

#### Available Commands

##### Get Frequency

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
    "result": {
        "frequency": 14250000
    },
    "message": "Current frequency: 14250000 Hz"
}
```

##### Set Frequency

**Request:**
```json
{
    "id": "req-002",
    "command": "setFrequency",
    "params": {
        "hz": 14250000
    }
}
```

**Response:**
```json
{
    "id": "req-002",
    "success": true,
    "result": {
        "frequency": 14250000
    },
    "message": "Frequency set to 14250000 Hz"
}
```

**Parameters:**
- `hz` (number, required) - Frequency in Hertz
  - Example: `14250000` for 14.250 MHz
  - Range: Depends on radio capabilities

##### Get Mode

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
    "result": {
        "mode": "USB",
        "bandwidth": 2400
    },
    "message": "Current mode: USB, bandwidth: 2400 Hz"
}
```

##### Set Mode

**Request:**
```json
{
    "id": "req-004",
    "command": "setMode",
    "params": {
        "mode": "USB",
        "bandwidth": 2400
    }
}
```

**Response:**
```json
{
    "id": "req-004",
    "success": true,
    "result": {
        "mode": "USB",
        "bandwidth": 2400
    },
    "message": "Mode set to USB"
}
```

**Parameters:**
- `mode` (string, required) - Mode name
  - Examples: `USB`, `LSB`, `CW`, `FM`, `AM`, `RTTY`
  - Supported modes depend on radio
- `bandwidth` (number, optional) - Bandwidth in Hz
  - Default: `0` (radio's default for mode)
  - Example: `2400` for typical SSB

##### Get PTT Status

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
    "result": {
        "ptt": false
    },
    "message": "PTT is OFF"
}
```

##### Set PTT (Activate/Release)

**Request:**
```json
{
    "id": "req-006",
    "command": "setPTT",
    "params": {
        "enable": true
    }
}
```

**Response (Success):**
```json
{
    "id": "req-006",
    "success": true,
    "result": {
        "ptt": true
    },
    "message": "PTT activated"
}
```

**Response (Denied - PTT Held by Another Client):**
```json
{
    "id": "req-006",
    "success": false,
    "result": {},
    "message": "PTT denied: currently held by 'ContestLogger'"
}
```

**Parameters:**
- `enable` (boolean, required)
  - `true` - Activate PTT (transmit)
  - `false` - Release PTT (receive)

 **Important:** PTT is **exclusive** - only one client can hold PTT at a time (first-come-first-served)

##### Get S-Meter Reading

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
    "result": {
        "sMeter": 5
    },
    "message": "S-meter: 5"
}
```

**Result:**
- `sMeter` (number) - Signal strength (0-9 for S0-S9, 10+ for S9+10dB, etc.)

##### Get All Status

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

**Use Case:** Get all rig information in one request

#### Error Responses

**Connection Error (rigctld not responding):**
```json
{
    "id": "req-009",
    "success": false,
    "result": {},
    "message": "Failed to connect to rigctld: Connection refused"
}
```

**Invalid Command:**
```json
{
    "id": "req-010",
    "success": false,
    "result": {},
    "message": "Unknown command: invalidCommand"
}
```

**Missing Parameters:**
```json
{
    "id": "req-011",
    "success": false,
    "result": {},
    "message": "Missing required parameter: hz"
}
```

### 2. Status WebSocket

**Endpoint:** `ws://<host>:8081/ws/rig/status`

**Purpose:** Receive real-time status updates (broadcast every 100ms)

#### Connection

```javascript
const statusWs = new WebSocket('ws://localhost:8081/ws/rig/status');

statusWs.onopen = () => {
    console.log('Status channel connected');
};

statusWs.onmessage = (event) => {
    const status = JSON.parse(event.data);
    updateUI(status);
};
```

#### Status Message Format

Received **every 100 milliseconds** (10 times per second):

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
- `frequency` (number) - Current frequency in Hz
- `mode` (string) - Current mode (USB, LSB, CW, etc.)
- `ptt` (boolean) - PTT status (`true` = transmitting, `false` = receiving)
- `sMeter` (number) - Signal strength (0-9+)
- `connected` (boolean) - Connection to rigctld status

**Use Cases:**
- Real-time frequency display
- S-meter display
- PTT indicator
- Mode indicator
- Connection monitoring

**Performance:**
- Updates: 10 Hz (every 100ms)
- Cached on server (reduces rigctld load)
- Only broadcasts when clients are connected

### 3. Events WebSocket

**Endpoint:** `ws://<host>:8081/ws/rig/events`

**Purpose:** Receive event notifications in real-time

#### Connection

```javascript
const eventsWs = new WebSocket('ws://localhost:8081/ws/rig/events');

eventsWs.onopen = () => {
    console.log('Events channel connected');
};

eventsWs.onmessage = (event) => {
    const rigEvent = JSON.parse(event.data);
    handleEvent(rigEvent);
};
```

#### Event Message Format

```json
{
    "timestamp": "2025-12-12T15:30:45.123",
    "eventType": "ptt_activated",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' activated PTT"
}
```

**Fields:**
- `timestamp` (string) - ISO 8601 timestamp
- `eventType` (string) - Type of event (see below)
- `clientId` (string) - Client that triggered the event
- `message` (string) - Human-readable description

#### Event Types

##### Client Connected
```json
{
    "timestamp": "2025-12-12T15:30:45.123",
    "eventType": "client_connected",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' connected"
}
```

##### Client Disconnected
```json
{
    "timestamp": "2025-12-12T15:30:50.456",
    "eventType": "client_disconnected",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' disconnected (PTT auto-released)"
}
```

**Note:** If client had PTT when disconnecting, it's automatically released

##### PTT Activated
```json
{
    "timestamp": "2025-12-12T15:31:00.789",
    "eventType": "ptt_activated",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' activated PTT"
}
```

##### PTT Released
```json
{
    "timestamp": "2025-12-12T15:31:05.012",
    "eventType": "ptt_released",
    "clientId": "ContestLogger",
    "message": "Client 'ContestLogger' released PTT"
}
```

##### PTT Denied
```json
{
    "timestamp": "2025-12-12T15:31:02.345",
    "eventType": "ptt_denied",
    "clientId": "RemoteControl",
    "message": "PTT denied for 'RemoteControl': held by 'ContestLogger'"
}
```

**Use Case:** Another client tried to activate PTT while someone else held it

##### Error
```json
{
    "timestamp": "2025-12-12T15:31:10.678",
    "eventType": "error",
    "clientId": "ContestLogger",
    "message": "Command failed: Connection timeout"
}
```

**Use Cases for Events:**
- Multi-user coordination
- Activity logging
- Audit trail
- User notifications
- Debugging

## Integration Examples

### Minimal Integration (JavaScript)

```javascript
class RigControl {
    constructor(host = 'localhost', port = 8081, clientName = 'MyApp') {
        this.baseUrl = `ws://${host}:${port}`;
        this.clientName = clientName;
        this.requestId = 0;
        this.pendingRequests = new Map();

        this.commandWs = null;
        this.statusWs = null;
        this.eventsWs = null;
    }

    connect() {
        return new Promise((resolve, reject) => {
            // Connect to command channel
            this.commandWs = new WebSocket(
                `${this.baseUrl}/ws/rig/command?clientName=${this.clientName}`
            );

            this.commandWs.onopen = () => {
                console.log('Command channel connected');
                this.connectStatusChannel();
                this.connectEventsChannel();
                resolve();
            };

            this.commandWs.onerror = (error) => {
                console.error('Command channel error:', error);
                reject(error);
            };

            this.commandWs.onmessage = (event) => {
                const response = JSON.parse(event.data);
                const callback = this.pendingRequests.get(response.id);
                if (callback) {
                    callback(response);
                    this.pendingRequests.delete(response.id);
                }
            };
        });
    }

    connectStatusChannel() {
        this.statusWs = new WebSocket(`${this.baseUrl}/ws/rig/status`);

        this.statusWs.onmessage = (event) => {
            const status = JSON.parse(event.data);
            this.onStatusUpdate(status);
        };
    }

    connectEventsChannel() {
        this.eventsWs = new WebSocket(`${this.baseUrl}/ws/rig/events`);

        this.eventsWs.onmessage = (event) => {
            const rigEvent = JSON.parse(event.data);
            this.onEvent(rigEvent);
        };
    }

    sendCommand(command, params) {
        return new Promise((resolve, reject) => {
            const id = `req-${++this.requestId}`;

            this.pendingRequests.set(id, (response) => {
                if (response.success) {
                    resolve(response.result);
                } else {
                    reject(new Error(response.message));
                }
            });

            const request = { id, command, params };
            this.commandWs.send(JSON.stringify(request));

            // Timeout after 5 seconds
            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 5000);
        });
    }

    // API Methods
    async getFrequency() {
        const result = await this.sendCommand('getFrequency', {});
        return result.frequency;
    }

    async setFrequency(hz) {
        await this.sendCommand('setFrequency', { hz });
    }

    async getMode() {
        const result = await this.sendCommand('getMode', {});
        return { mode: result.mode, bandwidth: result.bandwidth };
    }

    async setMode(mode, bandwidth = 0) {
        await this.sendCommand('setMode', { mode, bandwidth });
    }

    async setPTT(enable) {
        await this.sendCommand('setPTT', { enable });
    }

    async getStatus() {
        const result = await this.sendCommand('getStatus', {});
        return result.status;
    }

    // Override these in your application
    onStatusUpdate(status) {
        console.log('Status update:', status);
    }

    onEvent(event) {
        console.log('Event:', event);
    }

    disconnect() {
        if (this.commandWs) this.commandWs.close();
        if (this.statusWs) this.statusWs.close();
        if (this.eventsWs) this.eventsWs.close();
    }
}

// Usage
const rig = new RigControl('localhost', 8081, 'ContestLogger');

await rig.connect();

// Set frequency
await rig.setFrequency(14250000); // 14.250 MHz

// Set mode
await rig.setMode('USB', 2400);

// Get current status
const status = await rig.getStatus();
console.log('Frequency:', status.frequency);
console.log('Mode:', status.mode);
console.log('PTT:', status.ptt);

// Activate PTT
try {
    await rig.setPTT(true);
    console.log('Transmitting!');
} catch (error) {
    console.error('PTT denied:', error.message);
}

// Handle real-time updates
rig.onStatusUpdate = (status) => {
    document.getElementById('frequency').textContent =
        (status.frequency / 1000000).toFixed(3) + ' MHz';
    document.getElementById('mode').textContent = status.mode;
    document.getElementById('smeter').textContent = 'S' + status.sMeter;
};

rig.onEvent = (event) => {
    console.log(`[${event.eventType}] ${event.message}`);
};
```

### Python Integration Example

```python
import asyncio
import websockets
import json

class RigControl:
    def __init__(self, host='localhost', port=8081, client_name='PythonApp'):
        self.base_url = f'ws://{host}:{port}'
        self.client_name = client_name
        self.request_id = 0
        self.pending_requests = {}

        self.command_ws = None
        self.status_ws = None
        self.events_ws = None

    async def connect(self):
        # Connect to command channel
        self.command_ws = await websockets.connect(
            f'{self.base_url}/ws/rig/command?clientName={self.client_name}'
        )

        # Start status and events listeners
        asyncio.create_task(self.listen_status())
        asyncio.create_task(self.listen_events())
        asyncio.create_task(self.listen_responses())

    async def listen_status(self):
        async with websockets.connect(f'{self.base_url}/ws/rig/status') as ws:
            self.status_ws = ws
            async for message in ws:
                status = json.loads(message)
                await self.on_status_update(status)

    async def listen_events(self):
        async with websockets.connect(f'{self.base_url}/ws/rig/events') as ws:
            self.events_ws = ws
            async for message in ws:
                event = json.loads(message)
                await self.on_event(event)

    async def listen_responses(self):
        async for message in self.command_ws:
            response = json.loads(message)
            request_id = response['id']
            if request_id in self.pending_requests:
                future = self.pending_requests.pop(request_id)
                future.set_result(response)

    async def send_command(self, command, params):
        self.request_id += 1
        request_id = f'req-{self.request_id}'

        request = {
            'id': request_id,
            'command': command,
            'params': params
        }

        future = asyncio.Future()
        self.pending_requests[request_id] = future

        await self.command_ws.send(json.dumps(request))

        try:
            response = await asyncio.wait_for(future, timeout=5.0)
            if response['success']:
                return response['result']
            else:
                raise Exception(response['message'])
        except asyncio.TimeoutError:
            self.pending_requests.pop(request_id, None)
            raise Exception('Request timeout')

    async def set_frequency(self, hz):
        await self.send_command('setFrequency', {'hz': hz})

    async def get_frequency(self):
        result = await self.send_command('getFrequency', {})
        return result['frequency']

    async def set_mode(self, mode, bandwidth=0):
        await self.send_command('setMode', {'mode': mode, 'bandwidth': bandwidth})

    async def set_ptt(self, enable):
        await self.send_command('setPTT', {'enable': enable})

    async def get_status(self):
        result = await self.send_command('getStatus', {})
        return result['status']

    async def on_status_update(self, status):
        print(f"Status: {status['frequency']} Hz, {status['mode']}, S{status['sMeter']}")

    async def on_event(self, event):
        print(f"[{event['eventType']}] {event['message']}")

# Usage
async def main():
    rig = RigControl('localhost', 8081, 'PythonLogger')
    await rig.connect()

    # Set frequency to 14.250 MHz
    await rig.set_frequency(14250000)

    # Set mode
    await rig.set_mode('USB', 2400)

    # Get current status
    status = await rig.get_status()
    print(f"Frequency: {status['frequency']} Hz")
    print(f"Mode: {status['mode']}")

    # Keep running to receive updates
    await asyncio.Event().wait()

asyncio.run(main())
```

### Java Integration Example

```java
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RigControlClient {

    private final String baseUrl;
    private final String clientName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requestIdCounter = new AtomicInteger(0);
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    private WebSocketClient commandWs;
    private WebSocketClient statusWs;
    private WebSocketClient eventsWs;

    public RigControlClient(String host, int port, String clientName) {
        this.baseUrl = String.format("ws://%s:%d", host, port);
        this.clientName = clientName;
    }

    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            // Connect to command channel
            URI commandUri = new URI(baseUrl + "/ws/rig/command?clientName=" + clientName);
            commandWs = new WebSocketClient(commandUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Command channel connected");
                    connectStatusChannel();
                    connectEventsChannel();
                    future.complete(null);
                }

                @Override
                public void onMessage(String message) {
                    handleCommandResponse(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Command channel closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Command channel error: " + ex.getMessage());
                    future.completeExceptionally(ex);
                }
            };

            commandWs.connect();
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void connectStatusChannel() {
        try {
            URI statusUri = new URI(baseUrl + "/ws/rig/status");
            statusWs = new WebSocketClient(statusUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Status channel connected");
                }

                @Override
                public void onMessage(String message) {
                    handleStatusUpdate(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {}

                @Override
                public void onError(Exception ex) {}
            };
            statusWs.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectEventsChannel() {
        try {
            URI eventsUri = new URI(baseUrl + "/ws/rig/events");
            eventsWs = new WebSocketClient(eventsUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Events channel connected");
                }

                @Override
                public void onMessage(String message) {
                    handleEvent(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {}

                @Override
                public void onError(Exception ex) {}
            };
            eventsWs.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCommandResponse(String message) {
        try {
            Map<String, Object> response = objectMapper.readValue(message, Map.class);
            String id = (String) response.get("id");

            CompletableFuture<Map<String, Object>> future = pendingRequests.remove(id);
            if (future != null) {
                if ((Boolean) response.get("success")) {
                    future.complete((Map<String, Object>) response.get("result"));
                } else {
                    future.completeExceptionally(new Exception((String) response.get("message")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStatusUpdate(String message) {
        try {
            Map<String, Object> status = objectMapper.readValue(message, Map.class);
            onStatusUpdate(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            onEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<Map<String, Object>> sendCommand(String command, Map<String, Object> params) {
        String requestId = "req-" + requestIdCounter.incrementAndGet();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        pendingRequests.put(requestId, future);

        try {
            Map<String, Object> request = Map.of(
                "id", requestId,
                "command", command,
                "params", params
            );

            String json = objectMapper.writeValueAsString(request);
            commandWs.send(json);
        } catch (Exception e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future.orTimeout(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    // API Methods
    public CompletableFuture<Long> getFrequency() {
        return sendCommand("getFrequency", Map.of())
            .thenApply(result -> ((Number) result.get("frequency")).longValue());
    }

    public CompletableFuture<Void> setFrequency(long hz) {
        return sendCommand("setFrequency", Map.of("hz", hz))
            .thenApply(result -> null);
    }

    public CompletableFuture<Void> setMode(String mode, int bandwidth) {
        return sendCommand("setMode", Map.of("mode", mode, "bandwidth", bandwidth))
            .thenApply(result -> null);
    }

    public CompletableFuture<Void> setPTT(boolean enable) {
        return sendCommand("setPTT", Map.of("enable", enable))
            .thenApply(result -> null);
    }

    public CompletableFuture<Map<String, Object>> getStatus() {
        return sendCommand("getStatus", Map.of())
            .thenApply(result -> (Map<String, Object>) result.get("status"));
    }

    // Override these methods in your application
    protected void onStatusUpdate(Map<String, Object> status) {
        System.out.println("Status: " + status);
    }

    protected void onEvent(Map<String, Object> event) {
        System.out.println("Event: " + event);
    }

    public void disconnect() {
        if (commandWs != null) commandWs.close();
        if (statusWs != null) statusWs.close();
        if (eventsWs != null) eventsWs.close();
    }
}

// Usage
public class Main {
    public static void main(String[] args) throws Exception {
        RigControlClient rig = new RigControlClient("localhost", 8081, "JavaApp");

        rig.connect().get();

        // Set frequency
        rig.setFrequency(14250000).get();

        // Set mode
        rig.setMode("USB", 2400).get();

        // Get status
        Map<String, Object> status = rig.getStatus().get();
        System.out.println("Frequency: " + status.get("frequency"));
        System.out.println("Mode: " + status.get("mode"));

        Thread.sleep(10000); // Receive updates for 10 seconds

        rig.disconnect();
    }
}
```

## Multi-Client Considerations

### PTT Locking Behavior

**First-Come-First-Served:**
```
Client A: setPTT(true)  → SUCCESS (acquires lock)
Client B: setPTT(true)  → DENIED (A holds lock)
Client C: setPTT(true)  → DENIED (A holds lock)
Client A: setPTT(false) → SUCCESS (releases lock)
Client B: setPTT(true)  → SUCCESS (acquires lock now)
```

**Auto-Release on Disconnect:**
```
Client A: connects
Client A: setPTT(true)  → SUCCESS
Client A: disconnects   → PTT auto-released
Client B: setPTT(true)  → SUCCESS (lock available)
```

### Handling PTT Denials

```javascript
async function transmit() {
    try {
        await rig.setPTT(true);
        console.log('Transmitting');

        // Transmit for 5 seconds
        await new Promise(resolve => setTimeout(resolve, 5000));

        await rig.setPTT(false);
        console.log('Receiving');
    } catch (error) {
        if (error.message.includes('PTT denied')) {
            console.warn('Cannot transmit: Another client is transmitting');
            // Wait and retry, or notify user
        } else {
            console.error('PTT error:', error);
        }
    }
}
```

### Coordinating with Events

```javascript
let pttOwner = null;

rig.onEvent = (event) => {
    switch (event.eventType) {
        case 'ptt_activated':
            pttOwner = event.clientId;
            console.log(`${pttOwner} is now transmitting`);
            disableTransmitButton();
            break;

        case 'ptt_released':
            console.log(`${pttOwner} stopped transmitting`);
            pttOwner = null;
            enableTransmitButton();
            break;

        case 'ptt_denied':
            console.log(`PTT denied for ${event.clientId}`);
            break;
    }
};
```

### Command Serialization

All commands are automatically serialized (executed one at a time in FIFO order). You don't need to worry about race conditions:

```javascript
// These will execute in order, not simultaneously
await rig.setFrequency(14250000);
await rig.setMode('USB', 2400);
await rig.setPTT(true);
```

### Request Coalescing

If multiple clients issue identical read commands simultaneously, the service coalesces them into a single rigctld query:

```javascript
// Client A and Client B both call getFrequency() at the same time
// Service makes only ONE rigctld query, returns result to both
const freqA = await clientA.getFrequency();
const freqB = await clientB.getFrequency();
```

**Benefit:** Reduces rigctld load by 10-50x in multi-client scenarios

## Error Handling

### Connection Errors

```javascript
try {
    await rig.connect();
} catch (error) {
    console.error('Failed to connect:', error);
    // Retry logic
    setTimeout(() => rig.connect(), 5000);
}

// Handle disconnections
rig.commandWs.onclose = () => {
    console.warn('Disconnected from rig control service');
    // Auto-reconnect
    setTimeout(() => rig.connect(), 5000);
};
```

### Command Timeouts

```javascript
try {
    const result = await rig.setFrequency(14250000);
} catch (error) {
    if (error.message === 'Request timeout') {
        console.error('Command timed out - rigctld may be unresponsive');
    } else {
        console.error('Command failed:', error);
    }
}
```

### rigctld Down

```javascript
try {
    await rig.getStatus();
} catch (error) {
    if (error.message.includes('Connection refused')) {
        console.error('rigctld is not running');
        // Notify user, disable rig control UI
    }
}
```

## Best Practices

### 1. Use Client Names

Always provide a meaningful `clientName`:

```javascript
const rig = new RigControl('localhost', 8081, 'ContestLogger-Station1');
```

**Why:**
- Events show which client did what
- Easier debugging
- Better user coordination

### 2. Subscribe to All Three Channels

Always connect to all three WebSocket channels:

```javascript
await rig.connect(); // Connects command, status, and events
```

**Why:**
- Status: Real-time display updates
- Events: Coordination with other clients
- Commands: Control the rig

### 3. Handle PTT Denials Gracefully

```javascript
async function tryTransmit() {
    try {
        await rig.setPTT(true);
        return true;
    } catch (error) {
        if (error.message.includes('PTT denied')) {
            showUserMessage('Cannot transmit: Another operator is transmitting');
            return false;
        }
        throw error;
    }
}
```

### 4. Implement Auto-Reconnection

```javascript
function connectWithRetry(maxRetries = 5) {
    let retries = 0;

    async function attemptConnect() {
        try {
            await rig.connect();
            retries = 0; // Reset on success
        } catch (error) {
            retries++;
            if (retries < maxRetries) {
                console.log(`Retry ${retries}/${maxRetries} in 5s...`);
                setTimeout(attemptConnect, 5000);
            } else {
                console.error('Max retries reached');
            }
        }
    }

    attemptConnect();
}
```

### 5. Use Status Updates for UI

Don't poll - use the status WebSocket:

```javascript
// Good
rig.onStatusUpdate = (status) => {
    updateFrequencyDisplay(status.frequency);
    updateModeDisplay(status.mode);
    updateSMeter(status.sMeter);
};

// Bad - don't poll
setInterval(async () => {
    const status = await rig.getStatus(); // Wasteful!
}, 100);
```

### 6. Clean Up on Exit

```javascript
window.addEventListener('beforeunload', () => {
    rig.disconnect();
});

// Or in your app's cleanup
async function cleanup() {
    await rig.setPTT(false); // Release PTT if held
    rig.disconnect();
}
```

### 7. Validate Input

```javascript
async function setFrequency(mhz) {
    // Validate
    if (mhz < 0.1 || mhz > 10000) {
        throw new Error('Invalid frequency');
    }

    // Convert MHz to Hz
    const hz = Math.round(mhz * 1000000);

    await rig.setFrequency(hz);
}
```

### 8. Handle WebSocket Buffering

```javascript
// Check if connection is ready before sending
if (rig.commandWs.readyState === WebSocket.OPEN) {
    await rig.setFrequency(14250000);
} else {
    console.warn('Not connected');
}
```

## Performance Considerations

### Status Update Frequency

- **100ms interval** = 10 updates/second
- **Typical data size:** ~100 bytes per update
- **Network usage:** ~1 KB/second per client

### Command Latency

- **Target:** <50ms round-trip
- **Typical:** 10-30ms (with caching)
- **Without cache:** 50-100ms (depends on rigctld/radio)

### Caching Benefits

- **Read commands cached for 50ms** (20ms for S-meter)
- **Reduces rigctld queries by 10-50x**
- **Multiple clients benefit from shared cache**

### Scalability

- **Tested:** 10+ simultaneous clients
- **Resource usage:** ~10 MB memory per client
- **CPU usage:** <1% per client on modern hardware

## Testing

### Testing Without Radio Hardware

The service includes a mock mode for testing:

```bash
# Start service with mock rigctld
python3 scripts/mock-rigctld.py
```

Then connect your application normally. Commands will succeed with simulated responses.

### Unit Testing Your Integration

```javascript
// Mock WebSocket for testing
class MockWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = WebSocket.CONNECTING;
        setTimeout(() => {
            this.readyState = WebSocket.OPEN;
            this.onopen();
        }, 10);
    }

    send(data) {
        // Simulate response
        setTimeout(() => {
            const request = JSON.parse(data);
            const response = {
                id: request.id,
                success: true,
                result: { frequency: 14250000 },
                message: 'Success'
            };
            this.onmessage({ data: JSON.stringify(response) });
        }, 50);
    }

    close() {
        this.readyState = WebSocket.CLOSED;
        this.onclose();
    }
}

// Use in tests
global.WebSocket = MockWebSocket;
```

## Troubleshooting

### Connection Refused

**Problem:** `Failed to connect to ws://localhost:8081`

**Solutions:**
1. Verify service is running: `curl http://localhost:8081/actuator/health`
2. Check firewall settings
3. Verify correct host/port

### Request Timeout

**Problem:** Commands timing out after 5 seconds

**Solutions:**
1. Check rigctld is running
2. Verify radio is connected and powered on
3. Check rigctld logs for errors
4. Try increasing timeout in your client code

### PTT Always Denied

**Problem:** PTT denied even when no one else is transmitting

**Solutions:**
1. Check events channel for who holds PTT
2. Disconnect and reconnect (forces PTT release)
3. Restart rig control service (clears all locks)

### Status Updates Stopped

**Problem:** Not receiving status updates

**Solutions:**
1. Check status WebSocket connection state
2. Reconnect status WebSocket
3. Verify rigctld is responding

## Support

### Documentation

- **User Guide:** `/docs/RIG_CONTROL_USER_GUIDE.md`
- **Integration Guide:** This document
- **Quick Start:** `/RIG_CONTROL_QUICKSTART.md`
- **Technical Details:** `/RIG_CONTROL_INTEGRATION.md`

### Example Code

See `/docs/examples/` directory for:
- JavaScript/TypeScript examples
- Python examples
- Java examples
- C# examples (coming soon)

### Community

- **Issues:** Report bugs on GitHub
- **Discussions:** Join the discussions board
- **Contributing:** Pull requests welcome

---

**Last Updated:** 2025-12-12
**Version:** 1.0
**API Version:** 1.0
**Service Version:** 1.0.0-SNAPSHOT

## Appendix: Complete API Summary

### WebSocket Endpoints

| Endpoint | Type | Purpose |
|----------|------|---------|
| `ws://host:8081/ws/rig/command?clientName=X` | Bidirectional | Commands and responses |
| `ws://host:8081/ws/rig/status` | Receive-only | Status broadcasts (100ms) |
| `ws://host:8081/ws/rig/events` | Receive-only | Event notifications |

### Commands

| Command | Parameters | Returns |
|---------|-----------|---------|
| `getFrequency` | `{}` | `{ frequency: number }` |
| `setFrequency` | `{ hz: number }` | `{ frequency: number }` |
| `getMode` | `{}` | `{ mode: string, bandwidth: number }` |
| `setMode` | `{ mode: string, bandwidth?: number }` | `{ mode: string, bandwidth: number }` |
| `getPTT` | `{}` | `{ ptt: boolean }` |
| `setPTT` | `{ enable: boolean }` | `{ ptt: boolean }` |
| `getSMeter` | `{}` | `{ sMeter: number }` |
| `getStatus` | `{}` | `{ status: {...} }` |

### Event Types

| Event Type | Triggered When |
|------------|----------------|
| `client_connected` | Client connects to rig control |
| `client_disconnected` | Client disconnects |
| `ptt_activated` | Client activates PTT |
| `ptt_released` | Client releases PTT |
| `ptt_denied` | PTT request denied (held by another) |
| `error` | Error occurs |

Happy coding! 73! 
