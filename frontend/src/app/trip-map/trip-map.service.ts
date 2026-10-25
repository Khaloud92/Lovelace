import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MapTrip } from './trip-map.model';

@Injectable({
  providedIn: 'root'
})
export class TripMapService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/map/trips';

  getTrips(): Observable<MapTrip[]> {
    return this.http.get<MapTrip[]>(this.apiUrl);
  }
}
