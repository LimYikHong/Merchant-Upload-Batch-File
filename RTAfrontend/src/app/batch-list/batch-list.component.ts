import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { PortalService, RtaBatch } from '../services/portal.service';
import { ProfileService, MerchantProfile } from '../services/profile.service';

/**
 * BatchListComponent
 * - Shows uploaded RTA batch files for the current merchant
 * - Allows uploading a new batch file (renamed with merchantId + timestamp)
 * - Supports viewing simple batch details and deleting a batch
 * - Demonstrates Angular features: standalone component, routing links, *ngFor, pipes, service calls, error handling
 */

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="profile-container">
      <aside class="sidebar">
        <h2>RTA Merchant Upload</h2>
        <ul>
          <li routerLink="/batch-list" routerLinkActive="active">
            <b>Upload Batch File</b>
          </li>
          <li routerLink="/profile" routerLinkActive="active">View Profile</li>
        </ul>
      </aside>

      <main class="profile-content">
        <h1>Uploaded Batches</h1>

        <div class="merchant-info">
          <p>
            🧾 Merchant ID: <b>{{ merchant?.merchantId }}</b>
          </p>
          <p>
            🏢 Company: <b>{{ merchant?.company }}</b>
          </p>
          <p>
            👤 Name: <b>{{ merchant?.name }}</b>
          </p>
        </div>

        <div class="upload-section">
          <input
            type="file"
            accept=".xlsx,.xls,.csv,.txt"
            (change)="onFileSelected($event)"
          />
          <button (click)="uploadBatch()">Upload</button>
        </div>

        <table>
          <thead>
            <tr>
              <th>File Name</th>
              <th>Status</th>
              <th>Merchant</th>
              <th>Created By</th>
              <th>Created At</th>
              <th>Action</th>
            </tr>
          </thead>
         
          <tbody>
            <tr *ngFor="let batch of batches">
              <td>{{ batch.fileName }}</td>
              <td>{{ batch.status }}</td>
              <td>{{ batch.merchantId }}</td>
              <td>{{ batch.createdBy }}</td>
              <td>{{ batch.createdAt | date : 'short' }}</td>
              <td>
                <button class="btn-blue" (click)="viewBatch(batch.id!)">
                  View
                </button>
                <button class="btn-red" (click)="deleteBatch(batch.id!)">
                  Delete
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <section class="activity-log">
          <h3>Activity Log</h3>
          <ul>
            <li *ngFor="let log of activityLogs">{{ log }}</li>
          </ul>
        </section>
      </main>
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
        overflow-y: auto;
      }

      .merchant-info {
        margin-bottom: 15px;
        line-height: 1.5;
        color: #475569;
      }

      .upload-section {
        display: flex;
        gap: 10px;
        margin-bottom: 15px;
      }

      button {
        padding: 6px 12px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }

      .btn-blue {
        background-color: #3b82f6;
        color: white;
      }

      .btn-blue:hover {
        background-color: #2563eb;
      }

      .btn-red {
        background-color: #ef4444;
        color: white;
        margin-left: 5px;
      }

      .btn-red:hover {
        background-color: #dc2626;
      }

      table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 10px;
        background: white;
        border-radius: 6px;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
      }

      th,
      td {
        padding: 10px;
        border-bottom: 1px solid #e5e7eb;
        text-align: left;
      }

      th {
        background-color: #e2e8f0;
      }

      .activity-log {
        margin-top: 25px;
        background: white;
        padding: 20px;
        border-radius: 8px;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
      }
    `,
  ],
})
export class BatchListComponent implements OnInit {
  batches: RtaBatch[] = [];
  // Holds the file chosen from the input
  selectedFile?: File;
  merchant: MerchantProfile | null = null;
  activityLogs: string[] = [];

  // Inject services:
  // - PortalService: HTTP calls for batches (list/upload/delete)
  // - ProfileService: provides current merchant profile

  constructor(
    private portalService: PortalService,
    private profileService: ProfileService
  ) {}

  // read merchant profile from cache and load batch list
  ngOnInit(): void {
    this.merchant = this.profileService.getProfile();
    this.loadBatches();
  }

  // Fetch batches from backend and update table
  loadBatches(): void {
    this.portalService.getBatches().subscribe({
      next: (data) => (this.batches = data),
      error: (err) =>
        this.logActivity('Failed to fetch batches: ' + err.message),
    });
  }

  // Handle <input type="file"> change event
  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0];
  }

  // Upload selected file:
  // - Guard: require both file and merchant
  // - Rename file to {merchantId}_{yyyy-mm-dd_HH-mm-ss}.xlsx before upload
  // - Call service and refresh list; log success/error
  uploadBatch(): void {
    if (!this.selectedFile || !this.merchant) {
      alert('Missing file or merchant info.');
      return;
    }

    // Build timestamp for unique file naming
    const originalFileName = this.selectedFile.name;
    const timestamp = new Date();
    const formattedTime = `${timestamp.getFullYear()}-${(
      timestamp.getMonth() + 1
    )
      .toString()
      .padStart(2, '0')}-${timestamp
      .getDate()
      .toString()
      .padStart(2, '0')}_${timestamp
      .getHours()
      .toString()
      .padStart(2, '0')}-${timestamp
      .getMinutes()
      .toString()
      .padStart(2, '0')}-${timestamp.getSeconds().toString().padStart(2, '0')}`;

    // Create a new File object with the new name (content unchanged)
    const newFileName = `${this.merchant.merchantId}_${formattedTime}.xlsx`;
    const renamedFile = new File([this.selectedFile], newFileName, {
      type: this.selectedFile.type,
    });

    // Call upload API with renamed file, merchantId and original name for audit trail
    this.portalService
      .uploadBatch(renamedFile, this.merchant.merchantId, originalFileName)
      .subscribe({
        next: (res) => {
          this.logActivity(`File ${res.fileName} uploaded successfully`);
          this.loadBatches();
        },
        error: (err) => {
          this.logActivity(`Upload failed: ${err.message}`);
          this.loadBatches();
        },
      });
  }

  // Show a simple alert with batch details (for quick view)
  viewBatch(id: number): void {
    const batch = this.batches.find((b) => b.id === id);
    if (!batch) {
      this.logActivity(`Batch with ID ${id} not found`);
      return;
    }
    alert(
      `📄 Batch Details:\nFile: ${batch.fileName}\nMerchant: ${batch.merchantId}\nStatus: ${batch.status}`
    );
  }

  // Delete a batch by id and refresh table
  deleteBatch(id: number): void {
    this.portalService.deleteBatch(id).subscribe({
      next: () => {
        this.logActivity(`Batch ID ${id} deleted.`);
        this.loadBatches();
      },
      error: (err) =>
        this.logActivity(`Failed to delete batch: ${err.message}`),
    });
  }

  // Push a timestamped message to the top of the activity log
  private logActivity(message: string): void {
    const timestamp = new Date().toLocaleTimeString();
    this.activityLogs.unshift(`[${timestamp}] ${message}`);
  }
}
