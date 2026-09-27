# FinAudit-AI

FinAudit-AI is an autonomous financial audit copilot powered by Spring AI, Google Gemini, and PostgreSQL with pgvector. It parses uploaded financial documents (invoices, corporate expense claims), performs semantic RAG-grounded policy verification against company compliance policies, executes deterministic checks (duplicate invoice detection), and surfaces risk assessments in a modern dashboard.

---

## Architecture Overview

- **finaudit-core**: Clean Java domain models, DTOs, and shared utilities (framework-independent).
- **finaudit-api**: Spring Boot 3.4+ application with Spring AI (Google GenAI Gemini models), Spring Security (JWT & RBAC), Spring Data JPA, and pgvector.
- **frontend**: Modern React + Vite + TypeScript interface with Tailwind CSS.

---

## Prerequisites

- **Java 21** LTS
- **Maven 3.9+**
- **Docker** and Docker Compose
- **Google Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/apikey))

---

## Setup & Running Locally

### 1. Set Environment Variables
```bash
export GEMINI_API_KEY="your_gemini_api_key_here"
```

On Windows (PowerShell):
```powershell
$env:GEMINI_API_KEY="your_gemini_api_key_here"
```

### 2. Start PostgreSQL with pgvector
```bash
docker compose up -d
```

### 3. Build the Project
```bash
mvn clean install
```

### 4. Run the API Application
```bash
mvn spring-boot:run -pl finaudit-api
```

The application will be accessible at:
- **API Base URL**: `http://localhost:8080`
- **Swagger / OpenAPI Documentation**: `http://localhost:8080/swagger-ui.html`
- **Actuator Health Check**: `http://localhost:8080/actuator/health`

### 5. Run the Frontend (React + Vite + TypeScript)

```bash
cd frontend
npm install
npm run dev
```

The frontend interface will be available at `http://localhost:5173`.

#### Frontend Configuration & Proxy
- **Development Proxy**: Vite is configured with a built-in proxy forwarding all `/api` calls to `http://localhost:8080`.
- **Custom Backend URL**: You can set `VITE_API_URL` to point to an external or deployed backend:
  ```bash
  export VITE_API_URL="http://localhost:8080/api"
  npm run dev
  ```
- **Design System & Aesthetics**: Restrained, modern AI aesthetic inspired by Anthropic and OpenAI. Features off-white/near-black neutral base, cobalt accent, Geist font family, 8px-based spacing scale, and an accent+neutral severity scale for findings.
- **In-Memory JWT Storage**: Following enterprise security best practices, JWT tokens are kept in memory and attached via Axios request interceptors (never persisted in `localStorage`).

---

## License
MIT

