import {
    Navigate,
    Route,
    Routes,
} from 'react-router-dom'

import { DashboardShell } from './components/layout/DashboardShell'
import ComingSoonPage from './pages/ComingSoonPage'
import DashboardPage from './pages/DashboardPage'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'

function App() {
    return (
        <Routes>
            <Route
                path="/"
                element={<HomePage />}
            />

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

            <Route
                path="*"
                element={<NotFoundPage />}
            />
        </Routes>
    )
}

export default App