import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import App from './App.jsx'
import { AppProviders } from './providers/AppProviders'

function renderApp(path) {
    window.history.pushState({}, '', path)

    return render(
        <AppProviders>
            <App />
        </AppProviders>,
    )
}

describe('App routing', () => {
    afterEach(() => {
        window.history.pushState({}, '', '/')
    })

    it('renders the ExpenseMate landing page', async () => {
        renderApp('/')

        expect(
            await screen.findByRole('heading', {
                level: 1,
                name: /money management that finally feels simple/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('link', {
                name: /^start managing expenses$/i,
            }),
        ).toHaveAttribute('href', '/register')
    })

    it('renders the dashboard route', async () => {
        renderApp('/app/dashboard')

        expect(
            await screen.findByRole('heading', {
                level: 1,
                name: /good (morning|afternoon|evening),/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('navigation', {
                name: /primary navigation/i,
            }),
        ).toBeInTheDocument()
    })
})