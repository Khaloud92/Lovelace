package com.sep.sep_backend.map;

import com.sep.sep_backend.map.dto.MapTripResponse;
import com.sep.sep_backend.registration.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for the trip map view.
 */
@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "http://localhost:4200")
public class MapController {

    private final MapService service;
    private final AppUserRepository appUserRepository;

    public MapController(MapService service, AppUserRepository appUserRepository) {
        this.service = service;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the geocoded trips of the authenticated user and their friends.
     *
     * @return trips with map coordinates
     */
    @GetMapping("/trips")
    public List<MapTripResponse> getTrips() {
        return service.getTrips(resolveUserId());
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
