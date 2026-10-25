import { Component, OnInit, Input, inject } from '@angular/core';
import { CommonModule, DecimalPipe, SlicePipe } from '@angular/common';
import { DocumentSer } from './document-ser.service';
import { Document } from './document.model';
import { TravelEntryService } from '../travel-entry/travel-entry.service';

@Component({
  selector: 'app-file-manager',
  standalone: true,
  imports: [CommonModule, DecimalPipe, SlicePipe],
  templateUrl: './filemanager.component.html',
  styleUrls: []
})
export class FilemanagerComponent implements OnInit {

  @Input() tripId?: string;

  travelEntryId = '';
  documents: Document[] = [];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  trips: any[] = [];
  selectedFile: File | null = null;
  errorMessage = '';

  expandedTripId: string | null = null;
  // eslint-disable-next-line @typescript-eslint/consistent-indexed-object-style
  documentsMap: { [key: string]: Document[] } = {};
  activeUploadTripId: string | null = null;

  private documentService = inject(DocumentSer);
  private travelEntryService = inject(TravelEntryService);

  ngOnInit(): void {
    this.loadAllTrips();
  }

  loadAllTrips(): void {
    this.travelEntryService.getTravelEntries().subscribe({
      next: (data) => {
        this.trips = this.tripId
          ? data.filter(t => String(t.id) === String(this.tripId))
          : data;

        if (this.tripId && this.trips.length === 1) {
          this.toggleTrip(this.trips[0].id);
        }
      },
      error: (err) => console.error('Fehler beim Laden der Reisen', err)
    });
  }

  toggleTrip(tripId: string): void {
    if (this.expandedTripId === tripId) {
      this.expandedTripId = null;
    } else {
      this.expandedTripId = tripId;
      this.selectedFile = null;
      this.errorMessage = '';

      this.loadDocumentsForTrip(tripId);
    }
  }

  loadDocumentsForTrip(tripId: string): void {
    this.documentService.getDocuments(tripId).subscribe({
      next: (docs) => {
        this.documentsMap[tripId] = docs;
      },
      error: (err) => console.error(`Fehler beim Laden der Dokumente für Trip ${tripId}`, err)
    });
  }

  onFileSelected(event: Event, tripId: string): void {
    const element = event.target as HTMLInputElement;
    const file = element.files?.[0];
    if (file) {
      this.selectedFile = file;
      this.activeUploadTripId = tripId;
      this.errorMessage = '';
    }
  }

  onUpload(tripId: string): void {
    if (!this.selectedFile || this.activeUploadTripId !== tripId) return;

    this.documentService.uploadDocument(tripId, this.selectedFile).subscribe({
      next: (newDoc) => {
        if (!this.documentsMap[tripId]) {
          this.documentsMap[tripId] = [];
        }
        this.documentsMap[tripId].push(newDoc);

        this.selectedFile = null;
        this.activeUploadTripId = null;
        this.errorMessage = '';
      },
      error: (err) => {
        this.errorMessage = 'Upload failed.';
        console.error(err);
      }
    });
  }

  onDelete(documentId: string, tripId: string): void {
    if (confirm('Really delete this file?')) {
      this.documentService.deleteDocument(documentId).subscribe({
        next: () => {
          if (this.documentsMap[tripId]) {
            this.documentsMap[tripId] = this.documentsMap[tripId].filter(doc => doc.id !== documentId);
          }
        },
        error: (err) => console.error('Fehler beim Löschen des Dokuments', err)
      });
    }
  }

  onOpenDocument(doc: Document): void {
    if (!doc.id) {
      console.error('Dokumenten-ID fehlt.');
      return;
    }

    this.documentService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.target = '_blank';
        link.rel = 'noopener';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: (err) => {
        this.errorMessage = 'Download failed.';
        console.error('Fehler beim Öffnen des Dokuments', err);
      }
    });
  }

  /**
   * Hilfsmethode, um den Dateityp sicher für die UI zu splitten
   */
  getFileExtension(fileType: string): string {
    if (!fileType) return 'Datei';
    const parts = fileType.split('/');
    return parts.length > 1 ? parts[1] : 'Datei';
  }
}
