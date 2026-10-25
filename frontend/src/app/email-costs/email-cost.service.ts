import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import {
  EmailCost,
  FetchEmailCostsRequest,
  MailboxConnectionRequest,
  MailboxStatus,
} from './email-cost.model';

@Injectable({
  providedIn: 'root',
})
export class EmailCostService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/email-costs';

  getMailboxStatus(): Observable<MailboxStatus> {
    return this.http.get<MailboxStatus>(`${this.apiUrl}/mailbox/status`).pipe(timeout(30000));
  }

  connectMailbox(request: MailboxConnectionRequest): Observable<MailboxStatus> {
    return this.http
      .post<MailboxStatus>(`${this.apiUrl}/mailbox/connect`, request)
      .pipe(timeout(30000));
  }

  disconnectMailbox(): Observable<MailboxStatus> {
    return this.http.delete<MailboxStatus>(`${this.apiUrl}/mailbox/disconnect`).pipe(timeout(30000));
  }

  fetchCosts(request: FetchEmailCostsRequest = {}): Observable<EmailCost[]> {
    return this.http.post<EmailCost[]>(`${this.apiUrl}/fetch`, request).pipe(timeout(30000));
  }

  getEmailCosts(travelEntryId?: string | null): Observable<EmailCost[]> {
    const options = travelEntryId
      ? { params: new HttpParams().set('travelEntryId', travelEntryId) }
      : {};

    return this.http.get<EmailCost[]>(this.apiUrl, options).pipe(timeout(30000));
  }

  removeEmailCostFromTrip(emailCostId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${emailCostId}`).pipe(timeout(30000));
  }

  ignoreEmailCostForever(emailCostId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${emailCostId}/ignore`, {}).pipe(timeout(30000));
  }
}
