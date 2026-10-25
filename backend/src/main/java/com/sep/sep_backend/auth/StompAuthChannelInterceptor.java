package com.sep.sep_backend.auth;

import com.sep.sep_backend.chat.Conversation;
import com.sep.sep_backend.chat.ConversationMemberRepository;
import com.sep.sep_backend.chat.ConversationRepository;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;


/**
 * Authenticates WebSocket connections via JWT and authorizes STOMP subscriptions
 * so users can only access their own conversations and notification channels.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String CONVERSATION_PREFIX = "/topic/conversation.";
    private static final String FRIEND_REQUESTS_PREFIX = "/topic/friend-requests.";
    private static final String FRIEND_RESPONSES_PREFIX = "/topic/friend-responses.";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Authenticate once, when the socket opens.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }

        // Authorize every subscription against the relevant per-user access rule.
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String bearer = accessor.getFirstNativeHeader("Authorization");

        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);

            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                accessor.setUser(
                        new UsernamePasswordAuthenticationToken(email, null, List.of()));
            }
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith(CONVERSATION_PREFIX)) {
            authorizeConversation(accessor, destination);
        } else if (destination.startsWith(FRIEND_REQUESTS_PREFIX)) {
            authorizeOwnChannel(accessor, destination, FRIEND_REQUESTS_PREFIX);
        } else if (destination.startsWith(FRIEND_RESPONSES_PREFIX)) {
            authorizeOwnChannel(accessor, destination, FRIEND_RESPONSES_PREFIX);
        }
        // Any other destination carries no per-user rules; let it through.
    }

    private void authorizeConversation(StompHeaderAccessor accessor, String destination) {
        AppUser user = requireUser(accessor);

        UUID conversationId;
        try {
            conversationId = UUID.fromString(destination.substring(CONVERSATION_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid conversation destination");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AccessDeniedException("Conversation not found"));

        if (!memberRepository.existsByConversationAndUser(conversation, user)) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }

    /** Personal notification channels are keyed by user id: you may only watch your own. */
    private void authorizeOwnChannel(StompHeaderAccessor accessor, String destination, String prefix) {
        AppUser user = requireUser(accessor);

        UUID targetUserId;
        try {
            targetUserId = UUID.fromString(destination.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid destination");
        }

        if (!user.getId().equals(targetUserId)) {
            throw new AccessDeniedException("You can only subscribe to your own notifications");
        }
    }

    private AppUser requireUser(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }
}
