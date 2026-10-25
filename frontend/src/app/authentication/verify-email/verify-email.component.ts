import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './verify-email.component.html'
})
export class VerifyEmailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private router = inject(Router);

  status: 'loading' | 'success' | 'error' = 'loading';
  message = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.status = 'error';
      this.message = 'Invalid verification link.';
      return;
    }

    this.http.get<{ message: string }>(`http://localhost:8080/api/registration/confirm?token=${token}`)
      .subscribe({
        next: (res) => {
          this.status = 'success';
          this.message = res.message;
        },
        error: (err) => {
          this.status = 'error';
          this.message = err.error?.message ?? 'Verification failed.';
        }
      });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
