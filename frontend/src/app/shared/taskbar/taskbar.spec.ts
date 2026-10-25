import { Component, signal } from '@angular/core';
import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick,
} from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { Taskbar } from './taskbar';
import { AuthService } from '../../authentication/auth.service';
import { FriendshipService } from '../../friends/friendship.service';
import { ChatService } from '../../chat/chat.service';
import { SessionService } from '../../authentication/session.service';
import { LocationSharingService } from '../../profile/location-sharing.service';

@Component({
  standalone: true,
  template: '',
})
class DummyComponent {}

describe('Taskbar', () => {
  let component: Taskbar;
  let fixture: ComponentFixture<Taskbar>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let sessionServiceMock: jasmine.SpyObj<SessionService>;
  let locationSharingServiceMock: jasmine.SpyObj<LocationSharingService>;

  const friendshipServiceMock = {
    pendingRequestCount: signal(0),
    refreshPendingCount: jasmine.createSpy('refreshPendingCount'),
    listenForFriendRequests: jasmine.createSpy(
      'listenForFriendRequests',
    ),
  };

  const chatServiceMock = {
    hasUnread: signal(false),
    listenForMessages: jasmine.createSpy('listenForMessages'),
  };

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['getUserId'],
    );

    sessionServiceMock = jasmine.createSpyObj<SessionService>(
      'SessionService',
      ['logout'],
    );

    locationSharingServiceMock =
      jasmine.createSpyObj<LocationSharingService>(
        'LocationSharingService',
        ['init', 'reset'],
      );

    authServiceMock.getUserId.and.returnValue('test-user');

    friendshipServiceMock.pendingRequestCount.set(0);
    friendshipServiceMock.refreshPendingCount.calls.reset();
    friendshipServiceMock.listenForFriendRequests.calls.reset();

    chatServiceMock.hasUnread.set(false);
    chatServiceMock.listenForMessages.calls.reset();

    sessionServiceMock.logout.calls.reset();
    locationSharingServiceMock.init.calls.reset();
    locationSharingServiceMock.reset.calls.reset();

    localStorage.removeItem('theme_test-user');
    document.body.classList.remove('light-mode');

    await TestBed.configureTestingModule({
      imports: [Taskbar],
      providers: [
        provideRouter([
          { path: 'dashboard', component: DummyComponent },
          { path: 'calendar', component: DummyComponent },
          { path: 'budget', component: DummyComponent },
          { path: 'chats', component: DummyComponent },
          { path: 'filemanager', component: DummyComponent },
          { path: 'trip-map', component: DummyComponent },
          { path: 'profile', component: DummyComponent },
          { path: 'email-costs', component: DummyComponent },
          { path: 'friends', component: DummyComponent },
          { path: 'settings', component: DummyComponent },
          { path: 'home', component: DummyComponent },
        ]),
        {
          provide: AuthService,
          useValue: authServiceMock,
        },
        {
          provide: FriendshipService,
          useValue: friendshipServiceMock,
        },
        {
          provide: ChatService,
          useValue: chatServiceMock,
        },
        {
          provide: SessionService,
          useValue: sessionServiceMock,
        },
        {
          provide: LocationSharingService,
          useValue: locationSharingServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Taskbar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.removeItem('theme_test-user');
    document.body.classList.remove('light-mode');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the app title', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Lovelace Travel');
  });

  it('should render the main desktop nav links', () => {
    const linkTexts = getLinks().map(
      (link) => link.textContent?.trim(),
    );

    expect(linkTexts).toContain('Dashboard');
    expect(linkTexts).toContain('Calendar');
    expect(linkTexts).toContain('Budget Planer');
    expect(linkTexts).toContain('Chats');
    expect(linkTexts).toContain('Filemanager');
    expect(linkTexts).toContain('Trip Map');
  });

  it('should keep tutorial data attributes on the taskbar', () => {
    expect(
      fixture.debugElement.query(
        By.css('[data-tour="taskbar"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="nav-dashboard"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="nav-calendar"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="nav-budget"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="nav-chats"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="profile-menu"]'),
      ),
    ).toBeTruthy();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="mobile-menu"]'),
      ),
    ).toBeTruthy();
  });

  it('should show friends tutorial target when profile dropdown is open', () => {
    component.toggleDropdown();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(
        By.css('[data-tour="nav-friends"]'),
      ),
    ).toBeTruthy();
  });

  it('should highlight active route', fakeAsync(() => {
    const router = TestBed.inject(Router);

    void router.navigateByUrl('/dashboard');
    tick();
    fixture.detectChanges();

    const dashboardLink = getLinkByText('Dashboard');

    expect(dashboardLink).toBeTruthy();
    expect(dashboardLink?.classList.contains('active-link')).toBeTrue();
  }));

  it('should not highlight inactive routes', fakeAsync(() => {
    const router = TestBed.inject(Router);

    void router.navigateByUrl('/dashboard');
    tick();
    fixture.detectChanges();

    const calendarLink = getLinkByText('Calendar');

    expect(calendarLink).toBeTruthy();
    expect(calendarLink?.classList.contains('active-link')).toBeFalse();
  }));

  it('should toggle dropdown', () => {
    expect(component.isDropdownOpen).toBeFalse();

    component.toggleDropdown();

    expect(component.isDropdownOpen).toBeTrue();

    component.toggleDropdown();

    expect(component.isDropdownOpen).toBeFalse();
  });

  it('should close dropdown', () => {
    component.isDropdownOpen = true;

    component.closeDropdown();

    expect(component.isDropdownOpen).toBeFalse();
  });

  it('should open dropdown when profile button is clicked', () => {
    const profileButton = fixture.debugElement.query(
      By.css('[data-tour="profile-menu"]'),
    );

    expect(profileButton).toBeTruthy();

    profileButton.triggerEventHandler(
      'click',
      new MouseEvent('click', {
        bubbles: true,
      }),
    );

    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeTrue();

    expect(
      fixture.debugElement.query(
        By.css('[data-testid="profile-dropdown-menu"]'),
      ),
    ).toBeTruthy();
  });

  it('should close dropdown when clicking outside', () => {
    component.isDropdownOpen = true;
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(
        By.css('[data-testid="profile-dropdown-menu"]'),
      ),
    ).toBeTruthy();

    document.body.dispatchEvent(
      new MouseEvent('click', {
        bubbles: true,
      }),
    );

    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeFalse();

    expect(
      fixture.debugElement.query(
        By.css('[data-testid="profile-dropdown-menu"]'),
      ),
    ).toBeNull();
  });

  it('should keep dropdown open when clicking inside the dropdown container', () => {
    component.isDropdownOpen = true;
    fixture.detectChanges();

    const dropdownContainer = fixture.debugElement.query(
      By.css('[data-testid="profile-dropdown-container"]'),
    );

    expect(dropdownContainer).toBeTruthy();

    dropdownContainer.nativeElement.dispatchEvent(
      new MouseEvent('click', {
        bubbles: true,
      }),
    );

    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeTrue();
  });

  it('should close dropdown when Escape key is pressed', () => {
    component.isDropdownOpen = true;
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(
        By.css('[data-testid="profile-dropdown-menu"]'),
      ),
    ).toBeTruthy();

    document.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: 'Escape',
        bubbles: true,
      }),
    );

    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeFalse();

    expect(
      fixture.debugElement.query(
        By.css('[data-testid="profile-dropdown-menu"]'),
      ),
    ).toBeNull();
  });

  it('should close dropdown when a profile menu link is clicked', () => {
    component.isDropdownOpen = true;
    fixture.detectChanges();

    const profileLink = getLinkByText('Profile');

    expect(profileLink).toBeTruthy();

    profileLink?.click();
    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeFalse();
  });

  it('should toggle mobile menu', () => {
    expect(component.isMobileMenuOpen).toBeFalse();

    component.toggleMobileMenu();

    expect(component.isMobileMenuOpen).toBeTrue();

    component.toggleMobileMenu();

    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should close mobile menu', () => {
    component.isMobileMenuOpen = true;

    component.closeMobileMenu();

    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should close profile dropdown when mobile menu is opened', () => {
    component.isDropdownOpen = true;
    component.isMobileMenuOpen = false;

    component.toggleMobileMenu();

    expect(component.isMobileMenuOpen).toBeTrue();
    expect(component.isDropdownOpen).toBeFalse();
  });

  it('should close mobile menu when profile dropdown is opened', () => {
    component.isMobileMenuOpen = true;
    component.isDropdownOpen = false;

    component.toggleDropdown();

    expect(component.isDropdownOpen).toBeTrue();
    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should close both menus when Escape key is pressed', () => {
    component.isDropdownOpen = true;
    component.isMobileMenuOpen = true;
    fixture.detectChanges();

    document.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: 'Escape',
        bubbles: true,
      }),
    );

    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeFalse();
    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should toggle theme', () => {
    expect(component.isLightMode).toBeFalse();

    component.toggleTheme();

    expect(component.isLightMode).toBeTrue();
    expect(localStorage.getItem('theme_test-user')).toBe('light');
    expect(document.body.classList.contains('light-mode')).toBeTrue();
  });

  it('should use the shared session logout', () => {
    component.isMobileMenuOpen = true;
    component.isDropdownOpen = true;

    component.logout();

    expect(sessionServiceMock.logout).toHaveBeenCalled();
    expect(
      locationSharingServiceMock.reset,
    ).toHaveBeenCalled();
    expect(component.isMobileMenuOpen).toBeFalse();
    expect(component.isDropdownOpen).toBeFalse();
  });

  it('should refresh pending friend request count on init', () => {
    expect(
      friendshipServiceMock.refreshPendingCount,
    ).toHaveBeenCalled();
  });

  it('should listen for friend request websocket updates on init', () => {
    expect(
      friendshipServiceMock.listenForFriendRequests,
    ).toHaveBeenCalled();
  });

  it('should listen for chat messages on init', () => {
    expect(
      chatServiceMock.listenForMessages,
    ).toHaveBeenCalled();
  });

  it('should initialize location sharing on init', () => {
    expect(
      locationSharingServiceMock.init,
    ).toHaveBeenCalled();
  });

  it('should show friend request indicator when pending requests exist', () => {
    friendshipServiceMock.pendingRequestCount.set(2);
    fixture.detectChanges();

    const profileDropdown =
      fixture.debugElement.query(
        By.css(
          '[data-testid="profile-dropdown-container"] .bg-red-500',
        ),
      );

    expect(profileDropdown).toBeTruthy();
  });

  it('should show unread chat indicator when unread chats exist', () => {
    chatServiceMock.hasUnread.set(true);
    fixture.detectChanges();

    const chatLink = fixture.debugElement.query(
      By.css('[data-tour="nav-chats"]'),
    );

    expect(
      chatLink.query(By.css('.bg-red-500')),
    ).toBeTruthy();
  });

  it('should open and close the mobile menu for the tutorial event', () => {
    spyOnProperty(
      window,
      'innerWidth',
    ).and.returnValue(500);

    window.dispatchEvent(
      new Event('tutorial-open-mobile-navbar'),
    );
    fixture.detectChanges();

    expect(component.isMobileMenuOpen).toBeTrue();

    window.dispatchEvent(
      new Event('tutorial-close-mobile-navbar'),
    );
    fixture.detectChanges();

    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should open and close the profile menu for the tutorial event', () => {
    spyOnProperty(
      window,
      'innerWidth',
    ).and.returnValue(1024);

    window.dispatchEvent(
      new Event('tutorial-open-profile-menu'),
    );
    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeTrue();

    window.dispatchEvent(
      new Event('tutorial-close-profile-menu'),
    );
    fixture.detectChanges();

    expect(component.isDropdownOpen).toBeFalse();
  });

  function getLinks(): HTMLAnchorElement[] {
    return fixture.debugElement
      .queryAll(By.css('a[routerLink]'))
      .map(
        (link) =>
          link.nativeElement as HTMLAnchorElement,
      );
  }

  function getLinkByText(
    text: string,
  ): HTMLAnchorElement | undefined {
    return getLinks().find(
      (link) => link.textContent?.trim() === text,
    );
  }
});
