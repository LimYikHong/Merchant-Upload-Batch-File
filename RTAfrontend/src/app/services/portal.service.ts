import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RtaBatch {
  id?: number;
  fileName: string;
  status: string;
  merchantId: string;
  createdBy?: string;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PortalService {
  private apiUrl = 'http://localhost:8088/api/batches';

  constructor(private http: HttpClient) {}

  getBatches(): Observable<RtaBatch[]> {
    return this.http.get<RtaBatch[]>(this.apiUrl);
  }

  uploadBatch(file: File, merchantId: string, originalFileName: string): Observable<RtaBatch> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('merchantId', merchantId);
    formData.append('originalFileName', originalFileName);
    return this.http.post<RtaBatch>(`${this.apiUrl}/upload`, formData);
  }

  processBatch(id: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/${id}/process`, {}, { responseType: 'text' });
  }

  deleteBatch(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
