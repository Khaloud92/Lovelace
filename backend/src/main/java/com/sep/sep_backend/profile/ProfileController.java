package com.sep.sep_backend.profile;

import com.sep.sep_backend.profile.dto.FriendLocationResponse;
import com.sep.sep_backend.profile.dto.ProfileResponse;
import com.sep.sep_backend.profile.dto.UpdateLocationRequest;
import com.sep.sep_backend.profile.dto.UpdateLocationSharingRequest;
import com.sep.sep_backend.profile.dto.UpdateProfilePictureRequest;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * REST endpoints for the user profile, profile picture, and location sharing.
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AppUserRepository appUserRepository;


    /**
     * Returns the authenticated user's profile.
     *
     * @return the profile data
     */
    @GetMapping
    public ProfileResponse myProfile() {
        return profileService.getProfile(currentUser());
    }


    /**
     * Updates the profile picture.
     *
     * @param request the picture as a data URL
     * @return 200 on success
     */
    @PutMapping("/picture")
    public ResponseEntity<Void> updatePicture(@RequestBody UpdateProfilePictureRequest request) {
        profileService.updatePicture(currentUser(), request.picture());
        return ResponseEntity.ok().build();
    }


    /**
     * Removes the profile picture.
     *
     * @return 204 on success
     */
    @DeleteMapping("/picture")
    public ResponseEntity<Void> removePicture() {
        profileService.updatePicture(currentUser(), null);
        return ResponseEntity.noContent().build();
    }


    /**
     * Enables or disables location sharing.
     *
     * @param request the new sharing state
     * @return 200 on success
     */
    @PutMapping("/location-sharing")
    public ResponseEntity<Void> updateLocationSharing(@RequestBody UpdateLocationSharingRequest request) {
        profileService.updateLocationSharing(currentUser(), request.enabled());
        return ResponseEntity.ok().build();
    }


    /**
     * Stores the user's current position.
     *
     * @param request latitude and longitude
     * @return 200 on success
     */
    @PutMapping("/location")
    public ResponseEntity<Void> updateLocation(@RequestBody UpdateLocationRequest request) {
        profileService.updateLocation(currentUser(), request.latitude(), request.longitude());
        return ResponseEntity.ok().build();
    }


    /**
     * Returns the shared locations of the user's friends.
     *
     * @return the sharing friends' positions
     */
    @GetMapping("/friends/locations")
    public List<FriendLocationResponse> friendLocations() {
        return profileService.getFriendLocations(currentUser());
    }


    private AppUser currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
