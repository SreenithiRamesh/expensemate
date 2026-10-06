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

import RecurringExpensesPage from './RecurringExpensesPage'

const mocks = vi.hoisted(() => ({
    useRecurringExpenses: vi.fn(),
    setActiveAsync: vi.fn(),
    refetch: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock(
    '../features/recurring-expenses/useRecurringExpenseQueries',
    () => ({
        useRecurringExpenses:
        mocks.useRecurringExpenses,

        useSetRecurringExpenseActive: () => ({
            mutateAsync:
            mocks.setActiveAsync,
            isPending: false,
        }),
    }),
)

vi.mock('sonner', () => ({
    toast: {
        success: mocks.toastSuccess,
        error: mocks.toastError,
    },
}))

vi.mock(
    '../components/recurring/RecurringExpenseFormDialog',
    () => ({
        RecurringExpenseFormDialog: ({
                                         recurringExpense,
                                         onClose,
                                     }) => (
            <section
                aria-label="Recurring expense form"
            >
                <p>
                    {recurringExpense
                        ? `Editing ${recurringExpense.title}`
                        : 'Creating recurring expense'}
                </p>

                <button
                    type="button"
                    onClick={onClose}
                >
                    Close recurring form
                </button>
            </section>
        ),
    }),
)

vi.mock(
    '../components/recurring/RecordPaymentDialog',
    () => ({
        RecordPaymentDialog: ({
                                  recurringExpense,
                                  onClose,
                              }) => (
            <section
                aria-label="Record payment dialog"
            >
                <p>
                    Recording payment for{' '}
                    {recurringExpense.title}
                </p>

                <button
                    type="button"
                    onClick={onClose}
                >
                    Close payment dialog
                </button>
            </section>
        ),
    }),
)

vi.mock(
    '../components/recurring/DeleteRecurringExpenseDialog',
    () => ({
        DeleteRecurringExpenseDialog: ({
                                           recurringExpense,
                                           onClose,
                                       }) => (
            <section
                aria-label="Delete recurring expense dialog"
            >
                <p>
                    Deleting{' '}
                    {recurringExpense.title}
                </p>

                <button
                    type="button"
                    onClick={onClose}
                >
                    Close delete dialog
                </button>
            </section>
        ),
    }),
)

const recurringExpenses = [
    {
        id: 1,
        title: 'Netflix subscription',
        amount: 649,
        category: 'SUBSCRIPTION',
        frequency: 'MONTHLY',
        nextDueDate: '2026-11-10',
        active: true,
        createdAt:
            '2026-10-01T10:00:00',
        updatedAt:
            '2026-10-01T10:00:00',
    },
    {
        id: 2,
        title: 'Gym membership',
        amount: 1200,
        category: 'HEALTH',
        frequency: 'MONTHLY',
        nextDueDate: '2026-11-05',
        active: false,
        createdAt:
            '2026-09-01T10:00:00',
        updatedAt:
            '2026-10-01T10:00:00',
    },
]

function mockQuery(overrides = {}) {
    mocks.useRecurringExpenses.mockReturnValue({
        data: recurringExpenses,
        isLoading: false,
        isError: false,
        error: null,
        refetch: mocks.refetch,
        ...overrides,
    })
}

describe('RecurringExpensesPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()

        mockQuery()

        mocks.setActiveAsync.mockResolvedValue({
            id: 1,
        })
    })

    it('renders the page heading and schedules', () => {
        render(<RecurringExpensesPage />)

        expect(
            screen.getByRole('heading', {
                level: 1,
                name: /recurring expenses/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Netflix subscription',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Gym membership',
            ),
        ).toBeInTheDocument()
    })

    it('renders recurring-expense summary values', () => {
        render(<RecurringExpensesPage />)

        expect(
            screen.getByText(
                'Total schedules',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Active schedules',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Due within 30 days',
            ),
        ).toBeInTheDocument()
    })

    it('shows the loading state', () => {
        mockQuery({
            data: undefined,
            isLoading: true,
        })

        render(<RecurringExpensesPage />)

        expect(
            screen.getByRole('status'),
        ).toBeInTheDocument()
    })

    it('shows the error state and retries the query', async () => {
        const user = userEvent.setup()

        mockQuery({
            data: undefined,
            isError: true,
            error: new Error(
                'Unable to load recurring expenses.',
            ),
        })

        render(<RecurringExpensesPage />)

        expect(
            screen.getByText(
                /unable to load recurring expenses/i,
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /try again/i,
            }),
        )

        expect(
            mocks.refetch,
        ).toHaveBeenCalledTimes(1)
    })

    it('shows the empty state and opens the create dialog', async () => {
        const user = userEvent.setup()

        mockQuery({
            data: [],
        })

        render(<RecurringExpensesPage />)

        expect(
            screen.getByText(
                /no recurring expenses yet/i,
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name:
                    /create first schedule/i,
            }),
        )

        expect(
            screen.getByText(
                'Creating recurring expense',
            ),
        ).toBeInTheDocument()
    })

    it('opens the create dialog from the page action', async () => {
        const user = userEvent.setup()

        render(<RecurringExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name: /create schedule/i,
            }),
        )

        expect(
            screen.getByText(
                'Creating recurring expense',
            ),
        ).toBeInTheDocument()
    })

    it('filters active and paused schedules', async () => {
        const user = userEvent.setup()

        render(<RecurringExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name: /^active/i,
            }),
        )

        expect(
            screen.getByText(
                'Netflix subscription',
            ),
        ).toBeInTheDocument()

        expect(
            screen.queryByText(
                'Gym membership',
            ),
        ).not.toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /^paused/i,
            }),
        )

        expect(
            screen.queryByText(
                'Netflix subscription',
            ),
        ).not.toBeInTheDocument()

        expect(
            screen.getByText(
                'Gym membership',
            ),
        ).toBeInTheDocument()
    })

    it('opens edit, payment, and delete dialogs', async () => {
        const user = userEvent.setup()

        render(<RecurringExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name:
                    'Edit Netflix subscription',
            }),
        )

        expect(
            screen.getByText(
                'Editing Netflix subscription',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name:
                    'Close recurring form',
            }),
        )

        await user.click(
            screen.getByRole('button', {
                name:
                    'Record payment for Netflix subscription',
            }),
        )

        expect(
            screen.getByText(
                'Recording payment for Netflix subscription',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name:
                    'Close payment dialog',
            }),
        )

        await user.click(
            screen.getByRole('button', {
                name:
                    'Delete Netflix subscription',
            }),
        )

        expect(
            screen.getByText(
                'Deleting Netflix subscription',
            ),
        ).toBeInTheDocument()
    })

    it('pauses an active recurring expense', async () => {
        const user = userEvent.setup()

        render(<RecurringExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name:
                    'Pause Netflix subscription',
            }),
        )

        await waitFor(() => {
            expect(
                mocks.setActiveAsync,
            ).toHaveBeenCalledWith({
                recurringExpense:
                    recurringExpenses[0],
                active: false,
            })
        })

        expect(
            mocks.toastSuccess,
        ).toHaveBeenCalled()
    })

    it('activates a paused recurring expense', async () => {
        const user = userEvent.setup()

        render(<RecurringExpensesPage />)

        await user.click(
            screen.getByRole('button', {
                name:
                    'Activate Gym membership',
            }),
        )

        await waitFor(() => {
            expect(
                mocks.setActiveAsync,
            ).toHaveBeenCalledWith({
                recurringExpense:
                    recurringExpenses[1],
                active: true,
            })
        })

        expect(
            mocks.toastSuccess,
        ).toHaveBeenCalled()
    })

    it('shows no cards when the selected filter has no matches', async () => {
        const user = userEvent.setup()

        mockQuery({
            data: [
                recurringExpenses[0],
            ],
        })

        render(<RecurringExpensesPage />)

        const pausedFilter =
            screen.getByRole('button', {
                name: /^paused/i,
            })

        await user.click(pausedFilter)

        expect(
            pausedFilter,
        ).toHaveAttribute(
            'aria-pressed',
            'true',
        )

        expect(
            screen.queryByText(
                'Netflix subscription',
            ),
        ).not.toBeInTheDocument()

        expect(
            screen.queryByText(
                'Gym membership',
            ),
        ).not.toBeInTheDocument()
    })
})