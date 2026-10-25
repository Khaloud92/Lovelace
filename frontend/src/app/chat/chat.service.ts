import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { Conversation, Message, ProposeTripRequest, TripActionResponse, TripStatus } from './chat.model';
import { WebSocketService } from '../services/websocket.service';
import { AuthService } from '../authentication/auth.service';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly ws = inject(WebSocketService);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = 'http://localhost:8080/api/conversations';

  /** Conversation IDs with messages the user hasn't seen yet. */
  private readonly unread = signal<string[]>([]);
  /** Drives the red dot on the Chats tab — true when anything is unread. */
  readonly hasUnread = computed(() => this.unread().length > 0);

  /** The conversation currently open on screen (never marked unread). */
  private activeConversationId: string | null = null;
  private messageSub?: Subscription;

  /**
   * Start streaming per-user new-message notifications over the shared socket.
   * Idempotent — a second call while already listening is a no-op.
   */
  listenForMessages(): void {
    if (this.messageSub) return;
    const userId = this.auth.getUserId();
    if (!userId) return; // not logged in yet; the taskbar calls again next session
    this.unread.set(this.loadUnread(userId));
    this.messageSub = this.ws
      .watch<{ conversationId: string }>(`/topic/chat-notifications.${userId}`)
      .subscribe(n => this.markUnread(n.conversationId));
  }

  /** Stop streaming notifications (call on logout, before disconnect). */
  stopListening(): void {
    this.messageSub?.unsubscribe();
    this.messageSub = undefined;
  }

  /** Tell the service which conversation is on screen so it isn't flagged unread. */
  setActiveConversation(conversationId: string | null): void {
    this.activeConversationId = conversationId;
  }

  private markUnread(conversationId: string): void {
    if (conversationId === this.activeConversationId) return;
    if (this.unread().includes(conversationId)) return;
    this.persist([...this.unread(), conversationId]);
  }

  /** Clear the unread flag for one conversation (call when it is opened). */
  markConversationRead(conversationId: string): void {
    if (!this.unread().includes(conversationId)) return;
    this.persist(this.unread().filter(id => id !== conversationId));
  }

  /** Clear every unread flag (call when the chats list is opened). */
  markAllRead(): void {
    if (this.unread().length === 0) return;
    this.persist([]);
  }

  private persist(ids: string[]): void {
    this.unread.set(ids);
    const userId = this.auth.getUserId();
    if (userId) localStorage.setItem(`chat_unread_${userId}`, JSON.stringify(ids));
  }

  private loadUnread(userId: string): string[] {
    try {
      return JSON.parse(localStorage.getItem(`chat_unread_${userId}`) ?? '[]') as string[];
    } catch {
      return [];
    }
  }

  /** Open (or get the existing) 1:1 chat with a friend. */
  openDirect(friendId: string): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.apiUrl}/direct`, { friendId });
  }

  /** Create a named group chat with the given friends. */
  createGroup(name: string, memberIds: string[]): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.apiUrl}/group`, { name, memberIds });
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(this.apiUrl);
  }

  getMessages(conversationId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/${conversationId}/messages`);
  }

  sendMessage(conversationId: string, content: string): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/${conversationId}/messages`, { content });
  }

  /** Propose a group trip; it is posted to the chat as a message. */
  proposeTrip(conversationId: string, trip: ProposeTripRequest): Observable<TripActionResponse> {
    return this.http.post<TripActionResponse>(`${this.apiUrl}/${conversationId}/trips`, trip);
  }

  /** Accept or decline a group trip proposal. */
  respondToTrip(tripId: string, decision: TripStatus): Observable<TripActionResponse> {
    return this.http.post<TripActionResponse>(`${this.apiUrl}/trips/${tripId}/respond`, { decision });
  }
}
