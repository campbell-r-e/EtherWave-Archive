# Rig Control Integration Example

## Complete Working Example

Here's a step-by-step example of integrating rig control into the QSO entry page.

### Step 1: Update App Component (One-time setup)

**File:** `frontend/logbook-ui/src/app/app.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { WebSocketService } from './services/websocket.service';
import { RigControlService } from './services/rig-control.service';

@Component({
  selector: 'app-root',
  // ... other config
})
export class AppComponent implements OnInit {

  constructor(
    private websocketService: WebSocketService,
    private rigControlService: RigControlService
  ) {}

  ngOnInit() {
    // Connect RigControlService to WebSocketService for real-time updates
    this.websocketService.setRigControlService(this.rigControlService);
  }
}
```

### Step 2: Add Rig Control to QSO Entry Page

**File:** `frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RigControlComponent } from '../rig-control/rig-control.component';
import { RigControlService } from '../../services/rig-control.service';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-qso-entry',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RigControlComponent  // ← Add this
  ],
  templateUrl: './qso-entry.component.html',
  styleUrls: ['./qso-entry.component.css']
})
export class QSOEntryComponent implements OnInit {
  currentStationId: number = 1;  // From your station selection
  currentStationName: string = 'Station 1';

  // QSO form data
  qsoData = {
    callsign: '',
    frequency: 0,
    mode: '',
    // ... other fields
  };

  constructor(
    private rigControl: RigControlService,
    private websocket: WebSocketService
  ) {}

  ngOnInit() {
    // Subscribe to rig status updates for this station
    this.websocket.subscribeToRigStatus(this.currentStationId);
    this.websocket.subscribeToRigEvents(this.currentStationId);

    // Listen for frequency/mode changes and auto-populate form
    this.rigControl.onStatusUpdate().subscribe(update => {
      if (update.stationId === this.currentStationId && update.status) {
        // Auto-populate frequency and mode from rig
        if (update.status.frequency) {
          this.qsoData.frequency = update.status.frequency / 1000; // Hz to kHz
        }
        if (update.status.mode) {
          this.qsoData.mode = update.status.mode;
        }
      }
    });
  }

  // Optional: Method to manually sync from rig
  syncFromRig() {
    this.rigControl.getStatus(this.currentStationId).subscribe(response => {
      if (response.success && response.result) {
        const status = response.result.status;
        this.qsoData.frequency = status.frequency / 1000;
        this.qsoData.mode = status.mode;
      }
    });
  }
}
```

**File:** `frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.html`

```html
<div class="qso-entry-container">
  <!-- Your existing QSO form -->
  <div class="qso-form">
    <h2>New QSO</h2>

    <div class="form-group">
      <label>Callsign:</label>
      <input type="text" [(ngModel)]="qsoData.callsign">
    </div>

    <div class="form-group">
      <label>Frequency (kHz):</label>
      <input type="number" [(ngModel)]="qsoData.frequency">
      <button (click)="syncFromRig()" class="btn-sync">
        🔄 Sync from Rig
      </button>
    </div>

    <div class="form-group">
      <label>Mode:</label>
      <input type="text" [(ngModel)]="qsoData.mode">
    </div>

    <!-- ... other fields ... -->
  </div>

  <!-- Add Rig Control Panel -->
  <div class="rig-control-section">
    <app-rig-control
      [stationId]="currentStationId"
      [stationName]="currentStationName">
    </app-rig-control>
  </div>
</div>
```

**File:** `frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.css`

```css
.qso-entry-container {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  padding: 20px;
}

.rig-control-section {
  position: sticky;
  top: 20px;
  height: fit-content;
}

@media (max-width: 1024px) {
  .qso-entry-container {
    grid-template-columns: 1fr;
  }

  .rig-control-section {
    position: relative;
    top: 0;
  }
}
```

### Step 3: Test the Integration

1. **Start all services:**
   ```bash
   # Terminal 1: Rig Control Service
   cd rig-control-service
   mvn spring-boot:run

   # Terminal 2: Logbook Backend
   cd backend
   mvn spring-boot:run

   # Terminal 3: Frontend
   cd frontend/logbook-ui
   npm start
   ```

2. **Access the application:**
   - Navigate to `http://localhost:4200`
   - Go to QSO Entry page
   - You should see the rig control panel on the right side

3. **Test functionality:**
   - Click "Connect to Rig"
   - Change frequency/mode in rig control panel
   - See the QSO form fields auto-populate
   - Click "Sync from Rig" button to manually update

## Alternative: Minimal Integration

If you just want to add rig control without auto-populating forms:

```typescript
// In any component
import { RigControlComponent } from '../rig-control/rig-control.component';

@Component({
  imports: [RigControlComponent],
  template: `
    <h1>My Page</h1>

    <!-- Add rig control anywhere -->
    <app-rig-control
      [stationId]="1"
      [stationName]="'My Station'">
    </app-rig-control>
  `
})
export class MyComponent {}
```

## Backend Verification

Verify the backend endpoints are accessible:

```bash
# Check if rig control endpoints exist
curl http://localhost:8080/actuator/mappings | grep rig-control

# Test connection (requires authentication)
curl -X POST http://localhost:8080/api/rig-control/connect \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"stationId": 1, "host": "localhost", "port": 8081}'
```

## Database Setup

Ensure stations have rig control fields populated:

```sql
-- Update station with rig control settings
UPDATE stations
SET rig_control_enabled = true,
    rig_control_host = 'localhost',
    rig_control_port = 8081
WHERE id = 1;
```

## Testing Multi-Client Scenario

1. Open browser Tab 1:
   - Navigate to QSO entry
   - Connect to rig
   - Click "PTT ON"

2. Open browser Tab 2 (incognito or different user):
   - Navigate to same page
   - Connect to rig
   - Try "PTT ON" → Should fail with "PTT denied"

3. In Tab 1:
   - Click "PTT OFF"

4. In Tab 2:
   - Click "PTT ON" → Now succeeds

## Troubleshooting Checklist

- [ ] Rig control service running on port 8081
- [ ] Logbook backend running on port 8080
- [ ] Frontend running on port 4200
- [ ] Station configured with rig control settings
- [ ] WebSocket connection established (check browser console)
- [ ] JWT token valid (if authentication errors)
- [ ] CORS configured correctly
- [ ] No firewall blocking WebSocket connections

## Production Deployment

When deploying to production:

1. **Update application.properties:**
   ```properties
   rig.control.service.url=${RIG_CONTROL_SERVICE_URL:ws://rig-control.example.com:8081}
   ```

2. **Set environment variable:**
   ```bash
   export RIG_CONTROL_SERVICE_URL=ws://your-rig-control-host:8081
   ```

3. **Configure stations** to use production rig control host

4. **Enable HTTPS/WSS** for secure WebSocket connections:
   ```properties
   rig.control.service.url=wss://rig-control.example.com:8081
   ```

## Next Steps

- ✅ Integration complete
- ✅ Auto-population working
- ✅ Real-time updates flowing
- 🚀 Add to other pages as needed
- 🚀 Customize UI styling
- 🚀 Add keyboard shortcuts for PTT
- 🚀 Integrate with contest logging
- 🚀 Add frequency memory presets

---

**Status:** ✅ Fully functional and tested
**Last Updated:** 2025-12-12
