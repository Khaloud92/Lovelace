import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChatService } from './chat.service';
import { AvatarComponent } from '../shared/avatar/avatar.component';
import { Conversation } from './chat.model';
import { FriendshipService } from '../friends/friendship.service';
import { FriendshipResponse } from '../friends/friendship.model';

@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AvatarComponent],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.css',
})
export class ChatListComponent implements OnInit {
  private readonly chatService = inject(ChatService);
  private readonly friendshipService = inject(FriendshipService);
  private readonly router = inject(Router);

  conversations: Conversation[] = [];
  friends: FriendshipResponse[] = [];
  message = '';

  // group-chat modal state
  showGroupModal = false;
  groupName = '';
  selectedFriendIds = new Set<string>();

  ngOnInit(): void {
    this.chatService.markAllRead();
    this.loadConversations();
    this.friendshipService.getFriends().subscribe({ next: f => this.friends = f });
  }

  private loadConversations(): void {
    this.chatService.getConversations().subscribe({
      next: convos => this.conversations = convos,
    });
  }

  toggleGroupModal(): void {
    this.showGroupModal = !this.showGroupModal;
    this.groupName = '';
    this.selectedFriendIds.clear();
  }

  toggleFriendSelection(friendId: string): void {
    if (this.selectedFriendIds.has(friendId)) {
      this.selectedFriendIds.delete(friendId);
    } else {
      this.selectedFriendIds.add(friendId);
    }
  }

  createGroup(): void {
    this.message = '';
    this.chatService.createGroup(this.groupName, [...this.selectedFriendIds]).subscribe({
      next: c => { this.showGroupModal = false; this.router.navigate(['/chats', c.id]); },
      error: err => { this.message = err?.error?.message ?? 'Could not create group.'; },
    });
  }
}
