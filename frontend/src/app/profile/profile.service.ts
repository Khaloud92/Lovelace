import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { AuthService } from '../authentication/auth.service';
import { UserProfile } from './profile.model';

interface ProfileResponse {
  name: string;
  email: string;
  profilePicture: string | null;
  shareLocation: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = 'http://localhost:8080/api/profile';

  /**
   * The signed-in user's stored picture (data URL), or null to show the initial.
   * Persisted on the backend so other users see it in friends/chats; populated
   * by loadAvatar() and kept in sync by setAvatar()/removeAvatar().
   */
  readonly avatar = signal<string | null>(null);

  // The signed-in user's name and email, read from the login response cached at sign in.
  getProfile(): UserProfile | null {
    const userId = this.auth.getUserId();
    const user = this.auth.getUser();
    if (!userId || !user) {
      return null;
    }
    return { userId, name: user.name, email: user.email };
  }

  /** Fetch the current user's stored picture from the server into the signal. */
  loadAvatar(): Observable<string | null> {
    return this.http.get<ProfileResponse>(this.base).pipe(
      map(res => res.profilePicture ?? null),
      tap(picture => this.avatar.set(picture)),
    );
  }

  /** Save a new picture and update the signal on success. */
  setAvatar(dataUrl: string): Observable<void> {
    return this.http.put<void>(`${this.base}/picture`, { picture: dataUrl }).pipe(
      tap(() => this.avatar.set(dataUrl)),
    );
  }

  /** Clear the picture (revert to the name initial) and update the signal. */
  removeAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.base}/picture`).pipe(
      tap(() => this.avatar.set(null)),
    );
  }
}
