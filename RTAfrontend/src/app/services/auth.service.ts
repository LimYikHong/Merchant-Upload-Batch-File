import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MerchantProfile } from './profile.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8088/api/auth';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<MerchantProfile> {
    return this.http.post<MerchantProfile>(`${this.apiUrl}/login`, { username, password });
  }

  logout(): void {
    localStorage.removeItem('merchant');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('merchant');
  }

  getCurrentUser(): MerchantProfile | null {
    const data = localStorage.getItem('merchant');
    return data ? JSON.parse(data) as MerchantProfile : null;
  }
}
