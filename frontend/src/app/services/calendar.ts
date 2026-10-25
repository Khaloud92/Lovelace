import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CalendarEntry } from '../models/calendar-entry';
import { HttpClient } from '@angular/common/http';

/**
 * Service responsible for managing calendar entries.
 * Handles all HTTP communication with the backend API for CRUD operations.
 */
@Injectable({
  providedIn: 'root',
})
export class Calendar {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/calendar';

  /**
   * Retrieves all calendar entries from the backend.
   * * @returns An Observable containing an array of CalendarEntry objects.
   */
  getAllEntries(): Observable<CalendarEntry[]> {
    return this.http.get<CalendarEntry[]>(this.apiUrl);
  }

  /**
   * Creates a new calendar entry in the backend.
   * * @param newEntry The calendar entry object containing the details to be saved.
   * @returns An Observable containing the newly created CalendarEntry (including its generated ID).
   */
  addEntry(newEntry: CalendarEntry): Observable<CalendarEntry> {
    return this.http.post<CalendarEntry>(this.apiUrl, newEntry);
  }

  /**
   * Updates an existing calendar entry.
   * * @param id The unique identifier of the entry to be updated.
   * @param updatedEntry The calendar entry object containing the updated values.
   * @returns An Observable containing the updated CalendarEntry.
   */
  updateEntry(id: string, updatedEntry: CalendarEntry): Observable<CalendarEntry> {
    return this.http.put<CalendarEntry>(`${this.apiUrl}/${id}`, updatedEntry);
  }

  /**
   * Deletes a specific calendar entry from the backend.
   * * @param id The unique identifier of the entry to be deleted.
   * @returns An Observable that completes when the deletion is successful.
   */
  deleteEntry(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
