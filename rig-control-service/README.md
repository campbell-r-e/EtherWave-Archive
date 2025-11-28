# Rig Control Service

Lightweight microservice that interfaces with Hamlib `rigctld` to provide rig control and telemetry for the ham radio logbook system.

## Overview

This service runs **locally on each client machine** and:
- Communicates with Hamlib `rigctld` via TCP sockets
- Provides REST API for rig control (frequency, mode, PTT)
- Polls rig status periodically (every 2 seconds)
- Optionally sends telemetry to the main backend API

## Prerequisites

### 1. Install Hamlib

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

### 2. Start rigctld

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

## Running the Service

### Option 1: Maven
```bash
cd rig-control-service
mvn spring-boot:run
```

### Option 2: With Custom Configuration
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --station.id=1 \
  --station.name=20m-Station \
  --rigctld.host=localhost \
  --rigctld.port=4532 \
  --backend.api.url=http://192.168.1.100:8080"
```

### Option 3: JAR File
```bash
mvn package
java -jar target/rig-control-service-1.0.0-SNAPSHOT.jar \
  --station.id=2 \
  --station.name=VHF-Station
```

## API Endpoints

### Get Rig Status
```bash
curl http://localhost:8081/api/rig/status
```

Response:
```json
{
  "frequencyHz": 14250000,
  "mode": "USB",
  "bandwidth": "3000",
  "pttActive": false,
  "sMeter": -73,
  "connected": true,
  "timestamp": "2024-11-27T19:30:00"
}
```

### Set Frequency
```bash
curl -X POST "http://localhost:8081/api/rig/frequency?hz=7200000"
```

### Set Mode
```bash
curl -X POST "http://localhost:8081/api/rig/mode?mode=CW&bandwidth=500"
```

### Test Connection
```bash
curl http://localhost:8081/api/rig/test
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# rigctld connection
rigctld.host=localhost
rigctld.port=4532

# Backend API (main logbook server)
backend.api.url=http://localhost:8080
backend.api.enabled=true

# Station identification
station.id=1
station.name=HF-Station

# Telemetry polling
telemetry.polling.enabled=true
telemetry.polling.interval.ms=2000
```

## Architecture

```
┌─────────────────┐      TCP Socket       ┌──────────────┐
│  Rig Control    │ ◄──────────────────► │   rigctld    │
│    Service      │    (Hamlib Protocol)  │   (Hamlib)   │
│  (Port 8081)    │                       │              │
└────────┬────────┘                       └──────┬───────┘
         │                                       │
         │ HTTP/REST                            │ Serial/USB
         │                                       │
         ▼                                       ▼
┌─────────────────┐                       ┌──────────────┐
│  Main Backend   │                       │  Radio Rig   │
│   (Port 8080)   │                       │  (Hardware)  │
└─────────────────┘                       └──────────────┘
```

## Deployment Scenarios

### Single Station (Simple)
- Run rig control service on the same machine as the main backend
- `backend.api.url=http://localhost:8080`

### Multi-Station (Field Day)
- Run rig control service on **each client machine**
- Point to main backend: `backend.api.url=http://192.168.1.100:8080`
- Each service uses unique `station.id`

Example for 3 stations:
```bash
# Station 1 (20m CW)
java -jar rig-control-service.jar --station.id=1 --server.port=8081

# Station 2 (40m SSB)
java -jar rig-control-service.jar --station.id=2 --server.port=8082

# Station 3 (VHF FM)
java -jar rig-control-service.jar --station.id=3 --server.port=8083
```

## Supported Rigs

The service supports **any rig that Hamlib supports**. Common models:

- **Yaesu**: FT-991A, FT-891, FT-857D, FT-817ND
- **Icom**: IC-7300, IC-9700, IC-705, IC-7610
- **Kenwood**: TS-2000, TS-590SG, TS-480
- **Elecraft**: K3, KX3, KX2
- **Flex**: 6000 series

Check full list: `rigctl --list`

## Troubleshooting

### Cannot connect to rigctld
```bash
# Check if rigctld is running
ps aux | grep rigctld

# Test manually
telnet localhost 4532
```

### Permission denied on serial port (Linux)
```bash
sudo usermod -a -G dialout $USER
# Log out and back in
```

### Rig not responding
- Check serial cable connection
- Verify baud rate matches rig settings
- Check rig's CAT/CI-V settings are enabled

## Health Check

```bash
curl http://localhost:8081/actuator/health
```

Response:
```json
{
  "status": "UP"
}
```
