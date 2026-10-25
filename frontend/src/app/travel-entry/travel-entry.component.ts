import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TravelEntry } from './travel-entry.model';
import { TravelEntryService } from './travel-entry.service';
import { AuthService } from '../authentication/auth.service';
import { Budget } from '../models/budget.model';

interface TravelEntryFormData {
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  description?: string;
  budget: number;
  userId: string;
}

@Component({
  selector: 'app-travel-entry',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './travel-entry.component.html',
  styleUrl: './travel-entry.component.css'
})
export class TravelEntryComponent implements OnInit {
  private readonly travelEntryService = inject(TravelEntryService);
  private readonly authService = inject(AuthService);

  travelEntries: TravelEntry[] = [];
  editingId: string | null = null;
  errorMessage = '';
  successMessage = '';
  isSaving = false;
  form: TravelEntryFormData = this.createEmptyForm();

  constructor() {
    effect(() => {
      this.travelEntryService.refreshTrigger();
      this.loadTravelEntries();
    });
  }

  ngOnInit(): void {
    this.loadTravelEntries();
  }

  /**
   * Loads all travel entries from the backend and updates the local list.
   */
  loadTravelEntries(): void {
    this.errorMessage = '';

    this.travelEntryService.getTravelEntries().subscribe({
      next: (entries) => {
        this.travelEntries = entries;
      },
      error: () => {
        this.errorMessage = 'Backend unreachable or travel entries could not be loaded.';
      }
    });
  }

  /**
   * Saves or updates the current form entry depending on whether
   * an existing entry is being edited.
   */
  saveTravelEntry(): void {
    this.clearMessages();
    this.isSaving = true;

    const request = this.normalizeForm(this.form);

    const saveRequest = this.editingId
      ? this.travelEntryService.updateTravelEntry(this.editingId, request)
      : this.travelEntryService.createTravelEntry(request);

    saveRequest.subscribe({
      next: () => {
        this.successMessage = this.editingId
          ? 'Trip updated successfully.'
          : 'Trip created successfully.';

        this.isSaving = false;
        this.resetForm();
        this.loadTravelEntries();
      },
      error: (error) => {
        this.isSaving = false;
        this.errorMessage = error?.error?.message ?? 'Failed to save.';
      }
    });
  }

  /**
   * Populates the form with the data of an existing travel entry for editing.
   *
   * @param entry the travel entry to edit
   */
  editTravelEntry(entry: TravelEntry): void {
    this.editingId = entry.id ?? null;
    this.form = {
      title: entry.title,
      destination: entry.destination,
      startDate: entry.startDate,
      endDate: entry.endDate,
      description: entry.description,
      budget: entry.budget?.amount ?? 0,
      userId: entry.userId ?? this.authService.getUserId() ?? ''
    };
    this.clearMessages();
  }

  /**
   * Deletes a travel entry and reloads the list upon success.
   *
   * @param entry the travel entry to delete
   */
  deleteTravelEntry(entry: TravelEntry): void {
    if (!entry.id) return;

    this.clearMessages();

    this.travelEntryService.deleteTravelEntry(entry.id).subscribe({
      next: () => {
        this.successMessage = 'Trip deleted successfully.';
        this.loadTravelEntries();

        if (this.editingId === entry.id) {
          this.resetForm();
        }
      },
      error: () => {
        this.errorMessage = 'Failed to delete.';
      }
    });
  }

  /**
   * Resets the form to its initial empty state.
   */
  resetForm(): void {
    this.form = this.createEmptyForm();
    this.editingId = null;
    this.isSaving = false;
  }

  /**
   * Calculates the total planned budget across all travel entries.
   */
  get totalBudget(): number {
    return this.travelEntries.reduce(
      (sum, entry) => sum + (entry.budget?.amount ?? 0),
      0
    );
  }

  private createEmptyForm(): TravelEntryFormData {
    return {
      title: '',
      destination: '',
      startDate: '',
      endDate: '',
      description: '',
      budget: 0,
      userId: this.authService.getUserId() ?? ''
    };
  }

  private normalizeForm(form: TravelEntryFormData): Partial<TravelEntry> {
    const result: Partial<TravelEntry> = {
      title: form.title.trim(),
      destination: form.destination.trim(),
      description: form.description?.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
      userId: form.userId
    };

    if (form.budget && Number(form.budget) > 0) {
      result.budget = {
        amount: Number(form.budget),
        name: 'Budget für ' + form.title.trim()
      } as Budget;
    }

    return result;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
