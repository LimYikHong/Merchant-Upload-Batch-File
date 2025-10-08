import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap, throwError } from 'rxjs';

export interface MerchantProfile {
  merchantId: string;
  name: string;
  email: string;
  company: string;
  contact: string;
  address: string;
  joinedOn?: string;
  username?: string;
  password?: string;
  profilePhotoUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private apiUrl = 'http://localhost:8088/api/profile';
  private cachedProfile: MerchantProfile | null = null;

  constructor(private http: HttpClient) {}

  fetchProfile(merchantId: string): Observable<MerchantProfile> {
    return this.http.get<MerchantProfile>(`${this.apiUrl}/${merchantId}`).pipe(
      tap((profile) => this.setProfile(profile)),
      catchError((err) => {
        console.error('❌ Failed to fetch profile:', err);
        return of(this.emptyProfile());
      })
    );
  }

  updateProfile(merchantId: string, updatedProfile: MerchantProfile): Observable<MerchantProfile> {
    return this.http.put<MerchantProfile>(`${this.apiUrl}/${merchantId}`, updatedProfile).pipe(
      tap((profile) => {
        this.setProfile(profile);
        console.log('✅ Profile updated successfully');
      }),
      catchError((err) => {
        console.error('❌ Failed to update profile:', err);
        return throwError(() => err);
      })
    );
  }

  uploadProfilePhoto(merchantId: string, file: File): Observable<MerchantProfile> {
    const formData = new FormData();
    formData.append('profilePhoto', file);

    return this.http.post<MerchantProfile>(`${this.apiUrl}/${merchantId}/photo`, formData).pipe(
      tap((profile) => {
        this.setProfile(profile);
        console.log('✅ Photo uploaded successfully');
      }),
      catchError((err) => {
        console.error('❌ Failed to upload photo:', err);
        return throwError(() => err);
      })
    );
  }

  setProfile(profile: MerchantProfile): void {
    this.cachedProfile = profile;
    localStorage.setItem('merchantProfile', JSON.stringify(profile));
  }

getProfile(): MerchantProfile {
  return this.cachedProfile ?? this.emptyProfile();
}


  clearProfile(): void {
    this.cachedProfile = null;
    localStorage.removeItem('merchantProfile');
  }

  private emptyProfile(): MerchantProfile {
    return {
      merchantId: '',
      name: '',
      email: '',
      company: '',
      contact: '',
      address: '',
      joinedOn: '',
      username: '',
      password: '',
      profilePhotoUrl: '',
    };
  }
}
