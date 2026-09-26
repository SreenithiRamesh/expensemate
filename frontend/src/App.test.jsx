import {
    beforeEach,
    describe,
    expect,
    it,
} from 'vitest'
import {
    render,
    screen,
} from '@testing-library/react'

import App from './App'
import {
    clearSession,
    startSession,
} from './features/auth/authSession'
import { AppProviders } from './providers/AppProviders'

function renderRoute(path) {
    window.history.replaceState(
        {},
        '',
        path,
    )

    return render(
        <AppProviders>
            <App />
        </AppProviders>,
    )
}

beforeEach(() => {
    clearSession()

    window.history.replaceState(
        {},
        '',
        '/',
    )
})

describe('App routing', () => {
    it('renders the ExpenseMate landing page', async () => {
        renderRoute('/')

        expect(
            await screen.findByRole('heading', {
                level: 1,
                name: /money management that finally feels simple/i,
            }),
        ).toBeInTheDocument()
    })

    it('redirects a guest from the dashboard to login', async () => {
        renderRoute('/app/dashboard')

        expect(
            await screen.findByRole('heading', {
                level: 1,
                name: /welcome back/i,
            }),
        ).toBeInTheDocument()

        expect(window.location.pathname).toBe('/login')
    })

    it('renders the dashboard for an authenticated user', async () => {
        startSession({
            accessToken: 'test-access-token',
            refreshToken: 'test-refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
            userId: 1,
            name: 'Sree',
            email: 'sree@example.com',
        })

        renderRoute('/app/dashboard')

        expect(
            await screen.findByRole('heading', {
                level: 1,
                name: /good (morning|afternoon|evening),/i,
            }),
        ).toBeInTheDocument()

        expect(window.location.pathname).toBe(
            '/app/dashboard',
        )
    })
})