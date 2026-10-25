package com.sep.sep_backend.FriendshipTest;

import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipController;
import com.sep.sep_backend.friendship.FriendshipService;
import com.sep.sep_backend.friendship.dto.FriendshipResponseDto;
import com.sep.sep_backend.friendship.dto.RespondRequestDto;
import com.sep.sep_backend.friendship.dto.UserSearchResultDto;
import com.sep.sep_backend.friendship.dto.sendFriendRequestDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendshipControllerTest {

    @Mock
    private FriendshipService friendshipService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private FriendshipController controller;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        controller = new FriendshipController(friendshipService, appUserRepository);

        // SecurityContext Mocking vorbereiten
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("me@test.com");

        // Aktuellen Benutzer vorbereiten
        currentUser = new AppUser();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("me@test.com");
        currentUser.setName("My Name");

        when(appUserRepository.findByEmail("me@test.com")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // ---- search Tests ----
    // ==========================================

    @Test
    void search_shouldReturnUserSearchResults() {
        // ---- ARRANGE ----
        String query = "Other";
        List<UserSearchResultDto> expectedResults = List.of(
                new UserSearchResultDto(UUID.randomUUID(), "Other", "other@test.com", null, UserSearchResultDto.Relationship.NONE)
        );
        when(friendshipService.searchUsers(currentUser, query)).thenReturn(expectedResults);

        // ---- ACT ----
        List<UserSearchResultDto> result = controller.search(query);

        // ---- ASSERT ----
        assertEquals(expectedResults, result);
        verify(friendshipService).searchUsers(currentUser, query);
    }

    // ==========================================
    // ---- sendRequest Tests ----
    // ==========================================

    @Test
    void sendRequest_shouldReturnCreated_whenSuccessful() {
        // ---- ARRANGE ----
        UUID addresseeId = UUID.randomUUID();
        sendFriendRequestDto dto = new sendFriendRequestDto(addresseeId);
        doNothing().when(friendshipService).sendFriendRequest(currentUser, addresseeId);

        // ---- ACT ----
        ResponseEntity<?> response = controller.sendRequest(dto);

        // ---- ASSERT ----
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
        verify(friendshipService).sendFriendRequest(currentUser, addresseeId);
    }

    @Test
    void sendRequest_shouldReturnBadRequest_whenServiceThrowsException() {
        // ---- ARRANGE ----
        UUID addresseeId = UUID.randomUUID();
        sendFriendRequestDto dto = new sendFriendRequestDto(addresseeId);
        doThrow(new IllegalStateException("Friendship already exists"))
                .when(friendshipService).sendFriendRequest(currentUser, addresseeId);

        // ---- ACT ----
        ResponseEntity<?> response = controller.sendRequest(dto);

        // ---- ASSERT ----
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Friendship already exists", body.get("message"));
    }

    // ==========================================
    // ---- respond Tests ----
    // ==========================================

    @Test
    void respond_shouldReturnOkAndDto_whenSuccessful() {
        // ---- ARRANGE ----
        RespondRequestDto dto = new RespondRequestDto(UUID.randomUUID(), Friendship.Status.ACCEPTED);
        FriendshipResponseDto expectedResponse = new FriendshipResponseDto(dto.friendshipId(), UUID.randomUUID(), "Other", null, Friendship.Status.ACCEPTED);
        when(friendshipService.respondToRequest(currentUser, dto)).thenReturn(expectedResponse);

        // ---- ACT ----
        ResponseEntity<?> response = controller.respond(dto);

        // ---- ASSERT ----
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(friendshipService).respondToRequest(currentUser, dto);
    }

    @Test
    void respond_shouldReturnBadRequest_whenServiceThrowsException() {
        // ---- ARRANGE ----
        RespondRequestDto dto = new RespondRequestDto(UUID.randomUUID(), Friendship.Status.ACCEPTED);
        when(friendshipService.respondToRequest(currentUser, dto))
                .thenThrow(new IllegalArgumentException("Friend request not found"));

        // ---- ACT ----
        ResponseEntity<?> response = controller.respond(dto);

        // ---- ASSERT ----
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Friend request not found", body.get("message"));
    }

    // ==========================================
    // ---- List-Abfragen Tests ----
    // ==========================================

    @Test
    void friends_shouldReturnList() {
        // ---- ARRANGE ----
        List<FriendshipResponseDto> expectedList = List.of(
                new FriendshipResponseDto(UUID.randomUUID(), UUID.randomUUID(), "Friend", null, Friendship.Status.ACCEPTED)
        );
        when(friendshipService.getFriends(currentUser)).thenReturn(expectedList);

        // ---- ACT ----
        List<FriendshipResponseDto> result = controller.friends();

        // ---- ASSERT ----
        assertEquals(expectedList, result);
        verify(friendshipService).getFriends(currentUser);
    }

    @Test
    void incoming_shouldReturnList() {
        // ---- ARRANGE ----
        List<FriendshipResponseDto> expectedList = List.of(
                new FriendshipResponseDto(UUID.randomUUID(), UUID.randomUUID(), "Requester", null, Friendship.Status.PENDING)
        );
        when(friendshipService.getIncomingRequests(currentUser)).thenReturn(expectedList);

        // ---- ACT ----
        List<FriendshipResponseDto> result = controller.incoming();

        // ---- ASSERT ----
        assertEquals(expectedList, result);
        verify(friendshipService).getIncomingRequests(currentUser);
    }

    @Test
    void outgoing_shouldReturnList() {
        // ---- ARRANGE ----
        List<FriendshipResponseDto> expectedList = List.of(
                new FriendshipResponseDto(UUID.randomUUID(), UUID.randomUUID(), "Addressee", null, Friendship.Status.PENDING)
        );
        when(friendshipService.getOutgoingRequests(currentUser)).thenReturn(expectedList);

        // ---- ACT ----
        List<FriendshipResponseDto> result = controller.outgoing();

        // ---- ASSERT ----
        assertEquals(expectedList, result);
        verify(friendshipService).getOutgoingRequests(currentUser);
    }
}