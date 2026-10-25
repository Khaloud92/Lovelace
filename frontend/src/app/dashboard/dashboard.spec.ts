import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { BudgetService } from '../Component/service/budget.service';
import { DashboardService } from '../services/dashboard.services';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { Budget } from '../models/budget.model';
import { signal } from '@angular/core';

const activeTrip: TravelEntry = {
  id: '1',
  title: 'Japan Trip',
  destination: 'Tokyo',
  startDate: new Date(Date.now() - 86400000).toISOString(),
  endDate: new Date(Date.now() + 86400000).toISOString(),
  description: 'Active trip',
  budget: {
    id: 'b1',
    name: 'Japan Trip',
    amount: 1000,
    costs: [
      { id: 'c1', name: 'Hotel', amount: 400, budget: {} as Budget },
      { id: 'c2', name: 'Flight', amount: 300, budget: {} as Budget },
    ],
  },
};

const upcomingTrip: TravelEntry = {
  id: '2',
  title: 'Paris Trip',
  destination: 'Paris',
  startDate: new Date(Date.now() + 86400000 * 5).toISOString(),
  endDate: new Date(Date.now() + 86400000 * 10).toISOString(),
  description: 'Upcoming trip',
  budget: { id: 'b2', name: 'Paris Trip', amount: 2000, costs: [] },
};

const pastTrip: TravelEntry = {
  id: '3',
  title: 'Rome Trip',
  destination: 'Rome',
  startDate: new Date(Date.now() - 86400000 * 10).toISOString(),
  endDate: new Date(Date.now() - 86400000 * 2).toISOString(),
  description: 'Past trip',
  budget: { id: 'b3', name: 'Rome Trip', amount: 500, costs: [] },
};

