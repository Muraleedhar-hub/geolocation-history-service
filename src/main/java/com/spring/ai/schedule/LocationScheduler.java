package com.spring.ai.schedule;

import com.spring.ai.data.LocationData;
import com.spring.ai.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * LocationScheduler
 *
 * Fires every 10 seconds, fetches the current location via LocationService,
 * and broadcasts it over WebSocket to all connected browser clients.
 */
@Component
public class LocationScheduler {

    private static final Logger log = Logger.getLogger(LocationScheduler.class.getName());

    @Autowired
    private LocationService locationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Runs every 10,000 ms (10 seconds).
     * - Fetches latest location data
     * - Pushes update to /topic/location via WebSocket (STOMP)
     *
     * Change fixedRate to adjust the polling interval.
     */
    @Scheduled(fixedRate = 10_000)
    public void fetchAndBroadcastLocation() {
        try {
            LocationData location = locationService.fetchCurrentLocation();

            // Broadcast to all WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/location", location);

            log.info(String.format("Broadcasted location update #%d → lat=%.6f, lng=%.6f",
                    locationService.getFetchCount(),
                    location.getLatitude(),
                    location.getLongitude()));

        } catch (Exception e) {
            log.severe("Error fetching/broadcasting location: " + e.getMessage());
        }
    }
}
