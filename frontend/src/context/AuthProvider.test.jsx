import { useState } from 'react'
import {
    fireEvent,
    render,
    screen,
    waitFor,
} from '@testing-library/react'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    loginUser,
    logoutUser,
    registerUser,
} from '../api/authApi'
import {
    clearSession,
    startSession,
} from '../features/auth/authSession'
import { useAuth } from '../hooks/useAuth'
import { AuthProvider } from './AuthProvider'

vi.mock('../api/authApi', () => ({
    loginUser: vi.fn(),
    logoutUser: vi.fn(),
    registerUser: vi.fn(),
}))

const validLoginResponse = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    expiresIn: 3600,
    userId: 1,
    name: 'Sreenithi',
    email: 'sree@example.com',
}

function AuthProbe() {
    const {
        user,
        isAuthenticated,
        login,
        register,
        logout,
    } = useAuth()

    const [message, setMessage] =
        useState('')

    function handleLogin() {
        void login({
            email: 'sree@example.com',
            password: 'password123',
        }).catch(() => {
            setMessage('Login failed')
        })
    }

    function handleRegister() {
        void register({
            name: 'Sreenithi',
            email: 'sree@example.com',
            password: 'password123',
        }).then(() => {
            setMessage('Registration completed')
        }).catch(() => {
            setMessage('Registration failed')
        })
    }

    function handleLogout() {
        void logout()
            .catch(() => {
                setMessage(
                    'Backend logout failed',
                )
            })
    }

    return (
        <div>
            <p>
                {isAuthenticated
                    ? 'Authenticated'
                    : 'Signed out'}
            </p>

            <p>
                {user?.email ?? 'No user'}
            </p>

            {message && (
                <p>{message}</p>
            )}

            <button
                type="button"
                onClick={handleLogin}
            >
                Test login
            </button>

            <button
                type="button"
                onClick={handleRegister}
            >
                Test registration
            </button>

            <button
                type="button"
                onClick={handleLogout}
            >
                Test logout
            </button>
        </div>
    )
}

function renderProvider() {
    const queryClient =
        new QueryClient({
            defaultOptions: {
                queries: {
                    retry: false,
                },
                mutations: {
                    retry: false,
                },
            },
        })

    const result = render(
        <QueryClientProvider
            client={queryClient}
        >
            <AuthProvider>
                <AuthProbe />
            </AuthProvider>
        </QueryClientProvider>,
    )

    return {
        ...result,
        queryClient,
    }
}

describe('AuthProvider', () => {
    beforeEach(() => {
        clearSession()
        vi.clearAllMocks()
    })

    afterEach(() => {
        clearSession()
    })

    it('starts a session after successful login', async () => {
        vi.mocked(
            loginUser,
        ).mockResolvedValue(
            validLoginResponse,
        )

        renderProvider()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Test login',
                },
            ),
        )

        expect(
            await screen.findByText(
                'Authenticated',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'sree@example.com',
            ),
        ).toBeInTheDocument()

        expect(
            loginUser,
        ).toHaveBeenCalledWith({
            email: 'sree@example.com',
            password: 'password123',
        })
    })

    it('delegates registration without starting a session', async () => {
        vi.mocked(
            registerUser,
        ).mockResolvedValue({
            id: 1,
            name: 'Sreenithi',
            email: 'sree@example.com',
        })

        renderProvider()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Test registration',
                },
            ),
        )

        expect(
            await screen.findByText(
                'Registration completed',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Signed out',
            ),
        ).toBeInTheDocument()

        expect(
            registerUser,
        ).toHaveBeenCalledWith({
            name: 'Sreenithi',
            email: 'sree@example.com',
            password: 'password123',
        })
    })

    it('clears the local session during logout', async () => {
        startSession(
            validLoginResponse,
        )

        vi.mocked(
            logoutUser,
        ).mockResolvedValue()

        renderProvider()

        expect(
            screen.getByText(
                'Authenticated',
            ),
        ).toBeInTheDocument()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Test logout',
                },
            ),
        )

        expect(
            await screen.findByText(
                'Signed out',
            ),
        ).toBeInTheDocument()

        expect(
            logoutUser,
        ).toHaveBeenCalledWith(
            'refresh-token',
        )
    })

    it('remains locally signed out when backend logout fails', async () => {
        startSession(
            validLoginResponse,
        )

        vi.mocked(
            logoutUser,
        ).mockRejectedValue(
            new Error(
                'Backend unavailable',
            ),
        )

        renderProvider()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Test logout',
                },
            ),
        )

        expect(
            await screen.findByText(
                'Signed out',
            ),
        ).toBeInTheDocument()

        expect(
            await screen.findByText(
                'Backend logout failed',
            ),
        ).toBeInTheDocument()
    })

    it('does not call backend logout without a refresh token', async () => {
        renderProvider()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Test logout',
                },
            ),
        )

        await waitFor(() => {
            expect(
                screen.getByText(
                    'Signed out',
                ),
            ).toBeInTheDocument()
        })

        expect(
            logoutUser,
        ).not.toHaveBeenCalled()
    })
})