const mockTrips = [activeTrip, upcomingTrip, pastTrip];
const mockBudgets: Budget[] = [activeTrip.budget!];

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let travelEntryServiceSpy: jasmine.SpyObj<TravelEntryService>;
  let budgetServiceSpy: jasmine.SpyObj<BudgetService>;

  beforeEach(async () => {
    travelEntryServiceSpy = jasmine.createSpyObj('TravelEntryService', [
      'getTravelEntries',
      'createTravelEntry',
      'deleteTravelEntry',
      'triggerRefresh',
    ]);
    travelEntryServiceSpy.getTravelEntries.and.returnValue(of(mockTrips));
    Object.defineProperty(travelEntryServiceSpy, 'refreshTrigger', {
      value: signal(0),
      writable: true,
    });

    budgetServiceSpy = jasmine.createSpyObj('BudgetService', ['getAll']);
    budgetServiceSpy.getAll.and.returnValue(of(mockBudgets));

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TravelEntryService, useValue: travelEntryServiceSpy },
        { provide: BudgetService, useValue: budgetServiceSpy },
        { provide: DashboardService, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load trips on init', () => {
    expect(travelEntryServiceSpy.getTravelEntries).toHaveBeenCalled();
    expect(component.trips.length).toBe(3);
  });

  it('should load budgets on init', () => {
    expect(budgetServiceSpy.getAll).toHaveBeenCalled();
    expect(component.budgets.length).toBe(1);
  });

  it('should sort trips by startDate ascending', () => {
    const dates = component.trips.map(t => new Date(t.startDate).getTime());
    expect(dates[0]).toBeLessThan(dates[1]);
    expect(dates[1]).toBeLessThan(dates[2]);
  });

  it('should return "Active" for a currently running trip', () => {
    expect(component.getTripStatus(activeTrip)).toBe('Active');
  });

  it('should return "Upcoming" for a future trip', () => {
    expect(component.getTripStatus(upcomingTrip)).toBe('Upcoming');
  });

  it('should return "Past" for a past trip', () => {
    expect(component.getTripStatus(pastTrip)).toBe('Past');
  });

  it('should return emerald class for Active trip', () => {
    expect(component.getTripStatusClass(activeTrip)).toContain('emerald');
  });

  it('should return blue class for Upcoming trip', () => {
    expect(component.getTripStatusClass(upcomingTrip)).toContain('blue');
  });

  it('should return zinc class for Past trip', () => {
    expect(component.getTripStatusClass(pastTrip)).toContain('zinc');
  });

  it('should return emerald class when spending is below 80%', () => {
    expect(component.getProgressClass(50)).toContain('emerald');
  });

  it('should return yellow class when spending is between 80% and 100%', () => {
    expect(component.getProgressClass(85)).toContain('yellow');
  });

  it('should return red class when spending exceeds 100%', () => {
    expect(component.getProgressClass(105)).toContain('red');
  });

  it('should calculate totalBudget only from active trips', () => {
    expect(component.totalBudget).toBe(1000);
  });

  it('should calculate totalSpent from active trips costs', () => {
    expect(component.totalSpent).toBe(700);
  });

  it('should return max 3 upcoming budgets', () => {
    expect(component.upcomingBudgets.length).toBeLessThanOrEqual(3);
  });

  it('should calculate spent, remaining and percent correctly in upcomingBudgets', () => {
    const budget = component.upcomingBudgets.find(b => b.id === 'b1');
    expect(budget).toBeDefined();
    expect(budget!.spent).toBe(700);
    expect(budget!.remaining).toBe(300);
    expect(budget!.percent).toBeCloseTo(70, 0);
  });

  it('should cap percent at 100 in upcomingBudgets', () => {
    component.costsMap['b1'] = [{ name: 'Overbudget', amount: 9999 }];
    const budget = component.upcomingBudgets.find(b => b.id === 'b1');
    expect(budget!.percent).toBe(100);
  });

  it('should reset newTrip when opening modal', () => {
    component.newTrip = { title: 'Old Value' };
    component.openAddTripModal();
    expect(component.newTrip).toEqual({ color: '#10b981' });
  });

  it('should set showModal to true when opening modal', () => {
    component.showModal = false;
    component.openAddTripModal();
    expect(component.showModal).toBeTrue();
  });

  it('should close modal when clicking on backdrop', () => {
    component.showModal = true;
    const backdropEl = document.createElement('div');
    backdropEl.classList.add('modal-backdrop');
    const event = { target: backdropEl } as unknown as Event;
    spyOn(component, 'closeModalBtn');
    component.closeModal(event);
    expect(component.closeModalBtn).toHaveBeenCalled();
  });

  it('should not close modal when clicking inside modal box', () => {
    component.showModal = true;
    const innerEl = document.createElement('div');
    innerEl.classList.add('modal-box');
    const event = { target: innerEl } as unknown as Event;
    spyOn(component, 'closeModalBtn');
    component.closeModal(event);
    expect(component.closeModalBtn).not.toHaveBeenCalled();
  });

  it('should not call createTravelEntry when required fields are missing', () => {
    component.newTrip = { title: 'Only Title' };
    component.saveTrip();
    expect(travelEntryServiceSpy.createTravelEntry).not.toHaveBeenCalled();
  });

  it('should call createTravelEntry when all required fields are set', () => {
    travelEntryServiceSpy.createTravelEntry.and.returnValue(of(activeTrip));
    component.newTrip = {
      title: 'Test',
      destination: 'Berlin',
      startDate: new Date().toISOString(),
      endDate: new Date().toISOString(),
    };
    component.saveTrip();
    expect(travelEntryServiceSpy.createTravelEntry).toHaveBeenCalled();
  });

  it('should call triggerRefresh after successful saveTrip', () => {
    travelEntryServiceSpy.createTravelEntry.and.returnValue(of(activeTrip));
    component.newTrip = {
      title: 'Test',
      destination: 'Berlin',
      startDate: new Date().toISOString(),
      endDate: new Date().toISOString(),
    };
    component.saveTrip();
    expect(travelEntryServiceSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('should return true for a currently active trip', () => {
    expect(component.isTripCurrent(activeTrip)).toBeTrue();
  });

  it('should return false for a future trip', () => {
    expect(component.isTripCurrent(upcomingTrip)).toBeFalse();
  });

  it('should return false for a past trip', () => {
    expect(component.isTripCurrent(pastTrip)).toBeFalse();
  });

  it('should call deleteTravelEntry with correct id', () => {
    travelEntryServiceSpy.deleteTravelEntry.and.returnValue(of(void 0));
    const mockEvent = { stopPropagation: jasmine.createSpy() } as unknown as MouseEvent;
    component.deleteTrip(activeTrip, mockEvent);
    expect(travelEntryServiceSpy.deleteTravelEntry).toHaveBeenCalledWith('1');
  });

  it('should not call deleteTravelEntry if trip has no id', () => {
    const mockEvent = { stopPropagation: jasmine.createSpy() } as unknown as MouseEvent;
    const tripWithoutId = { ...activeTrip, id: undefined };
    component.deleteTrip(tripWithoutId as TravelEntry, mockEvent);
    expect(travelEntryServiceSpy.deleteTravelEntry).not.toHaveBeenCalled();
  });

  it('should return correct image URL for a destination', () => {
    const url = component.getDestinationImageUrl('Tokyo Japan');
    expect(url).toBe('http://localhost:8080/api/image?destination=Tokyo%20Japan');
  });

  it('should encode special characters in destination URL', () => {
    const url = component.getDestinationImageUrl('Paris, France');
    expect(url).toContain(encodeURIComponent('Paris, France'));
  });

  it('should navigate to calendar with correct month param', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    component.navigateToMonth({ name: 'June 2026', year: 2026, month: 5, eventCount: 3, days: [] });
    expect(navigateSpy).toHaveBeenCalledWith(['/calendar'], { queryParams: { month: '2026-06' } });
  });

  it('should pad single-digit months correctly', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    component.navigateToMonth({ name: 'March 2026', year: 2026, month: 2, eventCount: 0, days: [] });
    expect(navigateSpy).toHaveBeenCalledWith(['/calendar'], { queryParams: { month: '2026-03' } });
  });

  it('should initialize 3 calendar months starting from current month', () => {
    const now = new Date();
    expect(component.calendarMonths.length).toBe(3);
    expect(component.calendarMonths[0].month).toBe(now.getMonth());
    expect(component.calendarMonths[0].year).toBe(now.getFullYear());
  });

  it('should navigate to trip detail page when clicking a trip card', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    component.navigateToTrip(activeTrip);
    expect(navigateSpy).toHaveBeenCalledWith(['/trip', activeTrip.id]);
  });

  it('should navigate to trip detail page for upcoming trip', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    component.navigateToTrip(upcomingTrip);
    expect(navigateSpy).toHaveBeenCalledWith(['/trip', upcomingTrip.id]);
  });
});
