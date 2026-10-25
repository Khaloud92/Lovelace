package com.sep.sep_backend.chatTest;

import com.sep.sep_backend.chat.ChatController;
import com.sep.sep_backend.chat.ChatService;
import com.sep.sep_backend.chat.dto.*;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
public class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ChatController controller;

    private AppUser mockUser;
    private final String userEmail = "test@user.com";

    @BeforeEach
    void setUp() {
        // Prepares the SecurityContext for the controller's currentUser() lookup.
        SecurityContextHolder.setContext(securityContext);
        mockUser = new AppUser();
    }

    private void mockCurrentUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userEmail);
        when(appUserRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
    }

    // --- POST /api/conversations/direct ---

    @Test
    void openDirect_shouldReturn200WithConversation() {
        // Arrange
        mockCurrentUser();
        UUID friendId = UUID.randomUUID();
        CreateDirectRequest request = new CreateDirectRequest(friendId);
        ConversationDto mockDto = mock(ConversationDto.class);

        when(chatService.getOrCreateDirect(mockUser, friendId)).thenReturn(mockDto);

        // Act
        ResponseEntity<?> response = controller.openDirect(request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockDto, response.getBody());
        verify(chatService).getOrCreateDirect(mockUser, friendId);
    }

    @Test
    void openDirect_shouldReturn400WhenIllegalArgumentExceptionThrown() {
        // Arrange
        mockCurrentUser();
        UUID friendId = UUID.randomUUID();
        CreateDirectRequest request = new CreateDirectRequest(friendId);
        String errorMessage = "User not found";

        when(chatService.getOrCreateDirect(mockUser, friendId))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.openDirect(request);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(errorMessage, ((Map<?, ?>) response.getBody()).get("message"));
        verify(chatService).getOrCreateDirect(mockUser, friendId);
    }

    // --- POST /api/conversations/group ---

    @Test
    void createGroup_shouldReturn200WithCreatedGroup() {
        // Arrange
        mockCurrentUser();
        CreateGroupRequest request = new CreateGroupRequest("Tech Group", List.of(UUID.randomUUID()));
        ConversationDto mockDto = mock(ConversationDto.class);

        when(chatService.createGroup(mockUser, request)).thenReturn(mockDto);

        // Act
        ResponseEntity<?> response = controller.createGroup(request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockDto, response.getBody());
        verify(chatService).createGroup(mockUser, request);
    }

    @Test
    void createGroup_shouldReturn400WhenIllegalStateExceptionThrown() {
        // Arrange
        mockCurrentUser();
        CreateGroupRequest request = new CreateGroupRequest("Tech Group", List.of(UUID.randomUUID()));
        String errorMessage = "Invalid group members";

        when(chatService.createGroup(mockUser, request))
                .thenThrow(new IllegalStateException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.createGroup(request);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(errorMessage, ((Map<?, ?>) response.getBody()).get("message"));
        verify(chatService).createGroup(mockUser, request);
    }

    // --- GET /api/conversations ---

    @Test
    void myConversations_shouldReturnConversationsList() {
        // Arrange
        mockCurrentUser();
        List<ConversationDto> mockList = List.of(mock(ConversationDto.class), mock(ConversationDto.class));

        when(chatService.listMyConversations(mockUser)).thenReturn(mockList);

        // Act
        List<ConversationDto> response = controller.myConversations();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(mockList, response);
        verify(chatService).listMyConversations(mockUser);
    }

    // --- GET /api/conversations/{id}/messages ---

    @Test
    void messages_shouldReturn200WithMessagesList() {
        // Arrange
        mockCurrentUser();
        UUID conversationId = UUID.randomUUID();
        List<MessageDto> mockMessages = List.of(mock(MessageDto.class));

        when(chatService.getMessages(mockUser, conversationId)).thenReturn(mockMessages);

        // Act
        ResponseEntity<?> response = controller.messages(conversationId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockMessages, response.getBody());
        verify(chatService).getMessages(mockUser, conversationId);
    }

    @Test
    void messages_shouldReturn400WhenAccessDenied() {
        // Arrange
        mockCurrentUser();
        UUID conversationId = UUID.randomUUID();
        String errorMessage = "Not a member of this conversation";

        when(chatService.getMessages(mockUser, conversationId))
                .thenThrow(new IllegalStateException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.messages(conversationId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(errorMessage, ((Map<?, ?>) response.getBody()).get("message"));
        verify(chatService).getMessages(mockUser, conversationId);
    }

    // --- POST /api/conversations/{id}/messages ---

    @Test
    void send_shouldReturn200WithSentMessage() {
        // Arrange
        mockCurrentUser();
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("Hallo Welt");
        MessageDto mockMessageDto = mock(MessageDto.class);

        when(chatService.sendMessage(mockUser, conversationId, "Hallo Welt")).thenReturn(mockMessageDto);

        // Act
        ResponseEntity<?> response = controller.send(conversationId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockMessageDto, response.getBody());
        verify(chatService).sendMessage(mockUser, conversationId, "Hallo Welt");
    }

    @Test
    void send_shouldReturn400WhenSendingFails() {
        // Arrange
        mockCurrentUser();
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("");
        String errorMessage = "Message content cannot be empty";

        when(chatService.sendMessage(mockUser, conversationId, ""))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.send(conversationId, request);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(errorMessage, ((Map<?, ?>) response.getBody()).get("message"));
        verify(chatService).sendMessage(mockUser, conversationId, "");
    }
}
