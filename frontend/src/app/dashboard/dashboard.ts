import { Component, OnInit, inject, AfterViewInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { toObservable } from '@angular/core/rxjs-interop';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { BudgetService } from '../Component/service/budget.service';
import { Budget } from '../models/budget.model';
import { Calendar } from '../services/calendar';
import { CalendarEntry } from '../models/calendar-entry';
import { gsap } from 'gsap';

interface CalendarDay {
  day: number;
  isToday: boolean;
  hasEvent: boolean;
  eventColor?: string;
}

interface MonthData {
  name: string;
  year: number;
  month: number;
  eventCount: number;
  days: CalendarDay[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit, AfterViewInit {
  private travelEntryService = inject(TravelEntryService);
  private budgetService = inject(BudgetService);
  private calendarService = inject(Calendar);
  private router = inject(Router);

  private hasAnimated = false;
  private imageCache = new Map<string, string>();

  protected readonly Math = Math;
  readonly weekdays = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

  trips: TravelEntry[] = [];
  showModal = false;
  newTrip: Partial<TravelEntry> & { budgetAmount?: number } = {};
  budgets: Budget[] = [];
  costsMap: Record<string, { name: string; amount: number }[]> = {};

  constructor() {
    toObservable(this.travelEntryService.refreshTrigger).subscribe(() => {
      this.loadTrips();
      this.loadBudgets();
    });
  }

  private getActiveTrips(): TravelEntry[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.trips.filter(trip => new Date(trip.endDate) >= today);
  }

  private getBudgetForTrip(trip: TravelEntry): Budget | undefined {
    // Match on the travel-entry link (reliable for every budget, including
    // group-trip budgets whose name defaults to the destination rather than the
    // trip title). Fall back to name for any budget without a linked entry.
    return this.budgets.find(b => b.travelEntry?.id === trip.id)
      ?? this.budgets.find(b => b.name === trip.title);
  }

  get totalBudget(): number {
    return this.getActiveTrips().reduce((sum, trip) => {
      const budget = this.getBudgetForTrip(trip);
      return sum + (budget?.amount ?? 0);
    }, 0);
  }

  get totalSpent(): number {
    return this.getActiveTrips().reduce((sum, trip) => {
      const budget = this.getBudgetForTrip(trip);
      if (!budget?.id) return sum;
      const costs = this.costsMap[budget.id] || [];
      return sum + costs.reduce((s, c) => s + c.amount, 0);
    }, 0);
  }

  get upcomingBudgets(): (Budget & { spent: number; remaining: number; percent: number })[] {
    return this.getActiveTrips()
      .slice(0, 3)
      .map(trip => {
        const budget = this.getBudgetForTrip(trip);
        if (!budget) return null;
        const spent = (this.costsMap[budget.id!] || []).reduce((s, c) => s + c.amount, 0);
        return {
          ...budget,
          spent,
          remaining: budget.amount - spent,
          percent: Math.min((spent / budget.amount) * 100, 100),
        };
      })
      .filter((b): b is Budget & { spent: number; remaining: number; percent: number } => b !== null);
  }

  calendarMonths: MonthData[] = this.buildEmptyMonths();

  private buildEmptyMonths(): MonthData[] {
    const now = new Date();
    return Array.from({ length: 3 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
      return {
        name: d.toLocaleString('en', { month: 'long' }),
        year: d.getFullYear(),
        month: d.getMonth(),
        eventCount: 0,
        days: this.buildDays(d.getFullYear(), d.getMonth(), new Map()),
      };
    });
  }

  private buildDays(year: number, month: number, eventDayMap: Map<number, string>): CalendarDay[] {
    const today = new Date();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const rawFirst = new Date(year, month, 1).getDay();
    const firstWeekday = rawFirst === 0 ? 6 : rawFirst - 1;

    const empty: CalendarDay[] = Array(firstWeekday).fill({ day: 0, isToday: false, hasEvent: false });
    const real: CalendarDay[] = Array.from({ length: daysInMonth }, (_, idx) => {
      const d = idx + 1;
      return {
        day: d,
        isToday: today.getFullYear() === year && today.getMonth() === month && today.getDate() === d,
        hasEvent: eventDayMap.has(d),
        eventColor: eventDayMap.get(d),
      };
    });
    return [...empty, ...real];
  }

  ngOnInit() {
    this.loadTrips();
    this.loadBudgets();
  }

  ngAfterViewInit() {
    if (!this.hasAnimated) {
      this.hasAnimated = true;
      this.runEnterAnimations();
    }
    this.initCalendarScrollDots();
  }

  private loadBudgets() {
    this.budgetService.getAll().subscribe({
      next: (data) => {
        this.budgets = data;
        data.forEach(b => { if (b.id) this.costsMap[b.id] = b.costs || []; });
      },
      error: (err: unknown) => console.error(err),
    });
  }

  private loadTrips() {
    this.travelEntryService.getTravelEntries().subscribe({
      next: (trips) => {
        this.trips = trips.sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime());
        this.loadCalendarEvents();
        setTimeout(() => this.initCardHoverEffects(), 50);
      },
      error: (err: unknown) => console.error(err),
    });
  }

  private loadCalendarEvents() {
    this.calendarService.getAllEntries().subscribe({
      next: (events: CalendarEntry[]) => {
        this.calendarMonths = this.calendarMonths.map(month => {
          const dotDayMap = new Map<number, string>();
          let uniqueEventCount = 0;

          const monthStart = new Date(month.year, month.month, 1);
          const monthEnd = new Date(month.year, month.month + 1, 0);

          this.trips.forEach(trip => {
            const start = new Date(trip.startDate);
            const end = new Date(trip.endDate);

            if (start <= monthEnd && end >= monthStart) {
              uniqueEventCount++;
              const tripColor = trip.color ?? '#10b981';
              const rangeStart = start > monthStart ? start : monthStart;
              const rangeEnd = end < monthEnd ? end : monthEnd;
              for (let d = new Date(rangeStart); d <= rangeEnd; d.setDate(d.getDate() + 1)) {
                dotDayMap.set(d.getDate(), tripColor);
              }
            }
          });

          events.forEach(e => {
            const eventStart = e.startDate.substring(0, 10);
            const eventEnd = (e.endDate ?? e.startDate).substring(0, 10);

            const alreadyCoveredByTrip = this.trips.some(trip =>
              trip.startDate.substring(0, 10) <= eventStart &&
              trip.endDate.substring(0, 10) >= eventEnd
            );

            if (!alreadyCoveredByTrip) {
              const start = new Date(e.startDate);
              const end = new Date(e.endDate ?? e.startDate);

              if (start <= monthEnd && end >= monthStart) {
                uniqueEventCount++;
                const color = e.color ?? '#4f98a3';
                const rangeStart = start > monthStart ? start : monthStart;
                const rangeEnd = end < monthEnd ? end : monthEnd;
                for (let d = new Date(rangeStart); d <= rangeEnd; d.setDate(d.getDate() + 1)) {
                  dotDayMap.set(d.getDate(), color);
                }
              }
            }
          });

          return {
            ...month,
            eventCount: uniqueEventCount,
            days: this.buildDays(month.year, month.month, dotDayMap),
          };
        });
      },
      error: (err: unknown) => console.error(err),
    });
  }

  private refreshAll() {
    this.loadBudgets();
    this.loadCalendarEvents();
  }

  private initCalendarScrollDots() {
    const scrollContainer = document.querySelector<HTMLElement>('.calendar-months-scroll');
    const dots = document.querySelectorAll<HTMLElement>('.scroll-dot');

    if (!scrollContainer || dots.length === 0) return;

    dots[0]?.classList.add('active');

    scrollContainer.addEventListener('scroll', () => {
      const cardWidth = scrollContainer.scrollWidth / this.calendarMonths.length;
      const activeIndex = Math.round(scrollContainer.scrollLeft / cardWidth);
      dots.forEach((dot, i) => {
        dot.classList.toggle('active', i === activeIndex);
      });
    }, { passive: true });
  }

  navigateToMonth(month: MonthData) {
    const monthStr = `${month.year}-${String(month.month + 1).padStart(2, '0')}`;
    void this.router.navigate(['/calendar'], { queryParams: { month: monthStr } });
  }

  navigateToTrip(trip: TravelEntry) {
    if (!trip.id) return;
    void this.router.navigate(['/trip', trip.id]);
  }

  deleteTrip(trip: TravelEntry, event: MouseEvent) {
    event.stopPropagation();
    if (!trip.id) return;
    this.travelEntryService.deleteTravelEntry(trip.id).subscribe({
      next: () => {
        gsap.to(`#trip-card-${trip.id}`, {
          opacity: 0, scale: 0.85, x: -20, duration: 0.3, ease: 'power2.in',
          onComplete: () => {
            this.imageCache.delete(trip.destination);
            this.trips = this.trips.filter(t => t.id !== trip.id);
            this.refreshAll();
          },
        });
      },
      error: (err: unknown) => console.error(err),
    });
  }

  private runEnterAnimations() {
    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });
    tl.fromTo('.dashboard-header', { opacity: 0, y: -30 }, { opacity: 1, y: 0, duration: 0.6 })
      .fromTo('.trips-section', { opacity: 0, y: 20 }, { opacity: 1, y: 0, duration: 0.5 }, '-=0.3')
      .fromTo('.overview-card', { opacity: 0, y: 25 }, { opacity: 1, y: 0, duration: 0.45, stagger: 0.12 }, '-=0.1');
  }

  private initCardHoverEffects() {
    document.querySelectorAll<HTMLElement>('.trip-card').forEach(card => {
      const glow = card.querySelector<HTMLElement>('.trip-card-glow');
      card.addEventListener('mouseenter', () => {
        gsap.to(card, { scale: 1.03, duration: 0.25, ease: 'power2.out' });
        if (glow) gsap.to(glow, { opacity: 1, duration: 0.3 });
      });
      card.addEventListener('mouseleave', () => {
        gsap.to(card, { scale: 1, duration: 0.25, ease: 'power2.inOut' });
        if (glow) gsap.to(glow, { opacity: 0, duration: 0.3 });
      });
    });
  }

  openAddTripModal() {
    this.newTrip = { color: '#10b981' };
    this.showModal = true;
    gsap.to('.plus-icon', { rotation: 45, duration: 0.3, ease: 'back.out(1.5)' });
    setTimeout(() => {
      gsap.fromTo('.modal-backdrop', { opacity: 0 }, { opacity: 1, duration: 0.25 });
      gsap.fromTo('.modal-box', { scale: 0.88, opacity: 0, y: 20 }, { scale: 1, opacity: 1, y: 0, duration: 0.4, ease: 'back.out(1.4)' });
    }, 10);
  }

  closeModalBtn() {
    this.animateModalOut(() => {
      this.showModal = false;
      gsap.to('.plus-icon', { rotation: 0, duration: 0.3 });
    });
  }

  closeModal(event: Event) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.closeModalBtn();
    }
  }

  private animateModalOut(callback: () => void) {
    const tl = gsap.timeline({ onComplete: callback });
    tl.to('.modal-box', { scale: 0.9, opacity: 0, y: 15, duration: 0.25, ease: 'power2.in' })
      .to('.modal-backdrop', { opacity: 0, duration: 0.2 }, '-=0.1');
  }

  saveTrip() {
    if (!this.newTrip.title || !this.newTrip.destination || !this.newTrip.startDate || !this.newTrip.endDate) return;
    const payload: Partial<TravelEntry> = {
      title: this.newTrip.title,
      destination: this.newTrip.destination,
      startDate: this.newTrip.startDate,
      endDate: this.newTrip.endDate,
      description: this.newTrip.description,
      color: this.newTrip.color ?? '#10b981',
      ...(this.newTrip.budgetAmount && { budget: { name: this.newTrip.title, amount: this.newTrip.budgetAmount } as Budget }),
    };
    this.travelEntryService.createTravelEntry(payload).subscribe({
      next: () => {
        this.closeModalBtn();
        this.travelEntryService.triggerRefresh();
        setTimeout(() => {
          const cards = document.querySelectorAll('.trip-card');
          const lastCard = cards[cards.length - 1];
          if (lastCard) gsap.fromTo(lastCard,
            { opacity: 0, scale: 0.85, x: 30 },
            { opacity: 1, scale: 1, x: 0, duration: 0.45, ease: 'back.out(1.3)' }
          );
          this.initCardHoverEffects();
        }, 300);
      },
      error: (err: unknown) => console.error(err),
    });
  }

  isTripCurrent(trip: TravelEntry): boolean {
    const today = new Date();
    return new Date(trip.startDate) <= today && new Date(trip.endDate) >= today;
  }

  getTripStatus(trip: TravelEntry): string {
    const today = new Date();
    if (today < new Date(trip.startDate)) return 'Upcoming';
    if (today > new Date(trip.endDate)) return 'Past';
    return 'Active';
  }

  getTripStatusClass(trip: TravelEntry): string {
    switch (this.getTripStatus(trip)) {
      case 'Active':   return 'bg-emerald-500 text-white font-semibold shadow-md';
      case 'Upcoming': return 'bg-blue-500 text-white font-semibold shadow-md';
      case 'Past':     return 'bg-zinc-600 text-white font-semibold shadow-md';
      default:         return '';
    }
  }

  getProgressClass(percent: number): string {
    if (percent > 100) return 'bg-red-500';
    if (percent > 80)  return 'bg-yellow-400';
    return 'bg-emerald-500';
  }

  getDestinationImageUrl(destination: string): string {
    if (!this.imageCache.has(destination)) {
      this.imageCache.set(destination, `http://localhost:8080/api/image?destination=${encodeURIComponent(destination)}`);
    }
    return this.imageCache.get(destination)!;
  }
}
