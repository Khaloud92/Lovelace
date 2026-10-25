package com.sep.sep_backend.profile;

import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipRepository;
import com.sep.sep_backend.profile.dto.FriendLocationEvent;
import com.sep.sep_backend.profile.dto.FriendLocationResponse;
import com.sep.sep_backend.profile.dto.ProfileResponse;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Business logic for the user profile: picture, location sharing, and friend locations.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Returns the profile data of the given user.
     *
     * @param me the authenticated user
     * @return the profile data
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(AppUser me) {
        return new ProfileResponse(me.getName(), me.getEmail(), me.getProfilePicture(), me.isShareLocation());
    }


    /**
     * Sets or removes the profile picture.
     *
     * @param me      the authenticated user
     * @param picture the picture as a data URL, or null to remove it
     */
    @Transactional
    public void updatePicture(AppUser me, String picture) {
        me.setProfilePicture(picture);
        appUserRepository.save(me);
    }


    /**
     * Enables or disables location sharing; disabling clears the stored position.
     *
     * @param me      the authenticated user
     * @param enabled the new sharing state
     */
    @Transactional
    public void updateLocationSharing(AppUser me, boolean enabled) {
        me.setShareLocation(enabled);
        if (!enabled) {
            me.setCurrentLatitude(null);
            me.setCurrentLongitude(null);
            me.setLocationUpdatedAt(null);
        }
        appUserRepository.save(me);
        if (!enabled) {
            // friends drop the pin immediately instead of waiting for a poll
            broadcastToFriends(me, new FriendLocationEvent(
                    me.getId(), me.getName(), me.getProfilePicture(), null, null, null, false));
        }
    }


    /**
     * Stores the user's current position and broadcasts it to their friends.
     *
     * @param me        the authenticated user
     * @param latitude  current latitude
     * @param longitude current longitude
     */
    @Transactional
    public void updateLocation(AppUser me, double latitude, double longitude) {
        if (!me.isShareLocation()) {
            throw new IllegalStateException("Location sharing is disabled");
        }
        me.setCurrentLatitude(latitude);
        me.setCurrentLongitude(longitude);
        me.setLocationUpdatedAt(LocalDateTime.now());
        appUserRepository.save(me);
        broadcastToFriends(me, new FriendLocationEvent(
                me.getId(), me.getName(), me.getProfilePicture(),
                latitude, longitude, me.getLocationUpdatedAt(), true));
    }


    /**
     * Returns the locations of accepted friends who currently share theirs.
     *
     * @param me the authenticated user
     * @return the sharing friends' positions
     */
    @Transactional(readOnly = true)
    public List<FriendLocationResponse> getFriendLocations(AppUser me) {
        return friendsOf(me).stream()
                .filter(friend -> friend.isShareLocation()
                        && friend.getCurrentLatitude() != null
                        && friend.getCurrentLongitude() != null)
                .map(friend -> new FriendLocationResponse(
                        friend.getId(),
                        friend.getName(),
                        friend.getProfilePicture(),
                        friend.getCurrentLatitude(),
                        friend.getCurrentLongitude(),
                        friend.getLocationUpdatedAt()))
                .toList();
    }


    private void broadcastToFriends(AppUser me, FriendLocationEvent event) {
        friendsOf(me).forEach(friend ->
                messagingTemplate.convertAndSend("/topic/friend-locations." + friend.getId(), event));
    }


    private List<AppUser> friendsOf(AppUser me) {
        return friendshipRepository.findAllForUserByStatus(me, Friendship.Status.ACCEPTED).stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getAddressee() : f.getRequester())
                .toList();
    }
}
