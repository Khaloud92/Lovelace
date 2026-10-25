export interface MailboxStatus {
  userId: string;
  connected: boolean;
  emailAddress: string | null;
  provider: string | null;
  connectedAt: string | null;
}

export interface MailboxConnectionRequest {
  emailAddress: string;
  provider: 'GMAIL' | string;
  appPassword: string;
}

export interface EmailCost {
  id: string;
  title: string;
  amount: number;
  currency: string;
  category: string;
  subject: string;
  source: string;
  externalMessageId: string | null;
  emailText: string;
  userId: string;
  travelEntryId: string | null;
  importedAt: string;
  ignored: boolean;
}

export interface FetchEmailCostsRequest {
  travelEntryId?: string | null;
}
