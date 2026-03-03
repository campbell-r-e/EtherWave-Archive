# Ham Radio Logbook - Database Schema

Complete database schema documentation with entity relationships and table structures.

## Table of Contents

1. [Entity Relationship Diagram](#entity-relationship-diagram)
2. [Multi-Tenant Architecture](#multi-tenant-architecture)
3. [Table Definitions](#table-definitions)
4. [Indexes and Performance](#indexes-and-performance)
5. [Data Types and Enums](#data-types-and-enums)
6. [Foreign Key Relationships](#foreign-key-relationships)
7. [Migration Strategy](#migration-strategy)

---

## Entity Relationship Diagram

```
┌─────────────┐
│    User     │
│─────────────│
│ id (PK)     │◄─────────┐
│ username    │          │
│ password    │          │ created_by
│ callsign    │          │
│ roles       │          │
└─────────────┘          │
      │                  │
      │ creator_id       │
      ▼                  │
┌─────────────┐    ┌─────────────┐
│    Log      │    │  Invitation │
│─────────────│    │─────────────│
│ id (PK)     │◄───┤ log_id (FK) │
│ name        │    │ inviter_id  │───┐
│ type        │    │ invitee_id  │───┤
│ creator_id  │    │ status      │   │
│ contest_id  │    │ role        │   │
│ start_date  │    └─────────────┘   │
│ end_date    │                      │
│ editable    │                      │
│ is_public   │                      │
└─────────────┘                      │
      │                              │
      │                              │
      ▼                              │
┌──────────────────┐                 │
│ LogParticipant   │                 │
│──────────────────│                 │
│ id (PK)          │                 │
│ log_id (FK)      │─────────────────┘
│ user_id (FK)     │─────────────────┐
│ role             │                 │
│ station_callsign │                 ▼
│ active           │           ┌─────────────┐
│ joined_at        │           │    User     │
└──────────────────┘           └─────────────┘
      │
      │
      ▼
┌─────────────┐
│    QSO      │
│─────────────│
│ id (PK)     │
│ log_id (FK) │───────────┐
│ station_id  │           │
│ operator_id │           │
│ contest_id  │           │
│ callsign    │           ▼
│ frequency   │     ┌─────────────┐
│ mode        │     │    Log      │
│ qso_date    │     └─────────────┘
│ time_on     │
│ rst_sent    │     ┌─────────────┐
│ rst_rcvd    │     │  Station    │
│ ...         │◄────┤ id (PK)     │
└─────────────┘     │ user_id     │
      │             │ name        │
      │             │ callsign    │
      │             └─────────────┘
      │
      │             ┌─────────────┐
      │             │  Operator   │
      └─────────────┤ id (PK)     │
                    │ user_id     │
                    │ callsign    │
                    └─────────────┘

┌─────────────┐
│  Contest    │
│─────────────│
│ id (PK)     │
│ code        │
│ name        │
│ start_date  │
│ end_date    │
└─────────────┘
```

---

## Multi-Tenant Architecture

### Isolation Strategy

**Pattern**: Single Database, Shared Schema, Row-Level Isolation

Every data-bearing table contains a `log_id` foreign key that isolates data by log (tenant).

**Benefits**:
- Simple deployment (one database)
- Easy backup and restore
- Cross-log queries possible when needed
- Consistent data model

**Isolation Mechanism**:
```sql
-- All QSO queries filter by log_id
SELECT * FROM qsos WHERE log_id = ? AND ...

-- Permission check before query
SELECT 1 FROM log_participants
WHERE log_id = ? AND user_id = ? AND active = true
```

---

## Table Definitions

### users

Stores user authentication and profile information.

```sql
CREATE TABLE users (
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username                    VARCHAR(50) UNIQUE NOT NULL,
    password                    VARCHAR(255) NOT NULL,  -- BCrypt hash
    callsign                    VARCHAR(20),
    full_name                   VARCHAR(100),
    grid_square                 VARCHAR(10),
    default_latitude            DOUBLE,
    default_longitude           DOUBLE,
    station_color_preferences   TEXT,  -- JSON: {"station1":"#hex","station2":"#hex","gota":"#hex"}
    enabled                     BOOLEAN DEFAULT TRUE,
    account_non_expired         BOOLEAN DEFAULT TRUE,
    account_non_locked          BOOLEAN DEFAULT TRUE,
    credentials_non_expired     BOOLEAN DEFAULT TRUE,
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_username (username),
    INDEX idx_callsign (callsign)
);
```

**Fields**:
- `id`: Primary key
- `username`: Unique username for login — no email required or stored
- `password`: BCrypt hashed password
- `callsign`: Amateur radio callsign (optional)
- `full_name`: Operator's full name (optional)
- `grid_square`: Maidenhead grid square (optional)
- `default_latitude` / `default_longitude`: Default operating location for map centering and QSO location resolution
- `station_color_preferences`: JSON string mapping station keys to hex color codes (e.g., `{"station1":"#0080ff","gota":"#ffaa00"}`). NULL means system defaults apply. Managed via `GET/PUT/DELETE /api/user/station-colors`.
- `enabled`, `account_non_expired`, etc.: Spring Security account status flags
- `created_at`: Account creation timestamp
- `updated_at`: Last update timestamp

**Notes**:
- Passwords are hashed with BCrypt (cost factor 10)
- Username must be unique — no email field exists in this system
- Callsign is optional but recommended

---

### user_roles

Maps users to roles (many-to-many relationship).

```sql
CREATE TABLE user_roles (
    user_id    BIGINT NOT NULL,
    role       VARCHAR(50) NOT NULL,

    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Roles**:
- `ROLE_USER`: Standard user
- `ROLE_ADMIN`: System administrator

**Notes**:
- Most users have only `ROLE_USER`
- Admin users have both `ROLE_USER` and `ROLE_ADMIN`
- Roles are stored as strings for flexibility

---

### logs

Main logbook entity.

```sql
CREATE TABLE logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    type            VARCHAR(20) NOT NULL,  -- PERSONAL or SHARED
    creator_id      BIGINT NOT NULL,
    contest_id      BIGINT,
    start_date      TIMESTAMP,
    end_date        TIMESTAMP,
    active          BOOLEAN DEFAULT TRUE,
    editable        BOOLEAN DEFAULT TRUE,
    is_public       BOOLEAN DEFAULT FALSE,
    bonus_metadata  TEXT,  -- JSON: {"bonus_key": count_or_flag_integer, ...}
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (creator_id) REFERENCES users(id),
    FOREIGN KEY (contest_id) REFERENCES contests(id),

    INDEX idx_creator (creator_id),
    INDEX idx_type (type),
    INDEX idx_active (active)
);
```

**Fields**:
- `id`: Primary key
- `name`: Descriptive name (max 100 chars)
- `description`: Optional details (max 500 chars)
- `type`: PERSONAL or SHARED
- `creator_id`: User who created the log
- `contest_id`: Optional associated contest
- `start_date`: Optional log period start (for contests)
- `end_date`: Optional log period end (auto-freeze after this)
- `active`: Soft delete flag
- `editable`: Manual freeze flag
- `is_public`: Whether log is publicly viewable
- `bonus_metadata`: JSON map of contest bonus key to count or flag integer (e.g., `{"natural_power":1,"public_location":0}`). Read by `ScoringService.calculateBonusPoints()`. NULL if no bonuses tracked.
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

**Business Rules**:
- A log is editable only if: `editable = true AND (end_date IS NULL OR NOW() <= end_date)`
- Creator automatically gets CREATOR role in log_participants
- Deleted logs set `active = false` (soft delete)
- PERSONAL logs always have `is_public = false` regardless of request value

---

### log_participants

Junction table for user-log relationships with roles.

```sql
CREATE TABLE log_participants (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id              BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    role                VARCHAR(20) NOT NULL,  -- CREATOR, STATION, VIEWER
    station_callsign    VARCHAR(20),
    active              BOOLEAN DEFAULT TRUE,
    joined_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (log_id) REFERENCES logs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    UNIQUE KEY uk_log_user (log_id, user_id),
    INDEX idx_log (log_id),
    INDEX idx_user (user_id),
    INDEX idx_active (active)
);
```

**Fields**:
- `id`: Primary key
- `log_id`: Reference to log
- `user_id`: Reference to user
- `role`: CREATOR, STATION, or VIEWER
- `station_callsign`: For multi-op contests (optional)
- `active`: Soft delete flag
- `joined_at`: When user joined the log

**Roles**:
- `CREATOR`: Full control, can manage participants, freeze log
- `STATION`: Can create/edit/delete QSOs
- `VIEWER`: Read-only access

**Constraints**:
- One user can only have one active role per log
- Creator role assigned automatically when log is created
- Removing participation sets `active = false`

---

### invitations

In-app invitation system for shared logs.

```sql
CREATE TABLE invitations (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id              BIGINT NOT NULL,
    inviter_id          BIGINT NOT NULL,
    invitee_id          BIGINT NOT NULL,
    proposed_role       VARCHAR(20) NOT NULL,  -- STATION or VIEWER
    station_callsign    VARCHAR(20),
    status              VARCHAR(20) DEFAULT 'PENDING',
    message             VARCHAR(500),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at        TIMESTAMP,
    expires_at          TIMESTAMP,

    FOREIGN KEY (log_id) REFERENCES logs(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES users(id),
    FOREIGN KEY (invitee_id) REFERENCES users(id),

    INDEX idx_log (log_id),
    INDEX idx_inviter (inviter_id),
    INDEX idx_invitee (invitee_id),
    INDEX idx_status (status)
);
```

**Fields**:
- `id`: Primary key
- `log_id`: Log being invited to
- `inviter_id`: User sending the invitation
- `invitee_id`: User receiving the invitation
- `proposed_role`: Role offered (STATION or VIEWER, not CREATOR)
- `station_callsign`: Optional callsign for multi-op
- `status`: PENDING, ACCEPTED, DECLINED, CANCELLED, EXPIRED
- `message`: Optional personal message (max 500 chars)
- `created_at`: When invitation was sent
- `responded_at`: When invitee accepted/declined
- `expires_at`: Optional expiration date

**Status Flow**:
1. Created: `status = PENDING`
2. Invitee accepts: `status = ACCEPTED`, creates log_participant record
3. Invitee declines: `status = DECLINED`
4. Inviter cancels: `status = CANCELLED`
5. System expires: `status = EXPIRED` (if past expires_at)

---

### qsos

QSO (contact) records.

```sql
CREATE TABLE qsos (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id              BIGINT NOT NULL,
    station_id          BIGINT NOT NULL,
    operator_id         BIGINT,
    contest_id          BIGINT,
    callsign            VARCHAR(20) NOT NULL,
    frequency_khz       DOUBLE NOT NULL,
    mode                VARCHAR(20) NOT NULL,
    qso_date            DATE NOT NULL,
    time_on             TIME NOT NULL,
    time_off            TIME,
    rst_sent            VARCHAR(10) NOT NULL,
    rst_rcvd            VARCHAR(10) NOT NULL,
    band                VARCHAR(10),
    power_watts         INTEGER,
    grid_square         VARCHAR(10),
    county              VARCHAR(50),
    state               VARCHAR(50),
    country             VARCHAR(50),
    dxcc                INTEGER,
    cq_zone             INTEGER,
    itu_zone            INTEGER,
    name                VARCHAR(100),
    license_class       VARCHAR(20),
    contest_data        JSON,
    qsl_sent            VARCHAR(10),
    qsl_rcvd            VARCHAR(10),
    lotw_sent           VARCHAR(10),
    lotw_rcvd           VARCHAR(10),
    is_valid            BOOLEAN DEFAULT TRUE,
    validation_errors   TEXT,
    notes               TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (log_id) REFERENCES logs(id) ON DELETE CASCADE,
    FOREIGN KEY (station_id) REFERENCES stations(id),
    FOREIGN KEY (operator_id) REFERENCES operators(id),
    FOREIGN KEY (contest_id) REFERENCES contests(id),

    INDEX idx_log (log_id),
    INDEX idx_callsign (callsign),
    INDEX idx_qso_date (qso_date),
    INDEX idx_band (band),
    INDEX idx_mode (mode),
    INDEX idx_country (country),
    INDEX idx_state (state),
    INDEX idx_created (created_at)
);
```

**Key Fields**:
- `log_id`: Multi-tenant isolation key
- `callsign`: Contacted station's callsign
- `frequency_khz`: Frequency in kHz (e.g., 14250.5)
- `mode`: Operating mode (SSB, CW, FT8, etc.)
- `qso_date`: Date of contact (UTC)
- `time_on`/`time_off`: Start/end times (UTC)
- `rst_sent`/`rst_rcvd`: Signal reports
- `contest_data`: JSON field for contest-specific exchange data

**ADIF Standard Fields**:
- `grid_square`: Maidenhead locator
- `dxcc`: DXCC entity code
- `cq_zone`, `itu_zone`: Zone numbers
- `qsl_sent`/`qsl_rcvd`: QSL card status
- `lotw_sent`/`lotw_rcvd`: LoTW confirmation status

**Validation**:
- `is_valid`: Whether QSO passed contest validation
- `validation_errors`: Pipe-separated error/warning messages

---

### stations

Station configurations.

```sql
CREATE TABLE stations (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    station_name    VARCHAR(100) NOT NULL,
    callsign        VARCHAR(20) NOT NULL,
    grid_square     VARCHAR(10),
    antenna         VARCHAR(200),
    power_watts     INTEGER,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_user (user_id),
    INDEX idx_active (active)
);
```

**Purpose**: Stores station equipment configurations (antennas, power, etc.)

**Fields**:
- `station_name`: Descriptive name (e.g., "Home Station", "Field Day Station 1")
- `callsign`: Station callsign (may differ from user callsign for /P, /M, etc.)
- `grid_square`: Station location as Maidenhead grid
- `antenna`: Antenna description
- `power_watts`: Transmit power

---

### operators

Operator records (for multi-op logging).

```sql
CREATE TABLE operators (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    callsign    VARCHAR(20) NOT NULL,
    name        VARCHAR(100),
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_user (user_id),
    INDEX idx_callsign (callsign)
);
```

**Purpose**: Tracks individual operators for multi-operator stations

**Usage**: In multi-op contests, each operator logs QSOs under their own callsign but all QSOs go into the shared log

---

### contests

Contest definitions.

```sql
CREATE TABLE contests (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    contest_code        VARCHAR(50) UNIQUE NOT NULL,
    name                VARCHAR(100) NOT NULL,
    category            VARCHAR(50),
    description         TEXT,
    start_date          TIMESTAMP,
    end_date            TIMESTAMP,
    valid_bands         JSON,
    valid_modes         JSON,
    required_fields     JSON,
    exchange_format     VARCHAR(100),
    scoring_rules       JSON,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_code (contest_code),
    INDEX idx_dates (start_date, end_date)
);
```

**Fields**:
- `contest_code`: Unique identifier (e.g., "CQWW-SSB", "ARRL-FD")
- `name`: Full contest name
- `category`: Contest type (DX, Domestic, Emergency, etc.)
- `valid_bands`: JSON array of allowed bands
- `valid_modes`: JSON array of allowed modes
- `required_fields`: JSON array of required QSO fields
- `exchange_format`: Expected exchange format string
- `scoring_rules`: JSON object with scoring logic

**Example JSON**:
```json
{
  "valid_bands": ["160m", "80m", "40m", "20m", "15m", "10m"],
  "valid_modes": ["SSB"],
  "required_fields": ["cq_zone"],
  "scoring_rules": {
    "same_continent": 1,
    "different_continent": 3,
    "multipliers": ["country", "cq_zone"]
  }
}
```

---

## Indexes and Performance

### Primary Indexes

All tables have primary key indexes on `id` (clustered index in MySQL/PostgreSQL).

### Foreign Key Indexes

All foreign key columns are indexed:
- `log_id` in all multi-tenant tables
- `user_id` in user-owned entities
- `creator_id`, `inviter_id`, `invitee_id`

### Query Optimization Indexes

**QSOs Table**:
```sql
INDEX idx_log (log_id)                    -- Multi-tenant filtering
INDEX idx_log_date (log_id, qso_date)     -- Date range queries
INDEX idx_log_callsign (log_id, callsign) -- Dupe checking
INDEX idx_log_band_mode (log_id, band, mode, callsign) -- Contest dupes
```

**Log Participants Table**:
```sql
UNIQUE KEY uk_log_user (log_id, user_id)  -- One role per user per log
INDEX idx_user_active (user_id, active)   -- User's active logs
```

**Invitations Table**:
```sql
INDEX idx_invitee_status (invitee_id, status) -- Pending invitations
INDEX idx_log_status (log_id, status)         -- Log's pending invitations
```

### Query Performance Tips

1. **Always filter by log_id first**: Leverages multi-tenant index
2. **Use pagination**: Large result sets should use LIMIT/OFFSET or cursor pagination
3. **Avoid SELECT ***: Only select needed columns
4. **Use covering indexes**: Include all queried columns in index where possible

---

## Data Types and Enums

### Java Enums (Stored as VARCHAR)

**Log.LogType**:
- `PERSONAL`
- `SHARED`

**LogParticipant.ParticipantRole**:
- `CREATOR`
- `STATION`
- `VIEWER`

**Invitation.InvitationStatus**:
- `PENDING`
- `ACCEPTED`
- `DECLINED`
- `CANCELLED`
- `EXPIRED`

**User.Role**:
- `ROLE_USER`
- `ROLE_ADMIN`

### Date/Time Handling

**Storage**: All timestamps stored in UTC

**Java Types**:
- `LocalDate`: Date only (no time)
- `LocalTime`: Time only (no date)
- `LocalDateTime`: Date and time
- `Instant`: Point in time (with timezone)

**Database Types**:
- `DATE`: Year-month-day
- `TIME`: Hour-minute-second
- `TIMESTAMP`: Full date-time

---

## Foreign Key Relationships

### Cascade Rules

**ON DELETE CASCADE**:
- `log_participants` → `logs`: Deleting log removes all participants
- `qsos` → `logs`: Deleting log removes all QSOs
- `invitations` → `logs`: Deleting log removes all invitations
- `user_roles` → `users`: Deleting user removes their roles
- `stations` → `users`: Deleting user removes their stations

**ON DELETE RESTRICT** (Implicit):
- `logs` → `users`: Cannot delete user who created logs (must delete logs first)
- `qsos` → `stations`: Cannot delete station with QSOs (must reassign QSOs first)

### Referential Integrity

All foreign keys enforce referential integrity:
- Cannot create QSO with non-existent log_id
- Cannot create log_participant with non-existent user_id or log_id
- Cannot delete log with active participants (violates cascade)

---

## Migration Strategy

### Schema Versioning

**Tool**: Flyway or Liquibase

**Migration Files**: Located in `src/main/resources/db/migration/`

**Naming Convention**:
```
V1__Initial_Schema.sql
V2__Add_Invitations_Table.sql
V3__Add_Contest_Data_Field.sql
```

### Example Migration (Flyway)

**V1__Initial_Schema.sql**:
```sql
-- Create users table (no email column — this system does not store email)
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    callsign    VARCHAR(20),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create logs table
CREATE TABLE logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    creator_id  BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- Add indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_logs_creator ON logs(creator_id);
```

**V2__Add_Log_Type.sql**:
```sql
-- Add type column to logs
ALTER TABLE logs ADD COLUMN type VARCHAR(20) DEFAULT 'PERSONAL' AFTER name;

-- Update existing logs to PERSONAL
UPDATE logs SET type = 'PERSONAL' WHERE type IS NULL;

-- Make NOT NULL
ALTER TABLE logs MODIFY COLUMN type VARCHAR(20) NOT NULL;
```

### Data Seeding

**Development Seeds**: `V999__Seed_Development_Data.sql`

```sql
-- Insert admin user (no email — bootstrapped via ADMIN_USERNAME/ADMIN_PASSWORD env vars)
INSERT INTO users (username, password, callsign)
VALUES ('admin', '$2a$10$...BCryptHash...', 'W1ABC');

-- Insert sample contests
INSERT INTO contests (contest_code, name, category, start_date, end_date)
VALUES ('CQWW-SSB', 'CQ World Wide DX Contest - SSB', 'DX',
        '2024-10-26 00:00:00', '2024-10-27 23:59:59');
```

---

## Database Compatibility

### SQLite (Development)

**Pros**:
- Zero configuration
- Single file database
- Perfect for field deployment
- Easy backup (copy file)

**Cons**:
- No concurrent writes (locks table)
- Limited ALTER TABLE support
- Some JSON functions missing

**Configuration**:
```properties
spring.datasource.url=jdbc:sqlite:hamradio.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

### PostgreSQL (Production)

**Pros**:
- Full ACID compliance
- Excellent concurrency
- Rich JSON support
- Advanced indexing (GiST, GIN)

**Cons**:
- Requires separate database server
- More complex deployment

**Configuration**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hamradio_logbook
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

---

## Query Examples

### Get User's Accessible Logs

```sql
SELECT l.*
FROM logs l
WHERE l.creator_id = ?  -- User is creator
   OR l.is_public = TRUE  -- Log is public
   OR EXISTS (  -- User is participant
       SELECT 1 FROM log_participants lp
       WHERE lp.log_id = l.id
         AND lp.user_id = ?
         AND lp.active = TRUE
   )
   AND l.active = TRUE
ORDER BY l.updated_at DESC;
```

### Get QSOs for Log with Pagination

```sql
SELECT q.*, s.station_name, o.callsign AS operator_callsign
FROM qsos q
LEFT JOIN stations s ON q.station_id = s.id
LEFT JOIN operators o ON q.operator_id = o.id
WHERE q.log_id = ?
ORDER BY q.created_at DESC
LIMIT ? OFFSET ?;
```

### Find Duplicate QSOs (Contest)

```sql
SELECT q.*
FROM qsos q
WHERE q.log_id = ?
  AND q.callsign = ?
  AND q.band = ?
  AND q.mode = ?
  AND q.contest_id = ?
ORDER BY q.qso_date, q.time_on;
```

### Get Pending Invitations for User

```sql
SELECT i.*, l.name AS log_name, u.username AS inviter_username
FROM invitations i
JOIN logs l ON i.log_id = l.id
JOIN users u ON i.inviter_id = u.id
WHERE i.invitee_id = ?
  AND i.status = 'PENDING'
  AND (i.expires_at IS NULL OR i.expires_at > CURRENT_TIMESTAMP)
ORDER BY i.created_at DESC;
```

### Get Log Statistics

```sql
SELECT
    COUNT(*) AS total_qsos,
    COUNT(DISTINCT callsign) AS unique_callsigns,
    COUNT(DISTINCT country) AS countries,
    COUNT(DISTINCT state) AS states,
    MIN(qso_date) AS first_qso,
    MAX(qso_date) AS last_qso
FROM qsos
WHERE log_id = ?;
```

---

## Backup and Restore

### SQLite Backup

**Simple File Copy**:
```bash
cp hamradio.db hamradio_backup_20240622.db
```

**Using SQLite Command**:
```bash
sqlite3 hamradio.db ".backup hamradio_backup.db"
```

### PostgreSQL Backup

**Full Database**:
```bash
pg_dump -U postgres hamradio_logbook > backup_20240622.sql
```

**Restore**:
```bash
psql -U postgres hamradio_logbook < backup_20240622.sql
```

**Automated Backups**:
```bash
# Daily cron job
0 2 * * * pg_dump -U postgres hamradio_logbook | gzip > /backups/hamradio_$(date +\%Y\%m\%d).sql.gz
```

---

## Performance Tuning

### Connection Pooling (HikariCP)

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### JPA/Hibernate Settings

```properties
# Show SQL (development only)
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Batch inserts
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Query caching
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.use_query_cache=true
```

### Database-Specific Tuning

**PostgreSQL**:
```sql
-- Analyze tables for query planner
ANALYZE qsos;
ANALYZE logs;

-- Vacuum to reclaim space
VACUUM ANALYZE;

-- Check slow queries
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

For additional help, see:
- [User Guide](USER_GUIDE.md)
- [Developer Guide](DEVELOPER_GUIDE.md)
- [API Reference](API_REFERENCE.md)
