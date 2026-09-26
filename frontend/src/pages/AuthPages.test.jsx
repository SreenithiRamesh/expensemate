import {
    fireEvent,
    render,
    screen,
    waitFor,
} from '@testing-library/react'
import {
    MemoryRouter,
    Route,
    Routes,
    useLocation,
} from 'react-router-dom'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import LoginPage from './LoginPage'
import RegisterPage from './RegisterPage'

const authMock = vi.hoisted(() => ({
    value: null,
}))

vi.mock('../hooks/useAuth', () => ({
    useAuth: () => authMock.value,
}))

function LoginDestination() {
    const location = useLocation()

    return (
        <div>
            <h1>Login destination</h1>

            {location.state?.accountCreated && (
                <p>Registration completed</p>
            )}
        </div>
    )
}

function renderApplication(
    initialPath,
    initialState = null,
) {
    return render(
        <MemoryRouter
            initialEntries={[
                {
                    pathname: initialPath,
                    state: initialState,
                },
            ]}
        >
            <Routes>
                <Route
                    path="/login"
                    element={<LoginPage />}
                />

                <Route
                    path="/register"
                    element={<RegisterPage />}
                />

                <Route
                    path="/app/dashboard"
                    element={
                        <h1>
                            Dashboard destination
                        </h1>
                    }
                />

                <Route
                    path="/login-destination"
                    element={<LoginDestination />}
                />
            </Routes>
        </MemoryRouter>,
    )
}

function fillLoginForm({
                           email = 'sree@example.com',
                           password = 'password123',
                       } = {}) {
    fireEvent.change(
        screen.getByLabelText(
            'Email address',
        ),
        {
            target: {
                value: email,
            },
        },
    )

    fireEvent.change(
        screen.getByLabelText(
            'Password',
        ),
        {
            target: {
                value: password,
            },
        },
    )
}

function fillRegisterForm({
                              name = 'Sreenithi',
                              email = 'sree@example.com',
                              password = 'password123',
                              confirmPassword = 'password123',
                          } = {}) {
    fireEvent.change(
        screen.getByLabelText(
            'Full name',
        ),
        {
            target: {
                value: name,
            },
        },
    )

    fireEvent.change(
        screen.getByLabelText(
            'Email address',
        ),
        {
            target: {
                value: email,
            },
        },
    )

    fireEvent.change(
        screen.getByLabelText(
            'Password',
        ),
        {
            target: {
                value: password,
            },
        },
    )

    fireEvent.change(
        screen.getByLabelText(
            'Confirm password',
        ),
        {
            target: {
                value: confirmPassword,
            },
        },
    )
}

