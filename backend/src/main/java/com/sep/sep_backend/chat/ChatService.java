package com.sep.sep_backend.chat;

import com.sep.sep_backend.chat.dto.ChatNotification;
import com.sep.sep_backend.chat.dto.ConversationDto;
import com.sep.sep_backend.chat.dto.CreateGroupRequest;
import com.sep.sep_backend.chat.dto.MemberDto;
import com.sep.sep_backend.chat.dto.MessageDto;
import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipRepository;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service handling chat operations including direct chats, group chats, and messaging.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final AppUserRepository appUserRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Retrieves an existing 1:1 chat or creates a new one if it doesn't exist.
     */
    @Transactional
    public ConversationDto getOrCreateDirect(AppUser me, UUID friendId) {
        if (me.getId().equals(friendId)) {
            throw new IllegalStateException("You cannot start a chat with yourself");
        }
        AppUser friend = appUserRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        requireFriendship(me, friend);

        Conversation conversation = conversationRepository
                .findDirectBetween(me, friend, Conversation.Type.DIRECT)
                .orElseGet(() -> createDirect(me, friend));

        return toDto(conversation, me);
    }


    private Conversation createDirect(AppUser me, AppUser friend) {
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.Type.DIRECT);
        conversation.setCreatedBy(me);
        addMember(conversation, me);
        addMember(conversation, friend);
        return conversationRepository.save(conversation);
    }

    /**
     * Creates a new group chat and registers all valid friends as members.
     */
    @Transactional
    public ConversationDto createGroup(AppUser me, CreateGroupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Group chat needs a name");
        }
        if (request.memberIds() == null || request.memberIds().isEmpty()) {
            throw new IllegalArgumentException("Add at least one friend to the group");
        }

        Conversation conversation = new Conversation();
        conversation.setType(Conversation.Type.GROUP);
        conversation.setName(request.name().trim());
        conversation.setCreatedBy(me);
        addMember(conversation, me);

        for (UUID memberId : request.memberIds()) {
            if (memberId.equals(me.getId())) {
                continue; // creator is already a member
            }
            AppUser member = appUserRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            requireFriendship(me, member);
            addMember(conversation, member);
        }

        return toDto(conversationRepository.save(conversation), me);
    }

    /**
     * Lists all conversations for the user, sorted by the latest message timestamp.
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> listMyConversations(AppUser me) {
        return conversationRepository.findAllForUser(me).stream()
                .map(c -> toDto(c, me))
                .sorted(Comparator.comparing(ConversationDto::lastMessageAt).reversed())
                .toList();
    }

    /**
     * Fetches all messages from a conversation, ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(AppUser me, UUID conversationId) {
        Conversation conversation = loadAsMember(me, conversationId);
        return messageRepository.findByConversationOrderBySentAtAsc(conversation).stream()
                .map(this::toMessageDto)
                .toList();
    }

    /**
     * Saves and sends a new message within a specific conversation.
     */
    @Transactional
    public MessageDto sendMessage(AppUser me, UUID conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        Conversation conversation = loadAsMember(me, conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(me);
        message.setContent(content.trim());
        Message saved = messageRepository.save(message);
        MessageDto dto = toMessageDto(saved);
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, dto);
        notifyNewMessage(conversation, me.getId());
        return dto;
    }

    /**
     * Pushes an unread notification to every member of the conversation except
     * the sender, on their personal chat-notifications topic.
     */
    public void notifyNewMessage(Conversation conversation, UUID senderId) {
        ChatNotification notification = new ChatNotification(conversation.getId(), senderId);
        for (ConversationMember member : conversation.getMembers()) {
            UUID memberId = member.getUser().getId();
            if (!memberId.equals(senderId)) {
                messagingTemplate.convertAndSend("/topic/chat-notifications." + memberId, notification);
            }
        }
    }

    private void addMember(Conversation conversation, AppUser user) {
        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        conversation.getMembers().add(member);
    }

    /**
     * Ensures that a chat is only allowed if the users are active friends.
     */
    private void requireFriendship(AppUser me, AppUser other) {
        boolean friends = friendshipRepository.findBetween(me, other)
                .map(f -> f.getStatus() == Friendship.Status.ACCEPTED)
                .orElse(false);
        if (!friends) {
            throw new IllegalStateException("You can only chat with your friends");
        }
    }

    /**
     * Validates that the conversation exists and the current user is a participant.
     */
    private Conversation loadAsMember(AppUser me, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (!memberRepository.existsByConversationAndUser(conversation, me)) {
            throw new IllegalStateException("You are not a member of this conversation");
        }
        return conversation;
    }

    /**
     * Converts a conversation entity to its DTO representation, adapting the display name dynamically.
     */
    private ConversationDto toDto(Conversation c, AppUser me) {
        List<MemberDto> members = c.getMembers().stream()
                .map(m -> new MemberDto(m.getUser().getId(), m.getUser().getName(), m.getUser().getProfilePicture()))
                .toList();

        String displayName;
        String displayAvatar;
        if (c.getType() == Conversation.Type.GROUP) {
            displayName = c.getName();
            displayAvatar = null;
        } else {
            // Dynamic naming: for 1:1 chats, show the other user's name and picture
            AppUser other = c.getMembers().stream()
                    .map(ConversationMember::getUser)
                    .filter(u -> !u.getId().equals(me.getId()))
                    .findFirst()
                    .orElse(null);
            displayName = other != null ? other.getName() : "Unknown";
            displayAvatar = other != null ? other.getProfilePicture() : null;
        }

        var lastMessage = messageRepository.findFirstByConversationOrderBySentAtDesc(c);

        return new ConversationDto(
                c.getId(),
                c.getType().name(),
                displayName,
                displayAvatar,
                members,
                lastMessage.map(Message::getContent).orElse(null),
                lastMessage.map(Message::getSentAt).orElse(c.getCreatedAt())
        );
    }

    private MessageDto toMessageDto(Message m) {
        return MessageDto.from(m);
    }
}