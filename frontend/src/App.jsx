import {
    lazy,
    Suspense,
} from 'react'
import {
    Navigate,
    Route,
    Routes,
} from 'react-router-dom'

import ProtectedRoute from './routes/ProtectedRoute'

const HomePage = lazy(() =>
    import('./pages/HomePage'),
)

const LoginPage = lazy(() =>
    import('./pages/LoginPage'),
)

const RegisterPage = lazy(() =>
    import('./pages/RegisterPage'),
)

const DashboardPage = lazy(() =>
    import('./pages/DashboardPage'),
)

const ComingSoonPage = lazy(() =>
    import('./pages/ComingSoonPage'),
)

const NotFoundPage = lazy(() =>
    import('./pages/NotFoundPage'),
)

const DashboardShell = lazy(() =>
    import('./components/layout/DashboardShell').then(
        (module) => ({
            default: module.DashboardShell,
        }),
    ),
)

function RouteLoader() {
    return (
        <div
            role="status"
            aria-live="polite"
            className="flex min-h-screen items-center justify-center px-6"
        >
            <div className="flex flex-col items-center gap-4">
                <div
                    aria-hidden="true"
                    className="h-11 w-11 animate-spin rounded-full border-4 border-brand-100 border-t-brand-600 motion-reduce:animate-none"
                />

                <p className="text-sm font-semibold text-brand-800">
                    Loading ExpenseMate…
                </p>
            </div>
        </div>
    )
}

function App() {
    return (
        <Suspense fallback={<RouteLoader />}>
            <Routes>
                <Route
                    path="/"
                    element={<HomePage />}
                />

                <Route
                    path="/login"
                    element={<LoginPage />}
                />

                <Route
                    path="/register"
                    element={<RegisterPage />}
                />

                <Route element={<ProtectedRoute />}>
                    <Route
                        path="/app"
                        element={<DashboardShell />}
                    >
                        <Route
                            index
                            element={
                                <Navigate
                                    to="dashboard"
                                    replace
                                />
                            }
                        />

                        <Route
                            path="dashboard"
                            element={<DashboardPage />}
                        />

                        <Route
                            path="expenses"
                            element={<ComingSoonPage />}
                        />

                        <Route
                            path="budgets"
                            element={<ComingSoonPage />}
                        />

                        <Route
                            path="recurring"
                            element={<ComingSoonPage />}
                        />

                        <Route
                            path="groups"
                            element={<ComingSoonPage />}
                        />

                        <Route
                            path="activity"
                            element={<ComingSoonPage />}
                        />

                        <Route
                            path="insights"
                            element={<ComingSoonPage />}
                        />
                    </Route>
                </Route>

                <Route
                    path="*"
                    element={<NotFoundPage />}
                />
            </Routes>
        </Suspense>
    )
}

export default App