import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './registration.component.html'
})
export class RegistrationComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form: FormGroup = this.fb.group({
    name:     ['', [Validators.required]],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  isLoading      = false;
  errorMessage   = '';
  successMessage = '';

  get name()     { return this.form.get('name')!; }
  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading    = true;
    this.errorMessage = '';

    this.authService.register(this.form.value).subscribe({
      next: () => {
        this.isLoading      = false;
        this.successMessage = 'Account created! Redirecting to login…';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.isLoading    = false;
        this.errorMessage = err.error?.message ?? 'Registration failed. Please try again.';
      }
    });
  }
}
