import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Taskbar } from './shared/taskbar/taskbar';
import { CommonModule } from '@angular/common';
import { TutorialComponent } from './features/tutorial/tutorial.component';
import { filter } from 'rxjs/operators';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Taskbar, TutorialComponent, CommonModule, ReactiveFormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('sep');

  private readonly router = inject(Router);

  showTaskbar = false;

  private readonly authRoutes = ['/login', '/register', '/home'];

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const url = (event as NavigationEnd).urlAfterRedirects;
        this.showTaskbar = !this.authRoutes.some((route) => url.startsWith(route));
      });
  }
}
