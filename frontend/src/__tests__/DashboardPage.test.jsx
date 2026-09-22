/**
 * Dashboard test suite.
 *
 * Assumes the project's existing test setup: Vitest + @testing-library/react
 * with jsdom, matching the standard Vite + React 19 test config. Adjust the
 * import paths below if your project's alias configuration differs.
 */
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DashboardPage from '../pages/DashboardPage'
import { QuickActions } from '../components/dashboard/QuickActions'
import { RecentTransactions } from '../components/dashboard/RecentTransactions'
import { AiInsightCard } from '../components/dashboard/AiInsightCard'
import { Sidebar } from '../components/layout/Sidebar'

// Mock the UI context used by Sidebar/Header so these components can render
// outside of the full AppProviders tree.
vi.mock('../hooks/useUi', () => ({
    useUi: () => ({
        isSidebarOpen: false,
        isSidebarCollapsed: false,
        closeSidebar: vi.fn(),
        toggleSidebarCollapsed: vi.fn(),
        openSidebar: vi.fn(),
    }),
}))

function renderWithRouter(ui) {
    return render(<MemoryRouter initialEntries={['/app/dashboard']}>{ui}</MemoryRouter>)
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.spyOn(console, 'info').mockImplementation(() => {})
    })

    it('renders the dashboard heading and greeting', () => {
        renderWithRouter(<DashboardPage />)
        expect(
            screen.getByRole('heading', { level: 1, name: /Sree/i }),
        ).toBeInTheDocument()
    })

    it('renders all four financial summary cards', () => {
        renderWithRouter(<DashboardPage />)
        expect(screen.getByText('Monthly spending')).toBeInTheDocument()
        expect(screen.getByText('Remaining budget')).toBeInTheDocument()
        expect(screen.getAllByText('You are owed')).toHaveLength(2)
        expect(screen.getByText('You owe')).toBeInTheDocument()
    })

    it('renders sidebar navigation with all primary routes', () => {
        renderWithRouter(<Sidebar />)
        const nav = screen.getByRole('navigation', { name: /primary navigation/i })
        expect(within(nav).getByText('Dashboard')).toBeInTheDocument()
        expect(within(nav).getByText('Expenses')).toBeInTheDocument()
        expect(within(nav).getByText('Budgets')).toBeInTheDocument()
        expect(within(nav).getByText('Groups')).toBeInTheDocument()
        expect(within(nav).getByText('AI insights')).toBeInTheDocument()
    })

    it('gives quick action buttons accessible names', () => {
        render(<QuickActions onAction={vi.fn()} />)
        expect(screen.getByRole('button', { name: 'Add expense' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Create group' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Settle balance' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Add budget' })).toBeInTheDocument()
    })

    it('shows the empty state when there are no transactions', () => {
        render(<RecentTransactions transactions={[]} />)
        expect(screen.getByText('No expenses yet')).toBeInTheDocument()
    })

    it('renders transaction rows when data is present', () => {
        render(
            <RecentTransactions
                transactions={[
                    { id: '1', title: 'Coffee', category: 'Food', date: 'Today', amount: -150, split: false },
                ]}
            />,
        )
        expect(screen.getByText('Coffee')).toBeInTheDocument()
        expect(screen.queryByText('No expenses yet')).not.toBeInTheDocument()
    })

    it('labels AI content clearly as a suggestion, not financial fact', () => {
        render(<AiInsightCard />)
        expect(screen.getByText('AI-generated suggestion')).toBeInTheDocument()
        expect(screen.getByText(/advisory only/i)).toBeInTheDocument()
    })

    it('mobile menu button remains accessible', () => {
        renderWithRouter(<Sidebar />)
        // The mobile close button inside the aside has an accessible name.
        expect(screen.getByRole('button', { name: /close sidebar/i })).toBeInTheDocument()
    })

    it('renders without any animation/image assets present', () => {
        // No mocking of <img> — component falls back to inline SVG mascot
        // via onError, so the page must not throw even if webp files 404
        // in the test environment.
        expect(() => renderWithRouter(<DashboardPage />)).not.toThrow()
    })
})
