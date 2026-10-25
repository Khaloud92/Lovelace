package com.sep.sep_backend.map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Resolves destination names to coordinates via the OpenStreetMap Nominatim API,
 * with an in-memory cache to avoid repeated lookups.
 */
@Service
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final RestTemplate restTemplate;
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    public GeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Looks up the coordinates of a destination.
     *
     * @param destination the place name
     * @return [latitude, longitude], or empty if the lookup fails
     */
    @SuppressWarnings("unchecked")
    public Optional<double[]> geocode(String destination) {
        if (destination == null || destination.isBlank()) {
            return Optional.empty();
        }

        String key = destination.trim().toLowerCase();
        double[] cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        String url = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .queryParam("q", destination)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "LovelaceTravel/1.0");

        try {
            List<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class).getBody();

            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> first = response.get(0);
            double lat = Double.parseDouble((String) first.get("lat"));
            double lon = Double.parseDouble((String) first.get("lon"));
            double[] coordinates = {lat, lon};

            cache.put(key, coordinates);
            return Optional.of(coordinates);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
