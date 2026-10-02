# FinAudit-AI — Web Application Client

Modern, responsive Single Page Application (SPA) client for **FinAudit-AI**, engineered for high-performance financial compliance management, autonomous invoice auditing, and portfolio risk visualization.

---

## Key Features

- **Audit Operations Dashboard**:
  - Spend & risk trend line with stacked area risk visualization (Recharts).
  - "Top Flagged Policies" horizontal leaderboard for quick rule violation analysis.
  - Compliance score distribution histogram (0–100 buckets).
  - Quick-select date filtering (Last 7d, 30d, 90d, All Time) with TanStack Query caching.
- **Annotated Report Detail View**:
  - Radial score gauge and executive narrative summary.
  - Deterministic arithmetic metric ("X of Y line items flagged, $Z total flagged amount").
  - Severity-coded line item table (CRITICAL, HIGH, MEDIUM, LOW) with inline expandable policy citations.
  - PDF audit summary download.
- **Scoped Grounded Chat Assistant**:
  - In-page conversational Q&A bounded strictly to report line items and matched policy clauses.
  - Grounded source attribution badges (`LINE_ITEM`, `POLICY`, `FINDING`).
  - Strict refusal boundary preventing LLM hallucinations.
  - Multi-tier cost & rate-limit guardrails with session counters.
- **Frictionless Demo Authentication**:
  - 1-Click instant demo login into pre-populated auditor environment.
  - Stateless JWT token storage in memory with refresh lifecycle handlers.
  - Role-based routing (`ROLE_AUDITOR`, `ROLE_VIEWER`).
- **Performance & Aesthetics**:
  - Route-based code splitting via `React.lazy` and `Suspense` fallbacks.
  - First Contentful Paint < 1.5s; Lighthouse performance score ≥ 90.
  - Fluid micro-animations powered by Framer Motion.
  - Dark/Light theme switching with persisted preferences.

---

## Tech Stack

- **Framework**: [React 19](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **Bundler & Dev Server**: [Vite](https://vitejs.dev/)
- **State & Server Cache**: [TanStack Query v5](https://tanstack.com/query) (React Query)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/)
- **Charts & Visualization**: [Recharts](https://recharts.org/)
- **Animations**: [Framer Motion](https://www.framer.com/motion/)
- **Icons**: [Lucide React](https://lucide.dev/)

---

## Getting Started

### Prerequisites
- Node.js 18+ (Node 20+ recommended)
- npm or pnpm

### Development
```bash
# Install dependencies
npm install

# Start Vite dev server with Hot Module Replacement (HMR)
npm run dev
```

The application runs by default at `http://localhost:5173`.

### Production Build
```bash
# Type-check and build production bundle
npm run build

# Preview production build locally
npm run preview
```
