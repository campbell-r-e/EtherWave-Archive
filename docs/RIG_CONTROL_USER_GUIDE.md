# Rig Control User Guide - Ham Radio Logbook

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Station Configuration](#station-configuration)
4. [Using Rig Control](#using-rig-control)
5. [Multi-User Operations](#multi-user-operations)
6. [Troubleshooting](#troubleshooting)
7. [Advanced Features](#advanced-features)

## Introduction

The Ham Radio Logbook includes integrated rig control capabilities that allow you to:

- **Control your radio remotely** via the web interface
- **Monitor real-time status** (frequency, mode, PTT, S-meter)
- **Multiple operators** can work with the same station simultaneously
- **PTT locking** prevents transmission conflicts during multi-user operations
- **Auto-populate QSO fields** from your rig's current settings

### How It Works

The logbook connects to a central **Rig Control Service** that manages communication with your radio(s) via `rigctld` (Hamlib). Multiple users can connect to the same station, and the system ensures safe operation through PTT locking.

```
Your Browser → Logbook → Rig Control Service → rigctld → Your Radio
```

## Getting Started

### Prerequisites

1. **Logbook Account** - You must have a user account with appropriate permissions
2. **Station Setup** - At least one station must be configured with rig control enabled
3. **Radio Connection** - Your radio must be connected via `rigctld` or the rig control service must be running

### First Time Setup

#### 1. Verify Station Configuration

Navigate to **Settings → Stations** and verify your station has rig control enabled:

-  **Rig Control Enabled** checkbox is checked
- **Rig Control Host** is set (usually `localhost` or the server IP)
- **Rig Control Port** is set (default: `8081`)

If these are not set, contact your system administrator.

#### 2. Navigate to a Page with Rig Control

Rig control is available on several pages:
- **QSO Entry** - Control rig while logging contacts
- **Contest Logging** - Real-time frequency/mode during contests
- **Station Dashboard** - Monitor and control your station

Look for the **Rig Control Panel** (usually on the right side or bottom of the page).

## Station Configuration

### For Administrators

To enable rig control for a station:

1. Navigate to **Settings → Stations**
2. Select the station to edit
3. Enable rig control:
   ```
   Rig Control Enabled: 
   Rig Control Host: localhost (or IP address of rig control service)
   Rig Control Port: 8081
   ```
4. Save the station

### Station Information Required

Each station needs:
- **Station Name** - Unique identifier (e.g., "Field Day Station 1")
- **Callsign** - Station callsign
- **Grid Square** - Maidenhead grid locator (for location-based features)
- **Rig Control Settings** - Host and port of rig control service

## Using Rig Control

### Connecting to Your Rig

1. **Locate the Rig Control Panel** on your page
2. **Click "Connect to Rig"** button
3. Wait for confirmation: "Connected to rig control service"
4. You should see real-time status updates appear

#### What You'll See After Connecting

The rig control panel displays:

```
┌─────────────────────────────────────┐
│  Rig Control - Field Day Station 1  │
├─────────────────────────────────────┤
│  Status: Connected                  │
│  Frequency: 14.250 MHz              │
│  Mode: USB                          │
│  PTT: OFF                           │
│  S-Meter: S5                        │
├─────────────────────────────────────┤
│  Set Frequency: [14.250] [Set]      │
│  Set Mode: [USB ▼] [Set]            │
│  PTT: [OFF] ← Click to transmit     │
├─────────────────────────────────────┤
│  Recent Events:                     │
│  • Connected to rig                 │
│  • Frequency changed to 14.250 MHz  │
└─────────────────────────────────────┘
```

### Controlling Your Rig

#### Change Frequency

1. **Enter frequency in MHz** in the frequency field
   - Example: `14.250` for 14.250 MHz
   - Example: `7.200` for 7.200 MHz
2. **Click "Set"** button
3. Your rig will change frequency
4. All connected users will see the update

#### Change Mode

1. **Select mode** from dropdown:
   - USB, LSB, CW, FM, AM, RTTY, etc.
2. **Click "Set"** button
3. Your rig will change mode
4. All connected users will see the update

#### Activate PTT (Transmit)

1. **Click the PTT button** (or "PTT ON")
2. Button turns **RED** indicating transmission
3. **Click again** to stop transmitting (or "PTT OFF")
4. Button returns to normal state

 **Important:** Only one user can hold PTT at a time (see Multi-User Operations below)

### Auto-Populating QSO Fields

When logging a QSO, you can auto-populate fields from your rig:

1. **Ensure rig control is connected**
2. **Click "Sync from Rig"** button (if available)
3. Frequency and mode fields will update automatically

Some pages may auto-populate continuously as you tune your rig.

### Disconnecting

1. **Click "Disconnect"** button
2. Rig control panel will return to disconnected state
3. Your rig settings remain unchanged

## Multi-User Operations

### How Multi-User Works

Multiple operators can connect to the same station simultaneously:

-  **All users see real-time updates** (frequency, mode, S-meter)
-  **All users can change frequency and mode**
-  **Events are broadcast to everyone** ("User A changed frequency")
-  **Only ONE user can hold PTT** at a time (safety feature)

### PTT Locking Explained

PTT (Push-To-Talk) locking prevents multiple users from transmitting at the same time.

#### How It Works

**First-Come-First-Served:**
- First user to activate PTT gets the lock
- Other users cannot transmit while lock is held
- Lock is automatically released when PTT is deactivated
- Lock is also released if the user disconnects

#### Example Scenario

**Field Day with 3 Operators:**

1. **Operator A** (logging) connects to "Field Day Station 1"
2. **Operator B** (band captain) connects to same station
3. **Operator C** (relief operator) connects to same station

**All three see real-time status updates every 100ms**

**Operator A wants to transmit:**
- Clicks "PTT ON"
-  **SUCCESS** - Button turns RED
- Event: "PTT activated by Operator A"
- Operators B and C see this event

**Operator B tries to transmit (while A is transmitting):**
- Clicks "PTT ON"
-  **DENIED** - Error message appears
- Message: "PTT denied: currently held by Operator A"
- Event: "PTT denied for Operator B"

**Operator A finishes transmitting:**
- Clicks "PTT OFF"
- PTT lock released
- Event: "PTT released by Operator A"
- Now Operator B or C can activate PTT

### Best Practices for Multi-User

1. **Coordinate PTT usage** - Use voice/chat to coordinate who's transmitting
2. **Watch the events panel** - See who's doing what
3. **Be patient** - If PTT is denied, wait for the other operator to release
4. **Use the S-meter** - Monitor signal strength together
5. **Disconnect when done** - Auto-releases PTT for others

## Troubleshooting

### "Failed to connect to rig control service"

**Possible Causes:**
- Rig control service is not running
- Network connectivity issue
- Incorrect host/port configuration

**Solutions:**
1. Verify service is running: Contact your administrator
2. Check station configuration: Settings → Stations
3. Try refreshing the page
4. Check browser console for errors (F12 → Console)

### "Station not connected to rig control service"

**Cause:** You haven't clicked "Connect to Rig" yet

**Solution:** Click the "Connect to Rig" button in the rig control panel

### "PTT denied: held by [Other User]"

**Cause:** Another user is currently transmitting

**Solutions:**
1. Wait for the other user to release PTT
2. Coordinate with the other user (voice/chat)
3. Check the events panel to see who has PTT
4. If stuck, disconnect and reconnect (forces PTT release after timeout)

### No Status Updates Appearing

**Possible Causes:**
- WebSocket connection dropped
- Rig control service lost connection to rigctld
- Radio is off or disconnected

**Solutions:**
1. Disconnect and reconnect
2. Refresh the page
3. Verify radio is powered on
4. Contact administrator to check rigctld status

### Commands Timing Out

**Possible Causes:**
- rigctld is not running
- Radio is not responding
- Serial/USB connection issue

**Solutions:**
1. Check physical radio connection
2. Verify radio is powered on
3. Contact administrator to restart rigctld
4. Check radio is not in a locked mode

### Frequency/Mode Not Changing

**Possible Causes:**
- Radio is in VFO lock mode
- Radio doesn't support the requested mode
- Serial communication error

**Solutions:**
1. Check radio is not in lock mode
2. Try a different frequency/mode
3. Disconnect and reconnect
4. Check radio manual for supported modes

## Advanced Features

### Keyboard Shortcuts

Some pages may support keyboard shortcuts (if implemented):

- **Ctrl+T** - Toggle PTT
- **Ctrl+F** - Focus frequency field
- **Ctrl+M** - Focus mode selector

Check the page's help section for available shortcuts.

### Real-Time Status Updates

The rig control system updates status **every 100 milliseconds** (10 times per second):

- Frequency changes are visible immediately
- Mode changes update in real-time
- S-meter readings update continuously
- PTT status shows instantly

This provides a near-real-time experience comparable to physical controls.

### Event History

The events panel shows recent rig control activity:

- Connection/disconnection events
- Frequency/mode changes
- PTT activation/release
- PTT denial events
- Error messages

Use this to:
- Track what other operators are doing
- Debug connectivity issues
- Audit rig control usage

### Integration with QSO Logging

Rig control integrates seamlessly with QSO logging:

**Auto-Population:**
- Frequency field updates as you tune
- Mode field updates when you change modes
- No manual entry needed

**One-Click Sync:**
- "Sync from Rig" button on QSO entry
- Instantly populates frequency/mode
- Saves time during rapid QSO logging

**Contest Logging:**
- Real-time frequency tracking
- Automatic band identification
- Duplicate detection based on current frequency

### Station Dashboard

If available, the station dashboard provides:

- **Multi-station overview** - See all stations at once
- **Comparative S-meter** - Compare signal strength across stations
- **Activity timeline** - Historical rig control events
- **Statistics** - Frequency changes, PTT usage, etc.

## Security and Permissions

### Who Can Use Rig Control?

Rig control access requires:
- Valid user account
- One of these roles:
  - `ROLE_USER` - Basic rig control
  - `ROLE_OPERATOR` - Full rig control
  - `ROLE_ADMIN` - Full rig control + configuration

### What's Logged?

For security and auditing:
- All rig control connections are logged
- PTT activation/release events are logged
- Frequency/mode changes are logged (with user ID)
- Failed PTT attempts are logged

Administrators can review logs to:
- Track station usage
- Debug issues
- Audit contest operations

### Privacy

- Your rig control actions are visible to other users connected to the same station
- Events show your username or station name
- This is intentional for coordination during multi-user operations

## Tips and Tricks

### Field Day Operations

**Best Setup:**
1. **GOTA Station** - All operators connect, coordinate PTT via voice
2. **Regular Stations** - Primary operator controls rig, others monitor
3. **Band Captain** - Monitors all stations via dashboard

**Coordination:**
- Use events panel to see which station is active
- Watch S-meter on all stations to compare propagation
- Sync frequencies when switching operators

### Contest Logging

**Workflow:**
1. Connect rig control before contest starts
2. Enable auto-populate for QSO fields
3. Tune rig physically OR via web interface
4. Log QSO with pre-populated frequency/mode
5. Repeat!

**Benefits:**
- No manual frequency entry (faster logging)
- Accurate frequency recording
- Real-time band tracking

### Remote Operations

If you're operating remotely:
1. Connect via VPN or secure network
2. Use rig control for frequency/mode
3. Use separate audio streaming for receive/transmit
4. PTT via rig control provides reliable keying

### Testing Without Radio

You can test rig control without a physical radio:

1. Administrator runs mock rigctld (see technical docs)
2. Connect via rig control panel
3. Commands will succeed (simulated responses)
4. Great for training or system testing

## Support and Help

### Getting Help

If you encounter issues:

1. **Check this guide** - Most common issues are covered
2. **Contact your administrator** - They can check service logs
3. **Report bugs** - Use the feedback/bug report feature
4. **Check documentation** - Technical docs available for admins

### Reporting Issues

When reporting rig control issues, include:

- **Station name** - Which station were you using?
- **Error message** - Exact text of any error
- **What you were doing** - Steps to reproduce
- **Browser** - Chrome, Firefox, Safari, etc.
- **Time** - When did it occur?

### Learning Resources

- **Hamlib Documentation** - Learn about rigctld capabilities
- **Radio Manual** - Understand your rig's remote control features
- **Contest Rules** - Know if rig control is allowed in your contest

## Appendix

### Supported Modes

Common modes supported (depends on your radio):

- **SSB:** USB, LSB
- **CW:** CW, CWR
- **Digital:** RTTY, PSK31, FT8, etc.
- **FM:** FM, NFM, WFM
- **AM:** AM

Check your radio's manual for full mode support.

### Frequency Ranges

Enter frequencies in **MHz** format:

| Band | Example Frequency |
|------|-------------------|
| 160m | 1.850 |
| 80m | 3.750 |
| 40m | 7.200 |
| 20m | 14.250 |
| 15m | 21.300 |
| 10m | 28.500 |
| 6m | 50.125 |
| 2m | 146.520 |

### Event Types

Events you may see in the panel:

| Event | Meaning |
|-------|---------|
| `client_connected` | A user connected to this station |
| `client_disconnected` | A user disconnected |
| `frequency_changed` | Frequency was changed |
| `mode_changed` | Mode was changed |
| `ptt_activated` | PTT turned on (transmitting) |
| `ptt_released` | PTT turned off (receiving) |
| `ptt_denied` | PTT request denied (held by another user) |
| `error` | An error occurred |

---

**Last Updated:** 2025-12-12
**Version:** 1.0
**For:** Ham Radio Logbook Users

Happy logging! 73! 
