import {
    Navigate,
    Outlet,
    useLocation,
} from 'react-router-dom'

import AuthLoadingState from '../components/auth/AuthLoadingState'
import { useAuth } from '../hooks/useAuth'

export default function ProtectedRoute() {
    const {
        isAuthenticated,
        isInitializing,
    } = useAuth()

    const location = useLocation()

    if (isInitializing) {
        return <AuthLoadingState />
    }

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{
                    from: {
                        pathname:
                        location.pathname,
                        search:
                        location.search,
                        hash:
                        location.hash,
                    },
                }}
            />
        )
    }

    return <Outlet />
}