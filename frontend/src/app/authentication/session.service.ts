import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { FriendshipService } from '../friends/friendship.service';
import { ChatService } from '../chat/chat.service';
import { WebSocketService } from '../services/websocket.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly auth = inject(AuthService);
  private readonly friendship = inject(FriendshipService);
  private readonly chat = inject(ChatService);
  private readonly ws = inject(WebSocketService);
  private readonly router = inject(Router);

  // Single exit point for all sign-outs: stop listeners, drop the socket, clear auth.
  logout(): void {
    this.friendship.stopListening();
    this.chat.stopListening();
    this.ws.disconnect();
    this.auth.logout();
    this.router.navigate(['/home']);
  }
}
