# FreakCode Top 100 🚀

[![Production Frontend](https://img.shields.io/badge/Frontend-Vercel-black?style=flat-square&logo=vercel)](https://interview-preparation-platform-roan.vercel.app)
[![Production Backend](https://img.shields.io/badge/Backend-Render-46E3B7?style=flat-square&logo=render&logoColor=white)](https://freakcode-backend.onrender.com)
[![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

A full-stack technical interview preparation platform architected to empower developers to systematically track, visualize, and master core Data Structures and Algorithms (DSA). 

---

## 📸 Application Interface

> **Visualize Your Progress:** High-contrast dark mode UI built for streamlined navigation and distraction-free learning.

![FreakCode Dashboard](<img width="959" height="532" alt="image" src="https://github.com/user-attachments/assets/671160be-f948-45af-96f0-1c1b6d64bb08" />
)

---

## 🌐 Live Application
* **Production URL:** [https://interview-preparation-platform-roan.vercel.app](https://interview-preparation-platform-roan.vercel.app)
* *Note: The React frontend completely handles dynamic API requests to the hosted Spring Boot environment deployed on Render.*

---

## 🛠️ Tech Stack & Architecture

### **Frontend Microservice**
* **Core Framework:** React.js (Functional components, Hook-driven state management)
* **Styling Engine:** Tailwind CSS (Custom dark-theme token configurations)
* **Data Fetching:** Axios (Configured with global interceptors for stateful token attachment)

### **Backend Enterprise Service**
* **Core Framework:** Java & Spring Boot (Leveraging the Controller-Service-Repository structural pattern)
* **Security & Auth:** Spring Security Core + Stateless JSON Web Tokens (JWT)
* **Data Security:** Cryptographic one-way password isolation using the **BCrypt** hashing algorithm

### **Infrastructure & DevOps**
* **Storage Engine:** Managed PostgreSQL relational database
* **UI Hosting:** Vercel (Edge network deployment with automatic build caching)
* **API Engine:** Render Web Services (Configured with automated build workflows and persistent environmental variables)

---

## ✨ Key Engineering Achievements

* 📊 **Dynamic Syllabus Tracker:** Built a state-driven metrics engine tracking individual problem mastery percentages segmented across *Easy*, *Medium*, and *Hard* difficulties.
* 🔒 **Stateless Identity Regulation:** Implemented an enterprise-grade authentication protocol using a custom `AuthTokenFilter`. Tokens are strictly validated on every endpoint request, ensuring secure data retrieval.
* 🌐 **Granular CORS Management:** Engineered custom Cross-Origin Resource Sharing filters within the Java security configuration to safely enable and secure cross-domain preflight `OPTIONS` blocks.
* 🏗️ **Strict Separation of Concerns:** Organized the application into decoupled data presentation, business execution, and database persistence layers to simplify feature scaling.

---

## 🚀 RESTful API Blueprint

| HTTP Method | API Endpoint | Operational Action | Access Token Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Provisions a new secure account (BCrypt protected) | ❌ No |
| `POST` | `/api/auth/login` | Validates credentials and generates a stateless JWT | ❌ No |
| `GET` | `/api/questions` | Pulls the curated technical problem tracking list | ✅ Yes |
| `GET` | `/api/my-status` | Generates active tracking metrics for the user dashboard | ✅ Yes |

---

## ⚙️ Local Development Environment Setup

### 1. Prerequisites
* **Java SDK:** Version 17 or higher
* **Runtime:** Node.js (v18+)
* **Database Engine:** Local instance of PostgreSQL

### 2. Backend Property Configuration
Set up your local configuration by adding the following properties to your application resource environment:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/interview_prep_db
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password

prepapi.app.jwtSecret=your_secure_signing_secret_key_string
3. Execution Execution Controls
Bash
# Spin up the React UI Client
cd frontend
npm install
npm start

# Spin up the Spring Boot API Service
cd backend
mvn clean install
mvn spring-boot:run
