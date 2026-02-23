# Rig Control Integration - Quick Start Guide

This guide will help you quickly set up and use the rig control integration between the logbook and rig control service.

## Prerequisites

- Rig Control Service running on port 8081 (see rig-control-service/README.md)
- Logbook Backend running on port 8080
- Logbook Frontend running on port 4200
- (Optional) rigctld connected to your radio hardware

## Quick Setup

### 1. Start the Services

```bash
# Terminal 1: Start Rig Control Service
cd rig-control-service
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
mvn spring-boot:run

# Terminal 2: Start Logbook Backend
cd backend
mvn spring-boot:run

# Terminal 3: Start Frontend
cd frontend/logbook-ui
npm start
```

### 2. Configure a Station for Rig Control

1. Log into the logbook frontend
2. Navigate to Station Management
3. Edit or create a station
4. Set the following fields:
   - **Rig Control Enabled**:  (checked)
   - **Rig Control Host**: `localhost` (or IP of rig control service)
   - **Rig Control Port**: `8081`
5. Save the station

### 3. Add Rig Control to Your Page

In any component where you want rig control, add:

```typescript
// your-component.ts
import { RigControlComponent } from './components/rig-control/rig-control.component';

@Component({
  selector: 'app-your-page',
  standalone: true,
  imports: [RigControlComponent],  // Add this
  template: `
    <!-- Your existing content -->

    <!-- Add rig control panel -->
    <app-rig-control
      [stationId]="currentStationId"
      [stationName]="currentStationName">
    </app-rig-control>
  `
})
```

### 4. Initialize WebSocket Connection

In your app initialization (e.g., `app.component.ts`):

```typescript
constructor(
  private websocket: WebSocketService,
  private rigControl: RigControlService
) {
  // Connect services
  websocket.setRigControlService(rigControl);
}
```

### 5. Use the Rig Control Component

The component will appear with:
- **Connect Button** - Click to establish connection
- **Current Status** - Shows frequency, mode, PTT, S-meter
- **Frequency Control** - Enter frequency in MHz and click "Set"
- **Mode Control** - Select mode from dropdown and click "Set"
- **PTT Control** - Toggle transmit on/off
- **Events Panel** - Shows recent rig control events

## Testing Without Radio Hardware

### Option 1: Mock rigctld (Python)

```bash
cd rig-control-service
python3 << 'EOF'
import socket

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('localhost', 4532))
server.listen(1)

print("Mock rigctld listening on port 4532...")

while True:
    client, addr = server.accept()
    while True:
        try:
            data = client.recv(1024).decode().strip()
            if not data:
                break

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
        except:
            break
    client.close()
EOF
```

### Option 2: Test Without rigctld

You can test the UI and WebSocket connectivity without rigctld:
- Connections will succeed
- Commands will return error messages (expected)
- All UI interactions work
- Events are broadcast correctly

## Example Usage Scenarios

### Scenario 1: Contest Logging

```typescript
// When logging a QSO, auto-populate from rig
this.rigControl.getStatus(stationId).subscribe(response => {
  if (response.success && response.result) {
    const status = response.result.status;
    this.qsoForm.patchValue({
      frequency: status.frequency / 1000,  // Convert Hz to kHz
      mode: status.mode
    });
  }
});
```

### Scenario 2: Frequency Scanning

```typescript
// Scan through frequencies
const frequencies = [7200000, 14250000, 21300000];  // Hz
let index = 0;

setInterval(() => {
  this.rigControl.setFrequency(stationId, frequencies[index]).subscribe();
  index = (index + 1) % frequencies.length;
}, 5000);  // Change every 5 seconds
```

### Scenario 3: Multi-Station Dashboard

```typescript
// Subscribe to all stations
stations.forEach(station => {
  this.websocket.subscribeToRigStatus(station.id);
  this.websocket.subscribeToRigEvents(station.id);
});

// Display status for all stations
this.rigControl.onStatusUpdate().subscribe(update => {
  this.stationStatus[update.stationId] = update.status;
});
```

## Common API Calls

### Connect to Rig

```typescript
this.rigControl.connect({
  stationId: 1,
  host: 'localhost',
  port: 8081
}).subscribe(response => {
  if (response.success) {
    console.log('Connected!');
  }
});
```

### Set Frequency

