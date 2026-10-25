import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideLocationMocks } from '@angular/common/testing';
import { routes } from './app.routes';

describe('AppRouting', () => {
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes.map(r => ({ ...r, canActivate: undefined }))),
        provideLocationMocks()
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should navigate to /home', async () => {
    await router.navigate(['/home']);
    expect(router.url).toBe('/home');
  });

  it('should navigate to /dashboard', async () => {
    await router.navigate(['/dashboard']);
    expect(router.url).toBe('/dashboard');
  });

  it('should navigate to /calendar', async () => {
    await router.navigate(['/calendar']);
    expect(router.url).toBe('/calendar');
  });

  it('should navigate to /budget', async () => {
    await router.navigate(['/budget']);
    expect(router.url).toBe('/budget');
  });

  it('should redirect empty path to /home', async () => {
    await router.navigate(['']);
    expect(router.url).toBe('/home');
  });

  it('should redirect unknown routes to /home', async () => {
    await router.navigate(['/irgendwas-unbekanntes']);
    expect(router.url).toBe('/home');
  });
});
