# FinAudit AI — Production Observability & Uptime Monitoring

This guide outlines the production observability architecture, automated uptime heartbeat monitors, and error tracking integrations for FinAudit AI.

---

## 1. Health & Readiness Endpoints

The Spring Boot backend exposes Spring Actuator health indicators:

- **Actuator Health URL:** [`https://finaudit-api-4yu2.onrender.com/actuator/health`](https://finaudit-api-4yu2.onrender.com/actuator/health)
- **Actuator Metrics URL:** [`https://finaudit-api-4yu2.onrender.com/actuator/metrics`](https://finaudit-api-4yu2.onrender.com/actuator/metrics)
- **Admin Observability Metrics:** [`https://finaudit-api-4yu2.onrender.com/api/admin/metrics`](https://finaudit-api-4yu2.onrender.com/api/admin/metrics) *(Admin JWT required)*

### Health Response Payload
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "free": 10737418240,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

## 2. Render Deployment Health Probes

In [`render.yaml`](../render.yaml), Render's infrastructure uses the healthcheck path to verify zero-downtime rolling deploys:

```yaml
services:
  - type: web
    name: finaudit-api
    env: docker
    healthCheckPath: /actuator/health
```

If `/actuator/health` fails to return HTTP 200 within the deployment timeout window, Render cancels the rollout and retains the previous healthy container.

---

## 3. External Synthetic Monitoring Setup

To prevent cold-start latency and detect outages before recruiters or interviewers do, external synthetic monitors ping the service:

### A. UptimeRobot Setup
1. **Monitor Type:** `HTTP(s)`
2. **Friendly Name:** `FinAudit AI API Health`
3. **URL / IP:** `https://finaudit-api-4yu2.onrender.com/actuator/health`
4. **Monitoring Interval:** `5 minutes` *(Keeps the server warm and prevents spinning down)*
5. **Keyword Monitoring (Optional):** Alert if response does not contain `"UP"`
6. **Notification Channels:** Email, Slack, Telegram, or Webhook

### B. Better Stack / Better Uptime Setup
1. **URL to monitor:** `https://finaudit-api-4yu2.onrender.com/actuator/health`
2. **Expected status code:** `200`
3. **Check frequency:** `3 minutes`
4. **Incident escalation policy:** Instant SMS / Push notification on downtime

---

## 4. Structured Correlation Logging & Sentry

- **Log Pattern:** Each log entry across `Upload -> Parsing -> RAG -> Audit` includes `[correlationId=... reportId=...]`.
- **Sentry Error Tracking:** 
  - Backend: `io.sentry:sentry-spring-boot-starter-jakarta` captures unhandled exceptions via `GlobalExceptionHandler`.
  - Frontend: `@sentry/react` wrapped with `Sentry.ErrorBoundary` in `src/main.tsx` to automatically report uncaught runtime errors.
