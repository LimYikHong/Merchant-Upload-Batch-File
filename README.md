# Merchant-Upload-Batch-File

A full-stack web system that allows merchants to upload batch payment files and manage profile details.  
In the future, it will include an **auto encryption feature** using **RSA** and **AES** encryption methods.  
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


# Future Enhancements

- Add RSA and AES encryption for automatic file encryption during upload  
- Integrate key management and secure storage  

---