describe('LoginPage', () => {
    beforeEach(() => {
        authMock.value = {
            login: vi.fn(),
            register: vi.fn(),
            logout: vi.fn(),
            user: null,
            isAuthenticated: false,
        }
    })

    it('renders the login form', () => {
        renderApplication('/login')

        expect(
            screen.getByRole(
                'heading',
                {
                    level: 1,
                    name: 'Welcome back',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Sign in',
                },
            ),
        ).toBeInTheDocument()
    })

    it('prevents submission when required fields are empty', async () => {
        renderApplication('/login')

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Sign in',
                },
            ),
        )

        await waitFor(() => {
            expect(
                screen.getAllByRole('alert')
                    .length,
            ).toBeGreaterThan(0)
        })

        expect(
            authMock.value.login,
        ).not.toHaveBeenCalled()
    })

    it('submits valid login credentials', async () => {
        authMock.value.login.mockResolvedValue({
            id: 1,
            name: 'Sreenithi',
            email: 'sree@example.com',
        })

        renderApplication('/login')

        fillLoginForm({
            email: '  sree@example.com  ',
        })

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Sign in',
                },
            ),
        )

        await waitFor(() => {
            expect(
                authMock.value.login,
            ).toHaveBeenCalledWith({
                email: 'sree@example.com',
                password: 'password123',
            })
        })
    })

    it('shows an error for incorrect credentials', async () => {
        authMock.value.login.mockRejectedValue({
            isAxiosError: true,
            response: {
                status: 401,
            },
        })

        renderApplication('/login')
        fillLoginForm()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Sign in',
                },
            ),
        )

        expect(
            await screen.findByRole(
                'alert',
            ),
        ).toHaveTextContent(
            'The email or password is incorrect.',
        )
    })

    it('shows a connection error when the backend is unreachable', async () => {
        authMock.value.login.mockRejectedValue({
            isAxiosError: true,
        })

        renderApplication('/login')
        fillLoginForm()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Sign in',
                },
            ),
        )

        expect(
            await screen.findByRole(
                'alert',
            ),
        ).toHaveTextContent(
            'Unable to reach ExpenseMate',
        )
    })

    it('shows the successful-registration message', () => {
        renderApplication(
            '/login',
            {
                accountCreated: true,
            },
        )

        expect(
            screen.getByRole(
                'status',
            ),
        ).toHaveTextContent(
            'Account created successfully',
        )
    })

    it('redirects an authenticated user to the dashboard', () => {
        authMock.value.isAuthenticated = true

        renderApplication('/login')

        expect(
            screen.getByRole(
                'heading',
                {
                    name: 'Dashboard destination',
                },
            ),
        ).toBeInTheDocument()
    })

    it('preserves the originally requested protected route', () => {
        authMock.value.isAuthenticated = true

        renderApplication(
            '/login',
            {
                from: {
                    pathname: '/app/dashboard',
                    search: '?view=monthly',
                    hash: '#summary',
                },
            },
        )

        expect(
            screen.getByRole(
                'heading',
                {
                    name: 'Dashboard destination',
                },
            ),
        ).toBeInTheDocument()
    })
})

describe('RegisterPage', () => {
    beforeEach(() => {
        authMock.value = {
            login: vi.fn(),
            register: vi.fn(),
            logout: vi.fn(),
            user: null,
            isAuthenticated: false,
        }
    })

    it('renders the registration form', () => {
        renderApplication('/register')

        expect(
            screen.getByRole(
                'heading',
                {
                    level: 1,
                    name: 'Create your account',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Create account',
                },
            ),
        ).toBeInTheDocument()
    })

    it('rejects mismatched passwords', async () => {
        renderApplication('/register')

        fillRegisterForm({
            confirmPassword:
                'different-password',
        })

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Create account',
                },
            ),
        )

        expect(
            await screen.findByText(
                'Passwords do not match.',
            ),
        ).toBeInTheDocument()

        expect(
            authMock.value.register,
        ).not.toHaveBeenCalled()
    })

    it('registers without sending confirmPassword', async () => {
        authMock.value.register.mockResolvedValue({
            id: 1,
            name: 'Sreenithi',
            email: 'sree@example.com',
        })

        renderApplication('/register')
        fillRegisterForm()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Create account',
                },
            ),
        )

        await waitFor(() => {
            expect(
                authMock.value.register,
            ).toHaveBeenCalledWith({
                name: 'Sreenithi',
                email: 'sree@example.com',
                password: 'password123',
            })
        })

        expect(
            await screen.findByRole(
                'heading',
                {
                    name: 'Welcome back',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('status'),
        ).toHaveTextContent(
            'Account created successfully',
        )
    })

    it('shows a duplicate-email error', async () => {
        authMock.value.register.mockRejectedValue({
            isAxiosError: true,
            response: {
                status: 409,
            },
        })

        renderApplication('/register')
        fillRegisterForm()

        fireEvent.click(
            screen.getByRole(
                'button',
                {
                    name: 'Create account',
                },
            ),
        )

        expect(
            await screen.findByRole(
                'alert',
            ),
        ).toHaveTextContent(
            'An account with this email already exists.',
        )
    })

    it('redirects authenticated users to the dashboard', () => {
        authMock.value.isAuthenticated = true

        renderApplication('/register')

        expect(
            screen.getByRole(
                'heading',
                {
                    name: 'Dashboard destination',
                },
            ),
        ).toBeInTheDocument()
    })
})