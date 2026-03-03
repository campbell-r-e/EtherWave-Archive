# Rig Control Integration - Logbook & Rig Control Service

This document describes the integration between the Ham Radio Logbook application and the Rig Control Service.

## Architecture Overview

The logbook now integrates with the rig control service to provide per-user rig control capabilities:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Logbook Frontend (Angular)                    │
│  ┌──────────────────┐           ┌─────────────────────────────┐ │
│  │ RigControlService│◄─────────►│ WebSocketService (STOMP)    │ │
│  └────────┬─────────┘           └─────────────────────────────┘ │
│           │                                                       │
└───────────┼───────────────────────────────────────────────────────┘
            │ HTTP/WebSocket
            ▼
┌─────────────────────────────────────────────────────────────────┐
│               Logbook Backend (Spring Boot)                      │
│  ┌─────────────────┐           ┌──────────────────────────────┐ │
│  │RigControlClient │◄─────────►│ RigControlController (REST)  │ │
│  └────────┬────────┘           └──────────────────────────────┘ │
│           │                                                       │
└───────────┼───────────────────────────────────────────────────────┘
            │ WebSocket Client
            ▼
┌─────────────────────────────────────────────────────────────────┐
│               Rig Control Service (Port 8081)                    │
│  ┌────────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ Command WS     │  │ Status WS    │  │ Events WS            │ │
│  └────────┬───────┘  └──────┬───────┘  └──────┬───────────────┘ │
│           └──────────────────┴──────────────────┘                │
│                    RigCommandDispatcher                          │
│                    PTTLockManager                                │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
                            rigctld (Hamlib)
                                 │
                                 ▼
                          Physical Radio Hardware
