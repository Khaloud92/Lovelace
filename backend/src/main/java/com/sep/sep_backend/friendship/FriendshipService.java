package com.sep.sep_backend.friendship;

import com.sep.sep_backend.friendship.dto.FriendshipResponseDto;
import com.sep.sep_backend.friendship.dto.RespondRequestDto;
import com.sep.sep_backend.friendship.dto.UserSearchResultDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing friendship logic, including searching, requests, and responses.
 */
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final AppUserRepository appUserRepository;
    private final SimpMessagingTemplate messagingTemplate;


    /**
     * Searches for users and determines the current friendship relationship status.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchUsers(AppUser me, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return appUserRepository.searchByNameOrEmail(query.trim(), me.getId()).stream()
                .map(other -> new UserSearchResultDto(
                        other.getId(),
                        other.getName(),
                        other.getEmail(),
                        other.getProfilePicture(),
                        relationshipBetween(me, other)))
                .toList();
    }

    /**
     * Helper to evaluate the exact relationship state between two users.
     */
    private UserSearchResultDto.Relationship relationshipBetween(AppUser me, AppUser other) {
        return friendshipRepository.findBetween(me, other)
                .map(f -> switch (f.getStatus()) {
                    case ACCEPTED -> UserSearchResultDto.Relationship.FRIENDS;
                    case PENDING  -> f.getRequester().getId().equals(me.getId())
                            ? UserSearchResultDto.Relationship.REQUEST_SENT
                            : UserSearchResultDto.Relationship.REQUEST_RECEIVED;
                    case DECLINED -> UserSearchResultDto.Relationship.NONE;
                })
                .orElse(UserSearchResultDto.Relationship.NONE);
    }

    /**
     * Sends a new friend request if no prior relation exists and target is valid.
     */
    @Transactional
    public void sendFriendRequest(AppUser requester, UUID addresseeId) {
        if (requester.getId().equals(addresseeId)) {
            throw new IllegalStateException("You cannot send a friend request to yourself");
        }
        AppUser addressee = appUserRepository.findById(addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean alreadyExists =
                friendshipRepository.existsByRequesterAndAddressee(requester, addressee) ||
                        friendshipRepository.existsByRequesterAndAddressee(addressee, requester);
        if (alreadyExists) {
            throw new IllegalStateException("Friendship already exists between these users");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendshipRepository.save(friendship);
        FriendshipResponseDto dto = toDto(friendship, addressee); // "other" = requester, status PENDING
        messagingTemplate.convertAndSend("/topic/friend-requests." + addressee.getId(), dto);
    }

    /**
     * Accepts or declines an existing pending friend request.
     */
    @Transactional
    public FriendshipResponseDto respondToRequest(AppUser me, RespondRequestDto dto) {
        Friendship f = friendshipRepository.findById(dto.friendshipId())
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!f.getAddressee().getId().equals(me.getId())) {
            throw new IllegalStateException("You can only respond to requests sent to you");
        }
        if (f.getStatus() != Friendship.Status.PENDING) {
            throw new IllegalStateException("This request has already been handled");
        }
        if (dto.decision() != Friendship.Status.ACCEPTED
                && dto.decision() != Friendship.Status.DECLINED) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or DECLINED");
        }

        f.setStatus(dto.decision());
        friendshipRepository.save(f);
        messagingTemplate.convertAndSend(
                "/topic/friend-responses." + f.getRequester().getId(), toDto(f, f.getRequester()));
        return toDto(f, me);
    }

    /**
     * Retrieves all accepted friends for a user.
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponseDto> getFriends(AppUser me) {
        return friendshipRepository.findAllForUserByStatus(me, Friendship.Status.ACCEPTED)
                .stream().map(f -> toDto(f, me)).toList();
    }

    /**
     * Retrieves all pending incoming friend requests.
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponseDto> getIncomingRequests(AppUser me) {
        return friendshipRepository.findByAddresseeAndStatus(me, Friendship.Status.PENDING)
                .stream().map(f -> toDto(f, me)).toList();
    }

    /**
     * Retrieves all pending outgoing friend requests.
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponseDto> getOutgoingRequests(AppUser me) {
        return friendshipRepository.findByRequesterAndStatus(me, Friendship.Status.PENDING)
                .stream().map(f -> toDto(f, me)).toList();
    }

    /**
     * Maps a friendship entity to a response DTO from the context of the current user.
     */
    private FriendshipResponseDto toDto(Friendship f, AppUser me) {
        AppUser other = f.getRequester().getId().equals(me.getId())
                ? f.getAddressee()
                : f.getRequester();
        return new FriendshipResponseDto(f.getId(), other.getId(), other.getName(), other.getProfilePicture(), f.getStatus());
    }
}