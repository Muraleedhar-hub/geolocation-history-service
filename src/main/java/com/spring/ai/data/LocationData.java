package com.spring.ai.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class LocationData {

    private double latitude;
    private double longitude;
    private double accuracy;   // meters
    private double speed;      // km/h
    private double heading;    // degrees
    private double altitude;   // meters
    private String source;     // "GPS", "NETWORK", "SIMULATED"
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Constructors
    public LocationData() {}

    public LocationData(double latitude, double longitude, double accuracy,
                        double speed, double heading, double altitude,
                        String source, String status, LocalDateTime timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.speed = speed;
        this.heading = heading;
        this.altitude = altitude;
        this.source = source;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getHeading() { return heading; }
    public void setHeading(double heading) { this.heading = heading; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("LocationData{lat=%.6f, lng=%.6f, speed=%.1f km/h, source=%s, time=%s}",
                latitude, longitude, speed, source, timestamp);
    }
}
