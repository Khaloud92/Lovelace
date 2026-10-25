import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { gsap } from 'gsap';
import { Subscription } from 'rxjs';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { Budget } from '../models/budget.model';
import { BudgetService } from '../Component/service/budget.service';
import { Cost } from '../models/cost.model';
import { CostService } from '../services/cost.service';
import { Calendar } from '../services/calendar';
import { CalendarEntry } from '../models/calendar-entry';
import { DocumentSer } from '../filemanager/document-ser.service';
import { Document } from '../filemanager/document.model';

interface SightseeingSuggestion {
  name: string;
  amount: number;
}

interface MiniDay {
  date: Date;
  dayNumber: number;
  isToday: boolean;
  events: CalendarEntry[];
}

const COLOR_PRESETS = ['#22d3ee', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

@Component({
  selector: 'app-trip-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, CurrencyPipe, DatePipe, NgClass, FormsModule],
  templateUrl: './trip-detail.component.html',
})
export class TripDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private travelEntryService = inject(TravelEntryService);
  private cdr = inject(ChangeDetectorRef);
  private budgetService = inject(BudgetService);
  private costService = inject(CostService);
  private calendarService = inject(Calendar);
  private documentService = inject(DocumentSer);
  private http = inject(HttpClient);

  @ViewChild('tripPage', { static: false }) tripPage?: ElementRef<HTMLElement>;

  trip: TravelEntry | null = null;
  loading = true;
  error = '';

  private routeSub?: Subscription;
  private hasAnimated = false;
  private animationTimer?: number;

  readonly colorPresets = COLOR_PRESETS;

  // ── BUDGET WIDGET STATE ────────────────────────────────────
  budget: Budget | null = null;
  costs: Cost[] = [];
  budgetLoading = true;
  budgetError = '';

  isEditingAmount = false;
  editAmount: number | null = null;

  showCostForm = false;
  costForm: {
    name: string;
    amount: number | null;
    addToCalendar: boolean;
    eventDate: string;
    eventTime: string;
    eventLocation: string;
    eventColor: string;
  } = {
    name: '', amount: null, addToCalendar: false,
    eventDate: '', eventTime: '', eventLocation: '', eventColor: '#22d3ee'
  };

  aiSuggestions: SightseeingSuggestion[] | null = null;
  loadingSuggestions = false;

  showSuggestionForm: SightseeingSuggestion | null = null;
  suggestionForm: { date: string; time: string; location: string } = { date: '', time: '', location: '' };

  isScanningReceipt = false;

  get spent(): number {
    return this.costs.reduce((sum, c) => sum + c.amount, 0);
  }

  get left(): number {
    return (this.budget?.amount ?? 0) - this.spent;
  }

  // ── CALENDAR WIDGET STATE ──────────────────────────────────
  days: MiniDay[] = [];
  calendarLoading = true;
  selectedDay: MiniDay | null = null;

  showEventForm = false;
  eventForm: {
    title: string;
    date: string;
    time: string;
    location: string;
    color: string;
  } = { title: '', date: '', time: '', location: '', color: '#22d3ee' };

  // ── FILE WIDGET STATE ──────────────────────────────────────
  documents: Document[] = [];
  filesLoading = true;
  fileError = '';
  scanningDocId: string | null = null;
  lastScanResult: { name: string; amount: number }[] | null = null;

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');

      this.loading = true;
      this.error = '';
      this.trip = null;
      this.hasAnimated = false;

      if (this.animationTimer) window.clearTimeout(this.animationTimer);

      if (!id) {
        this.error = 'No trip id found.';
        this.loading = false;
        return;
      }

      this.travelEntryService.getTravelEntries().subscribe({
        next: (trips) => {
          this.trip = trips.find(t => String(t.id) === id) ?? null;
          this.loading = false;
          this.cdr.detectChanges();
          this.scheduleAnimation();

          if (this.trip?.id) {
            this.loadBudget();
            this.loadCalendarDays();
            this.loadDocuments();
          }
        },
        error: () => {
          this.error = 'Could not load trip.';
          this.loading = false;
        },
      });
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    if (this.animationTimer) window.clearTimeout(this.animationTimer);
  }

  private scheduleAnimation(): void {
    if (this.loading || !this.trip || this.hasAnimated) return;

    if (this.animationTimer) window.clearTimeout(this.animationTimer);

    this.animationTimer = window.setTimeout(() => {
      const root = this.tripPage?.nativeElement;
      if (!root) {
        this.scheduleAnimation();
        return;
      }

      const hero = root.querySelector('.trip-hero') as HTMLElement | null;
      const stats = Array.from(root.querySelectorAll('.trip-stat')) as HTMLElement[];
      const widgets = Array.from(root.querySelectorAll('.trip-widget')) as HTMLElement[];

      if (!hero || stats.length === 0 || widgets.length === 0) {
        this.scheduleAnimation();
        return;
      }

      this.hasAnimated = true;

      gsap.set([hero, ...stats, ...widgets], { opacity: 0, y: 18 });

      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .to(hero, { opacity: 1, y: 0, duration: 0.65 })
        .to(stats, { opacity: 1, y: 0, duration: 0.5, stagger: 0.08 }, '-=0.3')
        .to(widgets, { opacity: 1, y: 0, duration: 0.55, stagger: 0.1 }, '-=0.2');
    }, 50);
  }

  goBack(): void {
    void this.router.navigate(['/dashboard']);
  }

  getStatus(): string {
    if (!this.trip) return '';
    const now = new Date();
    const start = new Date(this.trip.startDate);
    const end = new Date(this.trip.endDate);
    if (now < start) return 'Upcoming';
    if (now > end) return 'Past';
    return 'Active';
  }

  getStatusClass(): string {
    switch (this.getStatus()) {
      case 'Active':
        return 'inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] leading-none bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:border-emerald-500/30';
      case 'Upcoming':
        return 'inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] leading-none bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-500/15 dark:text-sky-300 dark:border-sky-500/30';
      case 'Past':
        return 'inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] leading-none bg-zinc-200 text-zinc-700 border-zinc-300 dark:bg-zinc-500/15 dark:text-zinc-300 dark:border-zinc-500/30';
      default:
        return 'inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] leading-none bg-zinc-200 text-zinc-700 border-zinc-300 dark:bg-zinc-500/15 dark:text-zinc-300 dark:border-zinc-500/30';
    }
  }

  getImageUrl(destination: string): string {
    return `http://localhost:8080/api/image?destination=${encodeURIComponent(destination)}`;
  }

  // ══════════════════════════════════════════════════════════
  // BUDGET WIDGET LOGIC
  // ══════════════════════════════════════════════════════════

  loadBudget(): void {
    if (!this.trip?.id) return;
    this.budgetLoading = true;
    this.budgetService.getAll().subscribe({
      next: (all) => {
        this.budget = all.find(b => String(b.travelEntry?.id) === String(this.trip!.id)) ?? null;
        this.costs = this.budget?.costs ?? [];
        this.budgetLoading = false;
      },
      error: () => {
        this.budgetError = 'Budget konnte nicht geladen werden.';
        this.budgetLoading = false;
      }
    });
  }

  startEditAmount(): void {
    this.editAmount = this.budget?.amount ?? 0;
    this.isEditingAmount = true;
  }

  saveAmount(): void {
    if (!this.budget?.id || this.editAmount === null) return;

    const updated: Budget = { ...this.budget, amount: this.editAmount };
    this.budgetService.update(this.budget.id, updated).subscribe({
      next: () => {
        this.isEditingAmount = false;
        this.loadBudget();
      },
      error: () => this.budgetError = 'Aktualisierung fehlgeschlagen.'
    });
  }

  cancelEditAmount(): void {
    this.isEditingAmount = false;
  }

  openCostForm(): void {
    this.showCostForm = true;
    this.costForm = {
      name: '', amount: null, addToCalendar: false,
      eventDate: '', eventTime: '', eventLocation: '', eventColor: '#22d3ee'
    };
  }

  closeCostForm(): void {
    this.showCostForm = false;
  }

  pickCostEventColor(color: string): void {
    this.costForm.eventColor = color;
  }

  saveCost(): void {
    if (!this.budget?.id || !this.costForm.name || !this.costForm.amount) return;

    const newCost = { name: this.costForm.name, amount: this.costForm.amount, budget: this.budget };

    this.costService.addCostToBudget(this.budget.id, newCost).subscribe({
      next: () => {
        if (this.costForm.addToCalendar && this.costForm.eventDate && this.trip?.id) {
          const isoDateTime = this.costForm.eventTime
            ? `${this.costForm.eventDate}T${this.costForm.eventTime}`
            : `${this.costForm.eventDate}T00:00`;

          const entry: CalendarEntry = {
            title: this.costForm.name,
            startDate: new Date(isoDateTime).toISOString(),
            location: this.costForm.eventLocation || undefined,
            color: this.costForm.eventColor,
            travelEntryId: this.trip.id
          };

          this.calendarService.addEntry(entry).subscribe({
            next: () => this.loadCalendarDays(),
            error: (err) => console.error('Kalendereintrag fehlgeschlagen:', err)
          });
        }
        this.closeCostForm();
        this.loadBudget();
      },
      error: (err) => console.error(err)
    });
  }

  deleteCost(costId: string | undefined): void {
    if (!costId) return;
    if (!confirm('Diese Ausgabe löschen?')) return;

    this.costService.deleteCost(costId).subscribe({
      next: () => this.loadBudget(),
      error: (err) => console.error(err)
    });
  }

  fetchSuggestions(): void {
    if (!this.budget?.id) return;
    this.loadingSuggestions = true;

    this.http.get<SightseeingSuggestion[]>(`http://localhost:8080/api/sightseeing/suggestions/${this.budget.id}`).subscribe({
      next: (res) => {
        this.aiSuggestions = res;
        this.loadingSuggestions = false;
      },
      error: () => {
        this.loadingSuggestions = false;
        alert('Fehler beim Laden der KI-Vorschläge.');
      }
    });
  }

  openSuggestionForm(sight: SightseeingSuggestion): void {
    this.showSuggestionForm = sight;
    this.suggestionForm = {
      date: this.trip ? new Date(this.trip.startDate).toISOString().split('T')[0] : '',
      time: '',
      location: ''
    };
  }

  closeSuggestionForm(): void {
    this.showSuggestionForm = null;
  }

  confirmAddSuggestion(): void {
    if (!this.budget?.id || !this.showSuggestionForm) return;
    const sight = this.showSuggestionForm;

    const newCost = { name: sight.name, amount: sight.amount, budget: this.budget };

    this.costService.addCostToBudget(this.budget.id, newCost).subscribe({
      next: () => {
        if (this.suggestionForm.date && this.trip?.id) {
          const isoDateTime = this.suggestionForm.time
            ? `${this.suggestionForm.date}T${this.suggestionForm.time}`
            : `${this.suggestionForm.date}T00:00`;

          const entry: CalendarEntry = {
            title: sight.name,
            startDate: new Date(isoDateTime).toISOString(),
            location: this.suggestionForm.location || undefined,
            color: '#8b5cf6',
            travelEntryId: this.trip.id
          };

          this.calendarService.addEntry(entry).subscribe({
            next: () => this.loadCalendarDays(),
            error: (err) => console.error('Kalendereintrag fehlgeschlagen:', err)
          });
        }

        this.aiSuggestions = this.aiSuggestions?.filter(s => s.name !== sight.name) ?? null;
        this.closeSuggestionForm();
        this.loadBudget();
      },
      error: (err) => console.error(err)
    });
  }

  triggerReceiptScan(input: HTMLInputElement): void {
    input.click();
  }

  onReceiptSelected(event: Event): void {
    if (!this.budget?.id) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.isScanningReceipt = true;
    this.costService.scanCostDocument(this.budget.id, file).subscribe({
      next: () => {
        this.isScanningReceipt = false;
        input.value = '';
        this.loadBudget();
        this.loadDocuments();
      },
      error: () => {
        this.isScanningReceipt = false;
        input.value = '';
        alert('Keine Kosten im Beleg gefunden.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════
  // CALENDAR WIDGET LOGIC
  // ══════════════════════════════════════════════════════════

  loadCalendarDays(): void {
    if (!this.trip?.id) return;
    this.calendarLoading = true;

    this.calendarService.getAllEntries().subscribe({
      next: (entries) => {
        this.buildDays(entries);
        this.calendarLoading = false;
      },
      error: () => { this.calendarLoading = false; }
    });
  }

  private buildDays(entries: CalendarEntry[]): void {
    if (!this.trip) return;

    const start = new Date(this.trip.startDate);
    start.setHours(0, 0, 0, 0);
    const end = new Date(this.trip.endDate);
    end.setHours(23, 59, 59, 999);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const tripTitle = this.trip.title?.trim().toLowerCase();
    const tripStartDay = start.toDateString();

    const relevantEntries = entries.filter(e => {
      if (String(e.travelEntryId) !== String(this.trip!.id)) return false;
      if (e.isAutoGenerated) return false;

      const isAutoTripEntry = !!tripTitle
        && e.title?.trim().toLowerCase() === tripTitle
        && new Date(e.startDate).toDateString() === tripStartDay;

      return !isAutoTripEntry;
    });

    const days: MiniDay[] = [];
    const cursor = new Date(start);

    while (cursor <= end) {
      const dayEvents = relevantEntries.filter(e => {
        const eventDate = new Date(e.startDate);
        return eventDate.toDateString() === cursor.toDateString();
      });

      days.push({
        date: new Date(cursor),
        dayNumber: cursor.getDate(),
        isToday: cursor.toDateString() === today.toDateString(),
        events: dayEvents
      });

      cursor.setDate(cursor.getDate() + 1);
    }

    this.days = days;

    if (this.selectedDay) {
      this.selectedDay = days.find(d => d.date.toDateString() === this.selectedDay!.date.toDateString()) ?? null;
    }
  }

  selectDay(day: MiniDay): void {
    this.selectedDay = this.selectedDay?.date.toDateString() === day.date.toDateString() ? null : day;
  }

  getEventTime(entry: CalendarEntry): string {
    const date = new Date(entry.startDate);
    return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
  }

  openEventForm(): void {
    this.showEventForm = true;
    const defaultDate = this.selectedDay
      ? this.selectedDay.date.toISOString().split('T')[0]
      : (this.trip ? new Date(this.trip.startDate).toISOString().split('T')[0] : '');

    this.eventForm = { title: '', date: defaultDate, time: '', location: '', color: '#22d3ee' };
  }

  closeEventForm(): void {
    this.showEventForm = false;
  }

  pickEventColor(color: string): void {
    this.eventForm.color = color;
  }

  saveEvent(): void {
    if (!this.trip?.id || !this.eventForm.title || !this.eventForm.date) return;

    const isoDateTime = this.eventForm.time
      ? `${this.eventForm.date}T${this.eventForm.time}`
      : `${this.eventForm.date}T00:00`;

    const entry: CalendarEntry = {
      title: this.eventForm.title,
      startDate: new Date(isoDateTime).toISOString(),
      location: this.eventForm.location || undefined,
      color: this.eventForm.color,
      travelEntryId: this.trip.id
    };

    this.calendarService.addEntry(entry).subscribe({
      next: () => {
        this.closeEventForm();
        this.loadCalendarDays();
      },
      error: (err) => console.error('Kalendereintrag fehlgeschlagen:', err)
    });
  }

  deleteCalendarEvent(eventId: string | number | undefined): void {
    if (!eventId) return;
    if (!confirm('Diesen Termin löschen?')) return;

    this.calendarService.deleteEntry(String(eventId)).subscribe({
      next: () => this.loadCalendarDays(),
      error: (err) => {
        console.error('Löschen fehlgeschlagen:', err);
        alert('Termin konnte nicht gelöscht werden.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════
  // FILE WIDGET LOGIC
  // ══════════════════════════════════════════════════════════

  loadDocuments(): void {
    if (!this.trip?.id) return;
    this.filesLoading = true;

    this.documentService.getDocuments(this.trip.id).subscribe({
      next: (docs) => { this.documents = docs; this.filesLoading = false; },
      error: () => { this.fileError = 'Dateien konnten nicht geladen werden.'; this.filesLoading = false; }
    });
  }

  onFileSelected(event: Event): void {
    if (!this.trip?.id) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.documentService.uploadDocument(this.trip.id, file).subscribe({
      next: () => {
        input.value = '';
        this.loadDocuments();
      },
      error: () => { this.fileError = 'Upload fehlgeschlagen.'; }
    });
  }

  extractCosts(doc: Document): void {
    if (!this.budget?.id) {
      this.fileError = 'Kein Budget für diesen Trip vorhanden.';
      return;
    }

    this.scanningDocId = doc.id;
    this.fileError = '';

    this.documentService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const file = new File([blob], doc.name, { type: blob.type || 'application/octet-stream' });

        this.costService.scanCostDocument(this.budget!.id, file).subscribe({
          next: (costs) => {
            this.lastScanResult = costs.map(c => ({ name: c.name, amount: c.amount }));
            this.scanningDocId = null;
            this.loadBudget();
          },
          error: () => {
            this.fileError = 'Keine Kosten im Beleg gefunden.';
            this.scanningDocId = null;
          }
        });
      },
      error: () => {
        this.fileError = 'Datei konnte nicht geladen werden.';
        this.scanningDocId = null;
      }
    });
  }

  openDocument(doc: Document): void {
    this.documentService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.target = '_blank';
        link.rel = 'noopener';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: () => { this.fileError = 'Download fehlgeschlagen.'; }
    });
  }

  deleteDocument(docId: string): void {
    if (!confirm('Datei wirklich löschen?')) return;
    this.documentService.deleteDocument(docId).subscribe({
      next: () => { this.documents = this.documents.filter(d => d.id !== docId); },
      error: () => { this.fileError = 'Löschen fehlgeschlagen.'; }
    });
  }
}
