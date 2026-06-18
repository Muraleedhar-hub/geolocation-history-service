package com.spring.ai.controller;

import com.spring.ai.data.LocationData;
import com.spring.ai.service.AnthropicService;
import com.spring.ai.service.GeocodeService;
import com.spring.ai.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LocationController
 *
 * Provides:
 *  GET /                              → serves the map HTML page
 *  GET /api/location                  → current location (JSON)
 *  GET /api/location/history          → last 50 locations (JSON)
 *  POST /api/location/browser         → receive browser geolocation (from HTML5 API)
 *  GET /api/geocode?lat=&lng=         → reverse geocode to address (JSON)
 *  GET /api/geocode/current           → address for current device location (JSON)
 *  GET /api/status                    → server status info
 */
@Controller
@CrossOrigin
public class LocationController {

    private static final Logger log = Logger.getLogger(LocationController.class.getName());

    @Autowired
    private LocationService locationService;

    @Autowired
    private GeocodeService geocodeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AnthropicService anthropicService;


    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Serve the main map page */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping(value = "/message/{prompt}")
    public ResponseEntity<String> triggerPrompt(@PathVariable String prompt) {
        String response = anthropicService.executePrompt(prompt);
        log.info("response : ---------------------- : "+ response);

        return ResponseEntity.ok(response);

    }
    @GetMapping(value = "/message1/{prompt}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String prompt) {
        byte[] data = anthropicService.executePrompt(prompt).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(data);
    }

    /** REST endpoint: latest location */
    @GetMapping("/api/location")
    @ResponseBody
    public ResponseEntity<LocationData> getLatestLocation() {
        LocationData loc = locationService.getLatestLocation();
        if (loc == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(loc);
    }

    /** REST endpoint: location history */
    @GetMapping("/api/location/history")
    @ResponseBody
    public ResponseEntity<List<LocationData>> getLocationHistory() {
        return ResponseEntity.ok(locationService.getLocationHistory());
    }

    /**
     * REST endpoint: receive browser geolocation data from client (HTML5 Geolocation API).
     * Called every 1 minute from JavaScript.
     *
     * Example: POST /api/location/browser
     * Body: { "latitude": 37.3861, "longitude": -122.0839, "accuracy": 10.5, ... }
     */
    @PostMapping("/api/location/browser")
    @ResponseBody
    public ResponseEntity<Map<String, String>> receiveBrowserLocation(@RequestBody LocationData location) {
        try {
            log.info(String.format("[Browser Geolocation] lat=%.6f, lng=%.6f, accuracy=%.1f, source=%s",
                    location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSource()));

            // Broadcast to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/location", location);

            Map<String, String> response = new HashMap<>();
            response.put("status", "received");
            response.put("message", "Browser location received and broadcasted");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.severe("Error processing browser location: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * REST endpoint: reverse geocode any lat/lng to a street address.
     *
     * Example: GET /api/geocode?lat=37.3861&lng=-122.0839
     */
    @GetMapping("/api/geocode")
    @ResponseBody
    public ResponseEntity<?> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {
        try {
            GeocodeService.AddressResult address = geocodeService.reverseGeocode(lat, lng);
            return ResponseEntity.ok(address);
        } catch (GeocodeService.GeocodingException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * REST endpoint: reverse geocode the CURRENT device location.
     *
     * Example: GET /api/geocode/current
     */
    @GetMapping("/api/geocode/current")
    @ResponseBody
    public ResponseEntity<?> geocodeCurrent() {
        LocationData loc = locationService.getLatestLocation();
        if (loc == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No location data yet — wait for the first fetch");
            return ResponseEntity.noContent().build();
        }
        try {
            GeocodeService.AddressResult address =
                    geocodeService.reverseGeocode(loc.getLatitude(), loc.getLongitude());
            // Merge location + address into one response
            Map<String, Object> result = new HashMap<>();
            result.put("location", loc);
            result.put("address", address);
            return ResponseEntity.ok(result);
        } catch (GeocodeService.GeocodingException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** REST endpoint: server status */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", true);
        status.put("fetchCount", locationService.getFetchCount());
        status.put("historySize", locationService.getLocationHistory().size());
        status.put("fetchIntervalSeconds", 10);
        LocationData latest = locationService.getLatestLocation();
        if (latest != null) {
            status.put("lastFetch", latest.getTimestamp());
            status.put("lastSource", latest.getSource());
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Proxies POST /api/anthropic/messages → https://api.anthropic.com/v1/messages
     * The browser calls this endpoint instead of Anthropic directly,
     * which sidesteps the CORS restriction.
     */
    @PostMapping("/messages")
    public ResponseEntity<String> proxyMessages(@RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key",         anthropicApiKey);
        headers.set("anthropic-version",  "2023-06-01");
        // Required for web search / MCP tools
        headers.set("anthropic-beta",     "web-search-2025-03-05");

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST,
                entity,
                String.class
        );
    }

}
