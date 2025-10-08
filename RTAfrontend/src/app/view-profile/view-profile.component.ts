import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProfileService, MerchantProfile } from '../services/profile.service';

declare var bootstrap: any;

@Component({
  selector: 'app-view-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="profile-container">
      <!-- Sidebar -->
      <aside class="sidebar">
        <h2>RTA Merchant Upload</h2>
        <ul>
          <li routerLink="/batch-list" routerLinkActive="active">
            <b>Upload Batch File</b>
          </li>
          <li routerLink="/profile" routerLinkActive="active">View Profile</li>
        </ul>
      </aside>

      <!-- Profile Content -->
      <main class="profile-content" *ngIf="profile; else loading">
        <div class="profile-header">
          <div class="avatar" (click)="openPhotoUpload()">
            <img
              *ngIf="profile.profilePhotoUrl"
              [src]="'http://localhost:8088' + profile.profilePhotoUrl"
              alt="Profile Photo"
              class="profile-photo"
            />
            <span *ngIf="!profile.profilePhotoUrl">{{
              profile.name.charAt(0)
            }}</span>
            <div class="edit-overlay">
              <i class="fas fa-pencil-alt"></i>
            </div>
            <input
              type="file"
              #fileInput
              style="display: none"
              (change)="onFileSelected($event)"
              accept="image/*"
            />
          </div>
          <div class="header-info">
            <h1>{{ profile.name }}</h1>
            <p class="email">{{ profile.email }}</p>
            <button class="edit-btn" (click)="openEditModal()">Edit Profile</button>
          </div>
        </div>

        <div class="profile-details">
          <h2>Profile Information</h2>
          <div class="details-grid">
            <div class="detail-item">
              <label>Merchant ID:</label><span>{{ profile.merchantId }}</span>
            </div>
            <div class="detail-item">
              <label>Company:</label><span>{{ profile.company }}</span>
            </div>
            <div class="detail-item">
              <label>Contact Number:</label><span>{{ profile.contact }}</span>
            </div>
            <div class="detail-item">
              <label>Joined On:</label><span>{{ profile.joinedOn }}</span>
            </div>
            <div class="detail-item">
              <label>Address:</label><span>{{ profile.address }}</span>
            </div>
          </div>
        </div>
      </main>

      <!-- Loading -->
      <ng-template #loading>
        <div class="loading-text">Loading profile...</div>
      </ng-template>
    </div>

    <!-- Edit Modal -->
    <div
      class="modal fade"
      id="editProfileModal"
      tabindex="-1"
      aria-labelledby="editProfileModalLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="editProfileModalLabel">Edit Profile</h5>
            <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Close"
            ></button>
          </div>

          <div class="modal-body">
            <form #editForm="ngForm">
              <div class="mb-3">
                <label class="form-label">Company</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editData.company"
                  name="company"
                  required
                />
              </div>

              <div class="mb-3">
                <label class="form-label">Contact Number</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editData.contact"
                  name="contact"
                  required
                />
              </div>

              <div class="mb-3">
                <label class="form-label">Address</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editData.address"
                  name="address"
                  required
                />
              </div>

              <div class="mb-3" *ngIf="previewUrl">
                <label class="form-label">Profile Photo</label>
                <div class="photo-preview">
                  <img [src]="previewUrl" alt="Preview" class="img-thumbnail" />
                </div>
              </div>
            </form>
          </div>

          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-secondary"
              data-bs-dismiss="modal"
            >
              Cancel
            </button>
            <button
              type="button"
              class="btn btn-primary"
              (click)="updateProfile()"
            >
              Save Changes
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .profile-container {
        display: flex;
        min-height: 100vh;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        background-color: #f9fafb;
      }

      .sidebar {
        background-color: #1e2a38;
        color: white;
        width: 220px;
        height: 100vh;
        padding: 30px 20px;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
      }

      .sidebar h2 {
        font-size: 20px;
        font-weight: bold;
        margin-bottom: 30px;
        border-bottom: 1px solid #334155;
        padding-bottom: 10px;
        text-transform: uppercase;
        width: 100%;
        text-align: center;
      }

      .sidebar li {
        margin: 15px 0;
        cursor: pointer;
        color: #e2e8f0;
        padding: 5px 10px;
        border-radius: 4px;
      }

      .sidebar li:hover,
      .sidebar li.active {
        color: #60a5fa;
        background-color: #243447;
      }

      .profile-content {
        flex: 1;
        padding: 40px 60px;
      }

      .profile-header {
        display: flex;
        align-items: center;
        background: white;
        padding: 30px;
        border-radius: 10px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        margin-bottom: 30px;
      }

      .avatar {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background-color: #3b82f6;
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 36px;
        font-weight: bold;
        margin-right: 25px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
      }

      .avatar:hover .edit-overlay {
        opacity: 1;
      }

      .edit-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        opacity: 0;
        transition: opacity 0.3s;
      }

      .profile-photo {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .edit-btn {
        background-color: #3b82f6;
        color: white;
        border: none;
        padding: 8px 16px;
        border-radius: 5px;
        cursor: pointer;
      }

      .edit-btn:hover {
        background-color: #2563eb;
      }

      .profile-details {
        background: white;
        padding: 30px;
        border-radius: 10px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }

      .details-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 15px 50px;
      }

      .detail-item label {
        font-weight: bold;
        color: #334155;
      }

      .loading-text {
        font-size: 18px;
        color: #475569;
        text-align: center;
        margin-top: 100px;
      }

      .photo-preview {
        display: flex;
        justify-content: center;
        align-items: center;
        margin-top: 10px;
      }

      .img-thumbnail {
        max-width: 100%;
        height: auto;
        border: 1px solid #ddd;
        border-radius: 4px;
        padding: 5px;
        background-color: white;
      }
    `,
  ],
})
export class ViewProfileComponent implements OnInit {
  profile: MerchantProfile | null = null;
  editData: MerchantProfile = {
    merchantId: '',
    name: '',
    email: '',
    company: '',
    contact: '',
    address: '',
    joinedOn: '',
    profilePhotoUrl: '',
  };
  selectedFile: File | null = null;
  previewUrl: string | ArrayBuffer | null = null;

  @ViewChild('fileInput') fileInput!: ElementRef;

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
    this.profile = this.profileService.getProfile();
  }

  loadProfile(merchantId: string): void {
    this.profileService.fetchProfile(merchantId).subscribe({
      next: (data) => {
        this.profile = data;
        this.profileService.setProfile(data);
      },
      error: (err) => {
        console.error('❌ Failed to load profile:', err);
        alert('Failed to fetch profile data from server.');
      },
    });
  }

  openPhotoUpload() {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => (this.previewUrl = e.target.result);
      reader.readAsDataURL(file);
      this.openEditModal();
    }
  }

  openEditModal(): void {
    if (!this.profile) return;
    this.editData = { ...this.profile };
    const modalEl = document.getElementById('editProfileModal');
    const modal = new bootstrap.Modal(modalEl!);
    modal.show();
  }

  updateProfile(): void {
    if (!this.editData) return;

    this.profileService
      .updateProfile(this.editData.merchantId, this.editData)
      .subscribe({
        next: (res: MerchantProfile) => {
          if (this.selectedFile) {
            this.uploadPhoto(this.editData.merchantId);
          } else {
            this.profile = res;
            this.profileService.setProfile(res);
            const modalEl = document.getElementById('editProfileModal');
            const modal = bootstrap.Modal.getInstance(modalEl!);
            modal.hide();
            alert('✅ Profile updated successfully!');
          }
        },
        error: (err: any) => {
          console.error('❌ Update failed:', err);
          alert('Failed to update profile!');
        },
      });
  }

  uploadPhoto(merchantId: string): void {
    if (!this.selectedFile) return;

    this.profileService.uploadProfilePhoto(merchantId, this.selectedFile).subscribe({
      next: (res: MerchantProfile) => {
        this.profile = res;
        this.profileService.setProfile(res);
        this.selectedFile = null;
        this.previewUrl = null;
        const modalEl = document.getElementById('editProfileModal');
        const modal = bootstrap.Modal.getInstance(modalEl!);
        modal.hide();
        alert('✅ Profile updated successfully!');
      },
      error: (err: any) => {
        console.error('❌ Photo upload failed:', err);
        alert('Failed to upload photo!');
      },
    });
  }
}
