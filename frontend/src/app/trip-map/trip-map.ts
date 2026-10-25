import { Component, ElementRef, OnDestroy, OnInit, ViewChild, ViewEncapsulation, inject } from '@angular/core';
import * as L from 'leaflet';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { MapTrip } from './trip-map.model';
import { TripMapService } from './trip-map.service';
import { ProfileService } from '../profile/profile.service';
import { LocationSharingService } from '../profile/location-sharing.service';
import { FriendLocation, FriendLocationEvent } from '../profile/friend-location.model';
import { WebSocketService } from '../services/websocket.service';
import { AuthService } from '../authentication/auth.service';

@Component({
  selector: 'app-trip-map',
  standalone: true,
  imports: [],
  templateUrl: './trip-map.html',
  styleUrl: './trip-map.css',
  encapsulation: ViewEncapsulation.None,
})
export class TripMap implements OnInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  private readonly tripMapService = inject(TripMapService);
  private readonly profileService = inject(ProfileService);
  private readonly locationSharing = inject(LocationSharingService);
  private readonly ws = inject(WebSocketService);
  private readonly auth = inject(AuthService);
  private map?: L.Map;
  private resizeObserver?: ResizeObserver;
  private locationMarker?: L.Marker;
  private geoWatchId?: number;
  private friendMarkers = new Map<string, L.Marker>();
  private friendLocationSub?: Subscription;
  private ownLocation?: L.LatLng;
  private friendLocations: FriendLocation[] = [];

  private readonly FRIEND_REFRESH_MS = 30000;

  private readonly COLLISION_TILT_DEG = 60;

  ngOnInit(): void {
    const container = this.mapContainer.nativeElement;
    const minZoom = Math.max(2, Math.ceil(Math.log2(Math.max(container.clientHeight, 256) / 256)));

    this.map = L.map(container, {
      worldCopyJump: true,
      minZoom,
      zoomControl: false,

      maxBounds: L.latLngBounds([-90, -36000], [90, 36000]),
      maxBoundsViscosity: 1.0,
    }).setView([20, 0], minZoom);

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
      subdomains: 'abcd',
    }).addTo(this.map);

    this.tripMapService.getTrips().subscribe(trips => this.renderTrips(trips));

    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    this.resizeObserver.observe(container);

    this.profileService.loadAvatar().subscribe({
      next: () => this.startLocationTracking(),
      error: () => this.startLocationTracking(),
    });

    this.friendLocationSub = interval(this.FRIEND_REFRESH_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.locationSharing.getFriendLocations()),
      )
      .subscribe(locations => this.renderFriendLocations(locations));

    const userId = this.auth.getUserId();
    if (userId) {
      this.friendLocationSub.add(
        this.ws
          .watch<FriendLocationEvent>(`/topic/friend-locations.${userId}`)
          .subscribe(event => this.onFriendLocationEvent(event)),
      );
    }
  }

  ngOnDestroy(): void {
    if (this.geoWatchId !== undefined) {
      navigator.geolocation.clearWatch(this.geoWatchId);
    }
    this.friendLocationSub?.unsubscribe();
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }

  private startLocationTracking(): void {
    if (!('geolocation' in navigator)) {
      return;
    }
    this.geoWatchId = navigator.geolocation.watchPosition(
      pos => this.updateLocationMarker(pos.coords.latitude, pos.coords.longitude),
      null,
      { enableHighAccuracy: true, maximumAge: 30000 },
    );
  }

  private updateLocationMarker(lat: number, lng: number): void {
    if (!this.map) {
      return;
    }
    this.ownLocation = L.latLng(lat, lng);
    if (!this.locationMarker) {
      this.locationMarker = L.marker([lat, lng], { icon: this.locationIcon(), zIndexOffset: 1000 })
        .bindPopup('You are here')
        .addTo(this.map);
      this.locationMarker.on('click', () => this.flyToMarker(this.locationMarker!));
    }
    this.applyLocationOffsets();
  }

  private flyToMarker(marker: L.Marker): void {
    if (!this.map) {
      return;
    }
    // zoom in on the pin, but never zoom back out if already closer
    const targetZoom = Math.max(this.map.getZoom(), 10);
    this.map.flyTo(marker.getLatLng(), targetZoom, { duration: 1 });
  }

  private locationIcon(): L.DivIcon {
    const avatar = this.profileService.avatar();
    const name = this.profileService.getProfile()?.name ?? '';
    const inner = avatar
      ? `<img src="${avatar}" alt="" class="trip-map-avatar-img" />`
      : `<span class="trip-map-avatar-fallback">${(name || '?').charAt(0).toUpperCase()}</span>`;

    return L.divIcon({
      className: 'trip-map-marker',
      html: `
        <div class="trip-map-location">
          <div class="trip-map-location-avatar">${inner}</div>
          <svg viewBox="0 0 48 60" width="48" height="60">
            <line x1="24" y1="0" x2="24" y2="28" stroke="#10b981" stroke-width="3"/>
            <path d="M4 0 L24 58 L24 24 Z" fill="#10b981"/>
            <path d="M44 0 L24 58 L24 24 Z" fill="#34d399"/>
          </svg>
        </div>
      `,
      iconSize: [48, 104],
      iconAnchor: [24, 102],
      popupAnchor: [0, -104],
    });
  }

  private onFriendLocationEvent(event: FriendLocationEvent): void {
    const others = this.friendLocations.filter(loc => loc.userId !== event.userId);
    if (event.sharing && event.latitude != null && event.longitude != null) {
      others.push({
        userId: event.userId,
        name: event.name,
        profilePicture: event.profilePicture,
        latitude: event.latitude,
        longitude: event.longitude,
        updatedAt: event.updatedAt ?? '',
      });
    }
    this.renderFriendLocations(others);
  }

  private renderFriendLocations(locations: FriendLocation[]): void {
    if (!this.map) {
      return;
    }
    this.friendLocations = locations;

    const seen = new Set<string>();
    for (const loc of locations) {
      seen.add(loc.userId);
      if (this.friendMarkers.has(loc.userId)) {
        continue;
      }
      const marker = L.marker([loc.latitude, loc.longitude], {
        icon: this.friendLocationIcon(loc),
        zIndexOffset: 900,
      })
        .bindPopup(`<div class="trip-map-popup"><strong>${loc.name}</strong><br/><em>Shared location</em></div>`)
        .addTo(this.map);
      marker.on('click', () => this.flyToMarker(marker));
      this.friendMarkers.set(loc.userId, marker);
    }

    // drop markers of friends who stopped sharing
    for (const [userId, marker] of this.friendMarkers) {
      if (!seen.has(userId)) {
        marker.remove();
        this.friendMarkers.delete(userId);
      }
    }

    this.applyLocationOffsets();
  }

  /**
   * Places every location pin at its true position. Pins that share the same
   * spot lean away from each other around their tips, so all of them stay
   * visible while their tips meet at the shared location.
   */
  private applyLocationOffsets(): void {
    if (!this.map) {
      return;
    }

    const entries: { marker: L.Marker; lat: number; lng: number }[] = [];
    if (this.locationMarker && this.ownLocation) {
      entries.push({ marker: this.locationMarker, lat: this.ownLocation.lat, lng: this.ownLocation.lng });
    }
    for (const loc of this.friendLocations) {
      const marker = this.friendMarkers.get(loc.userId);
      if (marker) {
        entries.push({ marker, lat: loc.latitude, lng: loc.longitude });
      }
    }

    const groups = new Map<string, typeof entries>();
    for (const entry of entries) {
      const key = `${entry.lat.toFixed(4)},${entry.lng.toFixed(4)}`;
      const group = groups.get(key);
      if (group) {
        group.push(entry);
      } else {
        groups.set(key, [entry]);
      }
    }

    for (const group of groups.values()) {
      group.forEach((entry, i) => {
        const spread = group.length === 1 ? 0 : i - (group.length - 1) / 2;
        const tilt = Math.max(-70, Math.min(70, spread * this.COLLISION_TILT_DEG));
        entry.marker.setLatLng([entry.lat, entry.lng]);
        this.setPinTilt(entry.marker, tilt);
      });
    }
  }

  private setPinTilt(marker: L.Marker, degrees: number): void {
    const pin = marker.getElement()?.querySelector<HTMLElement>('.trip-map-location');
    pin?.style.setProperty('--pin-tilt', `${degrees}deg`);
  }

  private friendLocationIcon(loc: FriendLocation): L.DivIcon {
    const inner = loc.profilePicture
      ? `<img src="${loc.profilePicture}" alt="" class="trip-map-avatar-img" />`
      : `<span class="trip-map-avatar-fallback">${(loc.name || '?').charAt(0).toUpperCase()}</span>`;

    return L.divIcon({
      className: 'trip-map-marker',
      html: `
        <div class="trip-map-location">
          <div class="trip-map-location-avatar trip-map-location-avatar--friend">${inner}</div>
          <svg viewBox="0 0 48 60" width="48" height="60">
            <line x1="24" y1="0" x2="24" y2="28" stroke="#3b82f6" stroke-width="3"/>
            <path d="M4 0 L24 58 L24 24 Z" fill="#3b82f6"/>
            <path d="M44 0 L24 58 L24 24 Z" fill="#60a5fa"/>
          </svg>
        </div>
      `,
      iconSize: [48, 104],
      iconAnchor: [24, 102],
      popupAnchor: [0, -104],
    });
  }

  private renderTrips(trips: MapTrip[]): void {
    if (!this.map || trips.length === 0) {
      return;
    }

    // bundles sharing a spot (e.g. a personal trip at a group trip's destination)
    // sit side by side instead of stacking
    const clusters = new Map<string, MapTrip[][]>();
    for (const members of this.groupTrips(trips)) {
      const key = `${members[0].latitude.toFixed(4)},${members[0].longitude.toFixed(4)}`;
      const cluster = clusters.get(key) ?? [];
      cluster.push(members);
      clusters.set(key, cluster);
    }

    const markers: L.Marker[] = [];
    for (const cluster of clusters.values()) {
      const gap = 4;
      const widths = cluster.map(members => this.tripIconWidth(members.length));
      const total = widths.reduce((sum, w) => sum + w, 0) + gap * (cluster.length - 1);
      let x = -total / 2;

      cluster.forEach((members, i) => {
        const dx = cluster.length === 1 ? 0 : x + widths[i] / 2;
        x += widths[i] + gap;

        const trip = members[0];
        const marker = L.marker([trip.latitude, trip.longitude], {
          icon: members.length > 1 ? this.groupAvatarIcon(members, dx) : this.avatarIcon(trip, dx),
        });
        marker.bindPopup(members.length > 1 ? this.groupPopupContent(members) : this.popupContent(trip));
        marker.on('click', () => this.flyToMarker(marker));
        markers.push(marker);
      });
    }

    const group = L.featureGroup(markers).addTo(this.map);
    this.map.fitBounds(group.getBounds().pad(0.2), { maxZoom: 6 });
  }

  private tripIconWidth(memberCount: number): number {
    return memberCount > 1 ? 10 + 30 + (memberCount - 1) * 20 : 40;
  }

  /**
   * Accepting a group trip gives every member their own copy of the entry, so
   * the map receives one identical trip per accepted user. Bundle those copies
   * (same title, destination and dates) so they render as one marker.
   */
  private groupTrips(trips: MapTrip[]): MapTrip[][] {
    const groups = new Map<string, Map<string, MapTrip>>();
    for (const trip of trips) {
      const key = `${trip.title}|${trip.destination}|${trip.startDate}|${trip.endDate}`;
      const members = groups.get(key) ?? new Map<string, MapTrip>();
      if (!members.has(trip.ownerId)) {
        members.set(trip.ownerId, trip);
      }
      groups.set(key, members);
    }
    return [...groups.values()].map(members =>
      [...members.values()].sort((a, b) => Number(b.own) - Number(a.own)),
    );
  }

  private avatarIcon(trip: MapTrip, dx = 0): L.DivIcon {
    const borderColor = trip.own ? '#10b981' : '#3b82f6';
    const inner = trip.ownerProfilePicture
      ? `<img src="${trip.ownerProfilePicture}" alt="" class="trip-map-avatar-img" />`
      : `<span class="trip-map-avatar-fallback">${(trip.ownerName || '?').charAt(0).toUpperCase()}</span>`;

    return L.divIcon({
      className: 'trip-map-marker',
      html: `<div class="trip-map-avatar" style="border-color:${borderColor}">${inner}</div>`,
      iconSize: [40, 40],
      iconAnchor: [20 - dx, 40],
      popupAnchor: [dx, -40],
    });
  }

  private groupAvatarIcon(members: MapTrip[], dx = 0): L.DivIcon {
    const avatars = members
      .map(member => {
        const borderColor = member.own ? '#10b981' : '#3b82f6';
        const inner = member.ownerProfilePicture
          ? `<img src="${member.ownerProfilePicture}" alt="" class="trip-map-avatar-img" />`
          : `<span class="trip-map-avatar-fallback">${(member.ownerName || '?').charAt(0).toUpperCase()}</span>`;
        return `<div class="trip-map-group-avatar" style="border-color:${borderColor}">${inner}</div>`;
      })
      .join('');

    // bubble = 4px padding + 1px border around 30px avatars overlapping by 10px
    const width = this.tripIconWidth(members.length);
    const height = 40;
    return L.divIcon({
      className: 'trip-map-marker',
      html: `<div class="trip-map-group-bubble">${avatars}</div>`,
      iconSize: [width, height],
      iconAnchor: [width / 2 - dx, height],
      popupAnchor: [dx, -height],
    });
  }

  private groupPopupContent(members: MapTrip[]): string {
    const trip = members[0];
    const names = members.map(member => (member.own ? 'You' : member.ownerName)).join(', ');
    return `
      <div class="trip-map-popup">
        <strong>${trip.title}</strong><br/>
        ${trip.destination}<br/>
        ${trip.startDate} &ndash; ${trip.endDate}<br/>
        <em>Group trip &middot; ${names}</em>
      </div>
    `;
  }

  private popupContent(trip: MapTrip): string {
    return `
      <div class="trip-map-popup">
        <strong>${trip.title}</strong><br/>
        ${trip.destination}<br/>
        ${trip.startDate} &ndash; ${trip.endDate}<br/>
        <em>${trip.own ? 'Your trip' : trip.ownerName}</em>
      </div>
    `;
  }
}
