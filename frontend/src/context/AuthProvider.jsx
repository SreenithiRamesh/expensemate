import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useSyncExternalStore,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { CanceledError } from 'axios'

import {
    loginUser,
    logoutUser,
    registerUser,
} from '../api/authApi'
import {
    clearSession,
    getSession,
    startSession,
    subscribeToSession,
} from '../features/auth/authSession'
import { AuthContext } from './AuthContext'

function getServerSessionSnapshot() {
    return null
}

export function AuthProvider({ children }) {
    const queryClient = useQueryClient()
    const operationId = useRef(0)

    const session = useSyncExternalStore(
        subscribeToSession,
        getSession,
        getServerSessionSnapshot,
    )

    useEffect(() => {
        let previousUser = getSession()?.user ?? null

        const unsubscribe = subscribeToSession(() => {
            const nextUser = getSession()?.user ?? null

            // Token refresh preserves the same frozen user object.
            // Login and logout change it, so cached user-specific
            // server data must be discarded.
            if (previousUser !== nextUser) {
                previousUser = nextUser
                queryClient.clear()
            }
        })

        return () => {
            unsubscribe()

            // Prevent an unfinished login from starting a session after
            // this provider has been unmounted.
            operationId.current += 1
        }
    }, [queryClient])

    const login = useCallback(async (credentials) => {
        const currentOperation = ++operationId.current
        const response = await loginUser(credentials)

        // A newer login or logout supersedes this request.
        if (currentOperation !== operationId.current) {
            throw new CanceledError(
                'Login was superseded by another authentication action',
            )
        }

        const nextSession = startSession(response)

        return nextSession.user
    }, [])

    const register = useCallback(async (details) => {
        // Registration returns user details but does not create a
        // frontend authentication session. The user signs in afterward.
        return registerUser(details)
    }, [])

    const logout = useCallback(async () => {
        operationId.current += 1

        const refreshToken = getSession()?.refreshToken

        // Always complete the local logout immediately, even when the
        // backend is offline.
        clearSession()
        queryClient.clear()

        if (!refreshToken) {
            return
        }

        // Backend revocation is best-effort. If it fails, the caller may
        // show a toast, but the local session stays cleared.
        await logoutUser(refreshToken)
    }, [queryClient])

    const user = session?.user ?? null

    const value = useMemo(
        () => ({
            user,
            isAuthenticated: user !== null,

            // Session storage is currently memory-only and synchronous,
            // so there is no asynchronous bootstrap operation.
            isInitializing: false,

            login,
            register,
            logout,
        }),
        [
            user,
            login,
            register,
            logout,
        ],
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}