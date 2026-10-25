import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DocumentSer } from './document-ser.service';
import { Document } from './document.model';

describe('DocumentSer', () => {
  let service: DocumentSer;
  let httpMock: HttpTestingController;

  const API_URL = 'http://localhost:8080/api/trips';
  const TRIP_ID = 'trip-1';

  const mockDocument: Document = {
    id: 'doc-1',
    name: 'Ticket.pdf',
    fileType: 'application/pdf',
    fileUrl: 'user-1/uuid_Ticket.pdf'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DocumentSer,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DocumentSer);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // --- getDocuments ---

  it('getDocuments() should send GET to /:tripId/documents and return documents', () => {
    service.getDocuments(TRIP_ID).subscribe(docs => {
      expect(docs.length).toBe(1);
      expect(docs[0].name).toBe('Ticket.pdf');
    });

    const req = httpMock.expectOne(`${API_URL}/${TRIP_ID}/documents`);
    expect(req.request.method).toBe('GET');
    req.flush([mockDocument]);
  });

  // --- uploadDocument ---

  it('uploadDocument() should send POST with FormData and return created document', () => {
    const file = new File(['dummy-content'], 'Ticket.pdf', { type: 'application/pdf' });

    service.uploadDocument(TRIP_ID, file).subscribe(doc => {
      expect(doc.id).toBe('doc-1');
      expect(doc.name).toBe('Ticket.pdf');
    });

    const req = httpMock.expectOne(`${API_URL}/${TRIP_ID}/documents`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockDocument);
  });

  // --- deleteDocument ---

  it('deleteDocument() should send DELETE to /documents/:id', () => {
    service.deleteDocument('doc-1').subscribe(result => {
      expect(result).toBeNull();
    });

    const req = httpMock.expectOne(`${API_URL}/documents/doc-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // --- downloadDocument ---

  it('downloadDocument() should send GET with blob responseType and return a blob', () => {
    const blob = new Blob(['dummy-content'], { type: 'application/pdf' });

    service.downloadDocument('doc-1').subscribe(result => {
      expect(result instanceof Blob).toBeTrue();
    });

    const req = httpMock.expectOne(`${API_URL}/documents/download/doc-1`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });
});
