# Merchant-Upload-Batch-File

A full-stack web system that allows merchants to upload batch payment files, automatically encrypt them using **AES-256-GCM + RSA-OAEP** hybrid encryption, and securely transmit them to the bank for processing.  
Works together with the [RTA_BANK](https://github.com/LimYikHong/RTA_BANK) backend (auditLog branch) which handles decryption, validation, batch grouping, and authorization.  
Developed using **Angular 19.2.15** for the frontend and **Spring Boot 3.5.3** for the backend.

---

# Getting Started

> **Note**: Make sure you have installed all the following tools before starting:
> - **Java 17 or above**
> - **Node.js 18+ and npm**
> - **Maven 3.9+**
> - **Angular CLI 19.2.15** (`npm install -g @angular/cli`)
> - *(Optional)* **MySQL 8.x** if you want to use a persistent database

---

## Step 0: Setup Database Connection

### Use MySQL (For Persistent Data)

Create a database in MySQL:

```sql
CREATE DATABASE rta_db;
```

Then update `application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

> 💡 **Note:**  
> Flyway will automatically execute the SQL migration files (`V1__`, `V2__`, etc.) when the backend starts.  
> You don’t need to manually import or execute any `.sql` files.  
>  
> If Flyway encounters a migration version conflict or checksum issue,  
> you can fix it by running the following command:
> ```
> mvn flyway:repair
> ```
> This will repair Flyway’s schema history table and allow migrations to continue normally.


---

## Step 1: Start the Spring Boot Server

First, open a terminal and run the following commands:

```bash
cd RTAbackend
mvn spring-boot:run
```

The backend will start at:  
**http://localhost:8088**

---

## Step 2: Start the Angular Frontend

Open another terminal and run:

```bash
cd RTAfrontend
npm install
ng serve --open
# or
npm start
```

If successful, the frontend will start at:  
**http://localhost:4200**

---

## Step 3: Login Account

Default test account:  
- **Username:** merchant1  
- **Password:** 123456  

After login, you can (CRUD implementation):
- Upload `.xlsx`, `.csv`, or `.txt` batch files  and delete uploaded bacth file (Create and Delete)
- View uploaded batches and logs  (Read)
- Edit merchant profile information  (Update)

---


# Performance Highlight (Key Achievements)

> The following highlights describe real, implemented features across the **Merchant Upload Batch File** system and the **RTA Bank** backend that together form the complete batch payment processing platform.

---

## 1. Fully Automated Processing & Validation

- **End-to-end automation** from merchant file upload → file-type & content-type validation → CSV/Excel parsing → transaction extraction → encrypted transfer to bank → bank-side validation → batch grouping → authorization — with zero manual data entry
- **Multi-format file parsing**: processes `.csv`, `.xlsx`, `.xls`, and `.txt` files, each with format-specific parsers (Apache POI for Excel, BufferedReader for CSV/TXT)
- **Per-row field validation** on the bank side using configurable field mappings (`RtaFieldMapping`) — validates data types, required fields, regex patterns, and allowed values per merchant's file profile
- Reduces manual intervention and human error — a batch of hundreds of transactions is processed in seconds rather than entered one by one

---

## 2. Secure Transaction Flow (AES + RSA Hybrid Encryption)

- **AES-256-GCM encryption** (Data Encryption Key) — each uploaded file is encrypted with a unique random AES key and a unique IV, ensuring no key reuse across files
- **RSA-OAEP key wrapping** (Key Encryption Key) — the per-file AES key is encrypted with the merchant's RSA public key, so only the bank holding the corresponding RSA private key can decrypt
- **Encrypted file storage** in MinIO object storage with `.enc` suffix; encryption metadata (IV, wrapped AES key) stored separately in the database
- **Bank-side decryption** (`FileDecryptionService`) parses the binary layout `[4B encKeyLen][N bytes encryptedAesKey][12B IV][cipherText]` and decrypts using the merchant's private RSA key
- **SHA-256 file hash deduplication** — bank computes SHA-256 hash of each incoming file and rejects duplicates, preventing accidental re-processing

---

## 3. Support Customizable Batch File Format (Bank-Side File Profiles)

- **Per-merchant file profiles** (`RtaFileProfile`) define file format parameters: file type (CSV/XLSX/TXT), encoding, delimiter, quote character, header/footer flags, date format, and record layout
- **Custom field mappings** (`RtaFieldMapping`) per profile — each merchant can map their own column names/positions to canonical fields (e.g., `accountNumber`, `amount`, `currency`) with configurable source column index, validation regex, allowed values, transform expressions, and default values
- **File format mismatch detection** — bank validates that the uploaded file type matches the merchant's configured profile before parsing
- Enables standardized processing of files from different merchants without code changes — onboard a new merchant by configuring a file profile and field mappings

---

## 4. High-Volume Transaction Handling (Scheduled Batch Grouping)

- **Scheduled batch maintenance** (`@Scheduled(fixedRate = 300000)`) — every 5 minutes, the `BatchMaintenanceScheduler` automatically:
  - **Phase 1**: Assigns batch IDs to all eligible incoming files (files with validated transactions that haven't been batched yet), creating a single `RtaBatch` per cycle
  - **Phase 2**: Groups all validated (SUCCESS) transactions without an authorization batch into a new `RtaAuthorizationBatch` with status `READY_TO_SEND`
- **Bulk transaction assignment** — uses `bulkAssignBatchByFileId()` to update all transactions for a file in one SQL statement rather than individual saves
- **Aggregate statistics** per batch — tracks total count, success count, fail count across all files in the batch cycle
- Maintains stable processing cadence under increasing load by batching work at regular intervals

---

## 5. Role-Based Access Control & Comprehensive Audit Trail (Bank-Side)

- **JWT-based stateless authentication** with `JwtAuthenticationFilter` — token-based auth with expiry, no server-side session storage
- **Permission-based RBAC** (`RtaRole` → `RtaRolePermission` → `RtaPermission`) — fine-grained permissions like `USER_CREATE`, `USER_EDIT`, `USER_DELETE`, `MERCHANT_CREATE`, `MERCHANT_EDIT`, `MERCHANT_DELETE`, `ROLE_EDIT` enforced at the endpoint level via Spring Security `hasAuthority()`
- **Google Authenticator TOTP 2FA** on the merchant side — two-step login (password → TOTP code), with automatic 2FA enrollment on first login and QR code generation
- **Dual-layer audit logging**:
  - Merchant side: `MerchantActivityLog` tracks uploads, deletions, encryptions, login attempts, and profile changes
  - Bank side: `AuditLog` with `REQUIRES_NEW` transaction propagation records both USER and SYSTEM actions (login, file processing, batch creation, merchant management) — never rolled back even if the outer transaction fails
- **Real-time dashboard** (`DashboardController`) aggregates KPIs: total batches, transactions, merchants, transaction trend (7 days), status breakdowns, per-merchant file counts, recurring vs one-time breakdown, and daily amount trends

---
