/**
 * Dashboard test suite.
 *
 * Uses Vitest, React Testing Library and jsdom.
 *
 * DashboardPage normally receives the authenticated user through
 * the Outlet context rendered by DashboardShell. The test router
 * below recreates that production component contract.
 */

import {
    render,
    screen,
    within,
} from '@testing-library/react'
import {
    MemoryRouter,
    Outlet,
    Route,
    Routes,
} from 'react-router-dom'
import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { ActivityTimeline } from '../components/dashboard/ActivityTimeline'
import { AiInsightCard } from '../components/dashboard/AiInsightCard'
import { QuickActions } from '../components/dashboard/QuickActions'
import { RecentTransactions } from '../components/dashboard/RecentTransactions'
import { Sidebar } from '../components/layout/Sidebar'
import DashboardPage from '../pages/DashboardPage'

/*
 * Sidebar and Header normally receive this state through UiProvider.
 *
 * Mocking the hook keeps this suite focused on dashboard rendering
 * rather than the provider's internal implementation.
 */
vi.mock('../hooks/useUi', () => ({
    useUi: () => ({
        isSidebarOpen: false,
        isSidebarCollapsed: false,
        closeSidebar: vi.fn(),
        toggleSidebarCollapsed: vi.fn(),
        openSidebar: vi.fn(),
    }),
}))

const authenticatedUser = {
    userId: 1,
    name: 'Sree',
    email: 'sree@example.com',
}

function renderWithRouter(component) {
    return render(
        <MemoryRouter
            initialEntries={[
                '/app/dashboard',
            ]}
        >
            <Routes>
                <Route
                    element={
                        <Outlet
                            context={{
                                user: authenticatedUser,
                            }}
                        />
                    }
                >
                    <Route
                        path="/app/dashboard"
                        element={component}
                    />
                </Route>
            </Routes>
        </MemoryRouter>,
    )
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.spyOn(
            console,
            'info',
        ).mockImplementation(() => {})
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('renders the authenticated user greeting', () => {
        renderWithRouter(
            <DashboardPage />,
        )

        expect(
            screen.getByRole(
                'heading',
                {
                    level: 1,
                    name: /good (morning|afternoon|evening), sree/i,
                },
            ),
        ).toBeInTheDocument()
    })

    it('renders all four financial summary cards', () => {
        renderWithRouter(
            <DashboardPage />,
        )

        expect(
            screen.getByText(
                'Monthly spending',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Remaining budget',
            ),
        ).toBeInTheDocument()

        /*
         * "You are owed" appears in both the summary card and
         * the group-balance card.
         */
        expect(
            screen.getAllByText(
                'You are owed',
            ),
        ).toHaveLength(2)

        expect(
            screen.getByText(
                'You owe',
            ),
        ).toBeInTheDocument()
    })

    it('renders sidebar navigation with all primary routes', () => {
        renderWithRouter(
            <Sidebar
                user={authenticatedUser}
                onLogout={vi.fn()}
            />,
        )

        const navigation =
            screen.getByRole(
                'navigation',
                {
                    name: /primary navigation/i,
                },
            )

        expect(
            within(navigation).getByText(
                'Dashboard',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'Expenses',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'Budgets',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'Recurring',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'Groups',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'Activity',
            ),
        ).toBeInTheDocument()

        expect(
            within(navigation).getByText(
                'AI insights',
            ),
        ).toBeInTheDocument()
    })

    it('renders the authenticated user in the sidebar', () => {
        renderWithRouter(
            <Sidebar
                user={authenticatedUser}
                onLogout={vi.fn()}
            />,
        )

        expect(
            screen.getByText(
                'Sree',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'sree@example.com',
            ),
        ).toBeInTheDocument()
    })

    it('gives quick action buttons accessible names', () => {
        render(
            <QuickActions
                onAction={vi.fn()}
            />,
        )

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Add expense',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Create group',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Settle balance',
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'button',
                {
                    name: 'Add budget',
                },
            ),
        ).toBeInTheDocument()
    })

    it('shows the empty state when there are no transactions', () => {
        render(
            <RecentTransactions
                transactions={[]}
            />,
        )

        expect(
            screen.getByText(
                'No expenses yet',
            ),
        ).toBeInTheDocument()
    })

    it('renders transaction rows when data is present', () => {
        const transactions = [
            {
                id: '1',
                title: 'Coffee',
                category: 'Food',
                date: 'Today',
                amount: -150,
                split: false,
            },
        ]

        render(
            <RecentTransactions
                transactions={transactions}
            />,
        )

        expect(
            screen.getByText(
                'Coffee',
            ),
        ).toBeInTheDocument()

        expect(
            screen.queryByText(
                'No expenses yet',
            ),
        ).not.toBeInTheDocument()
    })

    it('labels AI content as an advisory suggestion', () => {
        render(
            <AiInsightCard />,
        )

        expect(
            screen.getByText(
                'AI-generated suggestion',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /advisory only/i,
            ),
        ).toBeInTheDocument()
    })

    it('keeps the mobile sidebar close button accessible', () => {
        renderWithRouter(
            <Sidebar
                user={authenticatedUser}
                onLogout={vi.fn()}
            />,
        )

        expect(
            screen.getByRole(
                'button',
                {
                    name: /close sidebar/i,
                },
            ),
        ).toBeInTheDocument()
    })

    it('exposes an accessible logout button', () => {
        renderWithRouter(
            <Sidebar
                user={authenticatedUser}
                onLogout={vi.fn()}
            />,
        )

        expect(
            screen.getByRole(
                'button',
                {
                    name: /^log out$/i,
                },
            ),
        ).toBeInTheDocument()
    })

    it('renders without animation or image assets being available', () => {
        /*
         * Animated WebP files are optional.
         *
         * Dashboard components provide inline SVG mascot fallbacks,
         * so missing animation assets must not crash the page.
         */
        expect(() => {
            renderWithRouter(
                <DashboardPage />,
            )
        }).not.toThrow()
    })

    it('renders recent activity items', () => {
        const activityItems = [
            {
                id: 'activity-1',
                type: 'expense',
                title: 'You added an expense',
                timestamp: 'A moment ago',
            },
        ]

        render(
            <ActivityTimeline
                items={activityItems}
            />,
        )

        expect(
            screen.getByText(
                'You added an expense',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'A moment ago',
            ),
        ).toBeInTheDocument()
    })
})