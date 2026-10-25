package com.sep.sep_backend.chat;

import com.sep.sep_backend.chat.dto.MessageDto;
import com.sep.sep_backend.chat.dto.ProposeTripRequest;
import com.sep.sep_backend.chat.dto.TripActionResponse;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.travelentry.TravelEntryService;
import com.sep.sep_backend.travelentry.dto.CreateTravelEntryrequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service handling group trip proposals inside group chats: proposing a trip,
 * collecting accept/decline responses, and adding accepted trips to a member's
 * calendar as a travel entry (where they can attach a budget).
 */
@Service
@RequiredArgsConstructor
public class GroupTripService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final GroupTripRepository tripRepository;
    private final GroupTripParticipantRepository participantRepository;
    private final TravelEntryService travelEntryService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Proposes a trip in a group chat. The proposal is posted to the chat as a
     * message, every member gets a pending response, and the trip is added to
     * the proposer's own calendar right away.
     */
    @Transactional
    public TripActionResponse proposeTrip(AppUser me, UUID conversationId, ProposeTripRequest request) {
        Conversation conversation = loadAsMember(me, conversationId);
        if (conversation.getType() != Conversation.Type.GROUP) {
            throw new IllegalStateException("Group trips can only be proposed in group chats");
        }
        validate(request);

        GroupTrip trip = new GroupTrip();
        trip.setConversation(conversation);
        trip.setProposedBy(me);
        trip.setTitle(request.title().trim());
        trip.setDestination(request.destination().trim());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setDescription(request.description());

        for (ConversationMember member : conversation.getMembers()) {
            GroupTripParticipant participant = new GroupTripParticipant();
            participant.setTrip(trip);
            participant.setUser(member.getUser());
            if (member.getUser().getId().equals(me.getId())) {
                participant.setStatus(GroupTripParticipant.Status.ACCEPTED);
            }
            trip.getParticipants().add(participant);
        }
        GroupTrip saved = tripRepository.save(trip);
        UUID travelEntryId = addToCalendar(me, saved);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(me);
        message.setGroupTrip(saved);
        message.setContent("Group trip proposal: " + saved.getTitle() + " → " + saved.getDestination());
        MessageDto dto = MessageDto.from(messageRepository.save(message));
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, dto);
        chatService.notifyNewMessage(conversation, me.getId());
        return new TripActionResponse(dto, travelEntryId);
    }

    /**
     * Records a member's accept/decline for a trip proposal. On accept the trip
     * is added to their calendar as a travel entry. The updated proposal message
     * is re-broadcast so every member sees the new status.
     */
    @Transactional
    public TripActionResponse respondToTrip(AppUser me, UUID tripId, GroupTripParticipant.Status decision) {
        if (decision != GroupTripParticipant.Status.ACCEPTED
                && decision != GroupTripParticipant.Status.DECLINED) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or DECLINED");
        }
        GroupTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip proposal not found"));
        GroupTripParticipant participant = participantRepository.findByTripAndUser(trip, me)
                .orElseThrow(() -> new IllegalStateException("You are not part of this trip"));

        if (trip.getProposedBy().getId().equals(me.getId())) {
            throw new IllegalStateException("You cannot respond to your own trip proposal");
        }
        if (participant.getStatus() != GroupTripParticipant.Status.PENDING) {
            throw new IllegalStateException("You have already responded to this trip");
        }

        participant.setStatus(decision);
        participantRepository.save(participant);

        UUID travelEntryId = null;
        if (decision == GroupTripParticipant.Status.ACCEPTED) {
            travelEntryId = addToCalendar(me, trip);
        }

        Message message = messageRepository.findFirstByGroupTrip(trip)
                .orElseThrow(() -> new IllegalStateException("Trip proposal message not found"));
        MessageDto dto = MessageDto.from(message);
        messagingTemplate.convertAndSend(
                "/topic/conversation." + trip.getConversation().getId(), dto);
        return new TripActionResponse(dto, travelEntryId);
    }

    /**
     * Creates a personal travel entry (with its linked calendar entry) for the
     * trip and returns its id. The budget is left empty so the user can create
     * their own right after.
     */
    private UUID addToCalendar(AppUser user, GroupTrip trip) {
        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle(trip.getTitle());
        request.setDestination(trip.getDestination());
        request.setStartDate(trip.getStartDate());
        request.setEndDate(trip.getEndDate());
        request.setDescription(trip.getDescription());
        request.setUserId(user.getId().toString());
        return travelEntryService.create(request).getId();
    }

    private void validate(ProposeTripRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Trip needs a title");
        }
        if (request.destination() == null || request.destination().isBlank()) {
            throw new IllegalArgumentException("Trip needs a destination");
        }
        if (request.startDate() == null || request.endDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private Conversation loadAsMember(AppUser me, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (!memberRepository.existsByConversationAndUser(conversation, me)) {
            throw new IllegalStateException("You are not a member of this conversation");
        }
        return conversation;
    }
}
