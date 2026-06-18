package com.spring.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * GeocodeService
 *
 * Converts lat/lng coordinates into a human-readable address
 * using the Google Maps Geocoding API (reverse geocoding).
 *
 * API docs: https://developers.google.com/maps/documentation/geocoding/requests-reverse-geocoding
 */
@Service
public class GeocodeService {

    private static final Logger log = Logger.getLogger(GeocodeService.class.getName());

    private static final String GEOCODING_BASE_URL =
            "https://maps.googleapis.com/maps/api/geocode/json";

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reverse geocode: lat/lng → AddressResult
     *
     * @param latitude  decimal degrees
     * @param longitude decimal degrees
     * @return AddressResult with formatted address and address components
     * @throws GeocodingException if the API call fails or returns no results
     */
    public AddressResult reverseGeocode(double latitude, double longitude) {
        String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_BASE_URL)
                .queryParam("latlng", latitude + "," + longitude)
                .queryParam("key", apiKey)
                .toUriString();

        log.info(String.format("Reverse geocoding: lat=%.6f, lng=%.6f", latitude, longitude));

        try {
            String response = restTemplate.getForObject(url, String.class);
            return parseResponse(response, latitude, longitude);
        } catch (Exception e) {
            throw new GeocodingException("Failed to call Geocoding API: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Parse the Google Maps API JSON response
    // ─────────────────────────────────────────────────────────────────
    private AddressResult parseResponse(String json, double lat, double lng) {
        try {
            JsonNode root   = objectMapper.readTree(json);
            String status   = root.path("status").asText();

            if (!"OK".equals(status)) {
                String error = root.path("error_message").asText("No results found");
                throw new GeocodingException("Geocoding API returned status: " + status + " — " + error);
            }

            JsonNode firstResult = root.path("results").get(0);
            if (firstResult == null) {
                throw new GeocodingException("No address results for this location");
            }

            String formattedAddress = firstResult.path("formatted_address").asText();

            // Extract individual address components
            Map<String, String> components = new LinkedHashMap<>();
            for (JsonNode component : firstResult.path("address_components")) {
                String value    = component.path("long_name").asText();
                JsonNode types  = component.path("types");
                String type     = types.size() > 0 ? types.get(0).asText() : "unknown";

                switch (type) {
                    case "street_number"               -> components.put("streetNumber", value);
                    case "route"                       -> components.put("street", value);
                    case "neighborhood", "sublocality" -> components.put("district", value);
                    case "locality"                    -> components.put("city", value);
                    case "administrative_area_level_2" -> components.put("county", value);
                    case "administrative_area_level_1" -> components.put("state", value);
                    case "country"                     -> components.put("country", value);
                    case "postal_code"                 -> components.put("postalCode", value);
                }
            }

            // Build full street address
            String streetAddress = "";
            if (components.containsKey("streetNumber") && components.containsKey("street")) {
                streetAddress = components.get("streetNumber") + " " + components.get("street");
            } else if (components.containsKey("street")) {
                streetAddress = components.get("street");
            }

            // Place ID for this result
            String placeId = firstResult.path("place_id").asText(null);

            log.info("Resolved address: " + formattedAddress);

            return new AddressResult(
                    formattedAddress,
                    streetAddress,
                    components.getOrDefault("city", ""),
                    components.getOrDefault("state", ""),
                    components.getOrDefault("country", ""),
                    components.getOrDefault("postalCode", ""),
                    placeId,
                    lat,
                    lng,
                    components
            );

        } catch (GeocodingException e) {
            throw e;
        } catch (Exception e) {
            throw new GeocodingException("Failed to parse Geocoding API response: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Inner classes: result model + exception
    // ─────────────────────────────────────────────────────────────────

    public static class AddressResult {
        private String formattedAddress;
        private String streetAddress;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private String placeId;
        private double latitude;
        private double longitude;
        private Map<String, String> components;

        public AddressResult(String formattedAddress, String streetAddress, String city,
                             String state, String country, String postalCode,
                             String placeId, double latitude, double longitude,
                             Map<String, String> components) {
            this.formattedAddress = formattedAddress;
            this.streetAddress    = streetAddress;
            this.city             = city;
            this.state            = state;
            this.country          = country;
            this.postalCode       = postalCode;
            this.placeId          = placeId;
            this.latitude         = latitude;
            this.longitude        = longitude;
            this.components       = components;
        }

        // Getters
        public String getFormattedAddress() { return formattedAddress; }
        public String getStreetAddress()    { return streetAddress; }
        public String getCity()             { return city; }
        public String getState()            { return state; }
        public String getCountry()          { return country; }
        public String getPostalCode()       { return postalCode; }
        public String getPlaceId()          { return placeId; }
        public double getLatitude()         { return latitude; }
        public double getLongitude()        { return longitude; }
        public Map<String, String> getComponents() { return components; }
    }

    public static class GeocodingException extends RuntimeException {
        public GeocodingException(String message) { super(message); }
        public GeocodingException(String message, Throwable cause) { super(message, cause); }
    }
}
