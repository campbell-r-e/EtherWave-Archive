# Ham Radio Logbook - API Reference

Complete REST API documentation for the Ham Radio Contest Logbook system.

## Table of Contents

1. [Base URL and Authentication](#base-url-and-authentication)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Log Management Endpoints](#log-management-endpoints)
4. [Invitation Endpoints](#invitation-endpoints)
5. [QSO Endpoints](#qso-endpoints)
6. [Station Endpoints](#station-endpoints)
7. [Contest Endpoints](#contest-endpoints)
8. [Export Endpoints](#export-endpoints)
9. [Map Endpoints](#map-endpoints)
10. [Award Tracking Endpoints](#award-tracking-endpoints)
11. [DX Cluster Endpoints](#dx-cluster-endpoints)
12. [Propagation Endpoints](#propagation-endpoints)
13. [LoTW Sync Endpoints](#lotw-sync-endpoints)
14. [User Preference Endpoints](#user-preference-endpoints)
15. [Rig Control Endpoints](#rig-control-endpoints)
16. [DXCC Endpoints](#dxcc-endpoints)
17. [Error Responses](#error-responses)
18. [WebSocket Topics](#websocket-topics)

---

## Base URL and Authentication

**Base URL**: `http://localhost:8080/api`

**Authentication**: JWT Bearer Token

All endpoints except `/auth/**` require authentication via JWT token:

```
Authorization: Bearer <your-jwt-token>
```

---

## Authentication Endpoints

### Register User

Create a new user account.

**Endpoint**: `POST /auth/register`

**Authentication**: None required

**Request Body**:
```json
{
  "username": "johndoe",
  "password": "SecurePassword123",
  "callsign": "W1ABC"
}
```

**Response**: `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "johndoe",
  "callsign": "W1ABC",
  "roles": ["ROLE_USER"]
}
```

**Validation Rules**:
- `username`: Required, 3-50 characters, alphanumeric + underscore
- `password`: Required, minimum 8 characters
- `callsign`: Optional, valid amateur radio callsign format

**Notes**:
- No email field — this system does not collect or store email addresses for authentication

**Error Responses**:
- `400 Bad Request`: Validation failure
- `409 Conflict`: Username already exists

---

### Login

Authenticate and receive JWT token.

**Endpoint**: `POST /auth/login`

**Authentication**: None required

**Request Body**:
```json
{
  "username": "johndoe",
  "password": "SecurePassword123"
}
```

**Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "johndoe",
  "callsign": "W1ABC",
  "roles": ["ROLE_USER"]
}
```

**Error Responses**:
- `401 Unauthorized`: Invalid credentials

---

## Log Management Endpoints

### Get All Logs for User

Retrieve all logs accessible to the authenticated user.

**Endpoint**: `GET /logs`

**Authentication**: Required

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "ARRL Field Day 2024",
    "description": "2A classification, emergency power",
    "type": "SHARED",
    "creatorId": 5,
    "creatorUsername": "johndoe",
    "contestId": 12,
    "contestName": "ARRL Field Day",
    "startDate": "2024-06-22T18:00:00",
    "endDate": "2024-06-23T21:00:00",
    "active": true,
    "editable": true,
    "isPublic": false,
    "createdAt": "2024-06-01T10:00:00",
    "updatedAt": "2024-06-01T10:00:00",
    "userRole": "CREATOR",
    "participantCount": 3,
    "qsoCount": 427
  },
  {
    "id": 2,
    "name": "Personal Log",
    "description": "General operation",
    "type": "PERSONAL",
    "creatorId": 5,
    "creatorUsername": "johndoe",
    "contestId": null,
    "contestName": null,
    "startDate": null,
    "endDate": null,
    "active": true,
    "editable": true,
    "isPublic": false,
    "createdAt": "2024-01-15T12:00:00",
    "updatedAt": "2024-06-20T15:30:00",
    "userRole": "CREATOR",
    "participantCount": 1,
    "qsoCount": 1523
  }
]
```

**Fields**:
- `userRole`: User's role in this log (CREATOR, STATION, or VIEWER)
- `participantCount`: Number of active participants
- `qsoCount`: Total QSOs in this log
- `editable`: Whether log accepts edits (considers both freeze status and end date)

---

### Get Specific Log

Retrieve details of a single log.

**Endpoint**: `GET /logs/{logId}`

**Authentication**: Required

**Parameters**:
- `logId` (path): Log ID

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "ARRL Field Day 2024",
  "description": "2A classification, emergency power",
  "type": "SHARED",
  "creatorId": 5,
  "creatorUsername": "johndoe",
  "contestId": 12,
  "contestName": "ARRL Field Day",
  "startDate": "2024-06-22T18:00:00",
  "endDate": "2024-06-23T21:00:00",
  "active": true,
  "editable": true,
  "isPublic": false,
  "createdAt": "2024-06-01T10:00:00",
  "updatedAt": "2024-06-01T10:00:00",
  "userRole": "CREATOR",
  "participantCount": 3,
  "qsoCount": 427
}
```

**Error Responses**:
- `403 Forbidden`: User does not have access to this log
- `404 Not Found`: Log does not exist

---

### Create Log

Create a new logbook.

**Endpoint**: `POST /logs`

**Authentication**: Required

**Request Body**:
```json
{
  "name": "CQ WW DX SSB 2024",
  "description": "World-wide DX contest, SSB mode",
  "type": "SHARED",
  "contestId": 5,
  "startDate": "2024-10-26T00:00:00",
  "endDate": "2024-10-27T23:59:59",
  "isPublic": false
}
```

**Response**: `201 Created`
```json
{
  "id": 15,
  "name": "CQ WW DX SSB 2024",
  "description": "World-wide DX contest, SSB mode",
  "type": "SHARED",
  "creatorId": 5,
  "creatorUsername": "johndoe",
  "contestId": 5,
  "contestName": "CQ WW DX SSB",
  "startDate": "2024-10-26T00:00:00",
  "endDate": "2024-10-27T23:59:59",
  "active": true,
  "editable": true,
  "isPublic": false,
  "createdAt": "2024-06-20T14:30:00",
  "updatedAt": "2024-06-20T14:30:00",
  "userRole": "CREATOR",
  "participantCount": 1,
  "qsoCount": 0
}
```

**Validation Rules**:
- `name`: Required, max 100 characters
- `description`: Optional, max 500 characters
- `type`: Required, either "PERSONAL" or "SHARED"
- `contestId`: Optional, must exist in database
- `startDate`: Optional, ISO 8601 format
- `endDate`: Optional, must be after startDate
- `isPublic`: Optional, defaults to false

**Notes**:
- User who creates log is automatically assigned CREATOR role
- Log is created in active, editable state

---

### Update Log

Update log details.

**Endpoint**: `PUT /logs/{logId}`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID

**Request Body**:
```json
{
  "name": "CQ WW DX SSB 2024 - Updated",
  "description": "Multi-op, two radios",
  "type": "SHARED",
  "contestId": 5,
  "startDate": "2024-10-26T00:00:00",
  "endDate": "2024-10-27T23:59:59",
  "isPublic": true
}
```

**Response**: `200 OK`
```json
{
  "id": 15,
  "name": "CQ WW DX SSB 2024 - Updated",
  "description": "Multi-op, two radios",
  "type": "SHARED",
  "creatorId": 5,
  "creatorUsername": "johndoe",
  "contestId": 5,
  "contestName": "CQ WW DX SSB",
  "startDate": "2024-10-26T00:00:00",
  "endDate": "2024-10-27T23:59:59",
  "active": true,
  "editable": true,
  "isPublic": true,
  "createdAt": "2024-06-20T14:30:00",
  "updatedAt": "2024-06-20T15:45:00",
  "userRole": "CREATOR",
  "participantCount": 1,
  "qsoCount": 0
}
```

**Error Responses**:
- `403 Forbidden`: User is not the log creator
- `404 Not Found`: Log does not exist

---

### Delete Log

Delete a log (soft delete - sets active = false).

**Endpoint**: `DELETE /logs/{logId}`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID

**Response**: `204 No Content`

**Error Responses**:
- `403 Forbidden`: User is not the log creator
- `404 Not Found`: Log does not exist

**Notes**:
- This is a soft delete; log remains in database but inactive
- Associated QSOs are NOT deleted
- Log will not appear in user's log list

---

### Freeze Log

Freeze a log to prevent further edits.

**Endpoint**: `POST /logs/{logId}/freeze`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID

**Response**: `200 OK`
```json
{
  "id": 15,
  "name": "CQ WW DX SSB 2024",
  "editable": false,
  ...
}
```

**Error Responses**:
- `403 Forbidden`: User is not the log creator

**Notes**:
- Frozen logs cannot have QSOs added, edited, or deleted
- Only the creator can freeze/unfreeze logs
- Logs auto-freeze after contest end date

---

### Unfreeze Log

Unfreeze a log to allow edits.

**Endpoint**: `POST /logs/{logId}/unfreeze`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID

**Response**: `200 OK`
```json
{
  "id": 15,
  "name": "CQ WW DX SSB 2024",
  "editable": true,
  ...
}
```

**Error Responses**:
- `403 Forbidden`: User is not the log creator

**Notes**:
- Cannot unfreeze if current time is past log's end date

---

### Get Log Participants

Retrieve all participants for a log.

**Endpoint**: `GET /logs/{logId}/participants`

**Authentication**: Required

**Parameters**:
- `logId` (path): Log ID

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "logId": 15,
    "logName": "CQ WW DX SSB 2024",
    "userId": 5,
    "username": "johndoe",
    "userCallsign": "W1ABC",
    "role": "CREATOR",
    "stationCallsign": null,
    "stationNumber": 1,
    "isGota": false,
    "active": true,
    "joinedAt": "2024-06-20T14:30:00"
  },
  {
    "id": 2,
    "logId": 15,
    "logName": "CQ WW DX SSB 2024",
    "userId": 12,
    "username": "janedoe",
    "userCallsign": "K2XYZ",
    "role": "STATION",
    "stationCallsign": "W1ABC/2",
    "stationNumber": 2,
    "isGota": false,
    "active": true,
    "joinedAt": "2024-06-21T09:15:00"
  },
  {
    "id": 3,
    "logId": 15,
    "logName": "CQ WW DX SSB 2024",
    "userId": 18,
    "username": "newoperator",
    "userCallsign": "KD7NEW",
    "role": "STATION",
    "stationCallsign": null,
    "stationNumber": null,
    "isGota": true,
    "active": true,
    "joinedAt": "2024-06-21T10:00:00"
  }
]
```

**Fields**:
- `role`: User's role in the log (CREATOR, STATION, or VIEWER)
- `stationCallsign`: Optional station-specific callsign (e.g., W1ABC/2)
- `stationNumber`: Station assignment number (1-10, or null for unassigned)
- `isGota`: Boolean indicating if this is a GOTA (Get On The Air) station
- `active`: Whether participant is currently active in the log

**Error Responses**:
- `403 Forbidden`: User does not have access to this log

---

### Remove Participant

Remove a participant from a log.

**Endpoint**: `DELETE /logs/{logId}/participants/{participantId}`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID
- `participantId` (path): Participant ID

**Response**: `204 No Content`

**Error Responses**:
- `400 Bad Request`: Cannot remove log creator
- `403 Forbidden`: User is not the log creator
- `404 Not Found`: Participant does not exist

**Notes**:
- Cannot remove the log creator
- This is a soft delete (sets active = false)
- User loses all access to the log immediately

---

### Update Participant Station Assignment

Update a participant's station assignment in a log.

**Endpoint**: `PUT /logs/{logId}/participants/{participantId}/station`

**Authentication**: Required (CREATOR role only)

**Parameters**:
- `logId` (path): Log ID
- `participantId` (path): Participant ID

**Request Body**:
```json
{
  "stationNumber": 2,
  "isGota": false
}
```

**Alternative GOTA Assignment**:
```json
{
  "stationNumber": null,
  "isGota": true
}
```

**Unassigned Station**:
```json
{
  "stationNumber": null,
  "isGota": false
}
```

**Response**: `200 OK`
```json
{
  "id": 2,
  "logId": 15,
  "logName": "CQ WW DX SSB 2024",
  "userId": 12,
  "username": "janedoe",
  "userCallsign": "K2XYZ",
  "role": "STATION",
  "stationCallsign": "W1ABC/2",
  "stationNumber": 2,
  "isGota": false,
  "active": true,
  "joinedAt": "2024-06-21T09:15:00"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid station number (must be 1-10 or null)
- `403 Forbidden`: User is not the log creator
- `404 Not Found`: Participant does not exist

**Validation Rules**:
- `stationNumber`: Must be between 1-10 or null
- `isGota`: Boolean, true for GOTA station
- Cannot have both `stationNumber` and `isGota` set (either numbered station OR GOTA, not both)
- Setting `stationNumber` automatically sets `isGota` to false
- Setting `isGota` to true automatically sets `stationNumber` to null

**Station Assignment Rules**:
- **Numbered Stations (1-10)**: Primary operating positions
- **GOTA Station**: Educational station for Field Day (isGota = true, stationNumber = null)
- **Unassigned**: No station assignment (stationNumber = null, isGota = false)

**Notes**:
- Changes take effect immediately
- Previous QSOs retain their original station assignment
- New QSOs will use the updated station assignment
- Station assignments are used for filtering, scoring, and exports

---

### Leave Log

Remove yourself from a log.

**Endpoint**: `POST /logs/{logId}/leave`

**Authentication**: Required

**Parameters**:
- `logId` (path): Log ID

**Response**: `204 No Content`

**Error Responses**:
- `400 Bad Request`: Log creator cannot leave (must delete log instead)
- `403 Forbidden`: User is not a participant

**Notes**:
- Creator must delete the log instead of leaving
- You lose access immediately after leaving

---

## Invitation Endpoints

### Get Pending Invitations

Retrieve all pending invitations for the current user.

**Endpoint**: `GET /invitations/pending`

**Authentication**: Required

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "logId": 15,
    "logName": "CQ WW DX SSB 2024",
    "inviterId": 5,
    "inviterUsername": "johndoe",
    "inviteeId": 12,
    "inviteeUsername": "janedoe",
    "inviteeEmail": "jane@example.com",
    "inviteeCallsign": "K2XYZ",
    "proposedRole": "STATION",
    "stationCallsign": "W1ABC/2",
    "status": "PENDING",
    "message": "Would you like to join our multi-op team?",
    "createdAt": "2024-06-20T16:00:00",
    "expiresAt": "2024-06-25T23:59:59",
    "canRespond": true
  }
]
```

**Notes**:
- Only shows non-expired, pending invitations
- `canRespond`: Computed field indicating if invitation can be accepted/declined

---

### Get Sent Invitations

Retrieve all invitations sent by the current user.

**Endpoint**: `GET /invitations/sent`

**Authentication**: Required

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "logId": 15,
    "logName": "CQ WW DX SSB 2024",
    "inviterId": 5,
    "inviterUsername": "johndoe",
    "inviteeId": 12,
    "inviteeUsername": "janedoe",
    "inviteeEmail": "jane@example.com",
    "inviteeCallsign": "K2XYZ",
    "proposedRole": "STATION",
    "stationCallsign": "W1ABC/2",
    "status": "ACCEPTED",
    "message": "Would you like to join our multi-op team?",
    "createdAt": "2024-06-20T16:00:00",
    "expiresAt": null,
    "canRespond": false
  }
]
```

**Status Values**:
- `PENDING`: Awaiting response
- `ACCEPTED`: Invitee accepted
- `DECLINED`: Invitee declined
- `CANCELLED`: Inviter cancelled
- `EXPIRED`: Invitation expired

---

### Create Invitation

Send an invitation to join a log.

**Endpoint**: `POST /invitations`

**Authentication**: Required (CREATOR role in the log)

**Request Body**:
```json
{
  "logId": 15,
  "inviteeUsername": "janedoe",
  "proposedRole": "STATION",
  "stationCallsign": "W1ABC/2",
  "message": "Would you like to join our multi-op team?",
  "expiresAt": "2024-06-25T23:59:59"
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "logId": 15,
  "logName": "CQ WW DX SSB 2024",
  "inviterId": 5,
  "inviterUsername": "johndoe",
  "inviteeId": 12,
  "inviteeUsername": "janedoe",
  "inviteeEmail": "jane@example.com",
  "inviteeCallsign": "K2XYZ",
  "proposedRole": "STATION",
  "stationCallsign": "W1ABC/2",
  "status": "PENDING",
  "message": "Would you like to join our multi-op team?",
  "createdAt": "2024-06-20T16:00:00",
  "expiresAt": "2024-06-25T23:59:59",
  "canRespond": true
}
```

**Validation Rules**:
- `logId`: Required, must exist
- `inviteeUsername`: Required, can be username or callsign
- `proposedRole`: Required, either "STATION" or "VIEWER" (cannot invite as CREATOR)
- `stationCallsign`: Optional, max 20 characters
- `message`: Optional, max 500 characters
- `expiresAt`: Optional, must be in future

**Error Responses**:
- `400 Bad Request`: User already a participant or pending invitation exists
- `403 Forbidden`: User is not the log creator
- `404 Not Found`: Invitee not found or log does not exist

**Notes**:
- System searches for invitee by username first, then callsign (no email lookup)
- Cannot invite someone who is already a participant
- Only one pending invitation per user per log

---

### Accept Invitation

Accept an invitation to join a log.

**Endpoint**: `POST /invitations/{invitationId}/accept`

**Authentication**: Required (must be the invitee)

**Parameters**:
- `invitationId` (path): Invitation ID

**Response**: `200 OK`
```json
{
  "id": 1,
  "logId": 15,
  "status": "ACCEPTED",
  ...
}
```

**Error Responses**:
- `403 Forbidden`: Invitation not for you
- `409 Conflict`: Invitation already responded to or expired

**Notes**:
- Creates LogParticipant record with proposed role
- Can only accept PENDING, non-expired invitations
- Log will immediately appear in your log list

---

### Decline Invitation

Decline an invitation to join a log.

**Endpoint**: `POST /invitations/{invitationId}/decline`

**Authentication**: Required (must be the invitee)

**Parameters**:
- `invitationId` (path): Invitation ID

**Response**: `200 OK`
```json
{
  "id": 1,
  "status": "DECLINED",
  ...
}
```

**Error Responses**:
- `403 Forbidden`: Invitation not for you
- `409 Conflict`: Invitation already responded to or expired

---

### Cancel Invitation

Cancel a sent invitation (inviter only).

**Endpoint**: `POST /invitations/{invitationId}/cancel`

**Authentication**: Required (must be the inviter)

**Parameters**:
- `invitationId` (path): Invitation ID

**Response**: `200 OK`
```json
{
  "id": 1,
  "status": "CANCELLED",
  ...
}
```

**Error Responses**:
- `403 Forbidden`: You did not send this invitation
- `409 Conflict`: Only pending invitations can be cancelled

---

## QSO Endpoints

### Create QSO

Log a new QSO (contact).

**Endpoint**: `POST /qsos?logId={logId}`

**Authentication**: Required (CREATOR or STATION role)

**Query Parameters**:
- `logId`: Required, log ID to add QSO to

**Request Body**:
```json
{
  "stationId": 3,
  "operatorId": 7,
  "contestId": 12,
  "callsign": "DL1ABC",
  "frequencyKhz": 14250.5,
  "mode": "SSB",
  "qsoDate": "2024-06-22",
  "timeOn": "18:34:15",
  "timeOff": null,
  "rstSent": "59",
  "rstRcvd": "57",
  "band": "20m",
  "powerWatts": 100,
  "gridSquare": "JO62qm",
  "county": null,
  "state": null,
  "country": "Germany",
  "dxcc": 230,
  "cqZone": 14,
  "ituZone": 28,
  "name": "Hans",
  "licenseClass": null,
  "contestData": {
    "exchange": "001 07"
  },
  "notes": "Strong signal from Germany"
}
```

**Response**: `201 Created`
```json
{
  "id": 5472,
  "stationId": 3,
  "stationName": "Field Day Station 1",
  "operatorId": 7,
  "operatorCallsign": "W1ABC",
  "contestId": 12,
  "contestCode": "ARRL-FD",
  "callsign": "DL1ABC",
  "frequencyKhz": 14250.5,
  "mode": "SSB",
  "qsoDate": "2024-06-22",
  "timeOn": "18:34:15",
  "timeOff": null,
  "rstSent": "59",
  "rstRcvd": "57",
  "band": "20m",
  "powerWatts": 100,
  "gridSquare": "JO62qm",
  "county": null,
  "state": null,
  "country": "Germany",
  "dxcc": 230,
  "cqZone": 14,
  "ituZone": 28,
  "name": "Hans",
  "licenseClass": null,
  "contestData": {
    "exchange": "001 07"
  },
  "isValid": true,
  "validationErrors": null,
  "notes": "Strong signal from Germany",
  "createdAt": "2024-06-22T18:34:25",
  "updatedAt": "2024-06-22T18:34:25"
}
```

**Validation Rules**:
- `callsign`: Required, max 20 characters
- `frequencyKhz`: Required, positive number
- `mode`: Required, valid mode string
- `qsoDate`: Required, ISO date format
- `timeOn`: Required, HH:mm:ss format (UTC)
- `rstSent`, `rstRcvd`: Required, valid RST format
- `band`: Auto-calculated from frequency if not provided
- `contestData`: Required if contestId is set, must match contest requirements

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: User cannot edit this log
- `409 Conflict`: Log is frozen

**Notes**:
- QSO is validated against contest rules if contestId is set
- Duplicate checking is performed
- Real-time WebSocket broadcast to all log participants

---

### Get QSO by ID

Retrieve a specific QSO.

**Endpoint**: `GET /qsos/{qsoId}`

**Authentication**: Required

**Parameters**:
- `qsoId` (path): QSO ID

**Response**: `200 OK`
```json
{
  "id": 5472,
  "callsign": "DL1ABC",
  ...
}
```

**Error Responses**:
- `403 Forbidden`: User does not have access to this QSO's log
- `404 Not Found`: QSO does not exist

---

### Update QSO

Update an existing QSO.

**Endpoint**: `PUT /qsos/{qsoId}`

**Authentication**: Required (CREATOR or STATION role)

**Parameters**:
- `qsoId` (path): QSO ID

**Request Body**: Same as Create QSO

**Response**: `200 OK`
```json
{
  "id": 5472,
  "callsign": "DL1ABC",
  "updatedAt": "2024-06-22T19:15:42",
  ...
}
```

**Error Responses**:
- `403 Forbidden`: User cannot edit this log
- `404 Not Found`: QSO does not exist
- `409 Conflict`: Log is frozen

---

### Delete QSO

Delete a QSO.

**Endpoint**: `DELETE /qsos/{qsoId}`

**Authentication**: Required (CREATOR or STATION role)

**Parameters**:
- `qsoId` (path): QSO ID

**Response**: `204 No Content`

**Error Responses**:
- `403 Forbidden`: User cannot edit this log
- `404 Not Found`: QSO does not exist
- `409 Conflict`: Log is frozen

---

### Get All QSOs (Paginated)

Retrieve all QSOs for a log with pagination.

**Endpoint**: `GET /qsos?logId={logId}&page={page}&size={size}`

**Authentication**: Required

**Query Parameters**:
- `logId`: Required, log ID
- `page`: Optional, page number (0-indexed), default 0
- `size`: Optional, page size, default 20

**Response**: `200 OK`
```json
{
  "content": [
    {
      "id": 5472,
      "callsign": "DL1ABC",
      ...
    },
    {
      "id": 5471,
      "callsign": "JA1XYZ",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 92,
  "last": false,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 20,
  "first": true,
  "empty": false
}
```

**Error Responses**:
- `403 Forbidden`: User does not have access to this log

---

### Get Recent QSOs

Retrieve most recent QSOs for a log.

**Endpoint**: `GET /qsos/recent?logId={logId}&limit={limit}`

**Authentication**: Required

**Query Parameters**:
- `logId`: Required, log ID
- `limit`: Optional, max QSOs to return, default 10

**Response**: `200 OK`
```json
[
  {
    "id": 5472,
    "callsign": "DL1ABC",
    "qsoDate": "2024-06-22",
    "timeOn": "18:34:15",
    ...
  },
  {
    "id": 5471,
    "callsign": "JA1XYZ",
    "qsoDate": "2024-06-22",
    "timeOn": "18:28:42",
    ...
  }
]
```

---

### Get QSOs by Date Range

Retrieve QSOs within a date range.

**Endpoint**: `GET /qsos/range?logId={logId}&startDate={startDate}&endDate={endDate}`

**Authentication**: Required

**Query Parameters**:
- `logId`: Required, log ID
- `startDate`: Required, ISO date (YYYY-MM-DD)
- `endDate`: Required, ISO date (YYYY-MM-DD)

**Response**: `200 OK`
```json
[
  {
    "id": 5472,
    "callsign": "DL1ABC",
    "qsoDate": "2024-06-22",
    ...
  }
]
```

**Error Responses**:
- `400 Bad Request`: Invalid date format or endDate before startDate

---

### Get Contacted States

Retrieve distinct US states contacted in a log.

**Endpoint**: `GET /qsos/states?logId={logId}`

**Authentication**: Required

**Query Parameters**:
- `logId`: Required, log ID

**Response**: `200 OK`
```json
[
  "CT",
  "MA",
  "NY",
  "PA",
  "CA",
  "TX"
]
```

**Notes**:
- Only returns states for contacts where country = "USA"
- Sorted alphabetically

---

## Station Endpoints

### Get All Stations

Retrieve all stations for the current user.

**Endpoint**: `GET /stations`

**Authentication**: Required

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "stationName": "Home Station",
    "callsign": "W1ABC",
    "gridSquare": "FN31pr",
    "antenna": "Hex Beam @ 40ft",
    "powerWatts": 100,
    "active": true,
    "createdAt": "2024-01-15T10:00:00"
  },
  {
    "id": 2,
    "stationName": "Field Day Station 1",
    "callsign": "W1ABC/P",
    "gridSquare": "FN31pr",
    "antenna": "Inverted V @ 30ft",
    "powerWatts": 100,
    "active": true,
    "createdAt": "2024-06-20T14:00:00"
  }
]
```

---

### Create Station

Create a new station configuration.

**Endpoint**: `POST /stations`

**Authentication**: Required

**Request Body**:
```json
{
  "stationName": "Portable Station",
  "callsign": "W1ABC/M",
  "gridSquare": "FN42aa",
  "antenna": "Vertical",
  "powerWatts": 50
}
```

**Response**: `201 Created`
```json
{
  "id": 3,
  "stationName": "Portable Station",
  "callsign": "W1ABC/M",
  "gridSquare": "FN42aa",
  "antenna": "Vertical",
  "powerWatts": 50,
  "active": true,
  "createdAt": "2024-06-22T10:15:00"
}
```

**Validation Rules**:
- `stationName`: Required, max 100 characters
- `callsign`: Required, valid callsign format
- `gridSquare`: Optional, valid Maidenhead format (e.g., FN31pr)
- `antenna`: Optional, max 200 characters
- `powerWatts`: Optional, positive integer

---

## Contest Endpoints

### Get All Contests

Retrieve all available contests.

**Endpoint**: `GET /contests`

**Authentication**: Required

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "contestCode": "CQWW-SSB",
    "name": "CQ World Wide DX Contest - SSB",
    "category": "DX",
    "validBands": ["160m", "80m", "40m", "20m", "15m", "10m"],
    "validModes": ["SSB"],
    "startDate": "2024-10-26T00:00:00",
    "endDate": "2024-10-27T23:59:59"
  },
  {
    "id": 12,
    "contestCode": "ARRL-FD",
    "name": "ARRL Field Day",
    "category": "Emergency",
    "validBands": ["160m", "80m", "40m", "20m", "15m", "10m", "6m", "2m"],
    "validModes": ["SSB", "CW", "Digital"],
    "startDate": "2024-06-22T18:00:00",
    "endDate": "2024-06-23T21:00:00"
  }
]
```

---

### Get Contest by ID

Retrieve details of a specific contest.

**Endpoint**: `GET /contests/{contestId}`

**Authentication**: Required

**Parameters**:
- `contestId` (path): Contest ID

**Response**: `200 OK`
```json
{
  "id": 12,
  "contestCode": "ARRL-FD",
  "name": "ARRL Field Day",
  "category": "Emergency",
  "description": "Annual emergency preparedness exercise",
  "validBands": ["160m", "80m", "40m", "20m", "15m", "10m", "6m", "2m"],
  "validModes": ["SSB", "CW", "Digital"],
  "requiredFields": ["class", "section"],
  "exchangeFormat": "CLASS SECTION",
  "scoringRules": {
    "qsoPoints": {
      "cw": 2,
      "phone": 1,
      "digital": 2
    },
    "bonusPoints": {
      "emergency": 100,
      "publicLocation": 100,
      "batteryPower": 100
    }
  },
  "startDate": "2024-06-22T18:00:00",
  "endDate": "2024-06-23T21:00:00"
}
```

---

## Export Endpoints

### Export ADIF

Export QSOs in ADIF format.

**Endpoint**: `GET /export/adif?logId={logId}`

**Authentication**: Required (CREATOR role)

**Query Parameters**:
- `logId`: Required, log ID
- `startDate`: Optional, filter start date
- `endDate`: Optional, filter end date

**Response**: `200 OK`
```
Content-Type: text/plain
Content-Disposition: attachment; filename="log_20240622.adi"

ADIF Export from Ham Radio Logbook
<adif_ver:5>3.1.4
<programid:14>HamRadioLogbook
<programversion:5>1.0.0
<eoh>

<call:6>DL1ABC<qso_date:8>20240622<time_on:6>183415<freq:7>14.2505<mode:3>SSB<rst_sent:2>59<rst_rcvd:2>57<gridsquare:6>JO62qm<country:7>Germany<eor>
<call:6>JA1XYZ<qso_date:8>20240622<time_on:6>182842<freq:7>21.2255<mode:3>SSB<rst_sent:2>59<rst_rcvd:2>59<gridsquare:6>PM95<country:5>Japan<eor>
```

**Notes**:
- Returns ADIF 3.1.4 format
- Includes all ADIF standard fields
- File download with proper Content-Disposition header

---

### Export Cabrillo

Export contest log in Cabrillo format.

**Endpoint**: `POST /export/cabrillo`

**Authentication**: Required (CREATOR role)

**Request Body**:
```json
{
  "logId": 15,
  "contestName": "CQ-WW-SSB",
  "operators": ["W1ABC", "K2XYZ"],
  "category": "MULTI-OP TWO-TRANSMITTER",
  "overlay": null,
  "club": "Yankee Clipper Contest Club",
  "claimedScore": 4250000,
  "soapbox": [
    "Great conditions on 20m!",
    "Equipment worked flawlessly."
  ]
}
```

**Response**: `200 OK`
```
Content-Type: text/plain
Content-Disposition: attachment; filename="cqww_ssb_2024.log"

START-OF-LOG: 3.0
CONTEST: CQ-WW-SSB
CALLSIGN: W1ABC
CATEGORY-OPERATOR: MULTI-OP
CATEGORY-TRANSMITTER: TWO
CLAIMED-SCORE: 4250000
CLUB: Yankee Clipper Contest Club
OPERATORS: W1ABC K2XYZ
SOAPBOX: Great conditions on 20m!
SOAPBOX: Equipment worked flawlessly.
QSO: 14250 PH 2024-10-26 1834 W1ABC 59 05 DL1ABC 57 14
QSO: 21225 PH 2024-10-26 1842 W1ABC 59 05 JA1XYZ 59 25
END-OF-LOG:
```

---

## Map Endpoints

### Get QSO Locations (with clustering)

**Endpoint**: `GET /maps/qsos/{logId}`

**Authentication**: Required

**Query Parameters**:
- `zoom`: Optional, current map zoom level (influences clustering threshold)

**Response**: `200 OK` — GeoJSON FeatureCollection or ClusteredResponse

---

### Get Grid Square Coverage

**Endpoint**: `GET /maps/grids/{logId}`

**Authentication**: Required

**Query Parameters**:
- `precision`: Optional, `2`, `4`, `6`, or `8` (default `4`)
- Band, mode, station filter parameters

**Response**: `200 OK` — Map of grid square to QSO count

---

### Get Heatmap Data

**Endpoint**: `GET /maps/heatmap/{logId}`

**Authentication**: Required

**Response**: `200 OK` — Array of `[lat, lon, weight]` points

---

### Get Contest Overlays

**Endpoint**: `GET /maps/overlays/cq-zones/{logId}`
**Endpoint**: `GET /maps/overlays/itu-zones/{logId}`
**Endpoint**: `GET /maps/overlays/arrl-sections/{logId}`
**Endpoint**: `GET /maps/overlays/dxcc/{logId}`

**Authentication**: Required

**Response**: `200 OK` — GeoJSON FeatureCollection with worked/needed status per zone

---

### Export Map Data

**Endpoint**: `POST /maps/export/{logId}?format={format}`

**Authentication**: Required

**Query Parameters**:
- `format`: `geojson`, `kml`, `csv`, or `adif`

**Response**: `200 OK` — File download with appropriate Content-Type

---

### Session Location Management

```
POST   /maps/session-location/{logId}   # Set session operating location
GET    /maps/session-location/{logId}   # Get current session location
DELETE /maps/session-location/{logId}   # Clear session location
PUT    /maps/location/user              # Update user default location (Auth required)
```

---

## Award Tracking Endpoints

### Get Award Progress

Retrieve DXCC, WAS, and VUCC award progress for a log.

**Endpoint**: `GET /awards/{logId}`

**Authentication**: Required (log access required)

**Response**: `200 OK`
```json
{
  "dxcc": {
    "worked": 182,
    "confirmed": 143,
    "total": 340,
    "percentage": 42.1
  },
  "was": {
    "worked": 47,
    "confirmed": 38,
    "total": 50,
    "percentage": 76.0
  },
  "vucc": {
    "worked": 112,
    "confirmed": 89,
    "threshold": 100,
    "achieved": true
  }
}
```

**Notes**:
- `confirmed` = QSOs where `lotwRcvd = 'Y'` OR `qslRcvd = 'Y'`
- DXCC uses distinct `country` values; WAS uses distinct `state` values (USA only)
- VUCC uses distinct 4-character grid square prefixes (SUBSTRING of `gridSquare`)

**Error Responses**:
- `403 Forbidden`: No access to this log
- `404 Not Found`: Log does not exist

---

## DX Cluster Endpoints

### Get DX Cluster Spots

Retrieve recent DX cluster spots from DX Summit.

**Endpoint**: `GET /dx-cluster/spots`

**Authentication**: Required

**Query Parameters**:
- `limit`: Optional, number of spots to return (default 20)
- `band`: Optional, filter by band (e.g., `20m`, `40m`)

**Response**: `200 OK`
```json
[
  {
    "callsign": "DL1ABC",
    "frequency": "14250.0",
    "band": "20m",
    "mode": "SSB",
    "spotter": "W1XYZ",
    "comment": "Strong signal",
    "time": "1834"
  }
]
```

**Notes**:
- Spots are polled from DX Summit (`dxsummit.fi`) every 60 seconds
- Frontend uses 60-second RxJS `interval` poll

---

## Propagation Endpoints

### Get Propagation Conditions

Retrieve current solar/propagation conditions with per-band ratings.

**Endpoint**: `GET /propagation/conditions`

**Authentication**: Required

**Response**: `200 OK`
```json
{
  "solarFluxIndex": 142,
  "kIndex": 2,
  "aIndex": 8,
  "updated": "2026-03-03T14:00:00Z",
  "bandConditions": {
    "160m": "POOR",
    "80m": "FAIR",
    "40m": "GOOD",
    "20m": "EXCELLENT",
    "17m": "EXCELLENT",
    "15m": "GOOD",
    "12m": "GOOD",
    "10m": "FAIR",
    "6m": "POOR"
  }
}
```

**Notes**:
- Data sourced from NOAA SWPC JSON API
- Cached for 30 minutes server-side
- Band ratings: `EXCELLENT`, `GOOD`, `FAIR`, `POOR` — derived from SFI, K-index, and A-index

---

## LoTW Sync Endpoints

### Sync LoTW Confirmations

Download and process ARRL Logbook of the World (LoTW) QSL records. Matches downloaded records against existing QSOs in the specified log and sets `lotwRcvd = 'Y'` for matched contacts.

**Endpoint**: `POST /lotw/sync/{logId}`

**Authentication**: Required (CREATOR role)

**Request Body**:
```json
{
  "username": "your-lotw-username",
  "password": "your-lotw-password"
}
```

**Response**: `200 OK`
```json
{
  "matched": 34,
  "total": 58,
  "message": "Synced 34 confirmations from LoTW"
}
```

**Notes**:
- LoTW credentials are NOT stored — they are used per-request only
- Matching uses callsign (case-insensitive) + date + band
- Only updates QSOs that belong to the specified log

**Error Responses**:
- `400 Bad Request`: Invalid credentials or LoTW service unavailable
- `403 Forbidden`: Not the log creator

---

## User Preference Endpoints

### Get Station Color Preferences

Retrieve the user's custom station color assignments.

**Endpoint**: `GET /user/station-colors`

**Authentication**: Required

**Response**: `200 OK`
```json
{
  "station1": "#0080ff",
  "station2": "#ff4444",
  "station3": "#44cc44",
  "gota": "#ffaa00"
}
```

**Response**: `204 No Content` — if no custom colors are set (system defaults apply)

---

### Save Station Color Preferences

**Endpoint**: `PUT /user/station-colors`

**Authentication**: Required

**Request Body**:
```json
{
  "station1": "#0080ff",
  "station2": "#ff4444",
  "gota": "#ffaa00"
}
```

**Response**: `200 OK` — Updated preferences echoed back

---

### Reset Station Color Preferences

Reset station colors to system defaults.

**Endpoint**: `DELETE /user/station-colors`

**Authentication**: Required

**Response**: `204 No Content`

---

## Rig Control Endpoints

All endpoints require authentication (ROLE_USER, ROLE_OPERATOR, or ROLE_ADMIN).

```
POST   /rig-control/connect                          # Connect to rigctld for a station
POST   /rig-control/disconnect/{stationId}           # Disconnect from rig
POST   /rig-control/command/{stationId}              # Send raw command to rig
POST   /rig-control/frequency/{stationId}?frequencyHz={hz}  # Set frequency
POST   /rig-control/mode/{stationId}?mode={mode}&bandwidth={bw}  # Set mode
POST   /rig-control/ptt/{stationId}?enable={true|false}     # Toggle PTT (first-come-first-served lock)
GET    /rig-control/status/{stationId}               # Get current rig status
GET    /rig-control/connected/{stationId}            # Check connection state
```

### Connect to Rig

**Endpoint**: `POST /rig-control/connect`

**Request Body**:
```json
{
  "stationId": 1,
  "host": "localhost",
  "port": 4532
}
```

**Response**: `200 OK`
```json
{
  "stationId": 1,
  "connected": true,
  "message": "Connected to rigctld at localhost:4532"
}
```

### Get Rig Status

**Endpoint**: `GET /rig-control/status/{stationId}`

**Response**: `200 OK`
```json
{
  "stationId": 1,
  "connected": true,
  "frequency": 14250000,
  "mode": "USB",
  "ptt": false,
  "sMeter": 5
}
```

---

## DXCC Endpoints

```
POST   /dxcc/load              # Upload custom CTY.DAT file
POST   /dxcc/load-default      # Load bundled default CTY.DAT
GET    /dxcc/status            # Check loaded entity count
GET    /dxcc/lookup/{callsign} # Look up DXCC entity by callsign prefix
```

### Lookup DXCC Entity

**Endpoint**: `GET /dxcc/lookup/{callsign}`

**Response**: `200 OK`
```json
{
  "entity": "Germany",
  "prefix": "DL",
  "continent": "EU",
  "cqZone": 14,
  "ituZone": 28,
  "latitude": 51.0,
  "longitude": 9.0,
  "dxccNumber": 230
}
```

---

## Error Responses

All error responses follow this format:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message"
}
```

### Common Error Codes

**400 Bad Request**
```json
{
  "code": "BAD_REQUEST",
  "message": "Log name must not exceed 100 characters"
}
```

**401 Unauthorized**
```json
{
  "code": "UNAUTHORIZED",
  "message": "Invalid or expired token"
}
```

**403 Forbidden**
```json
{
  "code": "FORBIDDEN",
  "message": "User does not have permission to edit QSOs in this log"
}
```

**404 Not Found**
```json
{
  "code": "NOT_FOUND",
  "message": "Log not found: 999"
}
```

**409 Conflict**
```json
{
  "code": "CONFLICT",
  "message": "This log is frozen and cannot be edited"
}
```

**500 Internal Server Error**
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred"
}
```

---

## Rate Limiting

**Authentication Endpoints**: 5 requests per minute per IP
**Other Endpoints**: 100 requests per minute per user

**Rate Limit Headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1719082800
```

**Rate Limit Exceeded Response**: `429 Too Many Requests`
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again in 45 seconds."
}
```

---

## WebSocket Topics

For real-time updates, subscribe to WebSocket topics via STOMP over SockJS.

**QSO Updates**:
- Topic: `/topic/qsos`
- Message Type: QSOResponse
- Triggered: When a QSO is created, updated, or deleted

**Live Scoring**:
- Topic: `/topic/scoring/{logId}`
- Message Type: ScoreResponse
- Triggered: On every new or deleted QSO (score recalculated)

**Rig Telemetry** (all stations):
- Topic: `/topic/telemetry/*`
- Message Type: RigStatus

**Rig Telemetry** (specific station):
- Topic: `/topic/telemetry/{stationId}`
- Message Type: RigStatus
- Triggered: Every 100ms while rig is connected

**Rig Events** (specific station):
- Topic: `/topic/rig/events/{stationId}`
- Message Type: RigEvent
- Triggered: PTT acquired/released, client connect/disconnect, errors

**Connection**:
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // Subscribe to QSO updates
    stompClient.subscribe('/topic/qsos', (message) => {
        const qso = JSON.parse(message.body);
        console.log('New QSO:', qso);
    });

    // Subscribe to live scoring
    stompClient.subscribe('/topic/scoring/15', (message) => {
        const score = JSON.parse(message.body);
        console.log('Score update:', score);
    });

    // Subscribe to rig telemetry for station 1
    stompClient.subscribe('/topic/telemetry/1', (message) => {
        const status = JSON.parse(message.body);
        console.log('Rig status:', status);
    });
});
```

---

## Pagination

All paginated endpoints follow Spring Data's Page response format:

```json
{
  "content": [...],           // Array of items
  "pageable": {
    "pageNumber": 0,          // Current page (0-indexed)
    "pageSize": 20,           // Items per page
    "offset": 0,              // Item offset
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,            // Total number of pages
  "totalElements": 92,        // Total number of items
  "last": false,              // Is this the last page?
  "size": 20,                 // Page size
  "number": 0,                // Page number
  "first": true,              // Is this the first page?
  "empty": false              // Is the page empty?
}
```

---

## Date/Time Formats

**ISO 8601 Format**: Used throughout the API

- **Date**: `YYYY-MM-DD` (e.g., "2024-06-22")
- **Time**: `HH:mm:ss` (e.g., "18:34:15") - Always UTC
- **DateTime**: `YYYY-MM-DDTHH:mm:ss` (e.g., "2024-06-22T18:34:15") - Always UTC

**Note**: All times are in UTC. The frontend is responsible for converting to/from local time for display.

---

For additional help, see:
- [User Guide](USER_GUIDE.md)
- [Developer Guide](DEVELOPER_GUIDE.md)
- [Database Schema](DATABASE_SCHEMA.md)
