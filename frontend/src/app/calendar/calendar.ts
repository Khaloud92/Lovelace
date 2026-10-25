import { Component, OnInit, inject, ChangeDetectorRef, TemplateRef, ViewChild, HostListener, Input, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CalendarModule, CalendarEvent } from 'angular-calendar';
import { Calendar } from '../services/calendar';
import { CalendarEntry } from '../models/calendar-entry';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, CalendarModule, FormsModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.css',
  encapsulation: ViewEncapsulation.None
})
export class CalendarComponent implements OnInit {
  @Input() tripId?: string;

  http = inject(HttpClient);
  private calendarService = inject(Calendar);
  private travelEntryService = inject(TravelEntryService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);

  @ViewChild('weekEventTemplate', { static: true }) weekEventTemplate!: TemplateRef<unknown>;
  @ViewChild('weekHeaderTemplate', { static: true }) weekHeaderTemplate!: TemplateRef<unknown>;

  viewDate: Date = new Date();
  view: 'month' | 'week' | 'day' = 'month';
  events: CalendarEvent[] = [];
  isModalOpen = false;
  selectedColor = '#22d3ee';
  isSaving = false;
  newAppointment: Partial<CalendarEntry> = {};
  isEditMode = false;
  selectedEntryId?: string;
  rawEntries: CalendarEntry[] = [];
  isUploading = false;
  hasTripsThisMonth = true;
  showScrollHint = true;

  isFullTrip = true;

  daysInWeekView = 7;

  get navigationDaysInWeek(): number {
    return this.view === 'week' ? this.daysInWeekView : 7;
  }

  headerImages: { destination: string; country: string; imageUrl: string; accentColor: string }[] = [];
  private imageCache = new Map<string, string>();
  private allTrips: TravelEntry[] = [];

  weekDays = [
    { label: 'Sun', fullLabel: 'Sunday', dayIndex: 0, isWeekend: true },
    { label: 'Mon', fullLabel: 'Monday', dayIndex: 1, isWeekend: false },
    { label: 'Tue', fullLabel: 'Tuesday', dayIndex: 2, isWeekend: false },
    { label: 'Wed', fullLabel: 'Wednesday', dayIndex: 3, isWeekend: false },
    { label: 'Thu', fullLabel: 'Thursday', dayIndex: 4, isWeekend: false },
    { label: 'Fri', fullLabel: 'Friday', dayIndex: 5, isWeekend: false },
    { label: 'Sat', fullLabel: 'Saturday', dayIndex: 6, isWeekend: true }
  ];

  @HostListener('window:resize')
  onResize(): void {
    this.checkScreenSize();
  }

  onHeaderScroll(container: HTMLElement): void {
    this.showScrollHint = container.scrollLeft <= 5;
  }

  private checkScreenSize(): void {
    const width = window.innerWidth;
    if (width < 640) {
      this.daysInWeekView = 1;
    } else if (width < 1024) {
      this.daysInWeekView = 3;
    } else {
      this.daysInWeekView = 7;
    }
    this.cdr.detectChanges();
  }

  ngOnInit(): void {
    this.checkScreenSize();
    this.route.queryParams.subscribe(params => {
      if (params['month']) {
        const [year, month] = params['month'].split('-').map(Number);
        this.viewDate = new Date(year, month - 1, 1);
      }
    });
    this.loadEvents();
    this.loadTrips();
  }

  private loadTrips(): void {
    this.travelEntryService.getTravelEntries().subscribe({
      next: (trips) => {
        this.allTrips = this.tripId
          ? trips.filter(t => String(t.id) === String(this.tripId))
          : trips;
        this.updateHeaderImages();
      }
    });
  }

