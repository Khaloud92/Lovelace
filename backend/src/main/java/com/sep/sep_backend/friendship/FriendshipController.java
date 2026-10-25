package com.sep.sep_backend.friendship;

import com.sep.sep_backend.friendship.dto.FriendshipResponseDto;
import com.sep.sep_backend.friendship.dto.RespondRequestDto;
import com.sep.sep_backend.friendship.dto.UserSearchResultDto;
import com.sep.sep_backend.friendship.dto.sendFriendRequestDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user friendships and friend requests.
 * Provides endpoints for searching users, sending requests, responding to requests,
 * and fetching lists of friends or pending requests.
 */
@RestController
@RequestMapping("/api/friendships")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final AppUserRepository appUserRepository;

    /**
     * Searches for users based on a search query, excluding the currently authenticated user.
     *
     * @param query The search string (e.g., username or email portion).
     * @return A list of matching users mapped to DTOs.
     */
    @GetMapping("/search")
    public List<UserSearchResultDto> search(@RequestParam("query") String query) {
        return friendshipService.searchUsers(currentUser(), query);
    }

    /**
     * Sends a friend request from the currently authenticated user to another user.
     *
     * @param dto The data transfer object containing the addressee's UUID.
     * @return A response entity indicating success (201 Created) or an error message (400 Bad Request).
     */
    @PostMapping("/requests")
    public ResponseEntity<?> sendRequest(@RequestBody sendFriendRequestDto dto) {
        try {
            friendshipService.sendFriendRequest(currentUser(), dto.addresseeID());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Responds to an incoming friend request (e.g., accepting or declining it).
     *
     * @param dto The data transfer object containing the request ID and the desired status response.
     * @return A response entity containing the updated friendship details or an error message.
     */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestBody RespondRequestDto dto) {
        try {
            return ResponseEntity.ok(friendshipService.respondToRequest(currentUser(), dto));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Fetches a list of all accepted friends for the currently authenticated user.
     *
     * @return A list of friendship response DTOs representing active friends.
     */
    @GetMapping
    public List<FriendshipResponseDto> friends() {
        return friendshipService.getFriends(currentUser());
    }

    /**
     * Fetches all pending friendship requests received by the currently authenticated user.
     *
     * @return A list of incoming friendship requests.
     */
    @GetMapping("/requests/incoming")
    public List<FriendshipResponseDto> incoming() {
        return friendshipService.getIncomingRequests(currentUser());
    }

    /**
     * Fetches all pending friendship requests sent by the currently authenticated user.
     *
     * @return A list of outgoing friendship requests.
     */
    @GetMapping("/requests/outgoing")
    public List<FriendshipResponseDto> outgoing() {
        return friendshipService.getOutgoingRequests(currentUser());
    }

    /**
     * Helper method to retrieve the currently authenticated user from the security context.
     *
     * @return The AppUser entity corresponding to the authenticated email.
     * @throws IllegalStateException If the authenticated user cannot be found in the database.
     */
    private AppUser currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}