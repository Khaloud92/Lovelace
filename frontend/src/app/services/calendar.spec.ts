import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CalendarDataService } from '../features/calendar/calendar-data';
import { CalendarEntry } from '../models/calendar-entry';

describe('CalendarDataService', () => {
  let service: CalendarDataService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CalendarDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch entries from correct URL', () => {
    const mockEntries: CalendarEntry[] = [
      {
        id: '1',
        title: 'Test Event',
        startDate: '2026-06-01T10:00:00Z',
        color: '#4f98a3'  // ← fix
      }
    ];

    service.getEntries().subscribe(entries => {
      expect(entries).toEqual(mockEntries);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/calendar');
    expect(req.request.method).toBe('GET');
    req.flush(mockEntries);
  });
});
