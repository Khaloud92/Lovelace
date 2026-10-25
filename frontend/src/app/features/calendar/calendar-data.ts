import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CalendarEntry } from '../../models/calendar-entry';

@Injectable({
  providedIn: 'root'
})
export class CalendarDataService {
  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/calendar';

  getEntries(): Observable<CalendarEntry[]> {
    return this.http.get<CalendarEntry[]>(this.apiUrl);
  }
}
