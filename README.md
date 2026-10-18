# GeoTracker — Fetch Location History By Connecting Claude API

A Spring Boot application that fetches local geolocation data every 10 seconds
and displays it live on a Google Maps interface via WebSocket.

<img width="1917" height="1110" alt="image" src="https://github.com/user-attachments/assets/60820528-82e7-4a11-838c-aa1f04651c78" />

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot Server                   │
│                                                         │
│  LocationScheduler (every 10s)                          │
│       │                                                 │
│       ▼                                                 │
│  LocationService.fetchCurrentLocation()                 │
│    ├─ Real GPS device / NMEA serial port                │
│    ├─ OS geolocation API                                │
│    └─ Simulated data (default)                          │
│       │                                                 │
│       ▼                                                 │     │
│  REST API  /api/ClaudeAPI --> fetch Location History    │
└─────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Get a Google Maps API Key

1. Go to https://console.cloud.google.com/
2. Create a project → Enable **Maps JavaScript API**
3. Generate an API key
4. Replace in `src/main/resources/application.properties`:
   ```
   google.maps.api.key=YOUR_API_KEY_HERE
   ```

### 2. Build & Run

```bash
cd geo-tracker
mvn spring-boot:run
```

Then Local: **http://localhost:8080**
Application URL: **www.cityguide.website**

### 3. Replace Simulation with Real GPS

Open `LocationService.java` and modify `fetchCurrentLocation()`.

#### Option A — GPS Device (NMEA over serial port)
```java
// Add dependency: net.sf.marineapi:marine-api:0.20.0
SerialPort port = SerialPort.getCommPort("/dev/ttyUSB0"); // or COM3 on Windows
port.openPort();
BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()));
String line;
while ((line = reader.readLine()) != null) {
    if (line.startsWith("$GPRMC")) {
        // Parse NMEA sentence → lat/lng/speed/heading
    }
}
```

#### Option B — IP Geolocation REST API
```java
RestTemplate rest = new RestTemplate();
String url = "https://ipinfo.io/json?token=YOUR_TOKEN";
Map<String, String> resp = rest.getForObject(url, Map.class);
String[] loc = resp.get("loc").split(",");
double lat = Double.parseDouble(loc[0]);
double lng = Double.parseDouble(loc[1]);
```

#### Option C — Browser-side HTML5 Geolocation
The frontend already supports HTML5 `navigator.geolocation`.
Send the result to `/app/location` via WebSocket from JavaScript.

## REST API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/` | Map dashboard |
| GET | `/api/location` | Latest location (JSON) |
| GET | `/api/location/history` | Last 50 locations (JSON) |
| GET | `/api/status` | Server status |

## Configuration

`application.properties`:
```properties
server.port=8080
google.maps.api.key=YOUR_KEY_HERE
```

`LocationScheduler.java`:
```java
@Scheduled(fixedRate = 10_000)  // Change 10_000 to adjust interval (ms)
```

## Project Structure

```
geo-tracker/
├── pom.xml
└── src/main/
    ├── java/com/geotracker/
    │   ├── GeoTrackerApplication.java   ← Spring Boot entry point
    │   ├── LocationData.java            ← Data model
    │   ├── LocationService.java         ← Fetch logic (replace simulation here)
    │   ├── LocationScheduler.java       ← Every 10s trigger
    │   ├── LocationController.java      ← REST + page controller
    │   └── WebSocketConfig.java         ← STOMP WebSocket config
    └── resources/
        ├── application.properties
        └── templates/
            └── index.html               ← Google Maps + WebSocket client
```

## Dependencies

- **Spring Boot 3.2** — Web, WebSocket, Thymeleaf
- **Google Maps JavaScript API** — Map rendering
- **SockJS + STOMP.js** — WebSocket client library
- **Jackson** — JSON serialization