```typescript
// Set to 14.250 MHz
this.rigControl.setFrequency(stationId, 14250000).subscribe(response => {
  if (response.success) {
    console.log('Frequency changed');
  }
});
```

### Set Mode

```typescript
this.rigControl.setMode(stationId, 'USB', 0).subscribe(response => {
  if (response.success) {
    console.log('Mode changed to USB');
  }
});
```

### Activate PTT

```typescript
// Transmit
this.rigControl.setPTT(stationId, true).subscribe(response => {
  if (response.success) {
    console.log('Transmitting!');
  } else {
    console.warn('PTT denied:', response.message);
  }
});

// Receive
this.rigControl.setPTT(stationId, false).subscribe();
```

### Get Current Status

```typescript
this.rigControl.getStatus(stationId).subscribe(response => {
  if (response.success) {
    const status = response.result.status;
    console.log('Frequency:', status.frequency);
    console.log('Mode:', status.mode);
    console.log('PTT:', status.ptt);
    console.log('S-meter:', status.sMeter);
  }
});
```

## Multi-Client Testing

### Test PTT Locking

1. Open logbook in 2 browser tabs
2. In Tab 1: Connect to rig, click "PTT ON"
   -  Should succeed, button turns red
3. In Tab 2: Connect to rig, click "PTT ON"
   -  Should fail with "PTT denied: held by {Tab1 station}"
4. In Tab 1: Click "PTT OFF"
   -  PTT released
5. In Tab 2: Click "PTT ON"
   -  Now succeeds (lock was released)

### Test Event Broadcasting

1. Open logbook in 2 browser tabs
2. Connect both to the same station (or different stations)
3. In Tab 1: Change frequency
4. In Tab 2: Event panel shows "Frequency changed" event
5. All tabs see all events in real-time

## Troubleshooting

### "Failed to connect to rig control service"
- Check rig control service is running: `curl http://localhost:8081/actuator/health`
- Check configuration in `backend/src/main/resources/application.properties`

### "Station not connected"
- Click "Connect to Rig" button in the component
- Check station has rig control enabled in settings

### "PTT denied"
- Another user/tab has PTT lock
- Disconnect other clients or wait for PTT release

### No status updates appearing
- Check browser console for WebSocket errors
- Verify WebSocket service initialization
- Check STOMP connection: Look for "WebSocket connected" in console

## Architecture Diagram

```
Frontend (Angular)
    │
    ├─→ RigControlComponent ────→ User Interface
    │                              ├─ Connect Button
    │                              ├─ Status Display
    │                              ├─ Frequency/Mode Controls
    │                              └─ PTT Button
    │
    ├─→ RigControlService ────────→ HTTP API Calls
    │       ↓                       POST /api/rig-control/*
    │   WebSocketService ─────────→ STOMP Subscriptions
    │                              /topic/rig/status/{id}
    │                              /topic/rig/events/{id}
    ↓
Backend (Spring Boot)
    │
    ├─→ RigControlController ─────→ REST Endpoints
    │       ↓                       Authentication/Authorization
    │   RigControlClient ─────────→ WebSocket Client
    │                              Connects to Rig Control Service
    │                              Manages per-station connections
    ↓
Rig Control Service (Port 8081)
    │
    ├─→ RigCommandHandler ────────→ Command WebSocket
    ├─→ RigStatusHandler ─────────→ Status WebSocket (100ms updates)
    ├─→ RigEventsHandler ─────────→ Events WebSocket
    │
    ├─→ PTTLockManager ───────────→ First-come-first-served locking
    ├─→ RigCommandDispatcher ─────→ Serialization & Caching
    │
    └─→ rigctld ──────────────────→ Radio Hardware
```

## Next Steps

1.  Test basic connection
2.  Test frequency/mode changes
3.  Test PTT locking with multiple clients
4.  Review event stream
5.  Integrate with your QSO logging workflow
6.  Customize UI to match your needs
7.  Add rig control to contest screens

## Additional Resources

- **Full Integration Docs**: `RIG_CONTROL_INTEGRATION.md`
- **Rig Control Service Docs**: `rig-control-service/README.md`
- **Testing Guide**: `rig-control-service/TESTING_WITHOUT_HARDWARE.md`
- **API Reference**: See controller/service source files

---

Happy contesting! 73! 
