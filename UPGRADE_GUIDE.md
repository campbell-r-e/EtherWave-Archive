# Upgrade Guide - Java 25 & Spring Boot 4.0.0

This guide helps you upgrade from previous versions to the latest release with Java 25 and Spring Boot 4.0.0.

## Table of Contents

- [What's New](#whats-new)
- [Breaking Changes](#breaking-changes)
- [Upgrade Steps](#upgrade-steps)
- [For Developers](#for-developers)
- [For System Administrators](#for-system-administrators)
- [Troubleshooting](#troubleshooting)
- [Rollback Instructions](#rollback-instructions)

## What's New

### Major Framework Upgrades

| Component | Old Version | New Version | Key Changes |
|-----------|-------------|-------------|-------------|
| **Java** | 17/21 | **25.0.1** | Latest JDK with performance improvements |
| **Spring Boot** | 3.2.0 | **4.0.0** | Major version upgrade with new features |
| **Spring Framework** | 6.x | **7.0.0** | Core framework upgrade |
| **Hibernate** | 6.6.x | **7.1.8** | ORM improvements and bug fixes |
| **Apache Tomcat** | 10.1.x | **11.0.14** | Embedded servlet container upgrade |
| **Jackson** | 2.x (Fasterxml) | **3.0** (tools.jackson) | JSON processing library upgrade |
| **Lombok** | 1.18.34 | **1.18.38** | Java 25 compatibility |
| **JWT (JJWT)** | 0.12.3 | **0.12.6** | Security improvements |
| **SQLite JDBC** | 3.44.1.0 | **3.47.2.0** | Latest database driver |

### New Features

- **Java 25 Support**: Latest JDK features and performance improvements
- **Spring Security 7.x**: Enhanced security with updated APIs
- **Jackson 3.0**: Improved JSON processing with breaking changes
- **Better Performance**: Overall application performance improvements

## Breaking Changes

### 1. Java Version Requirement

**Old**: Java 17 or 21
**New**: Java 25 (minimum 25.0.1)

**Action Required**:
- Install Java 25 from [Eclipse Adoptium](https://adoptium.net/)
- Update JAVA_HOME environment variable
- Update Docker images if using custom builds

### 2. Spring Security API Changes

The `DaoAuthenticationProvider` API has changed in Spring Security 7.

**Old Code**:
```java
DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
authProvider.setUserDetailsService(customUserDetailsService);
authProvider.setPasswordEncoder(passwordEncoder());
```

**New Code**:
```java
DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
authProvider.setPasswordEncoder(passwordEncoder());
```

**Impact**: SecurityConfig.java has been updated automatically.

### 3. Jackson Configuration Changes

Jackson 3.0 (tools.jackson) removed some serialization features.

**Removed Property**:
```properties
# This no longer works in Jackson 3.0
spring.jackson.serialization.write-dates-as-timestamps=false
```

**New Behavior**:
- Dates are now serialized in ISO-8601 format by default
- No configuration needed for standard date handling
- Custom serializers may need updates

**Impact**: Application properties have been updated.

### 4. Lombok Annotation Processing

Java 25 requires explicit annotation processor configuration.

**Required Maven Configuration**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.38</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Impact**: Both backend and rig-control-service pom.xml files have been updated.

## Upgrade Steps

### For Docker Users (Recommended - Zero Downtime)

1. **Pull the latest code**:
   ```bash
   cd Hamradiologbook
   git pull origin main
   ```

2. **Rebuild Docker images**:
   ```bash
   docker-compose build --no-cache
   ```

3. **Stop current services**:
   ```bash
   docker-compose down
   ```

4. **Start updated services**:
   ```bash
   docker-compose up -d
   ```

5. **Verify deployment**:
   ```bash
   # Check all services are running
   docker-compose ps

   # Check backend health
   curl http://localhost:8080/actuator/health

   # Check backend version
   docker-compose logs backend | grep "Spring Boot"
   # Should show: Spring Boot ::  (v4.0.0)
   ```

### For Local Development

#### Step 1: Install Java 25

**macOS** (using Homebrew):
```bash
# Install Java 25
brew install --cask temurin25

# Verify installation
java -version
# Should show: openjdk version "25.0.1"

# Set JAVA_HOME (add to ~/.zshrc or ~/.bash_profile)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

**Ubuntu/Debian**:
```bash
# Download from Adoptium
wget https://github.com/adoptium/temurin25-binaries/releases/download/.../OpenJDK25U-jdk_x64_linux_hotspot_25.0.1_9.tar.gz

# Extract
sudo tar xzf OpenJDK25U-jdk_x64_linux_hotspot_25.0.1_9.tar.gz -C /opt

# Set JAVA_HOME (add to ~/.bashrc)
export JAVA_HOME=/opt/jdk-25.0.1+9
export PATH=$JAVA_HOME/bin:$PATH

# Verify
java -version
```

**Windows**:
1. Download MSI installer from [Adoptium.net](https://adoptium.net/)
2. Run installer and follow prompts
3. Set JAVA_HOME in System Environment Variables:
   - Variable: `JAVA_HOME`
   - Value: `C:\Program Files\Eclipse Adoptium\jdk-25.0.1.9-hotspot`
4. Add to PATH: `%JAVA_HOME%\bin`

#### Step 2: Update Project

```bash
# Navigate to project
cd Hamradiologbook

# Pull latest changes
git pull origin main

# Clean and rebuild backend
cd backend
mvn clean package

# Clean and rebuild rig-control-service
cd ../rig-control-service
mvn clean package
```

#### Step 3: Verify Build

```bash
# Check backend
java -jar backend/target/logbook-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
# Should start successfully and show:
# :: Spring Boot ::  (v4.0.0)
# Starting LogbookApplication using Java 25.0.1

# Check rig-control-service
java -jar rig-control-service/target/rig-control-service-1.0.0-SNAPSHOT.jar
# Should start successfully
```

## For Developers

### IDE Configuration

#### IntelliJ IDEA

1. **Set Project SDK**:
   - File → Project Structure → Project
   - SDK: Select Java 25
   - Language Level: 25

2. **Enable Lombok**:
   - Settings → Plugins → Install "Lombok"
   - Settings → Build → Compiler → Annotation Processors → Enable annotation processing

3. **Maven Settings**:
   - Settings → Build → Build Tools → Maven
   - JDK for Importer: Java 25
   - Click "Reload All Maven Projects"

#### VS Code

1. **Install Extensions**:
   - Extension Pack for Java
   - Lombok Annotations Support
   - Spring Boot Extension Pack

2. **Configure Java**:
   Edit `.vscode/settings.json`:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-25",
         "path": "/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home",
         "default": true
       }
     ]
   }
   ```

#### Eclipse

1. **Install Java 25**:
   - Window → Preferences → Java → Installed JREs
   - Add → Standard VM → Navigate to Java 25 installation
   - Check the checkbox to make it default

2. **Install Lombok**:
   - Download lombok.jar from https://projectlombok.org/download
   - Run: `java -jar lombok.jar`
   - Select Eclipse installation and click Install

### Building from Source

```bash
# Set Java 25 as active
export JAVA_HOME=/path/to/jdk-25

# Build backend
cd backend
mvn clean install -DskipTests

# Build rig-control-service
cd ../rig-control-service
mvn clean install -DskipTests

# Build frontend (if needed)
cd ../frontend/logbook-ui
npm install
npm run build
```

### Running Tests

```bash
# Backend tests
cd backend
mvn test

# With coverage
mvn clean test jacoco:report
```

## For System Administrators

### Production Deployment

#### Pre-Deployment Checklist

- [ ] Backup current database
- [ ] Note current application version
- [ ] Test upgrade in staging environment
- [ ] Schedule maintenance window
- [ ] Notify users of downtime

#### Docker Production Deployment

```bash
# 1. Backup database
docker exec hamradio-postgres pg_dump -U postgres logbook > backup-$(date +%Y%m%d).sql

# 2. Pull latest code
git pull origin main

# 3. Update Docker images
docker-compose build --no-cache

# 4. Stop services
docker-compose down

# 5. Start updated services
docker-compose up -d

# 6. Monitor startup
docker-compose logs -f backend

# 7. Verify health
curl http://localhost:8080/actuator/health
```

#### Kubernetes Deployment

Update your deployment manifests to use Java 25 base images:

```yaml
# backend-deployment.yaml
spec:
  containers:
  - name: backend
    image: eclipse-temurin:25-jdk-alpine
    # ... rest of configuration
```

### Environment Variables

No new environment variables required. Existing variables continue to work:

```bash
# Required
ADMIN_USERNAME=your_admin_user
ADMIN_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_key_here

# Optional
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/logbook
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_db_password
QRZ_USERNAME=your_qrz_username
QRZ_PASSWORD=your_qrz_password
```

### Performance Tuning

Java 25 introduces new garbage collectors and performance options:

```bash
# Recommended JVM options for production
JAVA_OPTS="-Xms512m -Xmx2g \
           -XX:+UseZGC \
           -XX:+ZGenerational \
           -XX:MaxRAMPercentage=75.0 \
           -XX:+HeapDumpOnOutOfMemoryError \
           -XX:HeapDumpPath=/var/log/heap-dump.hprof"
```

## Troubleshooting

### Common Issues

#### 1. "invalid target release: 25"

**Problem**: Maven is using an older Java version.

**Solution**:
```bash
# Check Maven's Java version
mvn --version

# Set JAVA_HOME before running Maven
export JAVA_HOME=/path/to/jdk-25
mvn clean package
```

#### 2. Lombok Annotations Not Working

**Problem**: Getters/setters not generated, "cannot find symbol" errors.

**Solution**:
```bash
# Clean build with annotation processing
mvn clean compile -X

# Verify Lombok in classpath
mvn dependency:tree | grep lombok
```

#### 3. Jackson Serialization Errors

**Problem**: Date serialization issues or "No enum constant" errors.

**Solution**:
- Dates are now ISO-8601 by default
- Remove any custom Jackson date configuration
- Update custom serializers to Jackson 3.0 API

#### 4. Docker Build Failures

**Problem**: "unable to prepare context: unable to evaluate symlinks"

**Solution**:
```bash
# Clean Docker build cache
docker system prune -a

# Rebuild from scratch
docker-compose build --no-cache --pull
```

#### 5. Spring Security Errors

**Problem**: "Bean creation exception" or authentication errors.

**Solution**:
- Verify SecurityConfig uses new DaoAuthenticationProvider API
- Check that all @Bean methods have correct signatures
- Review Spring Security 7 migration guide

### Logging and Debugging

Enable debug logging:

```properties
# application.properties
logging.level.root=INFO
logging.level.com.hamradio=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate=INFO
```

### Getting Help

- Check logs: `docker-compose logs -f backend`
- Health endpoint: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- GitHub Issues: [Report a bug](https://github.com/campbell-r-e/Hamradiologbook/issues)

## Rollback Instructions

If you need to rollback to the previous version:

### Docker Rollback

```bash
# Stop current services
docker-compose down

# Checkout previous version
git checkout <previous-tag-or-commit>

# Rebuild with old version
docker-compose build

# Restore database if needed
docker exec -i hamradio-postgres psql -U postgres logbook < backup-YYYYMMDD.sql

# Start services
docker-compose up -d
```

### Local Development Rollback

```bash
# Revert code
git checkout <previous-tag-or-commit>

# Switch back to Java 17/21
export JAVA_HOME=/path/to/jdk-17

# Rebuild
mvn clean package
```

## Verification Checklist

After upgrade, verify:

- [ ] Application starts without errors
- [ ] Database migrations completed successfully
- [ ] Login and authentication working
- [ ] QSO entry and retrieval functional
- [ ] WebSocket connections stable
- [ ] Rig control (if used) operational
- [ ] ADIF export working
- [ ] Contest validation functional
- [ ] Health checks passing

## Additional Resources

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 What's New](https://docs.spring.io/spring-framework/reference/7.0/whatsnew.html)
- [Java 25 Release Notes](https://jdk.java.net/25/release-notes)
- [Project Documentation](./docs/)

## Feedback

If you encounter issues not covered in this guide, please:
1. Check existing [GitHub Issues](https://github.com/campbell-r-e/Hamradiologbook/issues)
2. Create a new issue with:
   - Your environment (OS, Java version, Docker version)
   - Steps to reproduce
   - Error logs
   - Expected vs actual behavior
