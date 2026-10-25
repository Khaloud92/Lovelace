package com.sep.sep_backend.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redirects destination image requests to a matching Unsplash photo, with caching.
 */
@RestController
@RequestMapping("/api/image")
public class UnsplashController {

    @Value("${unsplash.access-key}")
    private String accessKey;

    private final RestTemplate restTemplate;
    private final Map<String, String> imageCache = new ConcurrentHashMap<>();

    public UnsplashController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Redirects to a random landscape photo of the destination.
     *
     * @param destination the place name
     * @return a redirect to the image URL
     */
    @GetMapping
    public RedirectView getDestinationImage(@RequestParam String destination) {
        String cached = imageCache.get(destination.toLowerCase());
        if (cached != null) {
            return new RedirectView(cached);
        }

        String url = "https://api.unsplash.com/photos/random?query="
                + destination + ",landmark,travel&orientation=landscape&client_id=" + accessKey;
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        Map<?, ?> urls = (Map<?, ?>) response.get("urls");
        String imageUrl = (String) urls.get("regular");

        imageCache.put(destination.toLowerCase(), imageUrl);
        return new RedirectView(imageUrl);
    }
}