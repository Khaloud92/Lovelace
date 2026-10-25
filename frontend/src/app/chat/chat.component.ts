import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChatService } from './chat.service';
import { Conversation, GroupTrip, Message, TripActionResponse, TripStatus } from './chat.model';
import { AuthService } from '../authentication/auth.service';
import { WebSocketService } from '../services/websocket.service';
import { BudgetService } from '../Component/service/budget.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent implements OnInit, OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly budgetService = inject(BudgetService);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly ws = inject(WebSocketService);
  private readonly destroyRef = inject(DestroyRef);

  conversationId = '';
  conversation: Conversation | null = null;
  myUserId = this.authService.getUserId();
  messages: Message[] = [];
  draft = '';

  // group-trip modal state
  showTripModal = false;
  tripTitle = '';
  tripDestination = '';
  tripStartDate = '';
  tripEndDate = '';
  tripDescription = '';
  tripError = '';

  // budget modal state (shown after proposing/accepting a trip)
  showBudgetModal = false;
  budgetTravelEntryId = '';
  budgetTripTitle = '';
  budgetName = '';
  budgetAmount: number | null = null;
  budgetError = '';

  ngOnInit(): void {
    this.conversationId = this.route.snapshot.paramMap.get('id') ?? '';

    // The user is now looking at this chat: clear its unread flag and keep it
    // suppressed so incoming messages here don't re-light the taskbar dot.
    this.chatService.setActiveConversation(this.conversationId);
    this.chatService.markConversationRead(this.conversationId);

    // Needed to know whether this is a group chat (shows the trip button).
    this.chatService.getConversations().pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(convos => this.conversation = convos.find(c => c.id === this.conversationId) ?? null);

    // Subscribe to live updates first so nothing sent between the history
    // load and the subscription is missed. New messages are pushed instantly.
    this.ws.watch<Message>(`/topic/conversation.${this.conversationId}`).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(msg => this.addMessage(msg));

    // Backfill the existing history once.
    this.chatService.getMessages(this.conversationId).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(history => history.forEach(msg => this.addMessage(msg)));
  }

  ngOnDestroy(): void {
    this.chatService.setActiveConversation(null);
  }

  get isGroup(): boolean {
    return this.conversation?.type === 'GROUP';
  }

  send(): void {
    const content = this.draft.trim();
    if (!content) return;
    this.chatService.sendMessage(this.conversationId, content).subscribe({
      next: msg => { this.addMessage(msg); this.draft = ''; },
    });
  }

  toggleTripModal(): void {
    this.showTripModal = !this.showTripModal;
    this.tripTitle = '';
    this.tripDestination = '';
    this.tripStartDate = '';
    this.tripEndDate = '';
    this.tripDescription = '';
    this.tripError = '';
  }

  get tripFormValid(): boolean {
    return !!this.tripTitle.trim()
      && !!this.tripDestination.trim()
      && !!this.tripStartDate
      && !!this.tripEndDate
      && this.tripEndDate >= this.tripStartDate;
  }

  proposeTrip(): void {
    this.tripError = '';
    this.chatService.proposeTrip(this.conversationId, {
      title: this.tripTitle.trim(),
      destination: this.tripDestination.trim(),
      startDate: this.tripStartDate,
      endDate: this.tripEndDate,
      description: this.tripDescription.trim() || null,
    }).subscribe({
      next: res => {
        this.addMessage(res.message);
        this.showTripModal = false;
        this.openBudgetModal(res);
      },
      error: err => { this.tripError = err?.error?.message ?? 'Could not propose the trip.'; },
    });
  }

  respondToTrip(trip: GroupTrip, decision: TripStatus): void {
    this.chatService.respondToTrip(trip.id, decision).subscribe({
      next: res => {
        this.addMessage(res.message);
        this.openBudgetModal(res);
      },
    });
  }

  /** Offer creating a personal budget for the travel entry just added. */
  private openBudgetModal(res: TripActionResponse): void {
    if (!res.travelEntryId) return;
    this.budgetTravelEntryId = res.travelEntryId;
    this.budgetTripTitle = res.message.trip?.title ?? '';
    this.budgetName = res.message.trip?.destination ?? '';
    this.budgetAmount = null;
    this.budgetError = '';
    this.showBudgetModal = true;
  }

  closeBudgetModal(): void {
    this.showBudgetModal = false;
  }

  get budgetFormValid(): boolean {
    return !!this.budgetName.trim() && this.budgetAmount !== null && this.budgetAmount >= 0;
  }

  createBudget(): void {
    this.budgetError = '';
    this.budgetService.createForTravelEntry(this.budgetTravelEntryId, {
      name: this.budgetName.trim(),
      amount: this.budgetAmount ?? 0,
    }).subscribe({
      next: () => this.showBudgetModal = false,
      error: err => { this.budgetError = err?.error?.message ?? 'Could not create the budget.'; },
    });
  }

  /** The current user's response status for a trip, or null if not a participant. */
  myTripStatus(trip: GroupTrip): TripStatus | null {
    return trip.participants.find(p => p.userId === this.myUserId)?.status ?? null;
  }

  /** Upsert a message: trip responses re-broadcast the proposal message with
   *  updated statuses, so an existing message is replaced instead of skipped.
   *  Keeps the list time-ordered. */
  private addMessage(msg: Message): void {
    const existing = this.messages.findIndex(m => m.id === msg.id);
    if (existing >= 0) {
      this.messages[existing] = msg;
      return;
    }
    this.messages.push(msg);
    this.messages.sort((a, b) => a.sentAt.localeCompare(b.sentAt));
  }
}
