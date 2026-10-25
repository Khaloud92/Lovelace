package com.sep.sep_backend.map;

import com.sep.sep_backend.friendship.FriendshipService;
import com.sep.sep_backend.friendship.dto.FriendshipResponseDto;
import com.sep.sep_backend.map.dto.MapTripResponse;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects the user's own and their friends' trips and resolves their
 * destinations to map coordinates.
 */
@Service
public class MapService {

    private final TravelEntryRepository travelEntryRepository;
    private final AppUserRepository appUserRepository;
    private final FriendshipService friendshipService;
    private final GeocodingService geocodingService;

    public MapService(TravelEntryRepository travelEntryRepository,
                       AppUserRepository appUserRepository,
                       FriendshipService friendshipService,
                       GeocodingService geocodingService) {
        this.travelEntryRepository = travelEntryRepository;
        this.appUserRepository = appUserRepository;
        this.friendshipService = friendshipService;
        this.geocodingService = geocodingService;
    }

    /**
     * Returns all geocodable trips of the user and their friends.
     *
     * @param userId id of the user
     * @return trips with coordinates; trips whose destination cannot be geocoded are omitted
     */
    public List<MapTripResponse> getTrips(String userId) {
        AppUser me = appUserRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Stream<Optional<MapTripResponse>> ownTrips = travelEntryRepository.findByUserId(userId).stream()
                .map(entry -> toResponse(entry, me.getId().toString(), me.getName(), me.getProfilePicture(), true));

        List<FriendshipResponseDto> friends = friendshipService.getFriends(me);
        Stream<Optional<MapTripResponse>> friendTrips = friends.stream()
                .flatMap(friend -> travelEntryRepository.findByUserId(friend.otherUserId().toString()).stream()
                        .map(entry -> toResponse(entry, friend.otherUserId().toString(),
                                friend.otherUserName(), friend.otherUserProfilePicture(), false)));

        return Stream.concat(ownTrips, friendTrips)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<MapTripResponse> toResponse(TravelEntry entry, String ownerId, String ownerName,
                                                  String ownerProfilePicture, boolean own) {
        return geocodingService.geocode(entry.getDestination())
                .map(coordinates -> new MapTripResponse(
                        entry.getId(), entry.getTitle(), entry.getDestination(),
                        entry.getStartDate(), entry.getEndDate(), entry.getColor(),
                        coordinates[0], coordinates[1],
                        ownerId, ownerName, ownerProfilePicture, own));
    }
}
