package com.sep.sep_backend.FriendshipTest;

import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipRepository;
import com.sep.sep_backend.friendship.FriendshipService;
import com.sep.sep_backend.friendship.dto.FriendshipResponseDto;
import com.sep.sep_backend.friendship.dto.RespondRequestDto;
import com.sep.sep_backend.friendship.dto.UserSearchResultDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private FriendshipService service;

    private AppUser me;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        service = new FriendshipService(friendshipRepository, appUserRepository, messagingTemplate);

        me = new AppUser();
        me.setId(UUID.randomUUID());
        me.setName("My Name");
        me.setEmail("me@test.com");

        otherUser = new AppUser();
        otherUser.setId(UUID.randomUUID());
        otherUser.setName("Other Name");
        otherUser.setEmail("other@test.com");
    }

    // ==========================================
    // ---- searchUsers Tests ----
    // ==========================================

    @Test
    void searchUsers_shouldReturnEmptyList_whenQueryIsBlank() {
        // ---- ACT ----
        List<UserSearchResultDto> result = service.searchUsers(me, "   ");

        // ---- ASSERT ----
        assertTrue(result.isEmpty());
        verifyNoInteractions(appUserRepository, friendshipRepository);
    }

    @Test
    void searchUsers_shouldReturnResults_withCorrectRelationship() {
        // ---- ARRANGE ----
        String query = "Other";
        when(appUserRepository.searchByNameOrEmail(query, me.getId())).thenReturn(List.of(otherUser));

        Friendship friendship = new Friendship();
        friendship.setRequester(me);
        friendship.setAddressee(otherUser);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findBetween(me, otherUser)).thenReturn(Optional.of(friendship));

        // ---- ACT ----
        List<UserSearchResultDto> result = service.searchUsers(me, query);

        // ---- ASSERT ----
        assertEquals(1, result.size());
        assertEquals(otherUser.getId(), result.get(0).userId());
        assertEquals(UserSearchResultDto.Relationship.REQUEST_SENT, result.get(0).relationship());
    }

    // ==========================================
    // ---- sendFriendRequest Tests ----
    // ==========================================

    @Test
    void sendFriendRequest_shouldSaveFriendship_whenValid() {
        // ---- ARRANGE ----
        UUID addresseeId = otherUser.getId();
        when(appUserRepository.findById(addresseeId)).thenReturn(Optional.of(otherUser));
        when(friendshipRepository.existsByRequesterAndAddressee(me, otherUser)).thenReturn(false);
        when(friendshipRepository.existsByRequesterAndAddressee(otherUser, me)).thenReturn(false);

        // ---- ACT ----
        service.sendFriendRequest(me, addresseeId);

        // ---- ASSERT ----
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(captor.capture());

        Friendship saved = captor.getValue();
        assertEquals(me, saved.getRequester());
        assertEquals(otherUser, saved.getAddressee());
        assertEquals(Friendship.Status.PENDING, saved.getStatus());
    }

    @Test
    void sendFriendRequest_shouldThrowException_whenSendingToYourself() {
        // ---- ACT & ASSERT ----
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendFriendRequest(me, me.getId()));
        assertEquals("You cannot send a friend request to yourself", ex.getMessage());
        verifyNoInteractions(appUserRepository, friendshipRepository);
    }

    @Test
    void sendFriendRequest_shouldThrowException_whenUserNotFound() {
        // ---- ARRANGE ----
        UUID randomId = UUID.randomUUID();
        when(appUserRepository.findById(randomId)).thenReturn(Optional.empty());

        // ---- ACT & ASSERT ----
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sendFriendRequest(me, randomId));
        assertEquals("User not found", ex.getMessage());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_shouldThrowException_whenFriendshipAlreadyExists() {
        // ---- ARRANGE ----
        UUID addresseeId = otherUser.getId();
        when(appUserRepository.findById(addresseeId)).thenReturn(Optional.of(otherUser));
        when(friendshipRepository.existsByRequesterAndAddressee(me, otherUser)).thenReturn(true);

        // ---- ACT & ASSERT ----
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendFriendRequest(me, addresseeId));
        assertEquals("Friendship already exists between these users", ex.getMessage());
        verify(friendshipRepository, never()).save(any());
    }

    // ==========================================
    // ---- respondToRequest Tests ----
    // ==========================================

    @Test
    void respondToRequest_shouldAcceptRequest_whenValid() {
        // ---- ARRANGE ----
        UUID friendshipId = UUID.randomUUID();
        RespondRequestDto dto = new RespondRequestDto(friendshipId, Friendship.Status.ACCEPTED);

        Friendship friendship = new Friendship();
        friendship.setId(friendshipId);
        friendship.setRequester(otherUser);
        friendship.setAddressee(me);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        // ---- ACT ----
        FriendshipResponseDto response = service.respondToRequest(me, dto);

        // ---- ASSERT ----
        assertNotNull(response);
        assertEquals(Friendship.Status.ACCEPTED, friendship.getStatus());
        assertEquals(otherUser.getId(), response.otherUserId());
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void respondToRequest_shouldThrowException_whenRequestNotFound() {
        // ---- ARRANGE ----
        RespondRequestDto dto = new RespondRequestDto(UUID.randomUUID(), Friendship.Status.ACCEPTED);
        when(friendshipRepository.findById(dto.friendshipId())).thenReturn(Optional.empty());

        // ---- ACT & ASSERT ----
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.respondToRequest(me, dto));
        assertEquals("Friend request not found", ex.getMessage());
    }

    @Test
    void respondToRequest_shouldThrowException_whenNotSentToMe() {
        // ---- ARRANGE ----
        UUID friendshipId = UUID.randomUUID();
        RespondRequestDto dto = new RespondRequestDto(friendshipId, Friendship.Status.ACCEPTED);

        Friendship friendship = new Friendship();
        friendship.setId(friendshipId);
        friendship.setRequester(me);
        friendship.setAddressee(otherUser);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        // ---- ACT & ASSERT ----
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.respondToRequest(me, dto));
        assertEquals("You can only respond to requests sent to you", ex.getMessage());
    }

    @Test
    void respondToRequest_shouldThrowException_whenAlreadyHandled() {
        // ---- ARRANGE ----
        UUID friendshipId = UUID.randomUUID();
        RespondRequestDto dto = new RespondRequestDto(friendshipId, Friendship.Status.ACCEPTED);

        Friendship friendship = new Friendship();
        friendship.setId(friendshipId);
        friendship.setRequester(otherUser);
        friendship.setAddressee(me);
        friendship.setStatus(Friendship.Status.ACCEPTED);

        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        // ---- ACT & ASSERT ----
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.respondToRequest(me, dto));
        assertEquals("This request has already been handled", ex.getMessage());
    }

    @Test
    void respondToRequest_shouldThrowException_whenDecisionIsInvalid() {
        // ---- ARRANGE ----
        UUID friendshipId = UUID.randomUUID();
        RespondRequestDto dto = new RespondRequestDto(friendshipId, Friendship.Status.PENDING);

        Friendship friendship = new Friendship();
        friendship.setId(friendshipId);
        friendship.setRequester(otherUser);
        friendship.setAddressee(me);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(friendship));

        // ---- ACT & ASSERT ----
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.respondToRequest(me, dto));
        assertEquals("Decision must be ACCEPTED or DECLINED", ex.getMessage());
    }

    // ==========================================
    // ---- Listen-Abfragen Tests ----
    // ==========================================

    @Test
    void getFriends_shouldReturnCorrectMappedDtos() {
        // ---- ARRANGE ----
        Friendship friendship = new Friendship();
        friendship.setId(UUID.randomUUID());
        friendship.setRequester(me);
        friendship.setAddressee(otherUser);
        friendship.setStatus(Friendship.Status.ACCEPTED);

        when(friendshipRepository.findAllForUserByStatus(me, Friendship.Status.ACCEPTED))
                .thenReturn(List.of(friendship));

        // ---- ACT ----
        List<FriendshipResponseDto> result = service.getFriends(me);

        // ---- ASSERT ----
        assertEquals(1, result.size());
        assertEquals(otherUser.getId(), result.get(0).otherUserId());
        assertEquals(Friendship.Status.ACCEPTED, result.get(0).status());
    }

    @Test
    void getIncomingRequests_shouldReturnPendingRequestsForMe() {
        // ---- ARRANGE ----
        Friendship friendship = new Friendship();
        friendship.setId(UUID.randomUUID());
        friendship.setRequester(otherUser);
        friendship.setAddressee(me);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findByAddresseeAndStatus(me, Friendship.Status.PENDING))
                .thenReturn(List.of(friendship));

        // ---- ACT ----
        List<FriendshipResponseDto> result = service.getIncomingRequests(me);

        // ---- ASSERT ----
        assertEquals(1, result.size());
        assertEquals(otherUser.getId(), result.get(0).otherUserId());
        assertEquals(Friendship.Status.PENDING, result.get(0).status());
    }

    @Test
    void getOutgoingRequests_shouldReturnPendingRequestsFromMe() {
        // ---- ARRANGE ----
        Friendship friendship = new Friendship();
        friendship.setId(UUID.randomUUID());
        friendship.setRequester(me);
        friendship.setAddressee(otherUser);
        friendship.setStatus(Friendship.Status.PENDING);

        when(friendshipRepository.findByRequesterAndStatus(me, Friendship.Status.PENDING))
                .thenReturn(List.of(friendship));

        // ---- ACT ----
        List<FriendshipResponseDto> result = service.getOutgoingRequests(me);

        // ---- ASSERT ----
        assertEquals(1, result.size());
        assertEquals(otherUser.getId(), result.get(0).otherUserId());
        assertEquals(Friendship.Status.PENDING, result.get(0).status());
    }
}