  private updateHeaderImages(): void {
    const monthStart = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth(), 1);
    const monthEnd = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1, 0);

    const tripsInMonth = this.allTrips.filter(trip => {
      const start = new Date(trip.startDate);
      const end = new Date(trip.endDate);
      return start <= monthEnd && end >= monthStart;
    });

    if (tripsInMonth.length > 0) {
      this.headerImages = tripsInMonth.map(trip => {
        let destination = trip.destination ?? '';
        let country = '';

        if (destination.includes(',')) {
          const parts = destination.split(',');
          destination = parts[0].trim();
          country = parts.slice(1).join(',').trim();
        } else if (destination.includes(' ')) {
          const parts = destination.trim().split(' ');
          destination = parts[0];
          country = parts.slice(1).join(' ');
        }

        return {
          destination,
          country,
          imageUrl: this.getImageUrl(trip.destination),
          accentColor: trip.color ?? '#10b981',
        };
      });
      this.hasTripsThisMonth = true;
    } else {
      this.headerImages = [];
      this.hasTripsThisMonth = false;
    }

    this.showScrollHint = true;
    this.cdr.detectChanges();
  }

  private getImageUrl(destination: string): string {
    const cacheKey = `trip-img-${destination}`;
    if (this.imageCache.has(destination)) return this.imageCache.get(destination)!;
    const stored = localStorage.getItem(cacheKey);
    if (stored) {
      this.imageCache.set(destination, stored);
      return stored;
    }
    const url = `http://localhost:8080/api/image?destination=${encodeURIComponent(destination)}`;
    localStorage.setItem(cacheKey, url);
    this.imageCache.set(destination, url);
    return url;
  }

  onViewDateChange(): void {
    this.showScrollHint = true;
    this.updateHeaderImages();
  }

  loadEvents(): void {
    this.calendarService.getAllEntries().subscribe({
      next: (backendDaten) => {
        this.rawEntries = this.tripId
          ? backendDaten.filter(e => String(e.travelEntryId) === String(this.tripId))
          : backendDaten;

        this.events = this.rawEntries.map(eintrag => ({
          id: eintrag.id,
          title: eintrag.title,
          start: new Date(eintrag.startDate),
          end: eintrag.endDate ? new Date(eintrag.endDate) : undefined,
          color: { primary: eintrag.color ?? '#22d3ee', secondary: '#D1E8FF' },
          meta: { isAutoGenerated: eintrag.isAutoGenerated, travelEntryId: eintrag.travelEntryId }
        }));
        this.cdr.detectChanges();
      }
    });
  }

  openModal(): void {
    this.isEditMode = false;
    this.selectedEntryId = undefined;
    this.isModalOpen = true;
    this.newAppointment = {};
    this.selectedColor = '#22d3ee';
    this.isFullTrip = true;
  }

  saveEntry(): void {
    if (this.isSaving) return;
    if (!this.newAppointment.title || !this.newAppointment.startDate) {
      alert('Please provide at least a title and a start date!');
      return;
    }

    this.isSaving = true;

    if (this.isFullTrip && !this.isEditMode) {
      const startStr = this.newAppointment.startDate.split('T')[0];

      const endStr = this.newAppointment.endDate
        ? this.newAppointment.endDate.split('T')[0]
        : startStr;

      const tripPayload = {
        title: this.newAppointment.title,
        destination: this.newAppointment.location || this.newAppointment.title,
        startDate: startStr,
        endDate: endStr,
        description: this.newAppointment.description || '',
        color: this.selectedColor,
        budget: {
          name: (this.newAppointment.title || 'New') + ' Budget',
          amount: 0
        }
      };

      this.http.post('http://localhost:8080/api/travel-entries', tripPayload).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeModal();
          this.travelEntryService.triggerRefresh();
          setTimeout(() => {
            this.loadTrips();
            this.loadEvents();
          }, 300);
        },
        error: (err) => {
          console.error('Trip creation failed:', err);
          alert('Trip konnte nicht erstellt werden! Fehler vom Server: ' + (err.error?.message || err.message) + '\nChecke die Konsole für mehr Details.');
          this.isSaving = false;
        }
      });
      return;
    }

    const payload: CalendarEntry = {
      id: this.isEditMode ? this.selectedEntryId : undefined,
      title: this.newAppointment.title,
      startDate: new Date(this.newAppointment.startDate).toISOString(),
      endDate: this.newAppointment.endDate ? new Date(this.newAppointment.endDate).toISOString() : undefined,
      location: this.newAppointment.location,
      description: this.newAppointment.description,
      color: this.selectedColor
    };

    if (this.isEditMode && this.selectedEntryId) {
      this.calendarService.updateEntry(this.selectedEntryId, payload).subscribe({
        next: () => {
          const originalEntry = this.rawEntries.find(e => e.id === this.selectedEntryId);

          if (originalEntry?.travelEntryId) {
            this.travelEntryService.updateTravelEntry(originalEntry.travelEntryId, {
              title: this.newAppointment.title,
              destination: originalEntry.location ?? '',
              startDate: new Date(this.newAppointment.startDate!).toISOString().split('T')[0],
              endDate: this.newAppointment.endDate
                ? new Date(this.newAppointment.endDate).toISOString().split('T')[0]
                : undefined,
            }).subscribe({
              next: () => this.travelEntryService.triggerRefresh(),
              error: (err: unknown) => console.error('Trip update failed:', err)
            });
          }

          this.isSaving = false;
          this.closeModal();
          setTimeout(() => this.loadEvents(), 100);
        },
        error: (err: unknown) => {
          this.isSaving = false;
          console.error(err);
        }
      });
    } else {
      this.calendarService.addEntry(payload).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeModal();
          setTimeout(() => this.loadEvents(), 100);
        },
        error: (err: unknown) => {
          this.isSaving = false;
          console.error(err);
        }
      });
    }
  }

  openEditModal(eventId: string | number | undefined): void {
    if (!eventId) return;

    const entryToEdit = this.rawEntries.find(e => e.id === eventId);
    if (!entryToEdit) return;

    if (entryToEdit.isAutoGenerated) {
      alert('Automatically created entries cannot be edited or deleted.');
      return;
    }

    this.isEditMode = true;
    this.selectedEntryId = entryToEdit.id;
    this.newAppointment = { ...entryToEdit };

    if (this.newAppointment.startDate) {
      this.newAppointment.startDate = new Date(entryToEdit.startDate).toISOString().slice(0, 16);
    }

    if (this.newAppointment.endDate && entryToEdit.endDate) {
      this.newAppointment.endDate = new Date(entryToEdit.endDate).toISOString().slice(0, 16);
    }

    this.selectedColor = entryToEdit.color || '#22d3ee';
    this.isModalOpen = true;
  }

  deleteSelectedEntry(): void {
    if (!this.selectedEntryId || this.isSaving) return;

    const selectedEntry = this.rawEntries.find(e => e.id === this.selectedEntryId);
    const isTripEntry = !!selectedEntry?.travelEntryId;

    const confirmMessage = isTripEntry
      ? 'This calendar entry is linked to a trip. Deleting it will also delete the trip and its budget. Are you sure?'
      : 'Are you sure you want to delete this entry?';

    if (confirm(confirmMessage)) {
      this.isSaving = true;
      this.calendarService.deleteEntry(this.selectedEntryId).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeModal();
          if (isTripEntry) this.travelEntryService.triggerRefresh();
          setTimeout(() => this.loadEvents(), 100);
        },
        error: (err: unknown) => {
          this.isSaving = false;
          console.error(err);
        }
      });
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.isSaving = false;
    this.newAppointment = {};
  }

  setView(newView: 'month' | 'week' | 'day'): void {
    this.view = newView;
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (!target.files?.length) return;

    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', target.files[0]);

    this.http.post('http://localhost:8080/api/calendar/upload-ticket', formData).subscribe({
      next: () => {
        this.isUploading = false;
        this.loadEvents();
        alert('Ticket scanned successfully!');
      },
      error: (err) => {
        console.error(err);
        this.isUploading = false;
        alert('Error scanning ticket!');
      }
    });
  }
}
