import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MerchantProfile } from './profile.service';

@Injectable({
  providedIn: 'root',
})

/**
 * AuthService
 * - Handles login/logout and simple session helpers.
 * - Stores the authenticated merchant in localStorage under 'merchant'.
 */
export class AuthService {
  private apiUrl = 'http://localhost:8088/api/auth';

  constructor(private http: HttpClient) {}

  /**
   * POST /api/auth/login
   * - Sends credentials and expects a MerchantProfile on success.
   * - Caller should persist the returned profile (e.g., localStorage) if needed.
   */
  login(username: string, password: string): Observable<MerchantProfile> {
    return this.http.post<MerchantProfile>(`${this.apiUrl}/login`, {
      username,
      password,
    });
  }

  /**
   * Clears the local session.
   * - Removes 'merchant' from localStorage.
   * - Caller can also clear other caches if used elsewhere.
   */

  logout(): void {
    localStorage.removeItem('merchant');
  }

  /**
   * Quick boolean flag for route guards/UI.
   * - True if 'merchant' exists in localStorage.
   * - Does not validate token/expiry (demo-friendly).
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('merchant');
  }

  /**
   * Reads the current user from localStorage.
   * - Returns parsed MerchantProfile or null if missing.
   */
  getCurrentUser(): MerchantProfile | null {
    const data = localStorage.getItem('merchant');
    return data ? (JSON.parse(data) as MerchantProfile) : null;
  }
}
