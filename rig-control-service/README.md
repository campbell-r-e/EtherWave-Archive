# Rig Control Service - Multi-Client Shared Rig Broker

## Overview

The **Rig Control Service** is a Spring Boot microservice that provides **shared, thread-safe access** to amateur radio equipment via Hamlib/rigctld. It acts as a **central broker** allowing multiple client applications (logbook, contest logger, monitoring tools) to safely control a single radio without conflicts.

### Key Features

✅ **Multi-Client Support** - Multiple applications can connect simultaneously
✅ **Conflict Prevention** - Serialized command execution prevents race conditions
✅ **Exclusive PTT Control** - First-come-first-served PTT locking with auto-release
✅ **Near-Real-Time Performance** - <50ms latency target with smart caching
✅ **WebSocket-Based** - Real-time bidirectional communication
✅ **Status Broadcasting** - 100ms status updates to all connected clients
✅ **Event Notifications** - Transparency about multi-client operations
✅ **Hardware-Independent Tests** - Complete test coverage without physical radio

---

## Prerequisites

### 1. Java 25

This service requires **Java 25** or higher.

Download from [Eclipse Adoptium](https://adoptium.net/)

Verify installation:
```bash
java -version
# Should show: openjdk version "25.0.1" or higher
```

### 2. Install Hamlib

**macOS (Homebrew)**:
```bash
brew install hamlib
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt-get install libhamlib-utils
```

**Windows**:
Download from: https://github.com/Hamlib/Hamlib/releases

### 3. Start rigctld

Find your rig model number:
```bash
rigctl --list
```

Start rigctld (example for Yaesu FT-991A, model 1035):
```bash
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400
```

Options:
- `-m`: Rig model number
- `-r`: Serial port device
- `-s`: Serial baud rate
- `-t 4532`: TCP port (default)

Verify it's running:
```bash
telnet localhost 4532
f  # Get frequency
m  # Get mode
```

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│   Multiple Client Applications                              │
│   (Logbook, Contest Logger, Monitoring Tools)                │
└──────────────┬────────────────────────────────────────────────┘
               │ WebSocket Connections
               ↓
┌─────────────────────────────────────────────────────────────┐
│   Rig Control Service (Spring Boot)                         │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  WebSocket Endpoints                                │    │
│  │  • /ws/rig/command  (bidirectional)                 │    │
│  │  • /ws/rig/status   (broadcast)                     │    │
│  │  • /ws/rig/events   (broadcast)                     │    │
│  └────────────────┬───────────────────────────────────┘    │
│                   ↓                                          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  RigCommandDispatcher                              │    │
│  │  • Command queue (serialization)                   │    │
│  │  • Request coalescing (deduplication)              │    │
│  │  • Smart caching (50ms reads, 20ms S-meter)        │    │
│  │  • PTT lock management                             │    │
│  └────────────────┬───────────────────────────────────┘    │
│                   ↓                                          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  RigctlConnection (Persistent Socket)              │    │
│  │  • Single connection to rigctld                    │    │
│  │  • Auto-reconnection                               │    │
│  │  • Thread-safe access                              │    │
│  └────────────────┬───────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────────┘
                    │
                    ↓
             [rigctld / Hamlib]
                    ↓
              [Radio Hardware]
```

### Key Architectural Features

#### Single Rig Authority
- **One persistent connection** to rigctld
- No client talks to Hamlib directly
- Service is the **only command source** for the radio

#### Command Serialization
- Single-threaded executor ensures one command at a time
- Prevents rigctld command collisions
- Write commands bypass cache, reads use smart caching

#### Request Coalescing
When 3 clients request frequency simultaneously:
- Only **ONE** rigctld command is sent
- All 3 clients receive the same response
- Dramatically reduces rigctld load

#### Smart Caching
- **50ms TTL** for frequency/mode/PTT reads (imperceptible to humans)
- **20ms TTL** for S-meter (changes rapidly)
- Cache invalidated automatically on write commands
- **Performance**: <10ms for cached reads, <50ms for uncached

#### Exclusive PTT Locking
- **First-Come-First-Served**: Only one client can transmit
- **Auto-Release**: PTT released when client disconnects (safety)
- **Event Broadcasting**: All clients notified of PTT changes

---

## WebSocket API

### 1. Command WebSocket (`/ws/rig/command`)

**Purpose**: Bidirectional - clients send commands, receive individual responses

**Connection**:
```javascript
ws://localhost:8081/ws/rig/command?clientName=Logbook
```

**Request Format** (Client → Server):
```json
{
  "id": "req-12345",
  "command": "setFrequency",
  "params": {
    "hz": 14250000
  }
}
```

**Response Format** (Server → Client):
```json
{
  "id": "req-12345",
  "success": true,
  "result": {
    "frequency": 14250000
  },
  "message": "Frequency set"
}
```

**Supported Commands**:

| Command | Parameters | Description |
|---------|------------|-------------|
| `setFrequency` | `hz` (long) | Set rig frequency in Hz |
| `setMode` | `mode` (string), `bandwidth` (int) | Set operating mode |
| `setPTT` | `enable` (boolean) | Activate/release PTT |
| `getStatus` | none | Force immediate status read |

---

### 2. Status Broadcast WebSocket (`/ws/rig/status`)

**Purpose**: One-way server → clients status updates every 100ms (10/second)

**Connection**:
```javascript
ws://localhost:8081/ws/rig/status
```

**Message Format** (Server → All Clients):
```json
{
  "frequencyHz": 14250000,
  "mode": "USB",
  "bandwidth": "3000",
  "pttActive": false,
  "sMeter": -73,
  "powerMeter": null,
  "swr": null,
  "connected": true,
  "timestamp": "2025-01-15T10:30:45",
  "error": null
}
```

**Update Rate**: 100ms interval (10 times per second)

---

### 3. Events Broadcast WebSocket (`/ws/rig/events`)

**Purpose**: One-way server → clients event notifications

**Connection**:
```javascript
ws://localhost:8081/ws/rig/events
```

**Message Format** (Server → All Clients):
```json
{
  "timestamp": "2025-01-15T10:30:45",
  "eventType": "ptt_activated",
  "clientId": "Logbook",
  "message": "Client 'Logbook' activated PTT"
}
```

**Event Types**:
- `client_connected` - New client connected
- `client_disconnected` - Client disconnected (with PTT auto-release info)
- `ptt_activated` - PTT activated by client
- `ptt_released` - PTT released by client
- `ptt_denied` - PTT request denied (another client holds it)
- `error` - Error occurred

---

## Multi-Client Behavior Examples

### PTT (Push-To-Talk) Locking

**Scenario**: Two clients want to transmit

```
1. Client "Logbook" sends setPTT: true
   → SUCCESS - PTT activated
   → Event broadcast: "Logbook activated PTT"

2. Client "Monitor" sends setPTT: true
   → DENIED - Logbook owns PTT
   → Event broadcast: "PTT denied for Monitor: held by Logbook"

3. Logbook disconnects (crash/close)
   → PTT AUTO-RELEASED (safety mechanism)
   → Event broadcast: "Logbook disconnected (PTT auto-released)"

4. Monitor sends setPTT: true
   → SUCCESS - PTT now owned by Monitor
```

### Request Coalescing

**Scenario**: Three clients poll frequency simultaneously

```
Time 0ms:  Client A requests frequency
Time 1ms:  Client B requests frequency
Time 2ms:  Client C requests frequency

Result:
  - Only ONE rigctl command sent to rig
  - All 3 clients receive same response: "14.250 MHz"
  - Total rigctl load: 1 command (not 3)
```

### Smart Caching

**Scenario**: Frequent status polling

```
Time 0ms:   Client A requests frequency
            → rigctl query → 14.250 MHz

Time 25ms:  Client B requests frequency
            → cache hit (25ms < 50ms TTL) → 14.250 MHz
            → NO rigctl command sent

Time 60ms:  Client C requests frequency
            → cache expired (60ms > 50ms TTL)
            → rigctl query → 14.250 MHz
```

---

## Running the Service

### Build
```bash
cd rig-control-service
mvn clean package
```

### Run
```bash
java -jar target/rig-control-service-1.0.0-SNAPSHOT.jar
```

### With Custom rigctld Host
```bash
java -jar target/rig-control-service-1.0.0-SNAPSHOT.jar \
  --rigctld.host=192.168.1.100 \
  --rigctld.port=4532
```

### Docker
```bash
docker build -t rig-control-service .
docker run -p 8081:8081 \
  -e RIGCTLD_HOST=host.docker.internal \
  -e RIGCTLD_PORT=4532 \
  rig-control-service
```

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

---

## Configuration

**application.properties**:
```properties
# Rigctld connection
rigctld.host=localhost
rigctld.port=4532
rigctld.timeout=1000

# Server port
server.port=8081
```

**Environment Variables**:
```bash
export RIGCTLD_HOST=192.168.1.100
export RIGCTLD_PORT=4532
```

---

## Client Implementation Example

### JavaScript/TypeScript WebSocket Client

```typescript
// Connect to command endpoint
const commandWs = new WebSocket(
  'ws://localhost:8081/ws/rig/command?clientName=MyApp'
);

// Connect to status broadcast
const statusWs = new WebSocket('ws://localhost:8081/ws/rig/status');

// Connect to events broadcast
const eventsWs = new WebSocket('ws://localhost:8081/ws/rig/events');

// Send frequency change command
function setFrequency(hz: number) {
  const command = {
    id: `req-${Date.now()}`,
    command: 'setFrequency',
    params: { hz }
  };
  commandWs.send(JSON.stringify(command));
}

// Handle command response
commandWs.onmessage = (event) => {
  const response = JSON.parse(event.data);
  if (response.success) {
    console.log('Frequency set:', response.result.frequency);
  } else {
    console.error('Command failed:', response.message);
  }
};

// Handle status updates (10 per second)
statusWs.onmessage = (event) => {
  const status = JSON.parse(event.data);
  updateFrequencyDisplay(status.frequencyHz);
  updateSMeter(status.sMeter);
  updatePTTIndicator(status.pttActive);
};

// Handle events (PTT changes, conflicts, etc.)
eventsWs.onmessage = (event) => {
  const eventData = JSON.parse(event.data);

  if (eventData.eventType === 'ptt_activated') {
    showNotification(`${eventData.clientId} is transmitting`);
  } else if (eventData.eventType === 'ptt_denied') {
    showError('Cannot transmit - another client is using PTT');
  }
};
```

---

## Testing

### Run All Tests
```bash
mvn test
```

### What Tests Cover
- ✅ **PTTLockManager**: Exclusive PTT locking, concurrent access, safety
- ✅ **RigCommandDispatcher**: Caching, coalescing, serialization, performance (<50ms)
- ✅ **RigService**: High-level operations
- ✅ **MockRigctlConnection**: No hardware required

### Example Test Output
```
PTTLockManagerTest
  ✓ acquirePTT - No Active PTT - Success
  ✓ acquirePTT - PTT Already Held - Denied
  ✓ forceReleasePTT - Disconnected Client - Released
  ✓ Concurrent Access - Only One Succeeds

RigCommandDispatcherTest
  ✓ executeReadCommand - Cached Value - Under 10ms
  ✓ executeReadCommand - Concurrent Requests - Coalesced
  ✓ executePTTCommand - PTT Already Held - Denied
  ✓ Performance - Cached Reads Under 10ms
```

All tests pass **without requiring physical radio hardware**.

---

## Performance Characteristics

| Operation | Latency | Notes |
|-----------|---------|-------|
| Cached read | <10ms | From in-memory cache |
| Uncached read | <50ms | Direct rigctl query |
| Write command | <50ms | Queued, serialized |
| Status broadcast | 100ms | Fixed interval |
| PTT lock acquire | <5ms | In-memory operation |
| Request coalescing | 0ms | Shares existing request |

---

## Troubleshooting

### Cannot connect to rigctld
```bash
# Check if rigctld is running
ps aux | grep rigctld

# Test manually
telnet localhost 4532
f  # Type 'f' and press Enter - should return frequency
```

### PTT always denied
- Check events WebSocket to see who owns PTT:
  ```bash
  wscat -c ws://localhost:8081/ws/rig/events
  ```
- Another client may be holding PTT
- If client crashed, restart service to force PTT release

### Status updates stopped
- Verify rigctld is still responding:
  ```bash
  echo "f" | nc localhost 4532
  ```
- Check service logs for connection errors
- Restart service to force reconnection

### Permission denied on serial port (Linux)
```bash
sudo usermod -a -G dialout $USER
# Log out and back in
```

### Rig not responding
- Check serial cable connection
- Verify baud rate matches rig settings
- Ensure rig's CAT/CI-V is enabled in menu

---

## Supported Rigs

The service supports **any rig that Hamlib supports**. Common models:

- **Yaesu**: FT-991A, FT-891, FT-857D, FT-817ND
- **Icom**: IC-7300, IC-9700, IC-705, IC-7610
- **Kenwood**: TS-2000, TS-590SG, TS-480
- **Elecraft**: K3, KX3, KX2
- **Flex**: 6000 series

Check full list: `rigctl --list`

---

## Migration from Previous Version

### Breaking Changes

**OLD (Single-Client REST)**:
- REST endpoints (`/api/rig/status`, `/api/rig/frequency`)
- New socket connection per command
- No multi-client coordination
- No PTT conflict management

**NEW (Multi-Client WebSocket)**:
- WebSocket-only (3 endpoints)
- Single persistent rigctl connection
- Thread-safe multi-client support
- Exclusive PTT locking with auto-release
- Real-time status broadcasting

### Migration Steps

1. **Update client code** to use WebSocket instead of REST
2. **Handle PTT denials** gracefully (wait for release, show user message)
3. **Subscribe to events** WebSocket for multi-client awareness
4. **Test concurrent access** with multiple client instances

---

## Architecture Decisions

### Why WebSocket instead of REST?
- Real-time bidirectional communication
- Lower latency (no HTTP request/response overhead)
- Natural fit for status broadcasting
- Better for multi-client event coordination

### Why single-threaded executor for commands?
- Guarantees strict serialization to rigctld
- Prevents command collisions and race conditions
- Simpler than complex locking mechanisms
- Still achieves <50ms via caching and coalescing

### Why 50ms cache TTL?
- Humans cannot perceive delays <50ms
- Dramatically reduces rigctld load (10x-50x reduction)
- Balances freshness with performance
- S-meter uses shorter 20ms (changes rapidly)

### Why first-come-first-served for PTT?
- Simple and predictable behavior
- Safety through automatic release on disconnect
- No complex priority/override logic needed

---

## Future Enhancements

Potential improvements (not yet implemented):

- **Authentication**: JWT tokens or API keys for client connections
- **Priority Levels**: Allow certain clients (e.g., emergency) to override PTT
- **PTT Safety Timeout**: Auto-release PTT after X seconds (prevent stuck TX)
- **Command History**: Audit trail of who changed what and when
- **Prometheus Metrics**: Monitoring endpoint for ops visibility
- **QSK Support**: Ultra-fast PTT sequencing for CW

---

## License

Part of the EtherWave Archive Ham Radio Logbook System.

---

**Refactored by Claude Code** - Transformed from single-client REST service to multi-client shared rig broker with WebSocket API, command serialization, request coalescing, smart caching, and exclusive PTT locking.
