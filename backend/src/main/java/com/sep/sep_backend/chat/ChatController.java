package com.sep.sep_backend.chat;

import com.sep.sep_backend.chat.dto.ConversationDto;
import com.sep.sep_backend.chat.dto.CreateDirectRequest;
import com.sep.sep_backend.chat.dto.CreateGroupRequest;
import com.sep.sep_backend.chat.dto.MessageDto;
import com.sep.sep_backend.chat.dto.ProposeTripRequest;
import com.sep.sep_backend.chat.dto.RespondTripRequest;
import com.sep.sep_backend.chat.dto.SendMessageRequest;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing chat conversations and messages.
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final GroupTripService groupTripService;
    private final AppUserRepository appUserRepository;

    /**
     * Opens or retrieves an existing 1-on-1 direct chat with a friend.
     */
    @PostMapping("/direct")
    public ResponseEntity<?> openDirect(@RequestBody CreateDirectRequest request) {
        try {
            return ResponseEntity.ok(chatService.getOrCreateDirect(currentUser(), request.friendId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Creates a new group chat with multiple participants.
     */
    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            return ResponseEntity.ok(chatService.createGroup(currentUser(), request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Lists all active conversations (direct and group) for the current user.
     */
    @GetMapping
    public List<ConversationDto> myConversations() {
        return chatService.listMyConversations(currentUser());
    }

    /**
     * Retrieves the history of messages for a specific conversation.
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(chatService.getMessages(currentUser(), id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Sends a new message to a specific conversation.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> send(@PathVariable UUID id, @RequestBody SendMessageRequest request) {
        try {
            return ResponseEntity.ok(chatService.sendMessage(currentUser(), id, request.content()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Proposes a group trip in a group chat; the proposal is posted as a chat message.
     */
    @PostMapping("/{id}/trips")
    public ResponseEntity<?> proposeTrip(@PathVariable UUID id, @RequestBody ProposeTripRequest request) {
        try {
            return ResponseEntity.ok(groupTripService.proposeTrip(currentUser(), id, request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Accepts or declines a group trip proposal.
     */
    @PostMapping("/trips/{tripId}/respond")
    public ResponseEntity<?> respondToTrip(@PathVariable UUID tripId, @RequestBody RespondTripRequest request) {
        try {
            return ResponseEntity.ok(groupTripService.respondToTrip(currentUser(), tripId, request.decision()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Fetches the currently authenticated user from the security context.
     */
    private AppUser currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}