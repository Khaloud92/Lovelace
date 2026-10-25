import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cost } from '../models/cost.model';

@Injectable({
  providedIn: 'root'
})
export class CostService {
  private http = inject(HttpClient);

  private readonly API_URL = 'http://localhost:8080/api/costs';

  /** Inserted by Angular inject() migration for backwards compatibility */


  addCostToBudget(budgetId: string, cost: Cost): Observable<Cost> {
    return this.http.post<Cost>(`${this.API_URL}/budget/${budgetId}`, cost);
  }

  scanCostDocument(budgetId: string, file: File): Observable<Cost[]> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Cost[]>(`${this.API_URL}/budget/${budgetId}/scan-document`, formData);
  }

  deleteCost(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getCostsByBudget(budgetId: string): Observable<Cost[]> {
    return this.http.get<Cost[]>(`${this.API_URL}/budget/${budgetId}`);
  }
}
