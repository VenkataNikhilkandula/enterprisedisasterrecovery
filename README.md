Enterprise Disaster Recovery System
📌 Project Overview

The Enterprise Disaster Recovery System is a Spring Boot based application designed to provide reliable backup and recovery capabilities for enterprise applications. It supports database backups, file backups, and complete system restoration to ensure business continuity during failures or disasters.
The system provides REST APIs to initiate backups, monitor backup progress, perform restores, and check restore status.
-----------------------------------------------------------------------------------------
🚀 Features
> Full system backup
> Database backup
> File storage backup
> Restore data from previous backups
> Backup and restore status tracking
> RESTful API architecture
> MySQL database integration
> Swagger/OpenAPI documentation
> Clean layered Spring Boot architecture
-------------------------------------------------------------------------------------------------
🛠️ Technology Stack
> Java 17+
> Spring Boot
> Spring Web
> Spring Data JPA
> Hibernate
> MySQL Database
> Maven
> Lombok
> Swagger / OpenAPI (SpringDoc)
---------------------------------------------------------------------------------------------------
📂 Project Structure
src/main/java/com/disasterrecoverysystem

├── controller          # REST APIs
├── service             # Business logic
│   ├── backup          # Backup services
│   └── restore         # Restore services
├── entity              # Database entities
├── repository          # JPA repositories
├── dto                 # Request/Response objects
├── config              # Application configurations
└── exception           # Exception handling
--------------------------------------------------------------------------------

⚙️ Prerequisites

Before running the project, install:
> Java 17 or higher
> Maven 3.8+
> MySQL Server
> Git
> ---------------------------------------------------------------------
Configure application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/disaster_recovery
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
---------------------------------------------------------------------------
▶️ Running the Application
Clone the repository
git clone <your-repository-url>
Navigate to project directory
cd enterprise-disaster-recovery
Run using Maven
mvn spring-boot:run

The application will start at:

http://localhost:8080
📘 API Endpoints
1. Start Backup

POST

/backup/start?type=FULL

Available backup types:

FULL
DB_ONLY
FILES_ONLY
2. Get Backup Status

GET

/backup/status/{backupId}
3. Start Restore

POST

/restore/{backupId}
4. Get Restore Status

GET

/restore/status/{restoreId}
🧪 Testing with Postman

Base URL:

http://localhost:8080

Example:

POST http://localhost:8080/backup/start?type=FULL

No request body is required for the available APIs.

📚 Swagger Documentation

After starting the application, open:

Swagger UI
http://localhost:8080/swagger-ui/index.html
OpenAPI JSON
http://localhost:8080/v3/api-docs
🔄 Backup Workflow
User Request
      |
      v
Backup Controller
      |
      v
Backup Service
      |
      +---- Database Backup
      |
      +---- File Backup
      |
      v
Backup Storage
