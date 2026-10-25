export type ConversationType = 'DIRECT' | 'GROUP';

export interface Member {
  userId: string;
  name: string;
  profilePicture: string | null;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  displayName: string;
  displayAvatar: string | null;
  members: Member[];
  lastMessage: string | null;
  lastMessageAt: string | null;
}

export type TripStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface TripParticipant {
  userId: string;
  userName: string;
  status: TripStatus;
}

export interface GroupTrip {
  id: string;
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  description: string | null;
  proposedById: string;
  proposedByName: string;
  participants: TripParticipant[];
}

export interface ProposeTripRequest {
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  description: string | null;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  sentAt: string;
  trip: GroupTrip | null;
}

export interface TripActionResponse {
  message: Message;
  travelEntryId: string | null;
}
