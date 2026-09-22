import {
    render,
    screen,
} from '@testing-library/react'
import {
    afterEach,
    describe,
    expect,
    it,
} from 'vitest'

import App from './App.jsx'
import { AppProviders } from './providers/AppProviders'

function renderApp() {
    return render(
        <AppProviders>
            <App />
        </AppProviders>,
    )
}

describe('App routing', () => {
    afterEach(() => {
        window.history.pushState(
            {},
            '',
            '/',
        )
    })

    it('renders the ExpenseMate landing page', () => {
        window.history.pushState(
            {},
            '',
            '/',
        )

        renderApp()

        expect(
            screen.getByRole('heading', {
                name: /your money, beautifully organized/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: /foundation ready/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders the dashboard route', async () => {
        window.history.pushState(
            {},
            '',
            '/app/dashboard',
        )

        renderApp()

        expect(
            await screen.findByRole('heading', {
                name: /welcome to expensemate/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('navigation', {
                name: /primary navigation/i,
            }),
        ).toBeInTheDocument()
    })
})