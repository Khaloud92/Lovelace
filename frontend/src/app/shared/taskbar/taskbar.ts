import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { gsap } from 'gsap';
import { NavigationEnd, NavigationStart, Router, RouterLink, RouterModule } from '@angular/router';
import { NgClass } from '@angular/common';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../authentication/auth.service';
import { FriendshipService } from '../../friends/friendship.service';
import { ChatService } from '../../chat/chat.service';
import { SessionService } from '../../authentication/session.service';
import { LocationSharingService } from '../../profile/location-sharing.service';
import { SearchBar } from '../search-bar/search-bar';

@Component({
  selector: 'app-taskbar',
  standalone: true,
  imports: [RouterModule, RouterLink, NgClass, SearchBar],
  templateUrl: './taskbar.html',
  styleUrl: './taskbar.css',
})
export class Taskbar implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('activeSlider') activeSlider!: ElementRef;
  @ViewChild('navLinksContainer') navLinksContainer!: ElementRef;

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly friendshipService = inject(FriendshipService);
  private readonly chatService = inject(ChatService);
  private readonly session = inject(SessionService);
  private readonly locationSharing = inject(LocationSharingService);

  @ViewChild('profileDropdown')
  profileDropdown?: ElementRef<HTMLElement>;

  readonly friendRequestCount =
    this.friendshipService.pendingRequestCount;

  readonly hasUnreadChats = this.chatService.hasUnread;

  isLightMode = false;
  isDropdownOpen = false;
  isMobileMenuOpen = false;

  private mobileMenuOpenedByTutorial = false;
  private profileMenuOpenedByTutorial = false;
  private routerSubscription?: Subscription;

  ngOnInit(): void {
    const userId = this.authService.getUserId();

    // Remove stale theme classes from the body first, otherwise light-mode CSS breaks.
    document.body.classList.remove('light-mode');

    this.isLightMode = localStorage.getItem(`theme_${userId}`) === 'light';
    document.body.classList.toggle('light-mode', this.isLightMode);
    document.body.classList.toggle('dark', !this.isLightMode);

    this.routerSubscription = this.router.events
      .pipe(
        filter((event) => event instanceof NavigationStart || event instanceof NavigationEnd)
      )
      .subscribe(() => {
        this.closeMobileMenu();
        this.closeDropdown();

        this.mobileMenuOpenedByTutorial = false;
        this.profileMenuOpenedByTutorial = false;
      });

    this.friendshipService.refreshPendingCount();
    this.friendshipService.listenForFriendRequests();
    this.chatService.listenForMessages();
    this.locationSharing.init();
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.resetSliderToActive(), 100);

    this.routerSubscription?.add(
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe(() => {
// RouterLinkActive updates .active-link in a microtask; defer so we read the new link
        setTimeout(() => this.resetSliderToActive());
      })
    );

    this.initHoverEffect();
  }

  private initHoverEffect(): void {
    if (!this.navLinksContainer) return;
    const container = this.navLinksContainer.nativeElement as HTMLElement;
    const links = container.querySelectorAll('.nav-item');

    links.forEach(link => {
      link.addEventListener('mouseenter', (e) => {
        const target = e.target as HTMLElement;
        this.moveSliderTo(target, 0.25);
      });
    });

    container.addEventListener('mouseleave', () => {
      this.resetSliderToActive();
    });
  }

  private resetSliderToActive(): void {
    if (!this.navLinksContainer || !this.activeSlider) return;
    const container = this.navLinksContainer.nativeElement as HTMLElement;
    const activeLink = container.querySelector('.active-link') as HTMLElement;

    if (activeLink) {
      this.moveSliderTo(activeLink, 0.4);
    } else {
      gsap.to(this.activeSlider.nativeElement, { opacity: 0, duration: 0.2 });
    }
  }

  private moveSliderTo(targetElement: HTMLElement, duration: number): void {
    if (!this.activeSlider || !this.navLinksContainer) return;

    const offsetLeft = targetElement.offsetLeft;
    const width = targetElement.offsetWidth;

    gsap.to(this.activeSlider.nativeElement, {
      x: offsetLeft,
      width: width,
      opacity: 1,
      duration: duration,
      ease: 'power3.out',
      overwrite: 'auto'
    });
  }

  toggleTheme(): void {
    this.isLightMode = !this.isLightMode;
    const userId = this.authService.getUserId();
    localStorage.setItem(`theme_${userId}`, this.isLightMode ? 'light' : 'dark');
    document.body.classList.toggle('light-mode', this.isLightMode);
    document.body.classList.toggle('dark', !this.isLightMode);
  }

  logout(): void {
    this.closeMobileMenu();
    this.closeDropdown();

    this.mobileMenuOpenedByTutorial = false;
    this.profileMenuOpenedByTutorial = false;

    this.locationSharing.reset();
    this.session.logout();
  }

  toggleDropdown(event?: MouseEvent): void {
    event?.stopPropagation();

    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.closeMobileMenu();
      this.mobileMenuOpenedByTutorial = false;
    } else {
      this.profileMenuOpenedByTutorial = false;
    }
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
    this.profileMenuOpenedByTutorial = false;
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if (this.isMobileMenuOpen) {
      this.closeDropdown();
      this.profileMenuOpenedByTutorial = false;
    } else {
      this.mobileMenuOpenedByTutorial = false;
    }
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
    this.mobileMenuOpenedByTutorial = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isDropdownOpen) {
      return;
    }

    const clickedElement = event.target;

    if (!(clickedElement instanceof Node)) {
      return;
    }

    const dropdownElement =
      this.profileDropdown?.nativeElement;

    if (
      dropdownElement &&
      !dropdownElement.contains(clickedElement)
    ) {
      this.closeDropdown();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.closeDropdown();
    this.closeMobileMenu();
  }

  @HostListener('window:tutorial-open-mobile-navbar')
  openMobileNavbarForTutorial(): void {
    if (window.innerWidth >= 768) return;
    this.closeDropdown();
    if (!this.isMobileMenuOpen) {
      this.isMobileMenuOpen = true;
      this.mobileMenuOpenedByTutorial = true;
    }
  }

  @HostListener('window:tutorial-close-mobile-navbar')
  closeMobileNavbarAfterTutorial(): void {
    if (!this.mobileMenuOpenedByTutorial) return;
    this.closeMobileMenu();
  }

  @HostListener('window:tutorial-open-profile-menu')
  openProfileMenuForTutorial(): void {
    if (window.innerWidth < 768) return;
    this.closeMobileMenu();
    if (!this.isDropdownOpen) {
      this.isDropdownOpen = true;
      this.profileMenuOpenedByTutorial = true;
    }
  }

  @HostListener('window:tutorial-close-profile-menu')
  closeProfileMenuAfterTutorial(): void {
    if (!this.profileMenuOpenedByTutorial) return;
    this.closeDropdown();
  }
}
