# Ham Radio Logbook - User Guide

Complete guide for using the Ham Radio Contest Logbook system.

## Table of Contents

1. [Getting Started](#getting-started)
2. [User Interface](#user-interface)
3. [User Authentication](#user-authentication)
4. [Log Management](#log-management)
5. [Logging QSOs](#logging-qsos)
6. [Rig Control](#rig-control)
7. [Contest Operations](#contest-operations)
8. [Multi-User Collaboration](#multi-user-collaboration)
9. [Station Management](#station-management)
10. [Export and Import](#export-and-import)
11. [Maps and Visualization](#maps-and-visualization)
12. [Troubleshooting](#troubleshooting)

---

## Getting Started

### First Time Access

1. Navigate to the application URL (e.g., `http://localhost:4200`)
2. Click **Register** to create a new account
3. Fill in your details:
   - **Username**: Your unique username
   - **Password**: Secure password (min 8 characters)
   - **Callsign**: Your amateur radio callsign (optional but recommended)
4. Click **Create Account**
5. You'll be redirected to the login page

### Logging In

1. Enter your **username** and **password**
2. Click **Login**
3. You'll be redirected to the main dashboard

---

## User Interface

### EtherWave Archive Branding

The **EtherWave Archive** logo appears on all pages of the application:

- **Login Page**: Theme-aware logo at the top
- **Register Page**: Theme-aware logo at the top
- **Dashboard**: Logo in the navigation bar (clickable to return home)
- **Invitations Page**: Logo in the navigation bar with link to dashboard

The logo automatically adapts to your selected theme (light or dark mode) for optimal visibility.

### Navigation Bar

The main navigation bar appears at the top of authenticated pages and includes:

- **EtherWave Archive Logo**: Click to navigate to dashboard
- **Welcome Message**: Shows your username and callsign
- **Station Badge**: Displays your current station assignment (if applicable)
- **Dashboard Link**: Quick navigation (on Invitations page)
- **Theme Toggle**: Switch between dark and light modes
- **Logout Button**: Sign out of your account

### Theme Toggle (Dark/Light Mode)

The system supports both light and dark themes for comfortable viewing in any environment.

#### Switching Themes

1. Look for the theme toggle button in the navigation bar
2. Click the button showing:
   - **Moon icon** (Dark mode available) - Currently in light mode
   - **Sun icon** (Light mode available) - Currently in dark mode
3. The theme switches immediately across the entire application

#### Theme Features

- **System Detection**: Automatically detects your system preference on first visit
- **Persistence**: Your theme choice is saved and remembered across sessions
- **Instant Switch**: No page reload required
- **Logo Adaptation**: Logos automatically switch to theme-appropriate versions
- **Accessibility**: Both themes designed for optimal readability

#### Tips for Using Themes

- **Light Mode**: Best for daytime operation and well-lit environments
- **Dark Mode**: Reduces eye strain during night-time contests or low-light operation
- **Field Day**: Use dark mode to preserve night vision
- **Presentations**: Light mode for better visibility on projectors

---

## User Authentication

### User Roles

The system has two user types:

- **Regular User**: Can create personal logs, join shared logs, and log QSOs
- **Admin User**: Has all regular permissions plus system administration access

### Security Features

- **JWT Token Authentication**: Secure, stateless authentication
- **Password Encryption**: BCrypt hashing for password security
- **Session Persistence**: Your session remains active until you logout or the token expires (24 hours)

---

## Log Management

### Understanding Logs

A **Log** is a collection of QSOs for a specific purpose:

- **Personal Log**: Your private logbook for general operation
- **Shared Log**: Multi-operator log for contests or club stations

### Creating a New Log

Logs come in two types with distinct creation paths:

**Personal Log** — private, single-user only. No participants, never public.

1. Click the **Log Dropdown** at the top of the dashboard
2. Click **New Personal Log** at the bottom of the "My Personal Logs" section
3. Fill in the log details:
   - **Name**: Descriptive name (e.g., "General 2024")
   - **Description**: Optional details
   - **Purpose**: Select the operating activity (Field Day, POTA, SOTA, etc.)
4. Click **Create Log**

**Shared Log** — multi-operator, can be made public for viewers.

1. Click the **Log Dropdown** at the top of the dashboard
2. Click **New Shared Log** at the bottom of the "Shared Logs" section
3. Fill in the log details:
   - **Name**: Descriptive name (e.g., "ARRL Field Day 2024")
   - **Description**: Optional details
   - **Purpose**: Select the operating activity
   - **Public**: Check if you want anyone to view (read-only access)
4. Click **Create Log**

**Note**: Personal logs can be converted to Shared logs later if you need to invite participants. This conversion is one-way.

### Selecting a Log

All QSO operations are log-specific. You must select a log before logging QSOs:

1. Click the **Log Dropdown** at the top
2. The dropdown shows two sections: **My Personal Logs** and **Shared Logs**
3. Click the log you want to work with
4. The log name and type badge will display in the dropdown button
5. All dashboard components now operate within this log context

### Log Information Display

The log dropdown shows logs in two labeled sections:

**My Personal Logs** — your private logs:
- **Log Name**
- **Purpose Badge**: Shown if not General (e.g., POTA, SOTA, Field Day)
- **QSO Count**: Total contacts in the log

**Shared Logs** — multi-operator logs you created or were invited to:
- **Log Name**
- **Invited Badge**: Shown if you didn't create the log
- **Purpose Badge**: Shown if not General
- **Your Role Badge**: CREATOR, STATION, or VIEWER
- **Participant Count**: Number of operators
- **QSO Count**: Total contacts in the log

### Managing Your Logs

#### Editing a Log
1. Select the log from the dropdown
2. Navigate to log settings (if you're the CREATOR)
3. Update details
4. Click **Save**

#### Deleting a Log
1. Select the log from the dropdown
2. Click the **Delete** icon (trash can) next to the log name
3. Confirm deletion
4. **Note**: Only log CREATORs can delete logs

#### Leaving a Shared Log
1. Select the log you want to leave
2. Click the **Leave** icon (arrow exit) next to the log name
3. Confirm you want to leave
4. **Note**: Log CREATORs cannot leave; they must delete the log instead

### Freezing Logs

Logs can be frozen to prevent further edits:

- **Manual Freeze**: CREATOR can manually freeze a log
- **Auto-Freeze**: Logs automatically freeze after the contest end date/time
- **Purpose**: Preserve contest logs for submission, prevent accidental changes

**To freeze a log:**
1. Navigate to log settings (CREATOR only)
2. Click **Freeze Log**
3. Log becomes read-only for all participants

**To unfreeze a log:**
1. Navigate to log settings (CREATOR only)
2. Click **Unfreeze Log**
3. Log becomes editable again (unless past end date)

---

## Logging QSOs

### QSO Entry Form

The QSO entry form is on the main dashboard (left side):

#### Required Fields
- **Callsign**: Station contacted
- **Frequency**: In kHz (e.g., 14250.5)
- **Mode**: Operating mode (SSB, CW, FT8, etc.)
- **Date**: QSO date
- **Time On**: Start time (UTC)
- **RST Sent**: Signal report sent
- **RST Received**: Signal report received

#### Optional Fields
- **Time Off**: End time for long QSOs
- **Band**: Automatically calculated from frequency
- **Power**: Transmit power in watts
- **Station**: Select your station configuration
- **Operator**: Select operator if different from logged-in user
- **Contest**: Select if contest QSO
- **Name**: Other station's operator name
- **QTH**: Other station's location
- **Grid Square**: Maidenhead grid locator
- **State/Province**: US state or Canadian province
- **County**: For state QSO parties
- **Country**: DXCC entity
- **Notes**: Any additional comments

#### Contest-Specific Fields
When a contest is selected, additional fields appear:
- **Exchange Sent**: Your contest exchange
- **Exchange Received**: Their contest exchange
- **Serial Number**: QSO sequence number
- **Section/Zone/Park**: Depending on contest type

### Creating a QSO

1. Select your active log from the dropdown
2. Fill in the QSO entry form
3. Click **Log QSO**
4. QSO is validated and saved
5. QSO appears in the QSO list below
6. Form is cleared for next contact

### Rig Control Integration

If rig control is connected:
- **Frequency** and **Mode** auto-populate from your radio
- Click **Get from Rig** to refresh values
- Real-time updates appear as you tune

### QSO Validation

The system automatically validates QSOs:

- **Green indicator**: Valid QSO
- **Yellow indicator**: Warnings (e.g., unusual frequency for mode)
- **Red indicator**: Errors (e.g., invalid exchange for contest)
- **Validation messages**: Hover over indicator to see details

### Duplicate Detection

For contest logs, the system checks for duplicates:
- Same callsign, same band, same mode
- Warning displayed if duplicate detected
- Contest rules determine if duplicates are allowed

### Editing a QSO

1. Find the QSO in the QSO List
2. Click the **Edit** icon (pencil)
3. Modify fields
4. Click **Update**
5. **Note**: Only CREATOR and STATION roles can edit
6. **Note**: Cannot edit QSOs in frozen logs

### Deleting a QSO

1. Find the QSO in the QSO List
2. Click the **Delete** icon (trash can)
3. Confirm deletion
4. **Note**: Only CREATOR and STATION roles can delete
5. **Note**: Cannot delete QSOs from frozen logs

### QSO List Features

The QSO list displays all QSOs in the current log:

- **Pagination**: 20 QSOs per page (configurable)
- **Sorting**: Click column headers to sort
- **Filtering**: Search/filter by callsign, date, band, mode
- **Recent QSOs**: Live feed of most recent contacts
- **Export Selection**: Select QSOs for targeted export

---

## Rig Control

### Setting Up Rig Control

Rig control requires the Hamlib rigctld service:

1. **Docker Installation** (Recommended):
   ```bash
   docker run -d --name rigctld \
     --device=/dev/ttyUSB0 \
     -p 4532:4532 \
     hamradio/rigctld --model=123 --rig-file=/dev/ttyUSB0
   ```

2. **Manual Installation**:
   - Install Hamlib on your system
   - Run: `rigctld -m 123 -r /dev/ttyUSB0`
   - Replace `123` with your radio model number

### Rig Status Panel

The **Rig Status** panel (top right) displays:
- **Connection Status**: Connected or Disconnected
- **Frequency**: Current VFO frequency
- **Mode**: Current operating mode
- **Connection Info**: rigctld host and port

### Using Rig Control

When connected:
1. **Auto-populate**: Frequency and mode automatically fill in QSO entry form
2. **Real-time Updates**: As you tune, the form updates
3. **Quick Log**: Focus on callsign and exchange, rig fills the rest

### Troubleshooting Rig Control

**Not Connecting?**
- Check rigctld is running: `ps aux | grep rigctld`
- Verify port is accessible: `telnet localhost 4532`
- Check radio is powered on and connected
- Verify USB/serial permissions

**Disconnects Frequently?**
- Check USB cable quality
- Verify radio CAT settings match rigctld configuration
- Check system logs for USB errors

---

## Contest Operations

### Selecting a Contest

1. In the **Contest Selection** panel, browse or search contests
2. Click on a contest to view details
3. Click **Select Contest** to activate
4. Contest-specific fields appear in QSO entry form
5. QSO validation rules switch to contest rules

### Supported Contests

The system includes validators for:
- **CQ World Wide DX Contest** (CW and SSB) — exchange: RST + CQ Zone
- **ARRL Sweepstakes** — exchange: serial, precedence, check, section
- **ARRL Field Day** — exchange: class, section
- **Winter Field Day** — exchange: class (indoor/outdoor/home), section
- **Parks on the Air (POTA)** — exchange: park reference
- **Summits on the Air (SOTA)** — exchange: summit reference
- **State QSO Parties** — exchange: serial, state/province

### Contest-Specific Features

#### ARRL Field Day
- **Class**: Station class (e.g., 2A, 1B)
- **Section**: ARRL section
- **Bonus Points**: Tracked automatically

#### Parks on the Air (POTA)
- **Park Reference**: Park identifier (e.g., K-0001)
- **Park Name**: Auto-populated from reference
- **Park Validation**: Ensures valid park codes

#### CQ WW DX
- **CQ Zone**: Your CQ zone
- **Zone Received**: Their CQ zone
- **Multiplier Tracking**: Countries and zones

### Contest Scoring

Real-time scoring displays:
- **Total QSOs**: Contact count
- **Points**: Based on contest rules
- **Multipliers**: Countries, states, zones, sections
- **Score**: QSO points × multipliers

### Dupe Checking

Automatic duplicate detection for contests:
- Highlights duplicate contacts
- Shows previous QSO details
- Follows contest-specific dupe rules (some allow dupes on different bands)

---

## Multi-User Collaboration

### Understanding Roles

When you're added to a shared log, you have one of three roles:

#### CREATOR
- Full control over the log
- Can edit log settings
- Can invite/remove participants
- Can freeze/unfreeze log
- Can create/edit/delete QSOs
- Can export log data

#### STATION
- Can create/edit QSOs
- Can edit own QSOs only (optional setting)
- Cannot modify log settings
- Cannot manage participants
- Can view all QSOs in the log

#### VIEWER
- Read-only access
- Can view all QSOs
- Can view statistics
- Cannot create or edit QSOs
- Cannot modify log settings

### Accessing the Invitations Page

The Invitations page provides a central location for managing all log invitations:

**Navigation:**
- From the Dashboard: Look for an **Invitations** link or button
- Direct URL: Navigate to `/invitations` when logged in
- Badge Indicator: Shows number of pending invitations you've received

**Page Features:**
- **EtherWave Archive Logo**: Click to return to dashboard
- **Welcome Message**: Shows your username and callsign
- **Theme Toggle**: Switch between dark and light modes
- **Logout Button**: Sign out of your account
- **Two Tabs**: Received and Sent invitations

### Sending Invitations

To invite someone to your log (CREATOR only):

1. Navigate to the **Invitations** page
2. Switch to the **Sent** tab
3. Click **Send Invitation** button
4. Fill in invitation details:
   - **Log**: Select which log to invite them to (must be a log you created)
   - **Username/Email/Callsign**: Enter their identifier
   - **Role**: Choose STATION or VIEWER
   - **Station Callsign** (optional): For multi-op contests
   - **Message** (optional): Personal note
5. Click **Send Invitation**
6. They'll receive the invitation in their account
7. Invitation appears in your **Sent** tab with status tracking

### Receiving Invitations

When you receive an invitation:

1. Navigate to the **Invitations** page (look for badge with pending count)
2. View pending invitations in the **Received** tab (default view)
3. Review invitation details for each invitation:
   - **Log Name**: The log you're being invited to
   - **From**: Who sent the invitation
   - **Role**: Proposed role (STATION or VIEWER)
   - **Station Callsign**: If specified
   - **Message**: Personal message from inviter
   - **Sent Date**: When invitation was sent
   - **Expires Date**: When invitation expires (if applicable)
4. Click **Accept** or **Decline** for each invitation
5. If accepted, log appears in your log list immediately
6. Accepted/declined invitations are removed from the pending list

### Managing Participants

The **Participant Management** panel appears on the dashboard for shared logs and provides complete control over log membership.

#### Viewing Participants (All Roles)

All participants can view the participant list:

1. Select a shared log from the dropdown
2. Locate the **Participant Management** panel on the dashboard
3. View list showing:
   - **Username**: Participant's username
   - **Callsign**: Their amateur radio callsign (if set)
   - **Role**: CREATOR, STATION, or VIEWER badge
   - **Station Assignment**: Their current station (color-coded badge)

#### Managing Participants (CREATOR Only)

As the log CREATOR, you have full control:

**Viewing Participant Details:**
1. Navigate to **Participant Management** panel
2. See all participants in the log
3. Each entry shows:
   - User information (username, callsign)
   - Current role with badge
   - Current station assignment with colored badge
   - Actions available

**Removing Participants:**
1. Find the participant in the list
2. Click the **Remove** button (trash icon) next to their name
3. Confirm removal
4. Participant loses access immediately
5. Their QSOs remain in the log
6. They can be re-invited later if needed

**Station Assignment:**
(See [Station Management](#station-management) section for detailed information)

#### Participant Roles in the Panel

**What Each Role Sees:**

- **CREATOR**:
  - Can view all participants
  - Can remove participants
  - Can assign/reassign stations
  - Cannot remove themselves (must delete log instead)

- **STATION**:
  - Can view all participants
  - Cannot remove anyone
  - Cannot change station assignments
  - View-only access to participant list

- **VIEWER**:
  - Can view all participants
  - Cannot remove anyone
  - Cannot change station assignments
  - View-only access to participant list

#### Participant Status Indicators

The panel uses color-coded badges for quick identification:

**Role Badges:**
- **CREATOR**: Blue badge
- **STATION**: Green badge
- **VIEWER**: Gray badge

**Station Badges:**
- **Station 1-10**: Numbered with unique colors
- **GOTA**: Gold badge
- **Unassigned**: Gray badge

#### Best Practices for Participant Management

1. **Assign Roles Carefully**: Give STATION role only to operators who will log QSOs
2. **Use VIEWER for Observers**: Friends, family, and non-operators
3. **Regular Review**: Check participant list before major contests
4. **Remove Inactive Users**: Clean up after events to maintain security
5. **Document Station Assignments**: Keep a record of who operates which station

### Cancelling Invitations

To cancel a sent invitation (CREATOR only):

1. Navigate to the **Invitations** page
2. Switch to **Sent** tab
3. View list of all invitations you've sent
4. Find the pending invitation (status shows as PENDING)
5. Click **Cancel** button next to the invitation
6. Confirm cancellation
7. Invitation is revoked immediately
8. Invitee can no longer accept it
9. Status changes to CANCELLED in your sent list

**Note**: You can only cancel invitations that are still PENDING. Accepted or declined invitations cannot be cancelled.

### Collaborative Logging

In shared logs:
- **Real-time Updates**: QSOs appear instantly for all users via WebSocket
- **Dupe Prevention**: System prevents simultaneous duplicate logging
- **Activity Feed**: See who's logging what in real-time
- **Station Identification**: Each QSO tagged with logging operator

---

## Station Management

### Understanding Station Assignments

For multi-operator contest stations, the system supports station assignment to organize operators across multiple transmitting stations.

#### Station Types

1. **Numbered Stations** (Station 1-10)
   - Primary operating positions
   - Each has a unique color-coded badge
   - Used for multi-transmitter categories (e.g., Multi-Multi, Multi-Two)

2. **GOTA Station** (Get On The Air)
   - Special educational station for Field Day and similar events
   - Dedicated badge color
   - For new or unlicensed operators under supervision

3. **Unassigned**
   - Participants without station assignment
   - Gray badge
   - Can log QSOs but not station-specific

### Station Color Coding

Each station has a unique color for easy identification:

- **Station 1**: Red
- **Station 2**: Blue
- **Station 3**: Green
- **Station 4**: Orange
- **Station 5**: Purple
- **Station 6**: Teal
- **Station 7**: Pink
- **Station 8**: Brown
- **Station 9**: Cyan
- **Station 10**: Yellow
- **GOTA**: Gold
- **Unassigned**: Gray

These colors appear in:
- Navigation bar station badges
- Participant management list
- QSO entry indicators
- Export filters

### Assigning Stations (CREATOR Only)

To assign operators to stations:

1. Select the shared log from the dropdown
2. Navigate to the **Station Management** panel
3. View list of all participants
4. For each participant:
   - Click the station dropdown next to their name
   - Select station number (1-10), GOTA, or Unassigned
   - Click **Update**
5. Changes take effect immediately
6. Participant sees their station badge in the navigation bar

### Station Assignment Best Practices

#### Field Day
- **Station 1-N**: One per transmitter (based on class, e.g., 2A = 2 stations)
- **GOTA**: Assign to GOTA operators
- **Unassigned**: Loggers, spotters, support crew

#### Multi-Multi Contests
- **Station 1-6**: Assign one per band (e.g., Station 1 = 20m, Station 2 = 40m)
- Coordinate band assignments beforehand
- Use station colors for quick visual identification

#### Multi-Two Contests
- **Station 1**: Run station
- **Station 2**: Multiplier station
- Clear role definition prevents confusion

### Station-Based Operations

#### Viewing Your Station Assignment

Your current station assignment appears in the navigation bar:
- **Badge**: Colored badge with station name
- **Color**: Unique color for quick identification
- **Visibility**: Shows on all pages while logged in

#### Logging QSOs by Station

When logging a QSO:
- Your station assignment is automatically recorded
- QSO is tagged with your station for filtering and reporting
- Other operators see which station logged each QSO

#### Filtering by Station

In the QSO list:
- Filter to see only QSOs from specific stations
- Useful for per-station score checking
- Helps identify band/station conflicts

### Station Callsigns

Some contests require station-specific callsigns (e.g., W1ABC/1, W1ABC/2):

1. When assigning a station, you can optionally specify a **Station Callsign**
2. This callsign appears in exports and Cabrillo files
3. Useful for multi-transmitter categories requiring unique call signs

### Reassigning Stations

Stations can be reassigned at any time (CREATOR only):

1. Navigate to **Station Management**
2. Change the station dropdown for the participant
3. Click **Update**
4. Previous QSOs retain original station assignment
5. New QSOs use the updated station

**Note**: Reassignment is useful for operator rotation or band changes during multi-day events.

### Station Indicators in QSO List

The QSO list shows which station logged each QSO:
- **Color Dot**: Station color indicator
- **Station Column**: Station name (if enabled)
- **Operator Column**: Which user logged it
- **Sortable**: Click to sort by station

### Multi-Station Scoring

For contests with per-station scoring:
- View total QSOs per station
- Check score breakdown by station
- Identify underperforming stations
- Balance operator assignments

### Removing Station Assignments

To remove a station assignment:

1. Navigate to **Station Management**
2. Select the participant
3. Choose **Unassigned** from station dropdown
4. Click **Update**
5. Participant's badge changes to gray "Unassigned"

---

## Export and Import

### Exporting QSOs

The **Export Panel** (bottom right) provides export options:

#### ADIF Export
1. Select export format: **ADIF**
2. Choose options:
   - **All QSOs**: Export entire log
   - **Date Range**: Export specific period
   - **Contest Only**: Export contest QSOs
   - **Station Filter**: Export QSOs from specific station (for shared logs)
3. Click **Export**
4. File downloads: `logname_YYYYMMDD.adi`

**ADIF Uses:**
- Import into other logging software
- Submit to LoTW (Logbook of the World)
- Share with QSL managers

#### Cabrillo Export
1. Select export format: **Cabrillo**
2. Fill in required contest info:
   - **Operator(s)**: Callsign(s)
   - **Category**: Contest category
   - **Overlay**: Optional overlay category
   - **Club**: Club affiliation
3. Click **Export**
4. File downloads: `logname_cabrillo.txt`

**Cabrillo Uses:**
- Submit contest logs to sponsors
- Official contest scoring
- Post-contest analysis

### Importing QSOs

To import QSOs from ADIF:

1. Navigate to **Import** section
2. Click **Choose File**
3. Select ADIF file (.adi or .adif)
4. Choose import options:
   - **Log**: Select destination log
   - **Validate**: Re-validate all QSOs
   - **Duplicate Handling**: Skip or merge
5. Click **Import**
6. Review import summary:
   - QSOs imported
   - Duplicates skipped
   - Validation errors
7. Click **Confirm**

---

## Maps and Visualization

### QSO Map

The **Map Visualization** panel shows:
- **Contacted Stations**: Plotted by grid square or coordinates
- **Beam Heading**: Lines from your QTH to contact
- **State/Province Boundaries**: US/Canada outlines
- **Zoom Controls**: Interactive pan and zoom

### Features

#### State/Province Heatmap
- Color-coded by QSO count
- Darker colors = more QSOs
- Click state to see QSO list

#### Grid Square Overlay
- Maidenhead grid squares
- QSO count per grid
- Highlight rare grids

#### Filters
- Filter by band
- Filter by mode
- Filter by date range
- Filter by contest

### Statistics Dashboard

View detailed statistics:
- **Total QSOs**: By log, all-time, or date range
- **Unique Callsigns**: Worked
- **Countries**: DXCC entities confirmed
- **States/Provinces**: WAS progress
- **Grids**: Grid square count
- **Bands**: QSO distribution by band
- **Modes**: QSO distribution by mode

---

## Troubleshooting

### Common Issues

#### Cannot Log QSOs

**Problem**: "User does not have permission to add QSOs to this log"

**Solution**:
- Check your role in the log (must be CREATOR or STATION)
- Verify log is not frozen
- Ensure you're logged in

---

#### Log is Frozen

**Problem**: "This log is frozen and cannot be edited"

**Solutions**:
- If you're the CREATOR, unfreeze the log in settings
- Check if contest end date has passed (auto-freeze)
- Contact log creator if you need changes

---

#### Rig Control Not Working

**Problem**: Frequency and mode not auto-filling

**Solutions**:
1. Check rigctld is running:
   ```bash
   ps aux | grep rigctld
   ```

2. Test rigctld connection:
   ```bash
   telnet localhost 4532
   f  # Should return frequency
   m  # Should return mode
   ```

3. Verify configuration in application settings

4. Check radio CAT interface is enabled

5. Restart rigctld service

---

#### Validation Errors

**Problem**: QSO marked as invalid

**Solutions**:
- Hover over validation indicator to see error message
- Common issues:
  - Invalid exchange for contest
  - Frequency outside band limits
  - Missing required fields
  - Invalid grid square format
- Correct the error and update QSO

---

#### Cannot Accept Invitation

**Problem**: "User is already a participant of this log"

**Solution**:
- You're already a member; check your log list
- Someone may have already added you directly

---

#### QSOs Not Appearing

**Problem**: QSOs don't show in list after logging

**Solutions**:
1. Refresh the page
2. Check you have the correct log selected
3. Check date range filter in QSO list
4. Verify WebSocket connection (look for connection status)

---

### Getting Help

If you encounter issues not covered here:

1. **Check Logs**: Browser console (F12) for frontend errors
2. **Backend Logs**: Check server logs for detailed errors
3. **Contact Support**: Open an issue on GitHub with:
   - Steps to reproduce
   - Error messages
   - Browser and OS information
   - Screenshots if applicable

---

## Best Practices

### For Contest Operators

1. **Create Contest-Specific Log**: One log per contest
2. **Set Start/End Dates**: Enable auto-freeze after contest
3. **Select Contest**: Before starting to activate validation
4. **Use Rig Control**: Speeds up logging significantly
5. **Check Dupes**: Review dupe warnings before final submission
6. **Export Cabrillo**: Before contest deadline
7. **Backup ADIF**: Keep ADIF backup of all contests

### For Multi-Op Stations

1. **Define Roles Clearly**: Assign STATION to operators, VIEWER to observers
2. **Use Station Callsigns**: For multi-transmitter contests
3. **Monitor Activity**: Watch real-time feed for coordination
4. **Freeze After Contest**: Prevent accidental changes
5. **Review Together**: Use viewer role for post-contest review

### For Personal Logging

1. **Single Personal Log**: Keep all general QSOs in one log
2. **Separate Contest Logs**: Create new log for each contest
3. **Regular Exports**: Export ADIF monthly for backup
4. **Upload to LoTW**: Export and upload to Logbook of the World
5. **Check QRZ Lookups**: Verify callsign data is accurate

---

## Keyboard Shortcuts

Speed up logging with keyboard shortcuts:

- **Ctrl/Cmd + L**: Focus on callsign field
- **Ctrl/Cmd + S**: Save current QSO
- **Ctrl/Cmd + R**: Get from rig
- **Tab**: Move to next field
- **Shift + Tab**: Move to previous field
- **Enter**: Submit QSO (when callsign field focused)

---

## Tips and Tricks

### Rapid Fire Logging

For fast-paced contests:
1. Connect rig control
2. Use Tab to move through fields
3. Let rig fill frequency/mode
4. Focus on callsign and exchange only
5. Hit Enter to log and move to next

### Search and Pounce

1. Use rig control to track frequency
2. Watch dupe checker
3. Log QSOs as you tune
4. Review QSO list for needed multipliers

### Running

1. Stay on one frequency
2. Manual frequency entry
3. Quick callsign/exchange logging
4. Monitor QSO rate in statistics

### Pre-Contest Preparation

1. Create log 24 hours before
2. Configure station and operator
3. Test rig control
4. Select contest
5. Make test QSO to verify
6. Delete test QSO
7. Ready to go at contest start!

---

## Frequently Asked Questions

**Q: Can I use this offline?**
A: Not currently. The system requires network access to the backend server. Future versions may support offline operation with sync.

**Q: How many QSOs can a log hold?**
A: No hard limit. Tested with 10,000+ QSOs per log without performance issues.

**Q: Can I import my old logs?**
A: Yes! Export from your old software as ADIF, then import into this system.

**Q: Is my data backed up?**
A: The database is backed up according to your deployment. Export ADIF regularly for personal backups.

**Q: Can I use multiple radios?**
A: Yes, each user can run their own rig control container for their radio.

**Q: How do I submit contest logs?**
A: Export as Cabrillo format and submit to the contest sponsor's website.

**Q: Can viewers see live logging during a contest?**
A: Yes! Grant VIEWER role to friends/family to watch your contest live.

**Q: How secure is the system?**
A: JWT authentication, BCrypt password hashing, and permission checks on all operations ensure security.

---

## Glossary

- **ADIF**: Amateur Data Interchange Format - standard for QSO data
- **Cabrillo**: Contest log submission format
- **CQ Zone**: CQ Magazine's WAZ zone system (40 zones)
- **DXCC**: DX Century Club - award for contacting 100 countries
- **Grid Square**: Maidenhead locator system for geographic coordinates
- **Hamlib**: Hardware abstraction library for radio control
- **ITU Zone**: International Telecommunication Union zones (90 zones)
- **LoTW**: Logbook of the World - ARRL's electronic QSL system
- **QRZ**: Callsign lookup database (QRZ.com)
- **QSO**: Two-way radio contact
- **QTH**: Location/station
- **rigctld**: Hamlib's radio control daemon
- **RST**: Readability-Signal-Tone report
- **UTC**: Coordinated Universal Time (Zulu time)
- **WebSocket**: Protocol for real-time bidirectional communication
