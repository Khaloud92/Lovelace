import { Injectable, inject } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { Observable, map } from 'rxjs';
import { AuthService } from '../authentication/auth.service';

/**
 * Single shared STOMP/WebSocket connection for the whole app.
 *
 * Every feature reuses this one socket: call `watch(topic)` to receive
 * server-pushed messages on a topic. The connection is opened lazily on the
 * first `watch(...)` and torn down on logout.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly auth = inject(AuthService);
  private readonly rxStomp = new RxStomp();
  private activated = false;

  /** Open the shared connection (idempotent — safe to call repeatedly). */
  connect(): void {
    if (this.activated) return;

    const token = this.auth.getToken();
    if (!token) return; // not logged in yet; watch() will retry once we are

    this.rxStomp.configure({
      brokerURL: 'ws://localhost:8080/ws',
      // The backend reads this on the STOMP CONNECT frame (browsers can't set
      // HTTP headers on a WebSocket handshake, so the JWT travels here instead).
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
    });
    this.rxStomp.activate();
    this.activated = true;
  }

  /**
   * Subscribe to a topic and stream its parsed message bodies.
   * Re-subscribes automatically after a dropped connection.
   */
  watch<T>(destination: string): Observable<T> {
    this.connect();
    return this.rxStomp
      .watch(destination)
      .pipe(map((frame) => JSON.parse(frame.body) as T));
  }

  /** Close the connection (call on logout). */
  disconnect(): void {
    if (!this.activated) return;
    this.rxStomp.deactivate();
    this.activated = false;
  }
}
