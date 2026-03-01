# Ham Radio Logbook - Rig Control Setup Guide

Complete guide for setting up and configuring radio control integration using Hamlib.

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Hardware Requirements](#hardware-requirements)
- [Software Requirements](#software-requirements)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
  - [Docker Deployment](#docker-deployment)
  - [Native Deployment](#native-deployment)
  - [Multi-Station Setup](#multi-station-setup)
- [Radio Configuration](#radio-configuration)
- [Supported Radios](#supported-radios)
- [Troubleshooting](#troubleshooting)
- [Advanced Configuration](#advanced-configuration)

---

## Overview

The Ham Radio Logbook system includes optional rig control integration that provides:

- **Live Frequency Updates**: Automatically captures current operating frequency
- **Mode Detection**: Detects USB, LSB, CW, RTTY, etc.
- **PTT Status**: Shows when you're transmitting
- **S-Meter Reading**: Signal strength monitoring
- **Auto-Fill QSO Entry**: Frequency and mode automatically populate in log entry form

**Important**: Rig control is **completely optional**. The logbook works perfectly without it for manual QSO entry.

---

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                    Radio (Transceiver)                       │
│  Yaesu FT-991A, Icom IC-7300, Kenwood TS-590, etc.         │
└─────────────────────────────────────────────────────────────┘
                          ↓ USB Cable (CAT Control)
┌─────────────────────────────────────────────────────────────┐
│               Computer USB Port (/dev/ttyUSB0)              │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  rigctld (Hamlib Daemon)                                    │
│  - Communicates with radio via serial port                  │
│  - Listens on TCP port 4532                                 │
│  - Responds to Hamlib protocol commands                     │
└─────────────────────────────────────────────────────────────┘
                          ↓ TCP Socket (localhost:4532)
┌─────────────────────────────────────────────────────────────┐
│  Rig Control Service (Java Spring Boot)                     │
│  - Polls rigctld for frequency, mode, PTT status            │
│  - Exposes REST API on port 8081                            │
│  - Returns JSON: {frequencyHz, mode, pttActive, ...}        │
└─────────────────────────────────────────────────────────────┘
                          ↓ HTTP REST API (localhost:8081)
┌─────────────────────────────────────────────────────────────┐
│  Frontend (Angular)                                          │
│  - Polls rig status every 2 seconds                         │
│  - Displays live frequency, mode, PTT in UI                 │
│  - Auto-fills QSO entry form with current rig settings      │
└─────────────────────────────────────────────────────────────┘
```

**Key Components**:
1. **Hamlib** (`rigctld`): Industry-standard radio control library
2. **Rig Control Service**: Java service that interfaces with rigctld
3. **Frontend Component**: Angular component that displays rig status

---

## Hardware Requirements

### Required
- **Amateur radio transceiver** with CAT (Computer Aided Transceiver) control
- **USB cable** or **Serial-to-USB adapter** (usually included with radio)
- **Computer** with available USB port

### Verified Radios
The following radios have been tested with this system:
- Yaesu FT-991/FT-991A
- Icom IC-7300
- Kenwood TS-590S/SG
- Elecraft K3/K3S
- Yaesu FT-891
- Icom IC-9700

**Note**: Hamlib supports 200+ radio models. See [Supported Radios](#supported-radios) for full list.

### Cable Requirements
- **Yaesu**: Usually USB-B to USB-A cable (included with radio)
- **Icom**: USB-B to USB-A cable (included with radio)
- **Kenwood**: May require specific Kenwood USB cable
- **Older radios**: May need serial-to-USB adapter (FTDI chipset recommended)

---

## Software Requirements

### For Docker Deployment
- Docker Engine 20.10+
- Docker Compose 2.0+
- **No additional software needed** - Hamlib included in container

### For Native Deployment
- **Hamlib 4.0+**
  - Ubuntu/Debian: `sudo apt-get install libhamlib4 libhamlib-utils`
  - macOS: `brew install hamlib`
  - Windows: Download from https://hamlib.github.io/
- **Java 25** (for rig-control-service)
- **USB driver** for your radio (if not built into OS)

---

## Quick Start

### Option 1: Docker (Recommended)

1. **Connect your radio** via USB cable

2. **Find USB device**:
   ```bash
   # Linux
   ls -l /dev/ttyUSB* /dev/ttyACM*
   # Output: /dev/ttyUSB0 (or similar)

   # macOS
   ls -l /dev/cu.usbserial* /dev/cu.SLAB*
   # Output: /dev/cu.usbserial-A1234567
   ```

3. **Find your rig model number**:
   ```bash
   # List all supported rigs
   docker run --rm eclipse-temurin:25-jre-jammy sh -c "
     apt-get update -qq &&
     apt-get install -y -qq software-properties-common &&
     add-apt-repository universe &&
     apt-get update -qq &&
     apt-get install -y -qq hamlib-utils &&
     rigctl --list | grep -i 'yaesu\|icom\|kenwood'
   "
   ```

   Common models:
   - Yaesu FT-991A: `1035`
   - Icom IC-7300: `3073`
   - Kenwood TS-590SG: `2014`
   - Elecraft K3: `2029`

4. **Edit docker-compose.yml**:
   ```bash
   cd Hamradiologbook
   nano docker-compose.yml
   ```

   Uncomment the rig-control section (lines 71-100) and configure:
   ```yaml
   rig-control:
     build:
       context: ./rig-control-service
       dockerfile: Dockerfile
     container_name: hamradio-rig-control-1
     ports:
       - "8081:8081"
       - "4532:4532"
     environment:
       STATION_ID: 1
       STATION_NAME: "Station-1"
       RIGCTLD_HOST: localhost
       RIGCTLD_PORT: 4532
       BACKEND_API_URL: http://backend:8080
     devices:
       - "/dev/ttyUSB0:/dev/ttyUSB0"  # ← CHANGE THIS to your device
     depends_on:
       - backend
     networks:
       - hamradio-network
     command: >
       sh -c "
         rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 &
         sleep 2
         java -jar app.jar
       "
   ```

   **Key settings to change**:
   - `/dev/ttyUSB0` → Your USB device path
   - `-m 1035` → Your rig model number
   - `-s 38400` → Your radio's baud rate (check radio manual)

5. **Start services**:
   ```bash
   docker compose up -d
   ```

6. **Verify rig control**:
   ```bash
   # Check logs
   docker compose logs rig-control

   # Should see:
   # "rigctld started successfully"
   # "Connected to rigctld"

   # Test connection
   curl http://localhost:8081/api/rig/status
   # Should return: {"frequencyHz":14074000,"mode":"USB","connected":true,...}
   ```

7. **Open frontend**: http://localhost
   - Look for " Rig Status" panel
   - Should show your current frequency and mode!

---

## Detailed Setup

### Docker Deployment

#### Step 1: Identify USB Device

**Linux**:
```bash
# Before connecting radio
ls -l /dev/ttyUSB* /dev/ttyACM*

# Connect radio via USB

# After connecting
ls -l /dev/ttyUSB* /dev/ttyACM*
# New device appears (e.g., /dev/ttyUSB0)

# Check device permissions
ls -l /dev/ttyUSB0
# Output: crw-rw---- 1 root dialout 188, 0 Nov 28 10:00 /dev/ttyUSB0

# Add your user to dialout group
sudo usermod -a -G dialout $USER
# Log out and back in for changes to take effect
```

**macOS**:
```bash
# List serial devices
ls -l /dev/cu.*

# Common device names:
# /dev/cu.usbserial-XXXXXXXX  (FTDI chipset)
# /dev/cu.SLAB_USBtoUART      (Silicon Labs chipset)
# /dev/cu.usbmodem14101       (CDC ACM device)
```

**Windows (using WSL2)**:
```bash
# Windows requires USB passthrough to WSL2
# See: https://docs.microsoft.com/en-us/windows/wsl/connect-usb

# Or run natively on Windows (not in Docker)
```

#### Step 2: Determine Rig Model Number

```bash
# Quick search for common manufacturers
rigctl --list | grep -i "yaesu"
rigctl --list | grep -i "icom"
rigctl --list | grep -i "kenwood"
rigctl --list | grep -i "elecraft"

# Full list (200+ models)
rigctl --list

# Example output:
# Rig #  Mfg          Model           Version
# 1035   Yaesu        FT-991          2021-01-01
# 3073   Icom         IC-7300         2021-01-01
# 2014   Kenwood      TS-590SG        2021-01-01
```

**Model numbers for popular rigs**:

| Manufacturer | Model | Hamlib ID | Common Baud Rate |
|-------------|-------|-----------|------------------|
| Yaesu | FT-991/FT-991A | 1035 | 38400 |
| Yaesu | FT-891 | 1035 | 38400 |
| Yaesu | FT-450D | 1026 | 38400 |
| Yaesu | FT-817 | 1020 | 9600 |
| Icom | IC-7300 | 3073 | 19200 |
| Icom | IC-9700 | 3081 | 19200 |
| Icom | IC-7610 | 3080 | 19200 |
| Kenwood | TS-590SG | 2014 | 115200 |
| Kenwood | TS-890S | 2033 | 115200 |
| Elecraft | K3/K3S | 2029 | 38400 |
| Elecraft | KX3 | 2043 | 38400 |

#### Step 3: Configure Radio Settings

**Most radios require CAT control to be enabled**:

**Yaesu FT-991A**:
1. Press `MENU` button
2. Navigate to: `030 CAT RATE` → Set to `38400 bps`
3. Navigate to: `031 CAT TOT` → Set to `100 msec`
4. Navigate to: `032 CAT RTS` → Set to `ENABLE`
5. Press `MENU` to exit

**Icom IC-7300**:
1. Press `MENU` button
2. Navigate to: `SET > Connectors > USB SEND/Keying`
3. Set `CI-V (Echo Back)` → `ON`
4. Set `CI-V Address` → `94` (default)
5. Set `CI-V Baud Rate` → `Auto` or `19200`
6. Press `EXIT`

**Kenwood TS-590SG**:
1. Press `MENU` button
2. Navigate to: `Menu 52 (COM)` → Set to `38400 bps` or `115200 bps`
3. Press `MENU` to exit
4. Ensure COM port is set to USB

#### Step 4: Configure docker-compose.yml

Edit the rig-control service section:

```yaml
rig-control:
  build:
    context: ./rig-control-service
    dockerfile: Dockerfile
  container_name: hamradio-rig-control-1
  ports:
    - "8081:8081"    # Rig control REST API
    - "4532:4532"    # rigctld port
  environment:
    # Station identification
    STATION_ID: 1
    STATION_NAME: "HF-Station"

    # rigctld connection
    RIGCTLD_HOST: localhost
    RIGCTLD_PORT: 4532

    # Backend API (for telemetry storage)
    BACKEND_API_URL: http://backend:8080

    # Optional: QRZ API for callsign lookup
    QRZ_USERNAME: ${QRZ_USERNAME:-}
    QRZ_PASSWORD: ${QRZ_PASSWORD:-}

  # USB device passthrough
  devices:
    - "/dev/ttyUSB0:/dev/ttyUSB0"  # Linux
    # - "/dev/cu.usbserial-A1234567:/dev/ttyUSB0"  # macOS

  # Alternative: privileged mode (less secure)
  # privileged: true

  depends_on:
    - backend

  networks:
    - hamradio-network

  # Start rigctld daemon, then start Java service
  command: >
    sh -c "
      echo 'Starting rigctld...'
      rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 -vvvvv &
      sleep 3
      echo 'Starting rig control service...'
      java -jar app.jar
    "
```

**Command breakdown**:
- `rigctld`: Hamlib daemon
- `-m 1035`: Model number (Yaesu FT-991A)
- `-r /dev/ttyUSB0`: Serial device
- `-s 38400`: Baud rate (must match radio setting)
- `-t 4532`: TCP port to listen on
- `-vvvvv`: Verbose logging (5 levels, use `-v` for less)
- `&`: Run in background
- `sleep 3`: Wait for rigctld to start
- `java -jar app.jar`: Start Java service

#### Step 5: Start and Test

```bash
# Start all services
docker compose up -d

# Watch rig control logs
docker compose logs -f rig-control

# Expected output:
# rigctld started successfully
# Rig control service started
# Connected to rigctld at localhost:4532
# Current frequency: 14074000 Hz
# Current mode: USB

# Test rigctld directly
docker exec hamradio-rig-control-1 rigctl -m 1035 -r /dev/ttyUSB0 f
# Output: 14074000

# Test REST API
curl http://localhost:8081/api/rig/status
# Output: {"frequencyHz":14074000,"mode":"USB","connected":true,...}

# Test from another container
docker exec hamradio-backend curl http://rig-control:8081/api/rig/status
```

---

### Native Deployment

For better performance or when Docker isn't suitable (e.g., Field Day operations).

#### Step 1: Install Hamlib

**Ubuntu/Debian**:
```bash
sudo apt-get update
sudo apt-get install -y libhamlib4 libhamlib-utils
rigctl --version
# Output: rigctl Hamlib 4.5.5
```

**macOS**:
```bash
brew install hamlib
rigctl --version
```

**Verify installation**:
```bash
# List supported rigs
rigctl --list | head -20

# Test with your rig
rigctl -m 1035 -r /dev/ttyUSB0 -s 38400 f
# Should output current frequency
```

#### Step 2: Start rigctld Manually

```bash
# Start rigctld daemon
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 -vvvvv

# In another terminal, test connection
rigctl -m 2 f  # -m 2 = NET rigctl (connects to rigctld)
```

#### Step 3: Build and Run Rig Control Service

```bash
cd rig-control-service

# Build
mvn clean package -DskipTests

# Run (configure via environment variables)
export RIGCTLD_HOST=localhost
export RIGCTLD_PORT=4532
export STATION_ID=1
export STATION_NAME="Station-1"
export BACKEND_API_URL=http://localhost:8080

java -jar target/rig-control-service-1.0.0-SNAPSHOT.jar
```

#### Step 4: Configure as systemd Service (Linux)

Create `/etc/systemd/system/rigctld.service`:

```ini
[Unit]
Description=Hamlib rigctld for Yaesu FT-991A
After=network.target

[Service]
Type=simple
User=hamradio
ExecStart=/usr/bin/rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Create `/etc/systemd/system/rig-control.service`:

```ini
[Unit]
Description=Ham Radio Logbook Rig Control Service
After=network.target rigctld.service
Requires=rigctld.service

[Service]
Type=simple
User=hamradio
WorkingDirectory=/opt/hamradio-logbook/rig-control
Environment="RIGCTLD_HOST=localhost"
Environment="RIGCTLD_PORT=4532"
Environment="STATION_ID=1"
ExecStart=/usr/bin/java -jar rig-control-service-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable rigctld
sudo systemctl enable rig-control
sudo systemctl start rigctld
sudo systemctl start rig-control

# Check status
sudo systemctl status rigctld
sudo systemctl status rig-control
```

---

### Multi-Station Setup

For Field Day, contests, or multi-operator stations.

#### Scenario: 3 Stations with Rig Control

**Architecture**:
```
┌──────────────────────────────────────────────────────────┐
│               Server (Backend + Frontend)                 │
│  IP: 192.168.1.100                                        │
└──────────────────────────────────────────────────────────┘
                          ↑
          ┌───────────────┼───────────────┐
          ↓               ↓               ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Station 1  │  │  Station 2  │  │  Station 3  │
│  Laptop 1   │  │  Laptop 2   │  │  Laptop 3   │
│  FT-991A    │  │  IC-7300    │  │  TS-590SG   │
│  :8081      │  │  :8082      │  │  :8083      │
└─────────────┘  └─────────────┘  └─────────────┘
```

**Server Setup**:
```bash
# On server: Run backend and frontend only
docker compose up -d backend frontend postgres
```

**Station 1 Setup** (Laptop 1 - Yaesu FT-991A):
```bash
# Start rigctld
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 &

# Start rig control service
export RIGCTLD_HOST=localhost
export RIGCTLD_PORT=4532
export STATION_ID=1
export STATION_NAME="20m-SSB-Station"
export BACKEND_API_URL=http://192.168.1.100:8080
export SERVER_PORT=8081

java -jar rig-control-service.jar
```

**Station 2 Setup** (Laptop 2 - Icom IC-7300):
```bash
rigctld -m 3073 -r /dev/ttyUSB0 -s 19200 -t 4532 &

export RIGCTLD_HOST=localhost
export RIGCTLD_PORT=4532
export STATION_ID=2
export STATION_NAME="40m-CW-Station"
export BACKEND_API_URL=http://192.168.1.100:8080
export SERVER_PORT=8082

java -jar rig-control-service.jar
```

**Station 3 Setup** (Laptop 3 - Kenwood TS-590SG):
```bash
rigctld -m 2014 -r /dev/ttyUSB0 -s 115200 -t 4532 &

export RIGCTLD_HOST=localhost
export RIGCTLD_PORT=4532
export STATION_ID=3
export STATION_NAME="80m-Digital-Station"
export BACKEND_API_URL=http://192.168.1.100:8080
export SERVER_PORT=8083

java -jar rig-control-service.jar
```

**Frontend Configuration**:
Each station opens frontend at `http://192.168.1.100` and configures their rig service URL in settings.

---

## Radio Configuration

### Common Issues and Solutions

#### Issue: "Cannot open /dev/ttyUSB0"

**Solution 1**: Check permissions
```bash
ls -l /dev/ttyUSB0
# Output: crw-rw---- 1 root dialout

sudo usermod -a -G dialout $USER
# Log out and back in
```

**Solution 2**: Use privileged mode (Docker)
```yaml
rig-control:
  privileged: true
```

#### Issue: "Communication timeout"

**Cause**: Wrong baud rate

**Solution**: Check radio manual for CAT baud rate, update `-s` parameter:
```bash
# Try common baud rates
rigctld -m 1035 -r /dev/ttyUSB0 -s 4800 -t 4532    # Very old radios
rigctld -m 1035 -r /dev/ttyUSB0 -s 9600 -t 4532    # Common
rigctld -m 1035 -r /dev/ttyUSB0 -s 19200 -t 4532   # Icom default
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532   # Yaesu default
rigctld -m 1035 -r /dev/ttyUSB0 -s 115200 -t 4532  # Kenwood
```

#### Issue: "Wrong model"

**Solution**: Verify model number
```bash
# Some radios are compatible with similar models
# FT-991 and FT-991A both use model 1035
# Try compatible models if exact model doesn't work

rigctl --list | grep -i "yaesu"
```

---

## Supported Radios

### Full List Command
```bash
rigctl --list
```

### Major Manufacturers

**Yaesu** (140+ models):
- FT-991/FT-991A (m=1035)
- FT-891 (m=1035)
- FT-450D (m=1026)
- FT-817ND (m=1020)
- FT-857D (m=1021)
- FT-897D (m=1022)
- FT-DX10 (m=1043)

**Icom** (180+ models):
- IC-7300 (m=3073)
- IC-9700 (m=3081)
- IC-7610 (m=3080)
- IC-7851 (m=3085)
- IC-718 (m=311)
- IC-746PRO (m=3009)

**Kenwood** (90+ models):
- TS-590SG (m=2014)
- TS-890S (m=2033)
- TS-2000 (m=2004)
- TS-480 (m=2006)

**Elecraft**:
- K3/K3S (m=2029)
- KX3 (m=2043)
- KX2 (m=2045)

**Others**:
- FlexRadio (m=2050+)
- Apache Labs ANAN (m=2060+)
- And 100+ more...

---

## Troubleshooting

### Debugging Commands

```bash
# Test rigctld connection
telnet localhost 4532
# Type: f
# Should return frequency

# Check rigctld logs (Docker)
docker compose logs rig-control | grep rigctld

# Test with verbose mode
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 -vvvvv

# Check if device exists
ls -l /dev/ttyUSB0

# Check if rigctld is running
ps aux | grep rigctld
netstat -tuln | grep 4532

# Test REST API
curl -v http://localhost:8081/api/rig/status
curl http://localhost:8081/api/rig/test
```

### Common Error Messages

**"rig_open: error = IO error"**
- Check USB cable is connected
- Verify correct device path
- Check device permissions
- Try different USB port

**"rig_open: error = Invalid argument"**
- Wrong model number
- Wrong baud rate
- Radio not in CAT mode

**"Connection refused"**
- rigctld not running
- Wrong port number
- Firewall blocking port 4532

**"Device or resource busy"**
- Another application using serial port
- Kill other apps: `sudo lsof /dev/ttyUSB0`

### Performance Tuning

**Reduce polling interval** (frontend):
```typescript
// rig-status.component.ts
interval(5000)  // Poll every 5 seconds instead of 2
```

**Adjust rigctld verbosity**:
```bash
# Less verbose (production)
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532

# Debug mode
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 -vvvvv
```

---

## Advanced Configuration

### Using Remote rigctld

Run rigctld on one computer, connect from another:

**Server** (connected to radio):
```bash
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -T 0.0.0.0 -t 4532
# -T 0.0.0.0 allows connections from any IP
```

**Client** (rig-control-service):
```yaml
environment:
  RIGCTLD_HOST: 192.168.1.50  # Server IP
  RIGCTLD_PORT: 4532
```

### Multiple Rigs on Same Computer

```bash
# Rig 1 on port 4532
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 &

# Rig 2 on port 4533
rigctld -m 3073 -r /dev/ttyUSB1 -s 19200 -t 4533 &

# Start two rig control services
RIGCTLD_PORT=4532 SERVER_PORT=8081 STATION_ID=1 java -jar rig-control.jar &
RIGCTLD_PORT=4533 SERVER_PORT=8082 STATION_ID=2 java -jar rig-control.jar &
```

### Serial Port Settings

Some radios require specific serial settings:

```bash
# Configure serial port (Linux)
stty -F /dev/ttyUSB0 38400 cs8 -cstopb -parenb -crtscts

# Or use rigctld parameters
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -d 8 -p n -f 1 -t 4532
# -d 8: data bits
# -p n: no parity
# -f 1: 1 stop bit
```

### Integration with Other Software

Rig control can be shared with other logging software:

```bash
# Start rigctld once
rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 &

# Ham Radio Logbook uses it
# WSJT-X can also connect to localhost:4532
# fldigi can connect to localhost:4532
# All share the same rigctld instance
```

---

## Next Steps

- [SETUP.md](SETUP.md) - General system setup
- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - Docker deployment guide
- [DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) - Developer documentation
- [Hamlib Documentation](https://hamlib.github.io/) - Official Hamlib docs

---

**73 and happy logging!**
