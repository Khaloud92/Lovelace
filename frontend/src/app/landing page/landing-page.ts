import { Component, AfterViewInit, OnInit, OnDestroy, ElementRef, inject, PLATFORM_ID, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterModule } from '@angular/router';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import '@splinetool/viewer';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './landing-page.html'
})
export class LandingPageComponent implements OnInit, AfterViewInit, OnDestroy {
  private platformId = inject(PLATFORM_ID);
  private el = inject(ElementRef);

  // The landing page is always dark-themed; remember the app's theme so it can be restored on leave.
  private wasLightMode = false;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.wasLightMode = document.body.classList.contains('light-mode');
      document.body.classList.remove('light-mode');
      document.body.classList.add('dark');
    }
  }

  ngOnDestroy(): void {
    if (isPlatformBrowser(this.platformId) && this.wasLightMode) {
      document.body.classList.add('light-mode');
      document.body.classList.remove('dark');
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      gsap.registerPlugin(ScrollTrigger);
      this.initAnimations();
    }
  }

  private initAnimations(): void {
    gsap.to('.intro-section', {
      scrollTrigger: {
        trigger: '.intro-section',
        start: 'top top',
        end: 'bottom top',
        scrub: 1
      },
      y: 100,
      opacity: 0,
      ease: 'none'
    });

    const logoTl = gsap.timeline({
      scrollTrigger: {
        trigger: '.marquee-section',
        start: 'top bottom',
        endTrigger: '.horizontal-wrapper',
        end: 'top 50%',
        scrub: 1
      }
    });

    logoTl.to('#flying-logo', {
      top: '18vh',
      scale: 3.5,
      ease: 'power2.out'
    })
      .to('#flying-logo', {
        top: '0.75rem',
        scale: 1,
        opacity: 0,
        ease: 'power2.inOut'
      });

    const marquees = this.el.nativeElement.querySelectorAll('.city-marquee');
    const marqueeTweens: gsap.core.Tween[] = [];

    marquees.forEach((marquee: HTMLElement, index: number) => {
      const tween = gsap.to(marquee, {
        xPercent: -50,
        repeat: -1,
        duration: 20 + (index * 3),
        ease: 'none'
      });
      marqueeTweens.push(tween);
    });

    ScrollTrigger.create({
      trigger: 'body',
      start: 'top top',
      end: 'bottom bottom',
      onUpdate: (self) => {
        marqueeTweens.forEach(tween => {
          gsap.to(tween, {
            timeScale: self.direction === 1 ? 1 : -1,
            duration: 0.5,
            overwrite: true
          });
        });
      }
    });

    const horizontalContainer = this.el.nativeElement.querySelector('.horizontal-container');
    const panels = gsap.utils.toArray('.panel');

    gsap.to(panels, {
      xPercent: -100 * (panels.length - 1),
      ease: "none",
      scrollTrigger: {
        trigger: ".horizontal-wrapper",
        pin: true,
        scrub: 1,
        snap: 1 / (panels.length - 1),
        end: () => "+=" + horizontalContainer.offsetWidth
      }
    });

    gsap.from('.bento-item', {
      scrollTrigger: {
        trigger: '.bento-section',
        start: 'top 70%',
        toggleActions: 'play none none reverse'
      },
      y: 100,
      opacity: 0,
      duration: 0.8,
      stagger: 0.2,
      ease: 'power3.out'
    });

    gsap.to('.cta-content', {
      scrollTrigger: {
        trigger: '.cta-section',
        start: 'top bottom',
        end: 'center center',
        scrub: 1
      },
      scale: 1,
      opacity: 1,
      y: -50,
      ease: 'none'
    });
  }
}
