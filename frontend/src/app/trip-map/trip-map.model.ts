export interface MapTrip {
  id: string;
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  color?: string;
  latitude: number;
  longitude: number;
  ownerId: string;
  ownerName: string;
  ownerProfilePicture?: string;
  own: boolean;
}
