import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { TravelEntry } from './travel-entry.model';

@Injectable({
  providedIn: 'root'
})
export class TravelEntryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/travel-entries';

  /**
   * Signal that increments whenever a travel entry is deleted externally
   * (e.g. via the calendar). Components can react to this to reload their data.
   */
  refreshTrigger = signal(0);

  /**
   * Triggers a refresh of all components that observe the refreshTrigger signal.
   * Call this after an external deletion (e.g. deleting a trip via the calendar).
   */
  triggerRefresh(): void {
    this.refreshTrigger.update(v => v + 1);
  }

  /**
   * Retrieves all travel entries, optionally filtered by user ID.
   *
   * @param userId optional user ID to filter entries
   * @returns an Observable containing an array of TravelEntry objects
   */
  getTravelEntries(): Observable<TravelEntry[]> {
    return this.http.get<TravelEntry[]>(this.apiUrl);
  }

  /**
   * Creates a new travel entry.
   *
   * @param travelEntry the travel entry to create
   * @returns an Observable containing the created TravelEntry
   */
  createTravelEntry(travelEntry: Partial<TravelEntry>): Observable<TravelEntry> {
    return this.http.post<TravelEntry>(this.apiUrl, travelEntry);
  }

  /**
   * Updates an existing travel entry.
   *
   * @param id the unique identifier of the travel entry to update
   * @param travelEntry the updated travel entry data
   * @returns an Observable containing the updated TravelEntry
   */
  updateTravelEntry(id: string, travelEntry: Partial<TravelEntry>): Observable<TravelEntry> {
    return this.http.put<TravelEntry>(`${this.apiUrl}/${id}`, travelEntry);
  }

  /**
   * Deletes a travel entry by its ID.
   *
   * @param id the unique identifier of the travel entry to delete
   * @returns an Observable that completes when the deletion is successful
   */
  deleteTravelEntry(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
