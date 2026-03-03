# Database Design Documentation

## Overview
This database schema supports multi-station ham radio logging with flexible contest support, rig control integration, and real-time validation.

## Core Design Principles

1. **Flexible Contest Data**: The `contest_data` JSON field in QSO table allows storing contest-specific fields without schema changes
2. **Station-Centric**: Each QSO is tied to a specific station, supporting multi-station operations
3. **Validation Support**: Built-in validation status tracking and error logging
4. **Caching**: Callsign lookups are cached to minimize API calls in field deployments

## Table Descriptions

### `operators`
Tracks individual operators who may log contacts from different stations.

**Key Fields:**
- `callsign`: Operator's ham radio callsign (unique)
- `license_class`: Technician, General, Extra, etc.

### `stations`
Represents physical logging stations (radios). Multiple operators can use the same station.

**Key Fields:**
- `station_name`: Human-friendly identifier (e.g., "VHF Station", "20m CW")
- `callsign`: Station callsign (may differ from operator)
- `rig_control_*`: Configuration for Hamlib integration

### `contests`
Contest definitions and configurations. Supports multiple simultaneous contests.

**Key Fields:**
- `contest_code`: Unique identifier (e.g., "ARRL-FD-2024", "POTA")
- `validator_class`: Java class implementing contest-specific validation
- `rules_config`: JSON defining required/optional fields

**Example `rules_config` for ARRL Field Day:**
```json
{
  "required_fields": ["class", "section"],
  "optional_fields": ["power"],
  "valid_classes": ["1A", "1B", "2A", "3A", "4A", "5A"],
  "scoring": {
    "phone": 1,
    "cw": 2,
    "digital": 2
  }
}
```

**Example for POTA:**
```json
{
  "required_fields": ["park_ref"],
  "validation_api": "https://api.pota.app/park/{park_ref}",
  "scoring": {
    "park_to_park": 2,
    "hunter": 1
  }
}
```

### `qsos`
Main contact log with standard ADIF fields plus contest-specific JSON data.

**Contest Data Examples:**

**ARRL Field Day:**
```json
{
  "class": "2A",
  "section": "ORG",
  "power": "LOW"
}
```

**POTA:**
```json
{
  "park_ref": "K-0817",
  "hunter_ref": "K-4566",
  "park_to_park": true
}
```

**SOTA:**
```json
{
  "summit_ref": "W7W/NG-001",
  "summit_name": "Mount Hood",
  "points": 10
}
```

**Winter Field Day:**
```json
{
  "class": "3O",
  "section": "OR",
  "indoor_outdoor": "OUTDOOR"
}
```

### `callsign_cache`
Caches callsign lookups from QRZ/FCC APIs to minimize bandwidth usage.

**Key Fields:**
- `lookup_source`: "QRZ", "FCC", "HAMDB", etc.
- `expires_at`: Cache expiration (typically 1 hour)

### `rig_telemetry`
Optional table for logging rig state over time. Useful for debugging and analytics.

## Querying Contest-Specific Data

### Find all POTA activations:
```sql
SELECT callsign, json_extract(contest_data, '$.park_ref') as park
FROM qsos
WHERE contest_data LIKE '%park_ref%'
```

### Find Field Day 2A entries:
```sql
SELECT callsign, frequency_khz, mode
FROM qsos
WHERE contest_id = (SELECT id FROM contests WHERE contest_code = 'ARRL-FD-2024')
  AND json_extract(contest_data, '$.class') = '2A'
```

## Migration Strategy

When adding support for a new contest:

1. Create contest configuration:
```sql
INSERT INTO contests (contest_code, contest_name, validator_class, rules_config)
VALUES ('WFD-2024', 'Winter Field Day 2024',
        'com.hamradio.logbook.validation.WinterFieldDayValidator',
        '{"required_fields": ["class", "section", "indoor_outdoor"]}');
```

2. Implement validator class extending `ContestValidator` interface

3. No schema changes needed - contest-specific data goes in JSON field

## Performance Considerations

- **Indexes** on callsign, date, station_id, contest_id for fast queries
- **JSON queries** supported by SQLite 3.38+ (json_extract, json_valid)
- **Partitioning** not needed - field deployment typically < 10K QSOs per event
- **Caching** reduces external API calls by 90%+ in typical contests

## ADIF Export Mapping

Standard fields map directly to ADIF. Contest data is exported as:
- `APP_CONTEST_CLASS`, `APP_CONTEST_SECTION`, etc. for Field Day
- `SIG: POTA`, `SIG_INFO: K-0817` for POTA
- `SOTA_REF: W7W/NG-001` for SOTA

See `AdifExportService.java` for full mapping logic.
