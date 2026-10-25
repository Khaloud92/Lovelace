package com.sep.sep_backend.chatTest;

import com.sep.sep_backend.chat.*;
import com.sep.sep_backend.chat.dto.ConversationDto;
import com.sep.sep_backend.chat.dto.CreateGroupRequest;
import com.sep.sep_backend.chat.dto.MessageDto;
import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipRepository;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;


import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class chatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository memberRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;


    @InjectMocks
    private ChatService chatService;

    private AppUser me;
    private AppUser friend;
    private UUID myId;
    private UUID friendId;

    @BeforeEach
    void setUp() {
        myId = UUID.randomUUID();
        friendId = UUID.randomUUID();

        me = new AppUser();
        me.setId(myId);
        me.setName("Mein Name");

        friend = new AppUser();
        friend.setId(friendId);
        friend.setName("Freund Name");
    }

    private void mockFriendship(Friendship.Status status) {
        Friendship friendship = new Friendship();
        friendship.setStatus(status);
        when(friendshipRepository.findBetween(me, friend)).thenReturn(Optional.of(friendship));
    }

    // --- getOrCreateDirect ---

    @Test
    void getOrCreateDirect_shouldThrowExceptionWhenChatWithSelf() {
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                chatService.getOrCreateDirect(me, myId)
        );
        assertEquals("You cannot start a chat with yourself", exception.getMessage());
    }

    @Test
    void getOrCreateDirect_shouldThrowExceptionWhenFriendNotFound() {
        // Arrange
        when(appUserRepository.findById(friendId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                chatService.getOrCreateDirect(me, friendId)
        );
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getOrCreateDirect_shouldThrowExceptionWhenNotFriends() {
        // Arrange
        when(appUserRepository.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRepository.findBetween(me, friend)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                chatService.getOrCreateDirect(me, friendId)
        );
        assertEquals("You can only chat with your friends", exception.getMessage());
    }

    @Test
    void getOrCreateDirect_shouldReturnExistingConversation() {
        // Arrange
        when(appUserRepository.findById(friendId)).thenReturn(Optional.of(friend));
        mockFriendship(Friendship.Status.ACCEPTED);

        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setType(Conversation.Type.DIRECT);

        ConversationMember m1 = new ConversationMember(); m1.setUser(me); m1.setConversation(conversation);
        ConversationMember m2 = new ConversationMember(); m2.setUser(friend); m2.setConversation(conversation);
        conversation.setMembers(new ArrayList<>(List.of(m1, m2)));

        when(conversationRepository.findDirectBetween(me, friend, Conversation.Type.DIRECT))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findFirstByConversationOrderBySentAtDesc(conversation)).thenReturn(Optional.empty());

        // Act
        ConversationDto result = chatService.getOrCreateDirect(me, friendId);

        // Assert
        assertNotNull(result);
        assertEquals(conversation.getId(), result.id());
        assertEquals("Freund Name", result.displayName()); // Zeigt den Namen des anderen an
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateDirect_shouldCreateNewConversationWhenNoneExists() {
        // Arrange
        when(appUserRepository.findById(friendId)).thenReturn(Optional.of(friend));
        mockFriendship(Friendship.Status.ACCEPTED);

        when(conversationRepository.findDirectBetween(me, friend, Conversation.Type.DIRECT))
                .thenReturn(Optional.empty());

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(messageRepository.findFirstByConversationOrderBySentAtDesc(any())).thenReturn(Optional.empty());

        // Act
        ConversationDto result = chatService.getOrCreateDirect(me, friendId);

        // Assert
        assertNotNull(result);
        assertEquals("DIRECT", result.type());
        verify(conversationRepository).save(any(Conversation.class));
    }

    // --- createGroup ---

    @Test
    void createGroup_shouldThrowExceptionWhenNameMissing() {
        // Arrange
        CreateGroupRequest request = new CreateGroupRequest("", List.of(friendId));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                chatService.createGroup(me, request)
        );
        assertEquals("Group chat needs a name", exception.getMessage());
    }

    @Test
    void createGroup_shouldThrowExceptionWhenMembersEmpty() {
        // Arrange
        CreateGroupRequest request = new CreateGroupRequest("Urlaub 2026", List.of());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                chatService.createGroup(me, request)
        );
        assertEquals("Add at least one friend to the group", exception.getMessage());
    }

    @Test
    void createGroup_shouldCreateSuccessfully() {
        // Arrange
        CreateGroupRequest request = new CreateGroupRequest("AG Sep", List.of(friendId));
        when(appUserRepository.findById(friendId)).thenReturn(Optional.of(friend));
        mockFriendship(Friendship.Status.ACCEPTED);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(messageRepository.findFirstByConversationOrderBySentAtDesc(any())).thenReturn(Optional.empty());

        // Act
        ConversationDto result = chatService.createGroup(me, request);

        // Assert
        assertNotNull(result);
        assertEquals("AG Sep", result.displayName());
        assertEquals("GROUP", result.type());
        verify(conversationRepository).save(any(Conversation.class));
    }

    // --- listMyConversations ---

    @Test
    void listMyConversations_shouldReturnSortedConversations() {
        // Arrange
        Conversation c1 = new Conversation(); c1.setId(UUID.randomUUID()); c1.setType(Conversation.Type.GROUP); c1.setName("Chat 1");
        Conversation c2 = new Conversation(); c2.setId(UUID.randomUUID()); c2.setType(Conversation.Type.GROUP); c2.setName("Chat 2");

        when(conversationRepository.findAllForUser(me)).thenReturn(List.of(c1, c2));

        Message m1 = new Message(); m1.setContent("Neuer"); m1.setSentAt(LocalDateTime.now().plusHours(1));
        Message m2 = new Message(); m2.setContent("Älterer"); m2.setSentAt(LocalDateTime.now());

        // c1 hat die neuere Nachricht, c2 die ältere
        when(messageRepository.findFirstByConversationOrderBySentAtDesc(c1)).thenReturn(Optional.of(m1));
        when(messageRepository.findFirstByConversationOrderBySentAtDesc(c2)).thenReturn(Optional.of(m2));

        // Act
        List<ConversationDto> result = chatService.listMyConversations(me);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Chat 1", result.get(0).displayName()); // c1 muss wegen Sortierung zuerst kommen
        assertEquals("Chat 2", result.get(1).displayName());
    }

    // --- getMessages ---

    @Test
    void getMessages_shouldThrowExceptionWhenNotMember() {
        // Arrange
        UUID convId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationAndUser(conversation, me)).thenReturn(false);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                chatService.getMessages(me, convId)
        );
        assertEquals("You are not a member of this conversation", exception.getMessage());
    }

    @Test
    void getMessages_shouldReturnMessageListOrdered() {
        // Arrange
        UUID convId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(convId);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationAndUser(conversation, me)).thenReturn(true);

        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setConversation(conversation);
        msg.setSender(me);
        msg.setContent("Hi!");
        msg.setSentAt(LocalDateTime.now());

        when(messageRepository.findByConversationOrderBySentAtAsc(conversation)).thenReturn(List.of(msg));

        // Act
        List<MessageDto> result = chatService.getMessages(me, convId);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Hi!", result.get(0).content());
        verify(messageRepository).findByConversationOrderBySentAtAsc(conversation);
    }

    // --- sendMessage ---

    @Test
    void sendMessage_shouldThrowExceptionWhenContentEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                chatService.sendMessage(me, UUID.randomUUID(), "   ")
        );
        assertEquals("Message cannot be empty", exception.getMessage());
    }

    @Test
    void sendMessage_shouldSaveAndReturnMessage() {
        // Arrange
        UUID convId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(convId);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationAndUser(conversation, me)).thenReturn(true);

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        // Act
        MessageDto result = chatService.sendMessage(me, convId, "  Test Nachricht  ");

        // Assert
        assertNotNull(result);
        assertEquals("Test Nachricht", result.content()); // Überprüft das Trimming
        assertEquals(myId, result.senderId());
        verify(messageRepository).save(any(Message.class));
    }
}