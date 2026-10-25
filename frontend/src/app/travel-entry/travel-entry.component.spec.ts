import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { TravelEntryComponent } from './travel-entry.component';
import { TravelEntryService } from './travel-entry.service';
import { TravelEntry } from './travel-entry.model';

describe('TravelEntryComponent', () => {
  let component: TravelEntryComponent;
  let fixture: ComponentFixture<TravelEntryComponent>;

  const travelEntryServiceMock = {
    getTravelEntries: jasmine.createSpy('getTravelEntries').and.returnValue(of([])),
    createTravelEntry: jasmine.createSpy('createTravelEntry').and.callFake((entry: Partial<TravelEntry>) => of(entry)),
    updateTravelEntry: jasmine.createSpy('updateTravelEntry').and.callFake((id: string, entry: Partial<TravelEntry>) => of(entry)),
    deleteTravelEntry: jasmine.createSpy('deleteTravelEntry').and.returnValue(of(void 0)),
    refreshTrigger: signal(0)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TravelEntryComponent],
      providers: [
        {
          provide: TravelEntryService,
          useValue: travelEntryServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TravelEntryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the Travel Entry component', () => {
    expect(component).toBeTruthy();
  });

  it('should load travel entries on init', () => {
    expect(travelEntryServiceMock.getTravelEntries).toHaveBeenCalled();
  });

  it('should create a new travel entry', () => {
    component.form = {
      title: 'Paris Trip',
      destination: 'Paris, France',
      startDate: '2026-06-01',
      endDate: '2026-06-07',
      description: 'Summer trip',
      budget: 2000,
      userId: 'user-1'
    };

    component.saveTravelEntry();

    expect(travelEntryServiceMock.createTravelEntry).toHaveBeenCalled();
  });
});
