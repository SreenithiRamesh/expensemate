import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useSyncExternalStore,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { CanceledError } from 'axios'

import { loginUser, logoutUser, registerUser } from '../api/authApi'
import {
    clearSession,
    getSession,
    startSession,
    subscribeToSession,
} from '../features/auth/authSession'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }) {
    const queryClient = useQueryClient()
    const operationId = useRef(0)

    const session = useSyncExternalStore(
        subscribeToSession,
        getSession,
    )

    useEffect(() => {
        let previousUser = getSession()?.user ?? null

        const unsubscribe = subscribeToSession(() => {
            const nextUser = getSession()?.user ?? null

            // Refresh preserves the user object.
            // Login/logout changes it: discard cached user data.
            if (previousUser !== nextUser) {
                previousUser = nextUser
                queryClient.clear()
            }
        })

        return () => {
            unsubscribe()

            // Prevent an unfinished login from starting a session
            // after this provider has been unmounted.
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
        // Registration returns user details, not authentication tokens.
        // The registration page will direct the user to login.
        return registerUser(details)
    }, [])

    const logout = useCallback(async () => {
        operationId.current += 1

        const refreshToken = getSession()?.refreshToken

        // Sign out locally immediately, even if the backend is offline.
        clearSession()
        queryClient.clear()

        if (refreshToken) {
            // If revocation fails, the caller can show a message.
            // The local session remains cleared.
            await logoutUser(refreshToken)
        }
    }, [queryClient])

    const user = session?.user ?? null

    const value = useMemo(
        () => ({
            user,
            isAuthenticated: user !== null,
            login,
            register,
            logout,
        }),
        [user, login, register, logout],
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}