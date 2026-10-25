import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Calendar } from '../../services/calendar';
import { CalendarEntry } from '../../models/calendar-entry';

describe('CalendarService', () => {
  let service: Calendar;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/calendar';

  const mockEntry: CalendarEntry = {
    id: '1',
    title: 'Flug Berlin',
    startDate: '2026-06-01T08:30:00Z',
    endDate: '2026-06-01T09:45:00Z',
    location: 'Berlin',
    color: '#4f98a3'  // ← fix
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        Calendar,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(Calendar);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all entries with GET', () => {
    const mockEntries: CalendarEntry[] = [mockEntry];

    service.getAllEntries().subscribe(entries => {
      expect(entries.length).toBe(1);
      expect(entries[0].title).toBe('Flug Berlin');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockEntries);
  });

  it('should add entry with POST', () => {
    const newEntry: CalendarEntry = {
      title: 'Hotel Hamburg',
      startDate: '2026-07-01T14:00:00Z',
      color: '#4f98a3'  // ← fix
    };

    service.addEntry(newEntry).subscribe(entry => {
      expect(entry.id).toBe('1');
      expect(entry.title).toBe('Flug Berlin');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newEntry);
    req.flush(mockEntry);
  });

  it('should update entry with PUT', () => {
    const updated: CalendarEntry = { ...mockEntry, title: 'Flug München' };

    service.updateEntry('1', updated).subscribe(entry => {
      expect(entry.title).toBe('Flug Berlin');
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockEntry);
  });

  it('should delete entry with DELETE', () => {
    service.deleteEntry('1').subscribe(() => {
      // Kein Rückgabewert erwartet
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
