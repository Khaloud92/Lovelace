export type Relationship = 'NONE' | 'REQUEST_SENT' | 'REQUEST_RECEIVED' | 'FRIENDS';
export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface UserSearchResult {
  userId: string;
  name: string;
  email: string;
  profilePicture: string | null;
  relationship: Relationship;
}

export interface FriendshipResponse {
  friendshipId: string;
  otherUserId: string;
  otherUserName: string;
  otherUserProfilePicture: string | null;
  status: FriendshipStatus;
}
