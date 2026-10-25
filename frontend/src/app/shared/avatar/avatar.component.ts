import { Component, Input } from '@angular/core';

/**
 * Round user avatar: shows the profile picture when present, otherwise the
 * first letter of the name on the app's brand gradient. Display-only — used
 * wherever another user appears (friends, requests, chat list).
 */
@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [],
  templateUrl: './avatar.component.html',
  styleUrl: './avatar.component.css',
})
export class AvatarComponent {
  /** Profile picture as a data URL, or null/empty to show the name initial. */
  @Input() src: string | null = null;
  /** Used to derive the fallback initial. */
  @Input() name = '';
  /** Diameter in pixels. */
  @Input() size = 40;

  get initial(): string {
    const name = this.name?.trim();
    return name ? name.charAt(0).toUpperCase() : '?';
  }
}
