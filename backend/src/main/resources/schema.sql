-- Ham Radio Logbook Database Schema (SQLite)
-- Supports multi-station logging with flexible contest data

-- Operators table: tracks individual operators
CREATE TABLE IF NOT EXISTS operators (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    callsign VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    email VARCHAR(100),
    license_class VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stations table: represents logging stations (radios)
CREATE TABLE IF NOT EXISTS stations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    station_name VARCHAR(50) NOT NULL UNIQUE,
    callsign VARCHAR(20) NOT NULL,
    location VARCHAR(100),
    grid_square VARCHAR(10),
    antenna VARCHAR(200),
    power_watts INTEGER,
    rig_model VARCHAR(100),
    rig_control_enabled BOOLEAN DEFAULT FALSE,
    rig_control_host VARCHAR(50),
    rig_control_port INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Contests table: contest configurations and rules
CREATE TABLE IF NOT EXISTS contests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contest_code VARCHAR(50) NOT NULL UNIQUE,
    contest_name VARCHAR(200) NOT NULL,
    description TEXT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    validator_class VARCHAR(255),
    rules_config TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- QSO table: main contact log
CREATE TABLE IF NOT EXISTS qsos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Core QSO fields (standard across all contests)
    station_id INTEGER NOT NULL,
    operator_id INTEGER,
    contest_id INTEGER,

    callsign VARCHAR(20) NOT NULL,
    frequency_khz BIGINT NOT NULL,
    mode VARCHAR(20) NOT NULL,
    qso_date DATE NOT NULL,
    time_on TIME NOT NULL,
    time_off TIME,

    rst_sent VARCHAR(10),
    rst_rcvd VARCHAR(10),

    band VARCHAR(10),
    power_watts INTEGER,

    -- Location data
    grid_square VARCHAR(10),
    county VARCHAR(50),
    state VARCHAR(50),
    country VARCHAR(50),
    dxcc INTEGER,
    cq_zone INTEGER,
    itu_zone INTEGER,

    -- Operator info from validation
    name VARCHAR(100),
    license_class VARCHAR(20),

    -- Contest-specific data (JSON)
    -- Examples: {"section": "ORG", "class": "2A", "park_ref": "K-0817", "summit_ref": "W7W/NG-001"}
    contest_data TEXT,

    -- QSL and confirmation
    qsl_sent VARCHAR(1),
    qsl_rcvd VARCHAR(1),
    lotw_sent VARCHAR(1),
    lotw_rcvd VARCHAR(1),

    -- Validation and status
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,

    -- Comments and notes
    notes TEXT,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (station_id) REFERENCES stations(id),
    FOREIGN KEY (operator_id) REFERENCES operators(id),
    FOREIGN KEY (contest_id) REFERENCES contests(id)
);

-- Callsign validation cache
CREATE TABLE IF NOT EXISTS callsign_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    callsign VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    address VARCHAR(200),
    state VARCHAR(50),
    country VARCHAR(50),
    license_class VARCHAR(20),
    grid_square VARCHAR(10),
    lookup_source VARCHAR(50),
    cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Rig telemetry log (optional, for tracking rig state over time)
CREATE TABLE IF NOT EXISTS rig_telemetry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    station_id INTEGER NOT NULL,
    frequency_khz BIGINT,
    mode VARCHAR(20),
    ptt_active BOOLEAN,
    s_meter INTEGER,
    alc_level INTEGER,
    swr DECIMAL(4,2),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (station_id) REFERENCES stations(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_qso_callsign ON qsos(callsign);
CREATE INDEX IF NOT EXISTS idx_qso_date ON qsos(qso_date);
CREATE INDEX IF NOT EXISTS idx_qso_station ON qsos(station_id);
CREATE INDEX IF NOT EXISTS idx_qso_contest ON qsos(contest_id);
CREATE INDEX IF NOT EXISTS idx_qso_frequency ON qsos(frequency_khz);
CREATE INDEX IF NOT EXISTS idx_callsign_cache_callsign ON callsign_cache(callsign);
CREATE INDEX IF NOT EXISTS idx_rig_telemetry_station ON rig_telemetry(station_id);
CREATE INDEX IF NOT EXISTS idx_rig_telemetry_timestamp ON rig_telemetry(timestamp);