```

## Components

### Backend Components

#### 1. RigControlClient Service
**File:** `backend/src/main/java/com/hamradio/logbook/service/RigControlClient.java`

Manages WebSocket connections to the rig control service on behalf of users/stations.

**Key Features:**
- Maintains per-station WebSocket connections (command, status, events)
- Forwards commands to rig control service
- Receives real-time status updates (100ms interval)
- Receives events (PTT changes, connections, etc.)
- Auto-reconnection handling
- Connection pooling per station

**Methods:**
```java
CompletableFuture<Boolean> connect(Long stationId, String stationName, String host, Integer port)
void disconnect(Long stationId)
CompletableFuture<Map<String, Object>> sendCommand(Long stationId, String command, Map<String, Object> params)
boolean isConnected(Long stationId)
```

#### 2. RigControlController
**File:** `backend/src/main/java/com/hamradio/logbook/controller/RigControlController.java`

REST API for users to control rigs.

**Endpoints:**
- `POST /api/rig-control/connect` - Connect to rig control service
- `POST /api/rig-control/disconnect/{stationId}` - Disconnect
- `POST /api/rig-control/command/{stationId}` - Send generic command
- `POST /api/rig-control/frequency/{stationId}?frequencyHz={hz}` - Set frequency
- `POST /api/rig-control/mode/{stationId}?mode={mode}&bandwidth={bw}` - Set mode
- `POST /api/rig-control/ptt/{stationId}?enable={true/false}` - Set PTT
- `GET /api/rig-control/status/{stationId}` - Get current status
- `GET /api/rig-control/connected/{stationId}` - Check connection status

**Authentication:** All endpoints require `ROLE_USER`, `ROLE_OPERATOR`, or `ROLE_ADMIN`

#### 3. DTOs
**Files:**
- `RigCommandRequest.java` - Command request payload
- `RigCommandResponse.java` - Command response
- `RigConnectionRequest.java` - Connection request

### Frontend Components

#### 1. RigControlService
**File:** `frontend/logbook-ui/src/app/services/rig-control.service.ts`

Angular service for rig control operations.

**Methods:**
```typescript
connect(request: RigConnectionRequest): Observable<any>
disconnect(stationId: number): Observable<any>
sendCommand(stationId: number, request: RigCommandRequest): Observable<RigCommandResponse>
setFrequency(stationId: number, frequencyHz: number): Observable<RigCommandResponse>
setMode(stationId: number, mode: string, bandwidth?: number): Observable<RigCommandResponse>
setPTT(stationId: number, enable: boolean): Observable<RigCommandResponse>
getStatus(stationId: number): Observable<RigCommandResponse>
isConnected(stationId: number): Observable<{connected: boolean, stationId: number}>
onStatusUpdate(): Observable<{stationId: number, status: RigStatus}>
onEventUpdate(): Observable<{stationId: number, event: RigEvent}>
```

#### 2. RigControlComponent
**File:** `frontend/logbook-ui/src/app/components/rig-control/rig-control.component.ts`

Standalone Angular component for rig control UI.

**Inputs:**
- `stationId: number` - Required
- `stationName: string` - Required
- `host?: string` - Optional (uses station config if not provided)
- `port?: number` - Optional (uses station config if not provided)

**Features:**
- Connection management
- Real-time status display (frequency, mode, PTT, S-meter)
- Frequency control
- Mode selection
- PTT control with visual feedback
- Event history display
- Error handling and user feedback

#### 3. WebSocketService Updates
**File:** `frontend/logbook-ui/src/app/services/websocket.service.ts`

Updated to subscribe to rig control WebSocket topics:
- `/topic/rig/status/{stationId}` - Real-time status updates
- `/topic/rig/events/{stationId}` - Event notifications

## Configuration

### Backend Configuration
**File:** `backend/src/main/resources/application.properties`

```properties
# Rig Control Service Configuration
rig.control.service.url=${RIG_CONTROL_SERVICE_URL:ws://localhost:8081}
```

**Environment Variable:** `RIG_CONTROL_SERVICE_URL` (default: `ws://localhost:8081`)

### Station Configuration

Each station can have rig control settings:

**Database Fields (Station entity):**
- `rig_control_enabled` - Boolean flag
- `rig_control_host` - Hostname/IP of rig control service
- `rig_control_port` - Port number (default: 8081)

## Usage

### Adding Rig Control to a Page

```typescript
import { Component } from '@angular/core';
import { RigControlComponent } from './components/rig-control/rig-control.component';

@Component({
  selector: 'app-my-page',
  standalone: true,
  imports: [RigControlComponent],
  template: `
    <app-rig-control
      [stationId]="currentStation.id"
      [stationName]="currentStation.name"
      [host]="currentStation.rigControlHost"
      [port]="currentStation.rigControlPort">
    </app-rig-control>
  `
})
export class MyPageComponent {
  currentStation = { id: 1, name: 'Field Day Station 1', rigControlHost: 'localhost', rigControlPort: 8081 };
}
```

### Subscribing to Status Updates

```typescript
constructor(private rigControl: RigControlService, private websocket: WebSocketService) {
  // Connect rig control service to websocket service
  websocket.setRigControlService(rigControl);

  // Subscribe to specific station
  websocket.subscribeToRigStatus(stationId);
  websocket.subscribeToRigEvents(stationId);

  // Listen for updates
  rigControl.onStatusUpdate().subscribe(update => {
    console.log('Status update for station', update.stationId, update.status);
  });

  rigControl.onEventUpdate().subscribe(update => {
    console.log('Event for station', update.stationId, update.event);
  });
}
```

## Multi-Client Behavior

The rig control service implements multi-client safety:

### PTT Locking
- **First-come-first-served**: First client to request PTT wins
- **Exclusive lock**: Only one client can hold PTT at a time
- **Auto-release**: PTT released when client disconnects
- **Event broadcasting**: All clients notified of PTT state changes

**Example Events:**
```json
{
  "timestamp": "2025-12-12T14:30:45",
  "eventType": "ptt_activated",
  "clientId": "Field Day Station 1",
  "message": "Client 'Field Day Station 1' activated PTT"
}

{
  "timestamp": "2025-12-12T14:30:46",
  "eventType": "ptt_denied",
  "clientId": "Field Day Station 2",
  "message": "PTT denied for 'Field Day Station 2': held by 'Field Day Station 1'"
}
```

### Command Serialization
- All commands are serialized through a single-threaded executor
- Prevents command conflicts and race conditions
- Guarantees FIFO ordering

### Request Coalescing
- Duplicate simultaneous read commands are deduplicated
- Reduces load on rigctld
- Improves performance

### Smart Caching
- Read commands cached for 50ms (20ms for S-meter)
- Reduces rigctld queries by 10-50x
- Near-real-time performance maintained

## Real-Time Updates

### Status Updates
Broadcast every 100ms to all connected clients:

```json
{
  "frequency": 14250000,
  "mode": "USB",
  "ptt": false,
  "sMeter": 5,
  "connected": true
}
```

### Event Types
- `ptt_activated` - PTT turned on
- `ptt_released` - PTT turned off
- `ptt_denied` - PTT request denied (held by another client)
- `client_connected` - Client connected to rig control
- `client_disconnected` - Client disconnected
- `error` - Error occurred

## Error Handling

### Connection Errors
- **Backend**: Logs error, returns error response to frontend
- **Frontend**: Displays error message to user, allows retry

### Command Errors
- **No Connection**: "Station not connected to rig control service"
- **rigctld Down**: "Failed to connect to rig control service"
- **PTT Denied**: "PTT denied: held by {other_client}"
- **Invalid Command**: "Unknown command: {command}"

### Auto-Recovery
- WebSocket auto-reconnection (5 second interval)
- Connection state monitoring
- Graceful degradation when rig control unavailable

## Testing

### Without Hardware

1. Start rig control service: `cd rig-control-service && mvn spring-boot:run`
2. Start logbook backend: `cd backend && mvn spring-boot:run`
3. Start frontend: `cd frontend/logbook-ui && npm start`
4. Navigate to rig control component
5. Click "Connect to Rig"
6. Commands will show connection errors (expected without rigctld)
7. WebSocket connections and UI interaction fully functional

### With Mock rigctld

See `rig-control-service/TESTING_WITHOUT_HARDWARE.md` for mock rigctld scripts.

### With Real Hardware

1. Start rigctld: `rigctld -m <model> -r <device>` (e.g., `rigctld -m 1 -r /dev/ttyUSB0`)
2. Start rig control service
3. Start logbook
4. Connect and control your radio!

## Performance

### Latency Targets
- Command response: <50ms
- Status updates: 100ms interval
- Event broadcasting: Real-time (<10ms)

### Resource Usage
- Backend: ~10MB per active station connection
- Frontend: Minimal (WebSocket + Angular)
- Network: ~1KB/sec per station (status updates)

## Security

### Authentication
- All rig control endpoints require authentication
- JWT token validation
- Role-based access control (USER, OPERATOR, ADMIN)

### Per-Station Isolation
- Each station has independent connection
- Commands scoped to station ID
- No cross-station command execution

### PTT Safety
- Exclusive locking prevents accidental transmission
- Auto-release on disconnect
- Event logging for audit trail

## Troubleshooting

### "Failed to connect to rig control service"
- Ensure rig control service is running on port 8081
- Check `rig.control.service.url` configuration
- Verify network connectivity

### "Station not connected to rig control service"
- Click "Connect to Rig" button first
- Check station has rig control enabled
- Verify station configuration (host/port)

### "PTT denied: held by {other_client}"
- Another client currently has PTT lock
- Wait for other client to release PTT
- Or disconnect other client

### Status updates not appearing
- Check WebSocket connection (browser dev tools)
- Verify subscriptions: `websocket.subscribeToRigStatus(stationId)`
- Ensure `websocket.setRigControlService(rigControl)` was called

### Commands timing out
- rigctld may not be running
- Radio may not be connected/powered on
- Check rigctld logs: `rigctld -vvv ...` (verbose mode)

## API Reference

See complete API documentation in:
- Backend: `backend/src/main/java/com/hamradio/logbook/controller/RigControlController.java`
- Frontend: `frontend/logbook-ui/src/app/services/rig-control.service.ts`
- Rig Control Service: `rig-control-service/README.md`

## Future Enhancements

Potential improvements:
- [ ] Rig control history/logging
- [ ] Frequency memory presets
- [ ] Automatic mode selection based on frequency
- [ ] Integration with QSO logging (auto-populate frequency/mode)
- [ ] Split operation support
- [ ] VFO A/B control
- [ ] Rig scanner integration
- [ ] Power level control
- [ ] Filter width control
- [ ] Rig control macros/scripting

## Migration from Old System

If migrating from the old telemetry system:

1. **Database**: Station fields already exist, no migration needed
2. **Frontend**: Replace telemetry polling with rig control component
3. **Backend**: TelemetryController still functional for backward compatibility
4. **WebSocket**: Old `/topic/telemetry/{stationId}` still works alongside new `/topic/rig/*`

## Support

For issues or questions:
- Logbook: Check main repository README
- Rig Control Service: Check `rig-control-service/README.md`
- GitHub Issues: Report bugs or feature requests

---

**Last Updated:** 2025-12-12
**Version:** 1.0.0
