import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { ProfileService } from './profile.service';
import { UserProfile } from './profile.model';
import { SessionService } from '../authentication/session.service';
import { LocationSharingService } from './location-sharing.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly session = inject(SessionService);
  private readonly locationSharing = inject(LocationSharingService);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  readonly profile: UserProfile | null = this.profileService.getProfile();
  readonly avatar = this.profileService.avatar;
  readonly locationSharingEnabled = this.locationSharing.enabled;
  readonly locationSharingError = this.locationSharing.error;

  errorMessage = '';
  locationToggleBusy = false;

  /** Avatars are downscaled to a small square thumbnail to keep the stored payload small. */
  private readonly MAX_DIMENSION = 256;
  /** Reject obviously oversized uploads before we even read them. */
  private readonly MAX_FILE_BYTES = 8 * 1024 * 1024;

  ngOnInit(): void {
    // Load the picture stored on the server for the signed-in user.
    this.profileService.loadAvatar().subscribe();
    this.locationSharing.init();
  }

  toggleLocationSharing(): void {
    if (this.locationToggleBusy) {
      return;
    }
    this.locationToggleBusy = true;
    this.locationSharing.setEnabled(!this.locationSharingEnabled()).subscribe({
      next: () => (this.locationToggleBusy = false),
      error: () => {
        this.locationToggleBusy = false;
        this.errorMessage = 'Could not update location sharing. Please try again.';
      },
    });
  }

  /** First letter of the user's name, used as the default avatar. */
  initial(): string {
    const name = this.profile?.name?.trim();
    return name ? name.charAt(0).toUpperCase() : '?';
  }

  pickPhoto(): void {
    this.errorMessage = '';
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // let the user re-pick the same file later
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please choose an image file.';
      return;
    }
    if (file.size > this.MAX_FILE_BYTES) {
      this.errorMessage = 'That image is too large (max 8 MB).';
      return;
    }

    this.resizeToDataUrl(file)
      .then(dataUrl =>
        this.profileService.setAvatar(dataUrl).subscribe({
          error: () => (this.errorMessage = 'Could not save your picture. Please try a smaller image.'),
        }),
      )
      .catch(() => (this.errorMessage = 'Could not load that image.'));
  }

  removePhoto(): void {
    this.errorMessage = '';
    this.profileService.removeAvatar().subscribe({
      error: () => (this.errorMessage = 'Could not remove your picture.'),
    });
  }

  logout(): void {
    this.locationSharing.reset();
    this.session.logout();
  }

  /** Center-crop to a square and scale down, returning a compact JPEG data URL. */
  private resizeToDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject();
      reader.onload = () => {
        const img = new Image();
        img.onerror = () => reject();
        img.onload = () => {
          const side = Math.min(img.width, img.height);
          const size = Math.min(this.MAX_DIMENSION, side);
          const canvas = document.createElement('canvas');
          canvas.width = size;
          canvas.height = size;
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            reject();
            return;
          }
          const sx = (img.width - side) / 2;
          const sy = (img.height - side) / 2;
          ctx.drawImage(img, sx, sy, side, side, 0, 0, size, size);
          resolve(canvas.toDataURL('image/jpeg', 0.85));
        };
        img.src = reader.result as string;
      };
      reader.readAsDataURL(file);
    });
  }
}
