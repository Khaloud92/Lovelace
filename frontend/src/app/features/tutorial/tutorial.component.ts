import { NgClass } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  inject,
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { TutorialService } from './tutorial.service';
import { TutorialStep } from './tutorial-step.model';

interface HighlightBox {
  top: number;
  left: number;
  width: number;
  height: number;
}

interface TooltipPosition {
  top: number;
  left: number;
}

@Component({
  selector: 'app-tutorial',
  standalone: true,
  imports: [NgClass],
  templateUrl: './tutorial.component.html',
  styleUrl: './tutorial.component.css',
})
export class TutorialComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly tutorialService = inject(TutorialService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  private readonly highlightPadding = 6;
  private readonly screenMargin = 14;
  private readonly minHighlightSize = 46;
  private readonly maxHighlightSize = 72;
  private readonly mobileBreakpoint = 768;

  private updateTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private routerSubscription?: Subscription;

  isVisible = false;
  currentStepIndex = 0;

  targetBox: HighlightBox | null = null;
  cardPosition: TooltipPosition | null = null;

  private readonly allSteps: TutorialStep[] = [
    {
      title: 'Welcome to Lovelace Travel',
      description:
        'Plan your trips, manage your budget and stay connected with your friends in one place.',
      icon: '🌍',
      centerOnly: true,
    },
    {
      title: 'Dashboard',
      description:
        'Here you get a quick overview of your trips, budgets and upcoming events.',
      icon: '📊',
      targetSelector: '[data-tour="dashboard-overview"]',
    },
    {
      title: 'Menu',
      description:
        'Open the navigation menu to access the main features of the app.',
      icon: '☰',
      targetSelector: '[data-tour="mobile-menu"]',
      mobileOnly: true,
    },
    {
      title: 'Calendar',
      description:
        'Use the calendar to organize your events and important travel dates.',
      icon: '📅',
      targetSelector: '[data-tour="nav-calendar"]',
    },
    {
      title: 'New Trip',
      description:
        'Create a new trip with destination, travel dates, description, color and budget.',
      icon: '🗺️',
      targetSelector: '[data-tour="new-trip"]',
      desktopOnly: true,
    },
    {
      title: 'Budget Planer',
      description:
        'Keep track of your planned travel budget and expected expenses.',
      icon: '💰',
      targetSelector: '[data-tour="nav-budget"]',
    },
    {
      title: 'Chats',
      description:
        'Use chats to communicate with your friends while planning trips together.',
      icon: '💬',
      targetSelector: '[data-tour="nav-chats"]',
    },
    {
      title: 'Profile',
      description:
        'Open your profile and account-related options from here.',
      icon: '👤',
      targetSelector: '[data-tour="profile-menu"]',
      mobileTargetSelector: '[data-tour="mobile-profile-menu"]',
    },
    {
      title: 'Friends',
      description:
        'Add friends, accept friend requests and plan trips together with them.',
      icon: '👥',
      targetSelector: '[data-tour="nav-friends"]',
      mobileTargetSelector: '[data-tour="mobile-nav-friends"]',
    },
  ];

  ngOnInit(): void {
    this.updateTutorialVisibility();

    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateTutorialVisibility();
      });
  }

  ngAfterViewInit(): void {
    if (this.isVisible) {
      this.scheduleTargetUpdate(120);
    }
  }

  ngOnDestroy(): void {
    this.clearScheduledUpdate();
    this.dispatchCloseMobileNavbarEvent();
    this.dispatchCloseProfileMenuEvent();
    this.routerSubscription?.unsubscribe();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.keepCurrentStepInRange();
    this.scheduleTargetUpdate();
  }

  @HostListener('window:scroll')
  onScroll(): void {
    if (this.isVisible) {
      this.scheduleTargetUpdate();
    }
  }

  get steps(): TutorialStep[] {
    const isMobile = this.isMobileView();

    return this.allSteps.filter((step) => {
      if (isMobile) {
        return !step.desktopOnly;
      }

      return !step.mobileOnly;
    });
  }

  get currentStep(): TutorialStep {
    return this.steps[this.currentStepIndex];
  }

  get isFirstStep(): boolean {
    return this.currentStepIndex === 0;
  }

  get isLastStep(): boolean {
    return this.currentStepIndex === this.steps.length - 1;
  }

  nextStep(): void {
    if (this.isLastStep) {
      this.closeTutorial();
      return;
    }

    this.currentStepIndex++;
    this.scheduleTargetUpdate(80);
  }

  previousStep(): void {
    if (this.isFirstStep) {
      return;
    }

    this.currentStepIndex--;
    this.scheduleTargetUpdate(80);
  }

  skipTutorial(): void {
    this.closeTutorial();
  }

  private updateTutorialVisibility(): void {
    const currentUrl = this.router.url;
    const isDashboardPage = currentUrl.startsWith('/dashboard');

    if (!isDashboardPage) {
      this.hideTutorialTemporarily();
      return;
    }

    this.keepCurrentStepInRange();
    this.isVisible = this.tutorialService.shouldShowTutorial();

    if (this.isVisible) {
      this.scheduleTargetUpdate(120);
    }
  }

  private hideTutorialTemporarily(): void {
    this.clearScheduledUpdate();
    this.dispatchCloseMobileNavbarEvent();
    this.dispatchCloseProfileMenuEvent();
    this.targetBox = null;
    this.cardPosition = null;
    this.isVisible = false;
    this.changeDetectorRef.detectChanges();
  }

  private closeTutorial(): void {
    this.clearScheduledUpdate();
    this.dispatchCloseMobileNavbarEvent();
    this.dispatchCloseProfileMenuEvent();
    this.tutorialService.markTutorialAsSeen();
    this.targetBox = null;
    this.cardPosition = null;
    this.isVisible = false;
  }

  private scheduleTargetUpdate(delay = 60): void {
    this.clearScheduledUpdate();

    this.updateTimeoutId = setTimeout(() => {
      if (!this.isVisible) {
        return;
      }

      this.prepareMenusIfNeeded();

      const updateDelay =
        this.shouldOpenMobileNavbarForCurrentStep() ||
        this.shouldOpenProfileMenuForCurrentStep()
          ? 160
          : 0;

      setTimeout(() => {
        this.updateTargetPosition();
      }, updateDelay);
    }, delay);
  }

  private clearScheduledUpdate(): void {
    if (this.updateTimeoutId) {
      clearTimeout(this.updateTimeoutId);
      this.updateTimeoutId = null;
    }
  }

  private prepareMenusIfNeeded(): void {
    if (this.shouldOpenMobileNavbarForCurrentStep()) {
      this.dispatchOpenMobileNavbarEvent();
    } else {
      this.dispatchCloseMobileNavbarEvent();
    }

    if (this.shouldOpenProfileMenuForCurrentStep()) {
      this.dispatchOpenProfileMenuEvent();
    } else {
      this.dispatchCloseProfileMenuEvent();
    }
  }

  private shouldOpenMobileNavbarForCurrentStep(): boolean {
    const selector = this.getCurrentTargetSelector();

    return (
      this.isMobileView() &&
      !!selector &&
      this.isNavbarTarget(selector)
    );
  }

  private shouldOpenProfileMenuForCurrentStep(): boolean {
    const selector = this.getCurrentTargetSelector();

    return (
      !this.isMobileView() &&
      selector === '[data-tour="nav-friends"]'
    );
  }

  private isNavbarTarget(selector: string): boolean {
    return [
      '[data-tour="nav-calendar"]',
      '[data-tour="nav-budget"]',
      '[data-tour="nav-chats"]',
      '[data-tour="mobile-profile-menu"]',
      '[data-tour="mobile-nav-friends"]',
    ].includes(selector);
  }

  private dispatchOpenMobileNavbarEvent(): void {
    window.dispatchEvent(new Event('tutorial-open-mobile-navbar'));
  }

  private dispatchCloseMobileNavbarEvent(): void {
    window.dispatchEvent(new Event('tutorial-close-mobile-navbar'));
  }

  private dispatchOpenProfileMenuEvent(): void {
    window.dispatchEvent(new Event('tutorial-open-profile-menu'));
  }

  private dispatchCloseProfileMenuEvent(): void {
    window.dispatchEvent(new Event('tutorial-close-profile-menu'));
  }

  private updateTargetPosition(): void {
    if (!this.isVisible) {
      return;
    }

    if (this.currentStep.centerOnly) {
      this.targetBox = null;
      this.cardPosition = this.getCenteredCardPosition();
      this.changeDetectorRef.detectChanges();
      return;
    }

    const targetSelector = this.getCurrentTargetSelector();

    if (!targetSelector) {
      this.targetBox = null;
      this.cardPosition = this.getCenteredCardPosition();
      this.changeDetectorRef.detectChanges();
      return;
    }

    const targetElement = this.findVisibleTarget(targetSelector);

    if (!targetElement) {
      this.targetBox = null;
      this.cardPosition = this.getCenteredCardPosition();
      this.changeDetectorRef.detectChanges();
      return;
    }

    targetElement.scrollIntoView({
      behavior: 'auto',
      block: 'center',
      inline: 'center',
    });

    setTimeout(() => {
      const rect = targetElement.getBoundingClientRect();

      const circleSize = this.clamp(
        Math.max(
          Math.min(rect.width, rect.height) + this.highlightPadding * 2,
          this.minHighlightSize
        ),
        this.minHighlightSize,
        this.maxHighlightSize
      );

      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      this.targetBox = {
        top: this.clamp(
          centerY - circleSize / 2,
          this.screenMargin,
          window.innerHeight - circleSize - this.screenMargin
        ),
        left: this.clamp(
          centerX - circleSize / 2,
          this.screenMargin,
          window.innerWidth - circleSize - this.screenMargin
        ),
        width: circleSize,
        height: circleSize,
      };

      this.cardPosition = this.calculateCardPosition(rect);
      this.changeDetectorRef.detectChanges();
    }, 60);
  }

  private getCurrentTargetSelector(): string | null {
    if (this.currentStep.centerOnly) {
      return null;
    }

    if (this.isMobileView() && this.currentStep.mobileTargetSelector) {
      return this.currentStep.mobileTargetSelector;
    }

    return this.currentStep.targetSelector ?? null;
  }

  private findVisibleTarget(selector: string): HTMLElement | null {
    const elements = Array.from(document.querySelectorAll<HTMLElement>(selector));

    return (
      elements.find((element) => {
        const rect = element.getBoundingClientRect();
        const style = window.getComputedStyle(element);

        return (
          rect.width > 0 &&
          rect.height > 0 &&
          style.display !== 'none' &&
          style.visibility !== 'hidden' &&
          style.opacity !== '0'
        );
      }) ?? null
    );
  }

  private calculateCardPosition(targetRect: DOMRect): TooltipPosition {
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const cardWidth = Math.min(320, viewportWidth - this.screenMargin * 2);
    const estimatedCardHeight = 205;

    if (this.isMobileView()) {
      const left = this.clamp(
        targetRect.left,
        this.screenMargin,
        viewportWidth - cardWidth - this.screenMargin
      );

      const belowTarget = targetRect.bottom + 12;
      const aboveTarget = targetRect.top - estimatedCardHeight - 12;

      let top: number;

      if (belowTarget + estimatedCardHeight <= viewportHeight - this.screenMargin) {
        top = belowTarget;
      } else if (aboveTarget >= this.screenMargin) {
        top = aboveTarget;
      } else {
        top = this.screenMargin;
      }

      return {
        top,
        left,
      };
    }

    if (this.currentStep.title === 'Dashboard') {
      return {
        top: this.clamp(
          targetRect.bottom + 14,
          this.screenMargin,
          viewportHeight - estimatedCardHeight - this.screenMargin
        ),
        left: this.clamp(
          targetRect.left,
          this.screenMargin,
          viewportWidth - cardWidth - this.screenMargin
        ),
      };
    }

    const centeredLeft = targetRect.left + targetRect.width / 2 - cardWidth / 2;

    const left = this.clamp(
      centeredLeft,
      this.screenMargin,
      viewportWidth - cardWidth - this.screenMargin
    );

    const belowTarget = targetRect.bottom + 14;
    const aboveTarget = targetRect.top - estimatedCardHeight - 14;

    let top: number;

    if (belowTarget + estimatedCardHeight <= viewportHeight - this.screenMargin) {
      top = belowTarget;
    } else if (aboveTarget >= this.screenMargin) {
      top = aboveTarget;
    } else {
      top = this.screenMargin;
    }

    return {
      top,
      left,
    };
  }

  private getCenteredCardPosition(): TooltipPosition {
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const cardWidth = Math.min(320, viewportWidth - this.screenMargin * 2);
    const estimatedCardHeight = 220;

    return {
      top: this.clamp(
        viewportHeight / 2 - estimatedCardHeight / 2,
        this.screenMargin,
        viewportHeight - estimatedCardHeight - this.screenMargin
      ),
      left: this.clamp(
        viewportWidth / 2 - cardWidth / 2,
        this.screenMargin,
        viewportWidth - cardWidth - this.screenMargin
      ),
    };
  }

  private keepCurrentStepInRange(): void {
    if (this.currentStepIndex >= this.steps.length) {
      this.currentStepIndex = this.steps.length - 1;
    }

    if (this.currentStepIndex < 0) {
      this.currentStepIndex = 0;
    }
  }

  private isMobileView(): boolean {
    return window.innerWidth < this.mobileBreakpoint;
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }
}
