import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { PortalService, ReturnBatchFile } from '../services/portal.service';
import { ProfileService, MerchantProfile } from '../services/profile.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-incoming-batch',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './incoming-batch.component.html',
  styleUrl: './incoming-batch.component.scss',
})
export class IncomingBatchComponent implements OnInit {
  returnBatches: ReturnBatchFile[] = [];
  merchant: MerchantProfile | null = null;

  constructor(
    private portalService: PortalService,
    private profileService: ProfileService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.merchant = this.profileService.getProfile();

    if (this.merchant && this.merchant.merchantId) {
      this.profileService.fetchProfile(this.merchant.merchantId).subscribe({
        next: (profile) => {
          this.merchant = profile;
        },
        error: (err) => console.error('Failed to refresh profile', err),
      });
    }

    this.loadReturnBatches();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  loadReturnBatches(): void {
    const merchantId = this.merchant?.merchantId;
    this.portalService.getReturnBatches(merchantId).subscribe({
      next: (data) => (this.returnBatches = data),
      error: (err) => console.error('Failed to fetch return batches', err),
    });
  }

  downloadFile(batch: ReturnBatchFile): void {
    if (!batch.id) return;
    this.portalService.downloadReturnBatch(batch.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = batch.originalFileName || batch.returnFileName;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Failed to download file', err),
    });
  }

  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'RECEIVED':
        return 'status-success';
      case 'PROCESSED':
        return 'status-success';
      case 'ERROR':
        return 'status-error';
      default:
        return 'status-default';
    }
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }
}
