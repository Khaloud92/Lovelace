export interface FriendLocation {
  userId: string;
  name: string;
  profilePicture?: string;
  latitude: number;
  longitude: number;
  updatedAt: string;
}

/** Pushed over the WebSocket whenever a friend moves or stops sharing. */
export interface FriendLocationEvent {
  userId: string;
  name: string;
  profilePicture?: string;
  latitude: number | null;
  longitude: number | null;
  updatedAt: string | null;
  sharing: boolean;
}
