import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError, Subscription } from 'rxjs';
import { gsap } from 'gsap';
import { HttpClient } from '@angular/common/http';
import { TripDetailComponent } from './trip-detail.component';
import { TravelEntryService } from '../travel-entry/travel-entry.service';
import { TravelEntry } from '../travel-entry/travel-entry.model';
import { DocumentSer } from '../filemanager/document-ser.service';
import { Calendar } from '../services/calendar';
import { BudgetService } from '../Component/service/budget.service';
import { CostService } from '../services/cost.service';
import { CalendarEntry } from '../models/calendar-entry';
import { Document } from '../filemanager/document.model';
import { Budget } from '../models/budget.model';
import { Cost } from '../models/cost.model';

interface TripDetailPrivateAccess {
  routeSub?: Subscription;
  animationTimer?: number;
  scheduleAnimation: () => void;
}

function asPrivate(component: TripDetailComponent): TripDetailPrivateAccess {
  return component as unknown as TripDetailPrivateAccess;
}

const activeTrip: TravelEntry = {
  id: '1',
  title: 'Japan Trip',
  destination: 'Tokyo',
  startDate: new Date(Date.now() - 86400000).toISOString(),
  endDate: new Date(Date.now() + 86400000).toISOString(),
  description: 'Active trip',
  color: '#10b981',
  budget: { id: 'b1', name: 'Japan Trip', amount: 1000, costs: [] },
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

function makeBudget(overrides: Partial<Budget> = {}): Budget {
  return {
    id: 'b1',
    name: 'Test',
    amount: 500,
    costs: [],
    travelEntry: { id: '1' } as TravelEntry,
    ...overrides,
  } as Budget;
}

function makeCost(overrides: Partial<Cost> = {}): Cost {
  return { id: 'c1', name: 'Cost', amount: 0, ...overrides } as Cost;
}

function makeDocument(overrides: Partial<Document> = {}): Document {
  return { id: 'd1', name: 'receipt.pdf', ...overrides } as Document;
}

function makeCalendarEntry(overrides: Partial<CalendarEntry> = {}): CalendarEntry {
  return { title: 'Event', startDate: new Date().toISOString(), ...overrides } as CalendarEntry;
}

describe('TripDetailComponent', () => {
  let component: TripDetailComponent;
  let fixture: ComponentFixture<TripDetailComponent>;
  let travelEntryServiceSpy: jasmine.SpyObj<TravelEntryService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let documentServiceSpy: jasmine.SpyObj<DocumentSer>;
  let calendarServiceSpy: jasmine.SpyObj<Calendar>;
  let budgetServiceSpy: jasmine.SpyObj<BudgetService>;
  let costServiceSpy: jasmine.SpyObj<CostService>;
  let httpClientSpy: jasmine.SpyObj<HttpClient>;
  let paramMapSubject: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  function setup(initialId: string | null = '1') {
    paramMapSubject = new BehaviorSubject(convertToParamMap(initialId ? { id: initialId } : {}));

    travelEntryServiceSpy = jasmine.createSpyObj('TravelEntryService', ['getTravelEntries']);
    travelEntryServiceSpy.getTravelEntries.and.returnValue(of(mockTrips));

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    documentServiceSpy = jasmine.createSpyObj('DocumentSer', [
      'getDocuments',
      'uploadDocument',
      'downloadDocument',
      'deleteDocument',
    ]);
    documentServiceSpy.getDocuments.and.returnValue(of([]));

    calendarServiceSpy = jasmine.createSpyObj('Calendar', ['getAllEntries', 'addEntry', 'deleteEntry']);
    calendarServiceSpy.getAllEntries.and.returnValue(of([]));

    budgetServiceSpy = jasmine.createSpyObj('BudgetService', ['getAll', 'update']);
    budgetServiceSpy.getAll.and.returnValue(of([]));

    costServiceSpy = jasmine.createSpyObj('CostService', [
      'addCostToBudget',
      'deleteCost',
      'scanCostDocument',
    ]);

    httpClientSpy = jasmine.createSpyObj('HttpClient', ['get']);

    TestBed.configureTestingModule({
      imports: [TripDetailComponent],
      providers: [
        { provide: TravelEntryService, useValue: travelEntryServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { paramMap: paramMapSubject.asObservable() } },
        { provide: DocumentSer, useValue: documentServiceSpy },
        { provide: Calendar, useValue: calendarServiceSpy },
        { provide: BudgetService, useValue: budgetServiceSpy },
        { provide: CostService, useValue: costServiceSpy },
        { provide: HttpClient, useValue: httpClientSpy },
      ],
    });

    fixture = TestBed.createComponent(TripDetailComponent);
    component = fixture.componentInstance;
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  describe('basic creation', () => {
    beforeEach(() => setup('1'));

    it('should create', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
    });
  });

  describe('ngOnInit - loading trip data', () => {
    it('should load the trip matching the route id', () => {
      setup('1');
      fixture.detectChanges();
      expect(travelEntryServiceSpy.getTravelEntries).toHaveBeenCalled();
      expect(component.trip).toEqual(activeTrip);
      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
    });

    it('should set trip to null when no trip matches the id', () => {
      setup('999');
      fixture.detectChanges();
      expect(component.trip).toBeNull();
      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
    });

    it('should set an error when no id param is present', () => {
      setup(null);
      fixture.detectChanges();
      expect(component.error).toBe('No trip id found.');
      expect(component.loading).toBeFalse();
      expect(component.trip).toBeNull();
      expect(travelEntryServiceSpy.getTravelEntries).not.toHaveBeenCalled();
    });

    it('should set an error when getTravelEntries fails', () => {
      setup('1');
      travelEntryServiceSpy.getTravelEntries.and.returnValue(throwError(() => new Error('network error')));
      fixture.detectChanges();
      expect(component.error).toBe('Could not load trip.');
      expect(component.loading).toBeFalse();
    });

    it('should set loading to true while fetching and reset state on new param emission', () => {
      setup('1');
      fixture.detectChanges();
      expect(component.trip).toEqual(activeTrip);

      paramMapSubject.next(convertToParamMap({ id: '2' }));
      expect(component.trip).toEqual(upcomingTrip);
      expect(component.loading).toBeFalse();
    });

    it('should match trip id as string even if types differ', () => {
      setup('2');
      fixture.detectChanges();
      expect(component.trip?.id).toBe('2');
    });

    it('should trigger loadBudget, loadCalendarDays and loadDocuments when a trip is found', () => {
      setup('1');
      spyOn(component, 'loadBudget');
      spyOn(component, 'loadCalendarDays');
      spyOn(component, 'loadDocuments');
      fixture.detectChanges();
      expect(component.loadBudget).toHaveBeenCalled();
      expect(component.loadCalendarDays).toHaveBeenCalled();
      expect(component.loadDocuments).toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe from the route paramMap subscription', () => {
      setup('1');
      fixture.detectChanges();
      const privateAccess = asPrivate(component);
      const unsubscribeSpy = spyOn(privateAccess.routeSub as Subscription, 'unsubscribe').and.callThrough();
      component.ngOnDestroy();
      expect(unsubscribeSpy).toHaveBeenCalled();
    });

    it('should clear a pending animation timer if set', () => {
      setup('1');
      fixture.detectChanges();
      spyOn(window, 'clearTimeout');
      asPrivate(component).animationTimer = 123;
      component.ngOnDestroy();
      expect(window.clearTimeout).toHaveBeenCalledWith(123);
    });
  });

  describe('goBack', () => {
    it('should navigate to /dashboard', () => {
      setup('1');
      fixture.detectChanges();
      component.goBack();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  describe('getStatus', () => {
    it('should return empty string when there is no trip', () => {
      setup('999');
      fixture.detectChanges();
      expect(component.getStatus()).toBe('');
    });

    it('should return "Active" for a currently running trip', () => {
      setup('1');
      fixture.detectChanges();
      expect(component.getStatus()).toBe('Active');
    });

    it('should return "Upcoming" for a future trip', () => {
      setup('2');
      fixture.detectChanges();
      expect(component.getStatus()).toBe('Upcoming');
    });

    it('should return "Past" for a past trip', () => {
      setup('3');
      fixture.detectChanges();
      expect(component.getStatus()).toBe('Past');
    });
  });

  describe('getStatusClass', () => {
    it('should return emerald classes for Active status', () => {
      setup('1');
      fixture.detectChanges();
      expect(component.getStatusClass()).toContain('emerald');
    });

    it('should return sky classes for Upcoming status', () => {
      setup('2');
      fixture.detectChanges();
      expect(component.getStatusClass()).toContain('sky');
    });

    it('should return zinc classes for Past status', () => {
      setup('3');
      fixture.detectChanges();
      expect(component.getStatusClass()).toContain('zinc');
    });

    it('should return default zinc classes when there is no trip', () => {
      setup('999');
      fixture.detectChanges();
      expect(component.getStatusClass()).toContain('zinc');
    });
  });

  describe('getImageUrl', () => {
    it('should build the correct image URL for a destination', () => {
      setup('1');
      fixture.detectChanges();
      expect(component.getImageUrl('Tokyo')).toBe('http://localhost:8080/api/image?destination=Tokyo');
    });

    it('should encode special characters in the destination', () => {
      setup('1');
      fixture.detectChanges();
      expect(component.getImageUrl('Paris, France')).toContain(encodeURIComponent('Paris, France'));
    });
  });

  describe('animation scheduling', () => {
    it('should call gsap.set and gsap.timeline once elements are rendered', fakeAsync(() => {
      setup('1');
      const setSpy = spyOn(gsap, 'set').and.callThrough();
      const timelineSpy = spyOn(gsap, 'timeline').and.callThrough();

      fixture.detectChanges();
      tick(200);
      fixture.detectChanges();

      expect(setSpy).toHaveBeenCalled();
      expect(timelineSpy).toHaveBeenCalled();
    }));

    it('should not schedule an animation while loading', () => {
      setup('1');
      travelEntryServiceSpy.getTravelEntries.and.returnValue(of(mockTrips));
      component.loading = true;
      const setSpy = spyOn(gsap, 'set');
      asPrivate(component).scheduleAnimation();
      expect(setSpy).not.toHaveBeenCalled();
    });

    it('should not schedule an animation when there is no trip', () => {
      setup('999');
      fixture.detectChanges();
      const setSpy = spyOn(gsap, 'set');
      asPrivate(component).scheduleAnimation();
      expect(setSpy).not.toHaveBeenCalled();
    });

    it('should not re-run the animation if it has already run', fakeAsync(() => {
      setup('1');
      fixture.detectChanges();
      tick(200);
      fixture.detectChanges();

      const setSpy = spyOn(gsap, 'set');
      asPrivate(component).scheduleAnimation();
      tick(200);
      expect(setSpy).not.toHaveBeenCalled();
    }));
  });

  // ══════════════════════════════════════════════════════════
  // BUDGET WIDGET
  // ══════════════════════════════════════════════════════════
  describe('budget widget', () => {
    beforeEach(() => {
      setup('1');
      fixture.detectChanges();
    });

    it('should load the matching budget and its costs', () => {
      const budget = makeBudget({ costs: [makeCost({ name: 'Hotel', amount: 200 })] });
      budgetServiceSpy.getAll.and.returnValue(of([budget]));
      component.loadBudget();
      expect(component.budget).toEqual(budget);
      expect(component.costs.length).toBe(1);
      expect(component.budgetLoading).toBeFalse();
    });

    it('should set budgetError when loading fails', () => {
      budgetServiceSpy.getAll.and.returnValue(throwError(() => new Error('fail')));
      component.loadBudget();
      expect(component.budgetError).toBe('Budget konnte nicht geladen werden.');
      expect(component.budgetLoading).toBeFalse();
    });

    it('should compute spent and left correctly', () => {
      component.budget = makeBudget({ amount: 1000 });
      component.costs = [makeCost({ name: 'A', amount: 100 }), makeCost({ name: 'B', amount: 50 })];
      expect(component.spent).toBe(150);
      expect(component.left).toBe(850);
    });

    it('should enter and cancel edit-amount mode', () => {
      component.budget = makeBudget({ amount: 500 });
      component.startEditAmount();
      expect(component.isEditingAmount).toBeTrue();
      expect(component.editAmount).toBe(500);

      component.cancelEditAmount();
      expect(component.isEditingAmount).toBeFalse();
    });

    it('should save the edited amount and reload the budget', () => {
      component.budget = makeBudget({ amount: 500 });
      component.editAmount = 750;
      budgetServiceSpy.update.and.returnValue(of(makeBudget({ amount: 750 })));
      spyOn(component, 'loadBudget');

      component.saveAmount();

      expect(budgetServiceSpy.update).toHaveBeenCalledWith('b1', jasmine.objectContaining({ amount: 750 }));
      expect(component.isEditingAmount).toBeFalse();
      expect(component.loadBudget).toHaveBeenCalled();
    });

    it('should set budgetError when saving the amount fails', () => {
      component.budget = makeBudget({ amount: 500 });
      component.editAmount = 750;
      budgetServiceSpy.update.and.returnValue(throwError(() => new Error('fail')));

      component.saveAmount();

      expect(component.budgetError).toBe('Aktualisierung fehlgeschlagen.');
    });

    it('should reset the cost form when opened and hide it when closed', () => {
      component.openCostForm();
      expect(component.showCostForm).toBeTrue();
      expect(component.costForm.name).toBe('');

      component.closeCostForm();
      expect(component.showCostForm).toBeFalse();
    });

    it('should save a cost and reload the budget without creating a calendar entry', () => {
      component.budget = makeBudget();
      component.costForm = { name: 'Taxi', amount: 20, addToCalendar: false, eventDate: '', eventTime: '', eventLocation: '', eventColor: '#22d3ee' };
      costServiceSpy.addCostToBudget.and.returnValue(of(makeCost({ name: 'Taxi', amount: 20 })));
      spyOn(component, 'loadBudget');

      component.saveCost();

      expect(costServiceSpy.addCostToBudget).toHaveBeenCalledWith('b1', jasmine.objectContaining({ name: 'Taxi', amount: 20 }));
      expect(calendarServiceSpy.addEntry).not.toHaveBeenCalled();
      expect(component.showCostForm).toBeFalse();
      expect(component.loadBudget).toHaveBeenCalled();
    });

    it('should save a cost and create a linked calendar entry when addToCalendar is set', () => {
      component.budget = makeBudget();
      component.costForm = { name: 'Museum', amount: 30, addToCalendar: true, eventDate: '2026-08-01', eventTime: '10:00', eventLocation: 'Tokyo', eventColor: '#f59e0b' };
      costServiceSpy.addCostToBudget.and.returnValue(of(makeCost({ name: 'Museum', amount: 30 })));
      calendarServiceSpy.addEntry.and.returnValue(of(makeCalendarEntry({ title: 'Museum' })));
      spyOn(component, 'loadBudget');
      spyOn(component, 'loadCalendarDays');

      component.saveCost();

      expect(calendarServiceSpy.addEntry).toHaveBeenCalledWith(jasmine.objectContaining({ title: 'Museum', location: 'Tokyo', color: '#f59e0b', travelEntryId: '1' }));
      expect(component.loadCalendarDays).toHaveBeenCalled();
    });

    it('should not save a cost when required fields are missing', () => {
      component.budget = makeBudget();
      component.costForm = { name: '', amount: null, addToCalendar: false, eventDate: '', eventTime: '', eventLocation: '', eventColor: '#22d3ee' };

      component.saveCost();

      expect(costServiceSpy.addCostToBudget).not.toHaveBeenCalled();
    });

    it('should delete a cost after confirmation and reload the budget', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      costServiceSpy.deleteCost.and.returnValue(of(undefined));
      spyOn(component, 'loadBudget');

      component.deleteCost('c1');

      expect(costServiceSpy.deleteCost).toHaveBeenCalledWith('c1');
      expect(component.loadBudget).toHaveBeenCalled();
    });

    it('should not delete a cost when confirmation is declined', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.deleteCost('c1');

      expect(costServiceSpy.deleteCost).not.toHaveBeenCalled();
    });

    it('should fetch AI sightseeing suggestions', () => {
      component.budget = makeBudget();
      const suggestions = [{ name: 'Shibuya Crossing', amount: 0 }];
      httpClientSpy.get.and.returnValue(of(suggestions));

      component.fetchSuggestions();

      expect(httpClientSpy.get).toHaveBeenCalledWith('http://localhost:8080/api/sightseeing/suggestions/b1');
      expect(component.aiSuggestions).toEqual(suggestions);
      expect(component.loadingSuggestions).toBeFalse();
    });

    it('should alert when fetching suggestions fails', () => {
      component.budget = makeBudget();
      httpClientSpy.get.and.returnValue(throwError(() => new Error('fail')));
      spyOn(window, 'alert');

      component.fetchSuggestions();

      expect(window.alert).toHaveBeenCalledWith('Fehler beim Laden der KI-Vorschläge.');
      expect(component.loadingSuggestions).toBeFalse();
    });

    it('should confirm a suggestion, add it as a cost and remove it from the list', () => {
      component.budget = makeBudget();
      component.aiSuggestions = [{ name: 'Shibuya Crossing', amount: 0 }];
      component.showSuggestionForm = { name: 'Shibuya Crossing', amount: 0 };
      component.suggestionForm = { date: '2026-08-02', time: '', location: '' };
      costServiceSpy.addCostToBudget.and.returnValue(of(makeCost({ name: 'Shibuya Crossing', amount: 0 })));
      calendarServiceSpy.addEntry.and.returnValue(of(makeCalendarEntry({ title: 'Shibuya Crossing' })));
      spyOn(component, 'loadBudget');
      spyOn(component, 'loadCalendarDays');

      component.confirmAddSuggestion();

      expect(costServiceSpy.addCostToBudget).toHaveBeenCalled();
      expect(component.aiSuggestions?.length).toBe(0);
      expect(component.showSuggestionForm).toBeNull();
    });
  });

  // ══════════════════════════════════════════════════════════
  // CALENDAR WIDGET
  // ══════════════════════════════════════════════════════════
  describe('calendar widget', () => {
    beforeEach(() => {
      setup('1');
      fixture.detectChanges();
    });

    it('should build days between trip start and end excluding the auto-generated trip entry', () => {
      const tripStart = new Date(activeTrip.startDate);
      const manualEntry = makeCalendarEntry({
        title: 'Dinner',
        startDate: tripStart.toISOString(),
        travelEntryId: '1',
        isAutoGenerated: false,
      });
      const autoEntry = makeCalendarEntry({
        title: activeTrip.title,
        startDate: tripStart.toISOString(),
        travelEntryId: '1',
        isAutoGenerated: true,
      });

      calendarServiceSpy.getAllEntries.and.returnValue(of([manualEntry, autoEntry]));

      component.loadCalendarDays();

      const dayWithEvent = component.days.find(d => d.events.length > 0);
      expect(dayWithEvent).toBeTruthy();
      expect(dayWithEvent?.events.some(e => e.title === 'Dinner')).toBeTrue();
      expect(dayWithEvent?.events.some(e => e.isAutoGenerated)).toBeFalse();
      expect(component.calendarLoading).toBeFalse();
    });

    it('should toggle the selected day', () => {
      component.days = [{ date: new Date(), dayNumber: 1, isToday: true, events: [] }];
      component.selectDay(component.days[0]);
      expect(component.selectedDay).toBe(component.days[0]);

      component.selectDay(component.days[0]);
      expect(component.selectedDay).toBeNull();
    });

    it('should reset and open the event form with a default date', () => {
      component.openEventForm();
      expect(component.showEventForm).toBeTrue();
      expect(component.eventForm.title).toBe('');
      expect(component.eventForm.date).toBeTruthy();

      component.closeEventForm();
      expect(component.showEventForm).toBeFalse();
    });

    it('should save a new calendar event and reload the days', () => {
      component.eventForm = { title: 'City Tour', date: '2026-08-03', time: '09:00', location: 'Tokyo', color: '#22d3ee' };
      calendarServiceSpy.addEntry.and.returnValue(of(makeCalendarEntry({ title: 'City Tour' })));
      spyOn(component, 'loadCalendarDays');

      component.saveEvent();

      expect(calendarServiceSpy.addEntry).toHaveBeenCalledWith(jasmine.objectContaining({ title: 'City Tour', location: 'Tokyo', travelEntryId: '1' }));
      expect(component.showEventForm).toBeFalse();
      expect(component.loadCalendarDays).toHaveBeenCalled();
    });

    it('should not save an event when title or date is missing', () => {
      component.eventForm = { title: '', date: '', time: '', location: '', color: '#22d3ee' };
      component.saveEvent();
      expect(calendarServiceSpy.addEntry).not.toHaveBeenCalled();
    });

    it('should delete a calendar event after confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      calendarServiceSpy.deleteEntry.and.returnValue(of(undefined));
      spyOn(component, 'loadCalendarDays');

      component.deleteCalendarEvent('e1');

      expect(calendarServiceSpy.deleteEntry).toHaveBeenCalledWith('e1');
      expect(component.loadCalendarDays).toHaveBeenCalled();
    });

    it('should alert when deleting a calendar event fails', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      calendarServiceSpy.deleteEntry.and.returnValue(throwError(() => new Error('fail')));
      spyOn(window, 'alert');

      component.deleteCalendarEvent('e1');

      expect(window.alert).toHaveBeenCalledWith('Termin konnte nicht gelöscht werden.');
    });
  });

  // ══════════════════════════════════════════════════════════
  // FILE WIDGET
  // ══════════════════════════════════════════════════════════
  describe('file widget', () => {
    beforeEach(() => {
      setup('1');
      fixture.detectChanges();
    });

    it('should load documents for the trip', () => {
      const docs: Document[] = [makeDocument()];
      documentServiceSpy.getDocuments.and.returnValue(of(docs));

      component.loadDocuments();

      expect(component.documents).toEqual(docs);
      expect(component.filesLoading).toBeFalse();
    });

    it('should set fileError when loading documents fails', () => {
      documentServiceSpy.getDocuments.and.returnValue(throwError(() => new Error('fail')));

      component.loadDocuments();

      expect(component.fileError).toBe('Dateien konnten nicht geladen werden.');
      expect(component.filesLoading).toBeFalse();
    });

    it('should upload a selected file and reload documents', () => {
      const file = new File(['content'], 'ticket.pdf');
      const input = document.createElement('input');
      input.type = 'file';
      Object.defineProperty(input, 'files', { value: [file] });
      documentServiceSpy.uploadDocument.and.returnValue(of(makeDocument({ name: 'ticket.pdf' })));
      spyOn(component, 'loadDocuments');

      component.onFileSelected({ target: input } as unknown as Event);

      expect(documentServiceSpy.uploadDocument).toHaveBeenCalledWith('1', file);
      expect(component.loadDocuments).toHaveBeenCalled();
    });

    it('should set fileError when extracting costs without a budget', () => {
      component.budget = null;
      const doc = makeDocument();

      component.extractCosts(doc);

      expect(component.fileError).toBe('Kein Budget für diesen Trip vorhanden.');
    });

    it('should extract costs from a document when a budget exists', () => {
      component.budget = makeBudget();
      const doc = makeDocument();
      const blob = new Blob(['data'], { type: 'application/pdf' });
      documentServiceSpy.downloadDocument.and.returnValue(of(blob));
      costServiceSpy.scanCostDocument.and.returnValue(of([makeCost({ name: 'Hotel', amount: 100 })]));
      spyOn(component, 'loadBudget');

      component.extractCosts(doc);

      expect(costServiceSpy.scanCostDocument).toHaveBeenCalledWith('b1', jasmine.any(File));
      expect(component.lastScanResult).toEqual([{ name: 'Hotel', amount: 100 }]);
      expect(component.loadBudget).toHaveBeenCalled();
      expect(component.scanningDocId).toBeNull();
    });

    it('should set fileError when scanning a document finds no costs', () => {
      component.budget = makeBudget();
      const doc = makeDocument();
      const blob = new Blob(['data'], { type: 'application/pdf' });
      documentServiceSpy.downloadDocument.and.returnValue(of(blob));
      costServiceSpy.scanCostDocument.and.returnValue(throwError(() => new Error('fail')));

      component.extractCosts(doc);

      expect(component.fileError).toBe('Keine Kosten im Beleg gefunden.');
      expect(component.scanningDocId).toBeNull();
    });

    it('should delete a document after confirmation', () => {
      component.documents = [makeDocument()];
      spyOn(window, 'confirm').and.returnValue(true);
      documentServiceSpy.deleteDocument.and.returnValue(of(undefined));

      component.deleteDocument('d1');

      expect(documentServiceSpy.deleteDocument).toHaveBeenCalledWith('d1');
      expect(component.documents.length).toBe(0);
    });

    it('should not delete a document when confirmation is declined', () => {
      component.documents = [makeDocument()];
      spyOn(window, 'confirm').and.returnValue(false);

      component.deleteDocument('d1');

      expect(documentServiceSpy.deleteDocument).not.toHaveBeenCalled();
      expect(component.documents.length).toBe(1);
    });
  });
});
