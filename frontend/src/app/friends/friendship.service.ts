import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject, Subscription } from 'rxjs';
import { UserSearchResult, FriendshipResponse, FriendshipStatus } from './friendship.model';
import { WebSocketService } from '../services/websocket.service';
import { AuthService } from '../authentication/auth.service';

@Injectable({ providedIn: 'root' })
export class FriendshipService {
  private readonly http = inject(HttpClient);
  private readonly ws = inject(WebSocketService);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = 'http://localhost:8080/api/friendships';

  /** Single source of truth for the red dot on the profile icon. */
  readonly pendingRequestCount = signal(0);

  /** A new request *I* received (someone wants to be my friend). */
  private readonly incomingRequest = new Subject<FriendshipResponse>();
  readonly onIncomingRequest = this.incomingRequest.asObservable();

  /** A request *I sent* was accepted or declined by the other person. */
  private readonly requestAnswered = new Subject<FriendshipResponse>();
  readonly onRequestAnswered = this.requestAnswered.asObservable();

  private socketSubs?: Subscription;

  search(query: string): Observable<UserSearchResult[]> {
    return this.http.get<UserSearchResult[]>(`${this.apiUrl}/search`, {
      params: new HttpParams().set('query', query),
    });
  }

  sendRequest(addresseeID: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/requests`, { addresseeID });
  }

  respond(friendshipId: string, decision: FriendshipStatus): Observable<FriendshipResponse> {
    return this.http.post<FriendshipResponse>(`${this.apiUrl}/respond`, { friendshipId, decision });
  }

  getFriends(): Observable<FriendshipResponse[]> {
    return this.http.get<FriendshipResponse[]>(this.apiUrl);
  }

  getIncoming(): Observable<FriendshipResponse[]> {
    return this.http.get<FriendshipResponse[]>(`${this.apiUrl}/requests/incoming`);
  }

  /** Re-fetch the pending incoming count and update the signal the taskbar reads. */
  refreshPendingCount(): void {
    this.getIncoming().subscribe({
      next: reqs => this.pendingRequestCount.set(reqs.length),
      error: () => this.pendingRequestCount.set(0),
    });
  }

  /**
   * Start streaming friend-request events over the shared WebSocket.
   * Idempotent — calling it again while already listening is a no-op.
   *
   * The server pushes the new request object, so we update state from the
   * payload directly rather than re-fetching (the push fires inside the
   * sender's transaction, so a re-fetch could miss the not-yet-committed row).
   */
  listenForFriendRequests(): void {
    if (this.socketSubs) return;
    const userId = this.auth.getUserId();
    if (!userId) return; // not logged in yet; the taskbar calls again next session
    this.socketSubs = new Subscription();

    this.socketSubs.add(
      this.ws.watch<FriendshipResponse>(`/topic/friend-requests.${userId}`).subscribe(req => {
        this.pendingRequestCount.update(n => n + 1);
        this.incomingRequest.next(req);
      }),
    );

    this.socketSubs.add(
      this.ws.watch<FriendshipResponse>(`/topic/friend-responses.${userId}`).subscribe(res => {
        this.requestAnswered.next(res);
      }),
    );
  }

  /** Stop streaming friend-request events (call on logout, before disconnect). */
  stopListening(): void {
    this.socketSubs?.unsubscribe();
    this.socketSubs = undefined;
  }
}
