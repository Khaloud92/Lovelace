import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cost } from '../model/cost.model';

@Injectable({ providedIn: 'root' })
export class CostService {
  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/costs';


  getAll(): Observable<Cost[]> {
    return this.http.get<Cost[]>(this.apiUrl);
  }

  getByBudgetId(budgetId: string): Observable<Cost[]> {
    return this.http.get<Cost[]>(`${this.apiUrl}/budget/${budgetId}`);
  }

  addCostToBudget(budgetId: string, cost: Cost): Observable<Cost> {
    return this.http.post<Cost>(`${this.apiUrl}/budget/${budgetId}`, cost);
  }

  update(id: string, cost: Cost): Observable<Cost> {
    return this.http.put<Cost>(`${this.apiUrl}/${id}`, cost);
  }

  deleteCost(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

}
