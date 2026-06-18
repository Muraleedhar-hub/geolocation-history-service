package com.spring.ai.service;

import com.spring.ai.data.LocationData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * LocationService
 *
 * Simulates fetching real device/local location data every 10 seconds.
 * In production, replace the simulation logic with:
 *   - A real GPS device reader (NMEA over serial port)
 *   - OS-level geolocation API call
 *   - A local hardware sensor integration
 *   - External IP-based geolocation API
 */
@Service
public class LocationService {

    private static final Logger log = Logger.getLogger(LocationService.class.getName());

    // History of last 50 location points (for trail on map)
    private final List<LocationData> locationHistory =
            Collections.synchronizedList(new ArrayList<>());

    // Latest location (thread-safe)
    private final AtomicReference<LocationData> latestLocation = new AtomicReference<>();

    // Simulation state (replace with real GPS logic)
    private final Random random = new Random();

    // Starting point: Sunnyvale, CA (Google HQ area)
    private double simLat = 37.3861;
    private double simLng = -122.0839;
    private double simSpeed = 0;
    private double simHeading = 45.0;
    private double simAltitude = 30.0;
    private int fetchCount = 0;

    /**
     * Main method called by the scheduler every 10 seconds.
     * Replace the body with actual device/API calls for real data.
     */
    public LocationData fetchCurrentLocation() {
        fetchCount++;

        // ─────────────────────────────────────────────
        // REPLACE THIS BLOCK with real location source:
        //   e.g., read NMEA sentence from COM port,
        //         call OS location service,
        //         call IP-geolocation REST API, etc.
        // ─────────────────────────────────────────────
        LocationData data = simulateLocationData();

        // Store in history (keep last 50 points)
        locationHistory.add(data);
        if (locationHistory.size() > 50) {
            locationHistory.remove(0);
        }

        latestLocation.set(data);

        log.info(String.format("[Fetch #%d] %s", fetchCount, data));
        return data;
    }

    /**
     * SIMULATION: Generates realistic moving GPS coordinates.
     * DELETE this method and replace fetchCurrentLocation() when using real GPS.
     */
    private LocationData simulateLocationData() {
        // Simulate slight movement (walking/driving speed)
        simSpeed = Math.max(0, simSpeed + (random.nextDouble() - 0.45) * 5);
        simSpeed = Math.min(simSpeed, 80); // cap at 80 km/h

        // Adjust heading slightly
        simHeading += (random.nextDouble() - 0.5) * 15;
        if (simHeading < 0) simHeading += 360;
        if (simHeading >= 360) simHeading -= 360;

        // Move position based on speed and heading
        double distanceKm = simSpeed / 3600.0 * 10; // 10 seconds of travel
        double headingRad = Math.toRadians(simHeading);
        simLat += distanceKm / 111.32 * Math.cos(headingRad);
        simLng += distanceKm / (111.32 * Math.cos(Math.toRadians(simLat))) * Math.sin(headingRad);

        // Altitude drift
        simAltitude += (random.nextDouble() - 0.5) * 2;

        // Accuracy varies slightly
        double accuracy = 5 + random.nextDouble() * 15;

        String source = simSpeed > 10 ? "GPS" : "NETWORK";

        return new LocationData(
                Math.round(simLat * 1_000_000.0) / 1_000_000.0,
                Math.round(simLng * 1_000_000.0) / 1_000_000.0,
                Math.round(accuracy * 10.0) / 10.0,
                Math.round(simSpeed * 10.0) / 10.0,
                Math.round(simHeading * 10.0) / 10.0,
                Math.round(simAltitude * 10.0) / 10.0,
                source,
                "ACTIVE",
                LocalDateTime.now()
        );
    }

    /** Returns the most recently fetched location. */
    public LocationData getLatestLocation() {
        return latestLocation.get();
    }

    /** Returns location history (last 50 points). */
    public List<LocationData> getLocationHistory() {
        return Collections.unmodifiableList(new ArrayList<>(locationHistory));
    }

    /** Total number of fetches performed since startup. */
    public int getFetchCount() {
        return fetchCount;
    }
}
