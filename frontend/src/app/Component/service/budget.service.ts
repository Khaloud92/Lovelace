import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Budget } from '../../models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/budgets';

  /** Inserted by Angular inject() migration for backwards compatibility */


  getAll(): Observable<Budget[]> {
    return this.http.get<Budget[]>(this.apiUrl);
  }

  create(budget: Budget): Observable<Budget> {
    return this.http.post<Budget>(this.apiUrl, budget);
  }

  /** Create a budget attached to one of the user's travel entries. */
  createForTravelEntry(travelEntryId: string, budget: { name: string; amount: number }): Observable<Budget> {
    return this.http.post<Budget>(`${this.apiUrl}/travel-entry/${travelEntryId}`, budget);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  update(id: string, updatedData: Budget): Observable<Budget> {
    return this.http.put<Budget>(`${this.apiUrl}/${id}`, updatedData);
  }
}
