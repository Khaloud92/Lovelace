import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { FriendLocation } from './friend-location.model';

@Injectable({ providedIn: 'root' })
export class LocationSharingService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api/profile';

  /** Minimum time between location pushes to the server. */
  private readonly PUSH_INTERVAL_MS = 5000;

  readonly enabled = signal(false);
  readonly error = signal('');

  private watchId?: number;
  private lastPushAt = 0;
  private initialized = false;

  /** Syncs the toggle from the server once per session and starts watching if it is on. */
  init(): void {
    if (this.initialized) {
      return;
    }
    this.initialized = true;
    this.http.get<{ shareLocation: boolean }>(this.base).subscribe({
      next: res => {
        this.enabled.set(res.shareLocation);
        if (res.shareLocation) {
          this.startWatching();
        }
      },
      error: () => (this.initialized = false),
    });
  }

  setEnabled(enabled: boolean): Observable<void> {
    return this.http.put<void>(`${this.base}/location-sharing`, { enabled }).pipe(
      tap(() => {
        this.enabled.set(enabled);
        this.error.set('');
        if (enabled) {
          this.startWatching();
        } else {
          this.stopWatching();
        }
      }),
    );
  }

  getFriendLocations(): Observable<FriendLocation[]> {
    return this.http.get<FriendLocation[]>(`${this.base}/friends/locations`);
  }

  /** Stops the geolocation watch without touching the server-side setting, e.g. on logout. */
  reset(): void {
    this.stopWatching();
    this.enabled.set(false);
    this.initialized = false;
  }

  private startWatching(): void {
    if (this.watchId !== undefined) {
      return;
    }
    if (!('geolocation' in navigator)) {
      this.error.set('Location is not available in this browser.');
      return;
    }
    this.lastPushAt = 0;
    this.watchId = navigator.geolocation.watchPosition(
      pos => this.pushLocation(pos.coords.latitude, pos.coords.longitude),
      err => {
        if (err.code === err.PERMISSION_DENIED) {
          this.error.set('Location access was denied. Allow it in your browser to share with friends.');
        }
      },
      { enableHighAccuracy: true, maximumAge: 30000 },
    );
  }

  private stopWatching(): void {
    if (this.watchId !== undefined) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = undefined;
    }
  }

  private pushLocation(latitude: number, longitude: number): void {
    const now = Date.now();
    if (now - this.lastPushAt < this.PUSH_INTERVAL_MS) {
      return;
    }
    this.lastPushAt = now;
    this.http.put<void>(`${this.base}/location`, { latitude, longitude }).subscribe({
      error: () => undefined,
    });
  }
}
