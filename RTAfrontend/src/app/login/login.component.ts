import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ProfileService } from '../services/profile.service';

/**
 * LoginComponent
 * - Simple merchant sign-in form using template-driven forms (ngModel)
 * - Calls AuthService.login(username, password) and stores profile on success
 * - Navigates to /batch-list after successful login
 * - Shows basic inline error when validation/auth fails
 * - Demonstrates: standalone component, two-way binding, form submit, routing, localStorage
 */

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h2>Merchant Login</h2>
        <form (ngSubmit)="onLogin()">
          <input
            type="text"
            placeholder="Username"
            [(ngModel)]="username"
            name="username"
            required
          />
          <input
            type="password"
            placeholder="Password"
            [(ngModel)]="password"
            name="password"
            required
          />
          <button type="submit">Login</button>
        </form>
        <p *ngIf="errorMessage" class="error">{{ errorMessage }}</p>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f9fafb;
    }

    .login-card {
      background: white;
      padding: 40px 30px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      width: 360px;
      text-align: center;
    }

    h2 {
      margin-bottom: 25px;
      font-size: 24px;
      color: #1e293b;
      font-weight: 600;
    }

    input {
      display: block;
      width: 100%;
      margin: 12px 0;
      padding: 10px;
      border-radius: 6px;
      border: 1px solid #cbd5e1;
      font-size: 15px;
      outline: none;
      transition: border-color 0.2s;
    }

    input:focus {
      border-color: #3b82f6;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
    }

    button {
      margin-top: 15px;
      background-color: #3b82f6;
      color: white;
      border: none;
      padding: 10px;
      border-radius: 6px;
      cursor: pointer;
      width: 100%;
      font-size: 16px;
      font-weight: 500;
    }

    button:hover {
      background-color: #2563eb;
    }

    .error {
      color: red;
      margin-top: 15px;
      font-size: 14px;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';

  // Inject auth flow + navigation + profile caching
  constructor(
    private auth: AuthService,
    private router: Router,
    private profileService: ProfileService
  ) {}

   /**
   * Handles form submit:
   * - Basic empty-field guard
   * - Calls AuthService.login and stores the returned profile
   * - Persists profile in localStorage (simple session) and navigates to batch list
   * - Sets an error message on login failure
   */

  onLogin(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password.';
      return;
    }

    // Call backend via AuthService; expect a MerchantProfile on success
    this.auth.login(this.username, this.password).subscribe({
      next: (profile) => {
        this.profileService.setProfile(profile);
        localStorage.setItem('merchant', JSON.stringify(profile));
        this.router.navigate(['/batch-list']);
      },
      error: () => {
        this.errorMessage = 'Invalid username or password';
      }
    });
  }
}
