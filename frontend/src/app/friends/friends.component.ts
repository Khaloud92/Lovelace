import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { FriendshipService } from './friendship.service';
import { UserSearchResult, FriendshipResponse } from './friendship.model';
import { ChatService } from '../chat/chat.service';
import { AvatarComponent } from '../shared/avatar/avatar.component';

@Component({
  selector: 'app-friends',
  standalone: true,
  imports: [CommonModule, FormsModule, AvatarComponent],
  templateUrl: './friends.component.html',
  styleUrl: './friends.component.css',
})
export class FriendsComponent implements OnInit, AfterViewInit {
  private readonly friendshipService = inject(FriendshipService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('searchBox') searchBox!: ElementRef<HTMLInputElement>;

  query = '';
  results: UserSearchResult[] = [];
  friends: FriendshipResponse[] = [];
  incoming: FriendshipResponse[] = [];
  isSearching = false;
  message = '';

  private readonly searchInput = new Subject<string>();

  ngAfterViewInit(): void {
    this.searchBox.nativeElement.focus();   // caret starts ticking on load
  }

  ngOnInit(): void {
    this.searchInput.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        this.isSearching = true;
        return this.friendshipService.search(q);
      }),
    ).subscribe({
      next: res => { this.results = res; this.isSearching = false; },
      error: () => { this.message = 'Search failed.'; this.isSearching = false; },
    });

    this.loadFriends();
    this.loadIncoming();

    // Live updates pushed over the shared WebSocket.
    // A new request lands at the top of the incoming list (ignoring duplicates).
    this.friendshipService.onIncomingRequest
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(req => {
        if (!this.incoming.some(r => r.friendshipId === req.friendshipId)) {
          this.incoming = [req, ...this.incoming];
        }
      });

    // A request I sent was answered: on accept, the new friend shows up at once.
    this.friendshipService.onRequestAnswered
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => {
        if (res.status === 'ACCEPTED' && !this.friends.some(f => f.friendshipId === res.friendshipId)) {
          this.friends = [res, ...this.friends];
        }
      });
  }

  onSearchChange(): void {
    const q = this.query.trim();
    if (!q) { this.results = []; return; }
    this.searchInput.next(q);
  }

  sendRequest(user: UserSearchResult): void {
    this.message = '';
    this.friendshipService.sendRequest(user.userId).subscribe({
      next: () => { user.relationship = 'REQUEST_SENT'; },
      error: err => { this.message = err?.error?.message ?? 'Could not send request.'; },
    });
  }

  accept(req: FriendshipResponse): void { this.respond(req, 'ACCEPTED'); }
  decline(req: FriendshipResponse): void { this.respond(req, 'DECLINED'); }

  private respond(req: FriendshipResponse, decision: 'ACCEPTED' | 'DECLINED'): void {
    this.message = '';
    this.friendshipService.respond(req.friendshipId, decision).subscribe({
      next: () => {
        this.incoming = this.incoming.filter(r => r.friendshipId !== req.friendshipId);
        if (decision === 'ACCEPTED') this.loadFriends();
        this.friendshipService.refreshPendingCount();
      },
      error: err => { this.message = err?.error?.message ?? 'Could not respond.'; },
    });
  }

  openChat(friendId: string): void {
    this.message = '';
    this.chatService.openDirect(friendId).subscribe({
      next: c => this.router.navigate(['/chats', c.id]),
      error: err => { this.message = err?.error?.message ?? 'Could not open chat.'; },
    });
  }

  private loadFriends(): void {
    this.friendshipService.getFriends().subscribe({ next: f => this.friends = f });
  }

  private loadIncoming(): void {
    this.friendshipService.getIncoming().subscribe({ next: i => this.incoming = i });
  }
}
