# ExpenseMate Frontend

ExpenseMate is a production-oriented personal and shared-finance application built with React and backed by a Spring Boot REST API.

The frontend provides personal-expense management, budgets, recurring expenses, shared groups, flexible expense splitting, settlements, financial activity, and advisory AI insights.

## Technology stack

- React 19
- Vite 8
- Tailwind CSS 4
- React Router 7
- TanStack Query 5
- Axios
- React Hook Form
- Zod
- Lucide React
- Sonner
- Vitest
- React Testing Library

## Design system

ExpenseMate uses a teal analytics design language:

- Background: `#E7F2F3`
- Primary: `#159FA4`
- Light primary: `#27B8B5`
- Heading text: `#252936`
- Muted text: `#8A929C`
- White and glass-effect cards
- Restrained teal, cyan, and blue gradients
- Responsive dashboard layout

Gradients are used for primary actions, brand elements, important metrics, and visual hierarchy. Financial data remains on clean, high-contrast surfaces for readability.

## Architecture boundaries

| Directory | Responsibility | Must not contain |
| --- | --- | --- |
| `api/` | Axios clients, HTTP configuration, and API error extraction | React components and business workflows |
| `components/` | Reusable presentational UI components and layouts | Direct API calls and server-data ownership |
| `features/` | Domain orchestration, TanStack Query hooks, mutations, and data transformation | Generic visual primitives |
| `pages/` | Route-level composition | Raw Axios configuration |
| `providers/` | Application-wide provider composition | Feature-specific business logic |
| `context/` | Small global client-side state such as UI and authentication | Server-state caching |
| `hooks/` | Reusable React hooks | Route-level page markup |
| `schemas/` | Zod validation schemas | API requests |
| `routes/` | Route guards and route configuration | Domain implementation |
| `utils/` | Pure reusable helper functions | React state and API ownership |

## State-management strategy

ExpenseMate intentionally avoids a general-purpose global state library during V1.

- TanStack Query manages server state.
- AuthContext will manage authentication state.
- UiContext manages sidebar and interface state.
- React Hook Form manages form state.
- Local component state manages isolated UI behaviour.

A new state-management dependency should only be introduced when a proven cross-feature requirement cannot be handled cleanly by these layers.

## Error and availability states

The frontend distinguishes between:

1. Ordinary API errors
2. Authentication expiry
3. Backend waking up
4. Backend unavailable
5. AI service unavailable
6. Unexpected React rendering errors

The backend-health monitor is intentionally separate from business queries so that health polling does not affect TanStack Query retries or application data.

## Source structure

```text
src/
├── api/
├── assets/
│   ├── icons/
│   └── images/
├── components/
│   ├── common/
│   └── layout/
├── constants/
├── context/
├── features/
│   └── server-health/
├── hooks/
├── lib/
├── pages/
├── providers/
├── routes/
├── schemas/
├── styles/
├── test/
│   └── mocks/
└── utils/