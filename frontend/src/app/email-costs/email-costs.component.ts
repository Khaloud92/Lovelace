import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { EmailCost, MailboxStatus } from './email-cost.model';
import { EmailCostService } from './email-cost.service';

@Component({
  selector: 'app-email-costs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './email-costs.component.html',
  styleUrl: './email-costs.component.css',
})
export class EmailCostsComponent implements OnInit {
  private readonly emailCostService = inject(EmailCostService);
  private readonly travelEntryService = inject(TravelEntryService);

  mailboxStatus: MailboxStatus | null = null;
  emailCosts: EmailCost[] = [];
  travelEntries: TravelEntry[] = [];

  emailAddress = '';
  appPassword = '';
  selectedTravelEntryId = '';

  processingEmailCostId: string | null = null;

  loadingStatus = false;
  loadingCosts = false;
  loadingTravelEntries = false;
  connecting = false;
  fetching = false;
  refreshingFromEmail = false;
  disconnecting = false;

  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadMailboxStatus();
    this.loadTravelEntries();
  }

  loadMailboxStatus(): void {
    this.loadingStatus = true;
    this.clearMessages();

    this.emailCostService.getMailboxStatus().subscribe({
      next: (status) => {
        this.mailboxStatus = status;
        this.loadingStatus = false;
      },
      error: () => {
        this.errorMessage = 'Could not load mailbox status. Please check if the backend is running.';
        this.loadingStatus = false;
      },
    });
  }

  loadTravelEntries(): void {
    this.loadingTravelEntries = true;

    this.travelEntryService.getTravelEntries().subscribe({
      next: (travelEntries) => {
        this.travelEntries = travelEntries;
        this.loadingTravelEntries = false;
        this.loadEmailCosts();
      },
      error: () => {
        this.loadingTravelEntries = false;
        this.errorMessage = 'Could not load travel entries.';
        this.loadEmailCosts();
      },
    });
  }

  loadEmailCosts(): void {
    this.loadingCosts = true;

    this.emailCostService.getEmailCosts(this.selectedTravelEntryId || null).subscribe({
      next: (costs) => {
        this.emailCosts = costs;
        this.loadingCosts = false;
      },
      error: () => {
        this.errorMessage = 'Could not load imported email costs.';
        this.loadingCosts = false;
      },
    });
  }

  onTravelEntryChange(): void {
    this.clearMessages();
    this.loadEmailCosts();
  }

  connectMailbox(): void {
    this.clearMessages();

    if (!this.emailAddress.trim()) {
      this.errorMessage = 'Email address is required.';
      return;
    }

    if (!this.appPassword.trim()) {
      this.errorMessage = 'Gmail App Password is required.';
      return;
    }

    this.connecting = true;

    this.emailCostService.connectMailbox({
      emailAddress: this.emailAddress.trim(),
      provider: 'GMAIL',
      appPassword: this.appPassword.replace(/\s/g, ''),
    }).subscribe({
      next: (status) => {
        this.mailboxStatus = status;
        this.appPassword = '';
        this.connecting = false;
        this.successMessage = 'Mailbox connected successfully.';
      },
      error: () => {
        this.connecting = false;
        this.errorMessage = 'Could not connect mailbox. Check the Gmail address and App Password.';
      },
    });
  }

  fetchCosts(): void {
    this.clearMessages();

    if (!this.selectedTravelEntryId) {
      this.errorMessage = 'Please select a trip before fetching costs.';
      return;
    }

    if (!this.isMailboxConnected()) {
      this.errorMessage = 'Please connect your mailbox first.';
      return;
    }

    this.fetching = true;

    this.emailCostService.fetchCosts({
      travelEntryId: this.selectedTravelEntryId,
    }).subscribe({
      next: (costs) => {
        this.emailCosts = costs;
        this.fetching = false;
        this.successMessage = 'New email costs were fetched and added to the selected trip.';
      },
      error: () => {
        this.fetching = false;
        this.errorMessage = 'Could not fetch costs from mailbox. Please check backend logs.';
      },
    });
  }

  refreshFromEmail(): void {
    this.clearMessages();

    if (!this.isMailboxConnected()) {
      this.errorMessage = 'Please connect your mailbox first.';
      return;
    }

    this.refreshingFromEmail = true;

    const request = this.selectedTravelEntryId
      ? { travelEntryId: this.selectedTravelEntryId }
      : {};

    this.emailCostService.fetchCosts(request).subscribe({
      next: (costs) => {
        this.emailCosts = costs;
        this.refreshingFromEmail = false;
        this.successMessage = 'Email costs refreshed successfully.';
      },
      error: () => {
        this.refreshingFromEmail = false;
        this.errorMessage = 'Could not refresh costs from email.';
      },
    });
  }

  removeEmailCostFromTrip(emailCostId: string): void {
    this.clearMessages();
    this.processingEmailCostId = emailCostId;

    this.emailCostService.removeEmailCostFromTrip(emailCostId).subscribe({
      next: () => {
        this.emailCosts = this.emailCosts.filter((cost) => cost.id !== emailCostId);
        this.processingEmailCostId = null;
        this.successMessage = 'Email cost removed from this trip.';
      },
      error: () => {
        this.processingEmailCostId = null;
        this.errorMessage = 'Could not remove email cost.';
      },
    });
  }

  ignoreEmailCostForever(emailCostId: string): void {
    this.clearMessages();
    this.processingEmailCostId = emailCostId;

    this.emailCostService.ignoreEmailCostForever(emailCostId).subscribe({
      next: () => {
        this.emailCosts = this.emailCosts.filter((cost) => cost.id !== emailCostId);
        this.processingEmailCostId = null;
        this.successMessage = 'Email cost ignored permanently.';
      },
      error: () => {
        this.processingEmailCostId = null;
        this.errorMessage = 'Could not ignore email cost.';
      },
    });
  }

  disconnectMailbox(): void {
    this.clearMessages();
    this.disconnecting = true;

    this.emailCostService.disconnectMailbox().subscribe({
      next: (status) => {
        this.mailboxStatus = status;
        this.disconnecting = false;
        this.emailAddress = '';
        this.appPassword = '';
        this.successMessage = 'Mailbox disconnected successfully.';
      },
      error: () => {
        this.disconnecting = false;
        this.errorMessage = 'Could not disconnect mailbox.';
      },
    });
  }

  isMailboxConnected(): boolean {
    return this.mailboxStatus?.connected === true;
  }

  canFetchCosts(): boolean {
    return (
      this.isMailboxConnected() &&
      !!this.selectedTravelEntryId &&
      !this.fetching &&
      !this.refreshingFromEmail &&
      this.processingEmailCostId === null
    );
  }

  canRefreshFromEmail(): boolean {
    return (
      this.isMailboxConnected() &&
      !this.refreshingFromEmail &&
      !this.fetching &&
      !this.loadingCosts &&
      this.processingEmailCostId === null
    );
  }

  canProcessEmailCost(emailCostId: string): boolean {
    return this.processingEmailCostId === null || this.processingEmailCostId === emailCostId;
  }

  getTravelEntryLabel(travelEntry: TravelEntry): string {
    const title = travelEntry.title || 'Untitled trip';
    const destination = travelEntry.destination || 'No destination';
    return `${title} - ${destination}`;
  }

  get selectedTravelEntry(): TravelEntry | undefined {
    return this.travelEntries.find((entry) => entry.id === this.selectedTravelEntryId);
  }

  get totalAmount(): number {
    return this.emailCosts.reduce((sum, cost) => sum + Number(cost.amount || 0), 0);
  }

  get connectedAtLabel(): string {
    return this.mailboxStatus?.connectedAt
      ? new Date(this.mailboxStatus.connectedAt).toLocaleString()
      : 'Not connected yet';
  }

  formatDate(value: string | null | undefined): string {
    return value ? new Date(value).toLocaleDateString() : 'Unknown date';
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
