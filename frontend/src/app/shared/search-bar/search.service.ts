import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SearchResult, SearchSuggestion } from './search.model';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/search';

  search(query: string): Observable<SearchResult[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<SearchResult[]>(this.baseUrl, { params });
  }

  getSuggestions(): Observable<SearchSuggestion[]> {
    return this.http.get<SearchSuggestion[]>(`${this.baseUrl}/suggestions`);
  }

  deleteSuggestion(query: string): Observable<void> {
    const params = new HttpParams().set('query', query);
    return this.http.delete<void>(`${this.baseUrl}/suggestions`, { params });
  }
}
