import {
    render,
    screen,
    waitFor,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    useBudgets,
} from '../features/budgets/useBudgetQueries'
import BudgetsPage from './BudgetsPage'

vi.mock(
    '../features/budgets/useBudgetQueries',
    () => ({
        useBudgets: vi.fn(),
    }),
)

vi.mock(
    '../components/budgets/BudgetFormDialog',
    () => ({
        BudgetFormDialog: ({
                               budget,
                               onClose,
                           }) => (
            <div
                role="dialog"
                aria-label={
                    budget
                        ? 'Edit budget'
                        : 'Create budget'
                }
            >
                <button
                    type="button"
                    onClick={onClose}
                >
                    Close budget form
                </button>
            </div>
        ),
    }),
)

vi.mock(
    '../components/budgets/DeleteBudgetDialog',
    () => ({
        DeleteBudgetDialog: ({
                                 budget,
                                 onClose,
                             }) => (
            <div
                role="dialog"
                aria-label="Delete budget"
            >
                <p>{budget.category}</p>

                <button
                    type="button"
                    onClick={onClose}
                >
                    Cancel deletion
                </button>
            </div>
        ),
    }),
)

function createQueryResult(
    overrides = {},
) {
    return {
        data: [],
        error: null,
        isError: false,
        isFetching: false,
        isLoading: false,
        refetch: vi.fn(),
        ...overrides,
    }
}

const sampleBudgets = [
    {
        id: 1,
        category: 'FOOD',
        monthlyLimit: 8000,
        month: 10,
        year: 2026,
        spent: 4000,
        remaining: 4000,
        percentageUsed: 50,
    },
    {
        id: 2,
        category: 'TRAVEL',
        monthlyLimit: 5000,
        month: 10,
        year: 2026,
        spent: 4500,
        remaining: 500,
        percentageUsed: 90,
    },
    {
        id: 3,
        category: 'SHOPPING',
        monthlyLimit: 3000,
        month: 10,
        year: 2026,
        spent: 3500,
        remaining: -500,
        percentageUsed: 116.67,
    },
]

describe('BudgetsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()

        vi.mocked(
            useBudgets,
        ).mockReturnValue(
            createQueryResult(),
        )
    })

    it('renders the heading and empty state', () => {
        render(<BudgetsPage />)

        expect(
            screen.getByRole('heading', {
                level: 1,
                name: 'Budgets',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /no budgets for/i,
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: 'Create first budget',
            }),
        ).toBeInTheDocument()
    })

    it('requests budgets for the current month', () => {
        const today = new Date()

        render(<BudgetsPage />)

        expect(
            useBudgets,
        ).toHaveBeenCalledWith({
            month:
                today.getMonth() + 1,
            year: today.getFullYear(),
        })
    })

    it('opens and closes the create dialog', async () => {
        const user = userEvent.setup()

        render(<BudgetsPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Create budget',
            }),
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Create budget',
            }),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: 'Close budget form',
            }),
        )

        expect(
            screen.queryByRole('dialog', {
                name: 'Create budget',
            }),
        ).not.toBeInTheDocument()
    })

    it('renders the loading state', () => {
        vi.mocked(
            useBudgets,
        ).mockReturnValue(
            createQueryResult({
                data: undefined,
                isLoading: true,
                isFetching: true,
            }),
        )

        render(<BudgetsPage />)

        expect(
            screen.getByRole('status', {
                name: 'Loading budgets',
            }),
        ).toBeInTheDocument()
    })

    it('renders an API error and retries', async () => {
        const user = userEvent.setup()
        const refetch = vi.fn()

        vi.mocked(
            useBudgets,
        ).mockReturnValue(
            createQueryResult({
                data: undefined,
                isError: true,
                error: {
                    response: {
                        data: {
                            detail:
                                'Budget service is unavailable.',
                        },
                    },
                },
                refetch,
            }),
        )

        render(<BudgetsPage />)

        expect(
            screen.getByRole('alert'),
        ).toHaveTextContent(
            'Budget service is unavailable.',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Try again',
            }),
        )

        expect(
            refetch,
        ).toHaveBeenCalledTimes(1)
    })

    it('renders summaries and budget states', () => {
        vi.mocked(
            useBudgets,
        ).mockReturnValue(
            createQueryResult({
                data: sampleBudgets,
            }),
        )

        render(<BudgetsPage />)

        expect(
            screen.getByText(
                'Total budget',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Total spent',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'On track',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Approaching limit',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Budget exceeded',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getAllByRole(
                'progressbar',
            ),
        ).toHaveLength(3)
    })

    it('opens edit and delete dialogs', async () => {
        const user = userEvent.setup()

        vi.mocked(
            useBudgets,
        ).mockReturnValue(
            createQueryResult({
                data: [sampleBudgets[0]],
            }),
        )

        render(<BudgetsPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Edit',
            }),
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Edit budget',
            }),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: 'Close budget form',
            }),
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Delete',
            }),
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Delete budget',
            }),
        ).toBeInTheDocument()
    })

    it('requests the previous month', async () => {
        const user = userEvent.setup()
        const today = new Date()

        const previousPeriod =
            new Date(
                today.getFullYear(),
                today.getMonth() - 1,
                1,
            )

        render(<BudgetsPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Previous month',
            }),
        )

        await waitFor(() => {
            expect(
                vi.mocked(useBudgets),
            ).toHaveBeenLastCalledWith({
                month:
                    previousPeriod
                        .getMonth() + 1,
                year:
                    previousPeriod
                        .getFullYear(),
            })
        })
    })

    it('requests the next month', async () => {
        const user = userEvent.setup()
        const today = new Date()

        const nextPeriod =
            new Date(
                today.getFullYear(),
                today.getMonth() + 1,
                1,
            )

        render(<BudgetsPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Next month',
            }),
        )

        await waitFor(() => {
            expect(
                vi.mocked(useBudgets),
            ).toHaveBeenLastCalledWith({
                month:
                    nextPeriod
                        .getMonth() + 1,
                year:
                    nextPeriod
                        .getFullYear(),
            })
        })
    })
})