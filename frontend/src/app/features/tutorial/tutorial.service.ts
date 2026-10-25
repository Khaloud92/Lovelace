import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../authentication/auth.service';

@Injectable({
  providedIn: 'root',
})
export class TutorialService {
  private readonly authService = inject(AuthService);
  private readonly storagePrefix = 'tutorial_seen_';

  shouldShowTutorial(): boolean {
    const userId = this.authService.getUserId();

    if (!userId) {
      return false;
    }

    return localStorage.getItem(this.getStorageKey(userId)) !== 'true';
  }

  markTutorialAsSeen(): void {
    const userId = this.authService.getUserId();

    if (!userId) {
      return;
    }

    localStorage.setItem(this.getStorageKey(userId), 'true');
  }

  resetTutorialForCurrentUser(): void {
    const userId = this.authService.getUserId();

    if (!userId) {
      return;
    }

    localStorage.removeItem(this.getStorageKey(userId));
  }

  private getStorageKey(userId: string): string {
    return `${this.storagePrefix}${userId}`;
  }
}
