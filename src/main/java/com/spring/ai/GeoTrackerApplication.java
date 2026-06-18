package com.spring.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeoTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoTrackerApplication.class, args);
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   GeoTracker Server Started!             ║");
        System.out.println("║   Open: http://localhost:8080            ║");
        System.out.println("║   Fetching location every 10 seconds     ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}
