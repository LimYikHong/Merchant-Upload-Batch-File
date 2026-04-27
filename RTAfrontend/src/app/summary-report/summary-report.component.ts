import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PortalService, BankSummaryReport } from '../services/portal.service';
import { ProfileService, MerchantProfile } from '../services/profile.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-summary-report',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './summary-report.component.html',
  styleUrl: './summary-report.component.scss'
})
export class SummaryReportComponent implements OnInit {
  reports: BankSummaryReport[] = [];
  merchant: MerchantProfile | null = null;
  selectedReport: BankSummaryReport | null = null;
  pdfUrl: SafeResourceUrl | null = null;
  pdfReportId: number | null = null;
  loadingPdf = false;

  constructor(
    private portalService: PortalService,
    private profileService: ProfileService,
    private authService: AuthService,
    private router: Router,
    private sanitizer: DomSanitizer
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

    this.loadReports();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  loadReports(): void {
    const merchantId = this.merchant?.merchantId;
    this.portalService.getSummaryReports(merchantId).subscribe({
      next: (data) => (this.reports = data),
      error: (err) => console.error('Failed to fetch reports', err),
    });
  }

  viewDetails(report: BankSummaryReport): void {
    this.selectedReport = this.selectedReport?.id === report.id ? null : report;
  }

  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'PROCESSED': return 'status-success';
      case 'PARTIALLY_PROCESSED': return 'status-warning';
      case 'REJECTED': return 'status-error';
      default: return 'status-default';
    }
  }

  viewPdf(report: BankSummaryReport): void {
    if (this.pdfReportId === report.id) {
      this.closePdf();
      return;
    }
    this.loadingPdf = true;
    this.pdfUrl = null;
    this.pdfReportId = report.id!;
    this.portalService.getReportPdf(report.id!).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.loadingPdf = false;
      },
      error: (err) => {
        console.error('Failed to load PDF', err);
        this.loadingPdf = false;
        this.pdfReportId = null;
      }
    });
  }

  closePdf(): void {
    this.pdfUrl = null;
    this.pdfReportId = null;
  }
}
