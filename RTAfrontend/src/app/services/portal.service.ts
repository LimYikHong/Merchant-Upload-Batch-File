import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * RtaBatch
 * - Minimal shape for batch list, upload result, etc.
 * - Optional fields (id/createdBy/createdAt) are filled by backend.
 */
export interface RtaBatch {
  id?: number;
  fileName: string;
  status: string;
  merchantId: string;
  createdBy?: string;
  createdAt?: string;
}

export interface BankSummaryReport {
  id?: number;
  batchId?: number;
  merchantId: string;
  fileName: string;
  originalFileName?: string;
  bankStatus: string;
  totalTransactions?: number;
  successfulTransactions?: number;
  failedTransactions?: number;
  totalAmount?: number;
  currency?: string;
  bankReference?: string;
  remarks?: string;
  processedAt?: string;
  receivedAt?: string;
}

export interface ReturnBatchFile {
  id?: number;
  batchId?: number;
  merchantId: string;
  originalFileName?: string;
  returnFileName: string;
  fileSize?: number;
  status: string;
  remarks?: string;
  receivedAt?: string;
}

@Injectable({
  providedIn: 'root',
})

/**
 * PortalService
 * - Wraps HTTP calls for batch operations: list, upload, process, delete.
 * - Keeps API URLs centralized and easy to change.
 */
export class PortalService {
  private apiUrl = 'https://localhost:8881/api/batches';
  private reportsUrl = 'https://localhost:8881/api/reports';
  private returnBatchesUrl = 'https://localhost:8881/api/return-batches';

  constructor(private http: HttpClient) {}
  /**
   * GET /api/batches?merchantId=xxx
   * - Fetch batches for the current merchant.
   */
  getBatches(merchantId?: string): Observable<RtaBatch[]> {
    const params = merchantId ? `?merchantId=${merchantId}` : '';
    return this.http.get<RtaBatch[]>(`${this.apiUrl}${params}`);
  }

  /**
   * GET /api/batches/activity?merchantId=xxx
   * - Fetch activity logs for the current merchant.
   */
  getActivityLogs(merchantId?: string): Observable<string[]> {
    const params = merchantId ? `?merchantId=${merchantId}` : '';
    return this.http.get<string[]>(`${this.apiUrl}/activity${params}`);
  }
  /**
   * POST /api/batches/upload
   * - Uploads a batch file using multipart/form-data.
   * - Includes merchantId and original file name for audit trail.
   * - Returns the created RtaBatch metadata from backend.
   */
  uploadBatch(
    file: File,
    merchantId: string,
    originalFileName: string
  ): Observable<RtaBatch> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('merchantId', merchantId);
    formData.append('originalFileName', originalFileName);
    return this.http.post<RtaBatch>(`${this.apiUrl}/upload`, formData);
  }
  /**
   * POST /api/batches/{id}/process
   * - Triggers server-side processing of a specific batch.
   * - Expects plain text response (status/summary).
   */
  processBatch(id: number): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/${id}/process`,
      {},
      { responseType: 'text' }
    );
  }
  /**
   * POST /api/batches/{id}/send-to-bank
   * - Sends the uploaded batch file to the bank's HTTPS API.
   */
  sendToBank(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/send-to-bank`, {});
  }

  /**
   * DELETE /api/batches/{id}
   * - Removes a batch by id.
   */
  deleteBatch(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  /**
   * GET /api/reports?merchantId=xxx
   * - Fetch bank summary reports for the current merchant.
   */
  getSummaryReports(merchantId?: string): Observable<BankSummaryReport[]> {
    const params = merchantId ? `?merchantId=${merchantId}` : '';
    return this.http.get<BankSummaryReport[]>(`${this.reportsUrl}${params}`);
  }

  /**
   * GET /api/reports/{id}
   * - Fetch a single report by ID.
   */
  getSummaryReportById(id: number): Observable<BankSummaryReport> {
    return this.http.get<BankSummaryReport>(`${this.reportsUrl}/${id}`);
  }

  /**
   * GET /api/reports/{id}/pdf
   * - Get PDF for a report (returns blob).
   */
  getReportPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.reportsUrl}/${id}/pdf`, {
      responseType: 'blob',
    });
  }

  /**
   * GET /api/return-batches?merchantId=xxx
   * - Fetch return batch files for the current merchant.
   */
  getReturnBatches(merchantId?: string): Observable<ReturnBatchFile[]> {
    const params = merchantId ? `?merchantId=${merchantId}` : '';
    return this.http.get<ReturnBatchFile[]>(`${this.returnBatchesUrl}${params}`);
  }

  /**
   * GET /api/return-batches/{id}/download
   * - Download a return batch file.
   */
  downloadReturnBatch(id: number): Observable<Blob> {
    return this.http.get(`${this.returnBatchesUrl}/${id}/download`, {
      responseType: 'blob',
    });
  }
}
