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
    useExpenses,
} from '../features/expenses/useExpenseQueries'
import ExpensesPage from './ExpensesPage'

vi.mock(
    '../features/expenses/useExpenseQueries',
    () => ({
        useExpenses: vi.fn(),
    }),
)

vi.mock(
    '../components/expenses/ExpenseFormDialog',
    () => ({
        ExpenseFormDialog: ({
                                expense,
                                onClose,
                            }) => (
            <div
                role="dialog"
                aria-label={
                    expense
                        ? 'Edit expense'
                        : 'Add expense'
                }
            >
                <button
                    type="button"
                    onClick={onClose}
                >
                    Close form
                </button>
            </div>
        ),
    }),
)

vi.mock(
    '../components/expenses/ExpenseDetailsDialog',
    () => ({
        ExpenseDetailsDialog: ({
                                   expense,
                                   onClose,
                               }) => (
            <div
                role="dialog"
                aria-label="Expense details"
            >
                <p>{expense.description}</p>

                <button
                    type="button"
                    onClick={onClose}
                >
                    Close details
                </button>
            </div>
        ),
    }),
)

vi.mock(
    '../components/expenses/DeleteExpenseDialog',
    () => ({
        DeleteExpenseDialog: ({
                                  expense,
                                  onClose,
                              }) => (
            <div
                role="dialog"
                aria-label="Delete expense"
            >
                <p>{expense.description}</p>

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

function createQueryResult(overrides = {}) {
    return {
        data: {
            content: [],
            number: 0,
            totalPages: 0,
            totalElements: 0,
            first: true,
            last: true,
        },
        error: null,
        isError: false,
        isFetching: false,
        isLoading: false,
        refetch: vi.fn(),
        ...overrides,
    }
}

const sampleExpense = {
    id: 12,
    amount: 640,
    category: 'FOOD',
    expenseDate: '2026-10-06',
    description: 'Team lunch',
}

describe('ExpensesPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()

        vi.mocked(
            useExpenses,
        ).mockReturnValue(
            createQueryResult(),
        )
    })

    it('renders the expenses heading and empty state', () => {
        render(<ExpensesPage />)

        expect(
            screen.getByRole('heading', {
                level: 1,
                name: 'Expenses',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText('No expenses yet'),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: 'Add first expense',
            }),
        ).toBeInTheDocument()
    })

    it('opens and closes the create-expense dialog', async () => {
        const user = userEvent.setup()

        render(<ExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Add expense',
            }),
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Add expense',
            }),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: 'Close form',
            }),
        )

        expect(
            screen.queryByRole('dialog', {
                name: 'Add expense',
            }),
        ).not.toBeInTheDocument()
    })

    it('renders the loading state', () => {
        vi.mocked(
            useExpenses,
        ).mockReturnValue(
            createQueryResult({
                data: undefined,
                isLoading: true,
                isFetching: true,
            }),
        )

        render(<ExpensesPage />)

        expect(
            screen.getByRole('status', {
                name: 'Loading expenses',
            }),
        ).toBeInTheDocument()
    })

    it('renders an API error and retries the request', async () => {
        const user = userEvent.setup()
        const refetch = vi.fn()

        vi.mocked(
            useExpenses,
        ).mockReturnValue(
            createQueryResult({
                data: undefined,
                isError: true,
                error: {
                    response: {
                        data: {
                            detail:
                                'Expense service is unavailable.',
                        },
                    },
                },
                refetch,
            }),
        )

        render(<ExpensesPage />)

        expect(
            screen.getByRole('alert'),
        ).toHaveTextContent(
            'Expense service is unavailable.',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Try again',
            }),
        )

        expect(refetch).toHaveBeenCalledTimes(1)
    })

    it('renders expenses and opens view, edit and delete dialogs', async () => {
        const user = userEvent.setup()

        vi.mocked(
            useExpenses,
        ).mockReturnValue(
            createQueryResult({
                data: {
                    content: [sampleExpense],
                    number: 0,
                    totalPages: 1,
                    totalElements: 1,
                    first: true,
                    last: true,
                },
            }),
        )

        render(<ExpensesPage />)

        expect(
            screen.getAllByText('Team lunch').length,
        ).toBeGreaterThan(0)

        expect(
            screen.getByText('1 expense'),
        ).toBeInTheDocument()

        await user.click(
            screen.getAllByRole('button', {
                name: 'View Team lunch',
            })[0],
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Expense details',
            }),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: 'Close details',
            }),
        )

        await user.click(
            screen.getAllByRole('button', {
                name: 'Edit Team lunch',
            })[0],
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Edit expense',
            }),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: 'Close form',
            }),
        )

        await user.click(
            screen.getAllByRole('button', {
                name: 'Delete Team lunch',
            })[0],
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Delete expense',
            }),
        ).toBeInTheDocument()
    })

    it('passes search and category filters to the query hook', async () => {
        const user = userEvent.setup()

        render(<ExpensesPage />)

        await user.type(
            screen.getByRole('searchbox', {
                name: 'Search',
            }),
            'lunch',
        )

        await waitFor(() => {
            expect(
                vi.mocked(useExpenses),
            ).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    search: 'lunch',
                    page: 0,
                }),
            )
        })

        const categoryButton =
            screen
                .getByText('All categories')
                .closest('button')

        expect(categoryButton).not.toBeNull()

        await user.click(categoryButton)

        await user.click(
            screen.getByRole('option', {
                name: 'Food & dining',
            }),
        )

        await waitFor(() => {
            expect(
                vi.mocked(useExpenses),
            ).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    search: 'lunch',
                    category: 'FOOD',
                    page: 0,
                }),
            )
        })

        await user.click(
            screen.getByRole('button', {
                name: 'Reset',
            }),
        )

        expect(
            screen.getByRole('searchbox', {
                name: 'Search',
            }),
        ).toHaveValue('')

        await waitFor(() => {
            expect(
                vi.mocked(useExpenses),
            ).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    search: '',
                    category: '',
                    startDate: '',
                    endDate: '',
                    page: 0,
                }),
            )
        })
    })

    it('requests the next result page', async () => {
        const user = userEvent.setup()

        vi.mocked(
            useExpenses,
        ).mockReturnValue(
            createQueryResult({
                data: {
                    content: [sampleExpense],
                    number: 0,
                    totalPages: 3,
                    totalElements: 21,
                    first: true,
                    last: false,
                },
            }),
        )

        render(<ExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name: 'Next',
            }),
        )

        await waitFor(() => {
            expect(
                vi.mocked(useExpenses),
            ).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    page: 1,
                }),
            )
        })
    })

    it('shows the filtered empty state and resets filters', async () => {
        const user = userEvent.setup()

        render(<ExpensesPage />)

        await user.type(
            screen.getByRole('searchbox', {
                name: 'Search',
            }),
            'missing expense',
        )

        expect(
            await screen.findByText(
                'No matching expenses',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Try changing or resetting the current filters.',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getAllByRole('button', {
                name: 'Reset filters',
            })[0],
        )

        expect(
            screen.getByRole('searchbox', {
                name: 'Search',
            }),
        ).toHaveValue('')

        expect(
            screen.getByText('No expenses yet'),
        ).toBeInTheDocument()
    })
})