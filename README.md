# 🚑 Smart Emergency Medical Response & Hospital Bed Management System
### Backend — Spring Boot 3.2 + Java 21

## 🚀 How to Run

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8.0+

### Step 1 — Setup Database
```sql
CREATE DATABASE smart_emergency_db;
```
Then run `src/main/resources/schema.sql` in MySQL Workbench or CLI.

### Step 2 — Configure
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=YOUR_MYSQL_USER
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3 — Run
```bash
cd smart-emergency-backend
mvn clean install
mvn spring-boot:run
```
API runs at: **http://localhost:8080/api**

---

## 🔑 Default Login Credentials
| Role     | Email                          | Password   |
|----------|-------------------------------|------------|
| ADMIN    | admin@smartemergency.com       | Admin@123  |
| HOSPITAL | cityhosp@smartemergency.com    | Admin@123  |

---

## 📡 Key API Endpoints

### Auth
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/auth/register | Register user |
| POST | /api/auth/login | Login + get JWT |
| POST | /api/auth/refresh | Refresh token |

### Patient
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/patient/hospitals | All hospitals |
| GET  | /api/patient/hospitals/nearby?lat=&lng=&radius= | Nearby hospitals |
| POST | /api/patient/emergency/request | Create request |
| POST | /api/patient/emergency/sos | Quick SOS |
| GET  | /api/patient/emergency/requests | My requests |

### Hospital
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/hospital/profile | Register profile |
| PUT  | /api/hospital/beds | Update bed counts |
| GET  | /api/hospital/requests/active | Active queue |
| PATCH| /api/hospital/requests/{id}/status | Update status |

### Admin
| Method | URL | Description |
|--------|-----|-------------|
| GET   | /api/admin/dashboard/stats | Analytics |
| PATCH | /api/admin/hospitals/{id}/verify | Verify hospital |
| PATCH | /api/admin/users/{id}/toggle-status | Toggle user |

## 🔌 WebSocket Topics
- `/topic/emergency/updates` — all emergency updates
- `/topic/hospital/{id}/emergency` — hospital-specific alerts
- `/topic/hospital/{id}/beds` — live bed changes
- `/user/queue/notifications` — user-specific alerts
