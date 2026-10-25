import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilemanagerComponent } from './filemanager.component';
import { DocumentSer } from './document-ser.service';
import { Document } from './document.model';
import { TravelEntryService } from '../travel-entry/travel-entry.service';

describe('FilemanagerComponent', () => {
  let component: FilemanagerComponent;
  let fixture: ComponentFixture<FilemanagerComponent>;
  let documentServiceMock: jasmine.SpyObj<DocumentSer>;
  let travelEntryServiceMock: jasmine.SpyObj<TravelEntryService>;

  const mockTrip = { id: 'trip-1', title: 'Florence', destination: 'Florence' };

  const mockDocument: Document = {
    id: 'doc-1',
    name: 'Ticket.pdf',
    fileType: 'application/pdf',
    fileUrl: 'user-1/uuid_Ticket.pdf'
  };

  beforeEach(async () => {
    documentServiceMock = jasmine.createSpyObj<DocumentSer>('DocumentSer', [
      'getDocuments',
      'uploadDocument',
      'deleteDocument',
      'downloadDocument'
    ]);

    travelEntryServiceMock = jasmine.createSpyObj<TravelEntryService>('TravelEntryService', [
      'getTravelEntries'
    ]);

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    travelEntryServiceMock.getTravelEntries.and.returnValue(of([mockTrip] as any));
    documentServiceMock.getDocuments.and.returnValue(of([mockDocument]));

    await TestBed.configureTestingModule({
      imports: [FilemanagerComponent],
      providers: [
        { provide: DocumentSer, useValue: documentServiceMock },
        { provide: TravelEntryService, useValue: travelEntryServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FilemanagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load all trips on init', () => {
    expect(travelEntryServiceMock.getTravelEntries).toHaveBeenCalled();
    expect(component.trips.length).toBe(1);
    expect(component.trips[0].destination).toBe('Florence');
  });

  it('should expand a trip and load its documents on toggle', () => {
    component.toggleTrip('trip-1');

    expect(component.expandedTripId).toBe('trip-1');
    expect(documentServiceMock.getDocuments).toHaveBeenCalledWith('trip-1');
    expect(component.documentsMap['trip-1'].length).toBe(1);
  });

  it('should collapse an already expanded trip on toggle', () => {
    component.toggleTrip('trip-1');
    component.toggleTrip('trip-1');

    expect(component.expandedTripId).toBeNull();
  });

  it('should set the selected file for the active trip', () => {
    const file = new File(['dummy-content'], 'Ticket.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file] } } as unknown as Event;

    component.onFileSelected(event, 'trip-1');

    expect(component.selectedFile).toBe(file);
    expect(component.activeUploadTripId).toBe('trip-1');
  });

  it('should upload the selected file and add it to the documents map', () => {
    const file = new File(['dummy-content'], 'Ticket.pdf', { type: 'application/pdf' });
    component.selectedFile = file;
    component.activeUploadTripId = 'trip-1';
    documentServiceMock.uploadDocument.and.returnValue(of(mockDocument));

    component.onUpload('trip-1');

    expect(documentServiceMock.uploadDocument).toHaveBeenCalledWith('trip-1', file);
    expect(component.documentsMap['trip-1']).toContain(mockDocument);
    expect(component.selectedFile).toBeNull();
  });

  it('should not upload when no file is selected', () => {
    component.selectedFile = null;

    component.onUpload('trip-1');

    expect(documentServiceMock.uploadDocument).not.toHaveBeenCalled();
  });

  it('should delete a document after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.documentsMap['trip-1'] = [mockDocument];
    documentServiceMock.deleteDocument.and.returnValue(of(void 0));

    component.onDelete('doc-1', 'trip-1');

    expect(documentServiceMock.deleteDocument).toHaveBeenCalledWith('doc-1');
    expect(component.documentsMap['trip-1'].length).toBe(0);
  });

  it('should not delete a document when confirmation is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.documentsMap['trip-1'] = [mockDocument];

    component.onDelete('doc-1', 'trip-1');

    expect(documentServiceMock.deleteDocument).not.toHaveBeenCalled();
    expect(component.documentsMap['trip-1'].length).toBe(1);
  });

  it('should download and open a document', () => {
    const blob = new Blob(['dummy-content'], { type: 'application/pdf' });
    documentServiceMock.downloadDocument.and.returnValue(of(blob));
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock-url');

    component.onOpenDocument(mockDocument);

    expect(documentServiceMock.downloadDocument).toHaveBeenCalledWith('doc-1');
  });

  it('should extract the file extension from a mime type', () => {
    expect(component.getFileExtension('application/pdf')).toBe('pdf');
    expect(component.getFileExtension('image/png')).toBe('png');
    expect(component.getFileExtension('')).toBe('Datei');
  });
});
