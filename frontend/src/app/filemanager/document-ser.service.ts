import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document } from './document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentSer {

  private baseUrl = 'http://localhost:8080/api/trips';
  private http = inject(HttpClient);

  getDocuments(travelEntryId: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.baseUrl}/${travelEntryId}/documents`);
  }

  uploadDocument(travelEntryId: string, file: File): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Document>(`${this.baseUrl}/${travelEntryId}/documents`, formData);
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/documents/${documentId}`);
  }

  // Loads the file as a blob via HttpClient so the JWT interceptor applies.
  downloadDocument(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/documents/download/${documentId}`, { responseType: 'blob' });
  }
}
