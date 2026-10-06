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

import { RecurringExpenseFormDialog } from './RecurringExpenseFormDialog'

const mocks = vi.hoisted(() => ({
    createAsync: vi.fn(),
    updateAsync: vi.fn(),
    createReset: vi.fn(),
    updateReset: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock(
    '../../features/recurring-expenses/useRecurringExpenseQueries',
    () => ({
        useCreateRecurringExpense: () => ({
            mutateAsync: mocks.createAsync,
            reset: mocks.createReset,
            isPending: false,
        }),
        useUpdateRecurringExpense: () => ({
            mutateAsync: mocks.updateAsync,
            reset: mocks.updateReset,
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

async function chooseCategory(user, categoryName) {
    await user.click(
        screen.getByRole('button', {
            name: /category select category/i,
        }),
    )

    await user.click(
        screen.getByRole('option', {
            name: categoryName,
        }),
    )
}

describe('RecurringExpenseFormDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks()

        mocks.createAsync.mockResolvedValue({
            id: 1,
        })

        mocks.updateAsync.mockResolvedValue({
            id: 4,
        })
    })

    it('renders the create-schedule form', () => {
        render(
            <RecurringExpenseFormDialog
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('heading', {
                name: /create recurring expense/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/title/i),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/amount/i),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/frequency/i),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/next due date/i),
        ).toBeInTheDocument()
    })

    it('creates a recurring expense', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <RecurringExpenseFormDialog
                onClose={onClose}
            />,
        )

        await user.type(
            screen.getByLabelText(/title/i),
            'Netflix subscription',
        )

        await user.type(
            screen.getByLabelText(/amount/i),
            '649',
        )

        await chooseCategory(
            user,
            'Subscription',
        )

        await user.selectOptions(
            screen.getByLabelText(/frequency/i),
            'MONTHLY',
        )

        await user.clear(
            screen.getByLabelText(/next due date/i),
        )

        await user.type(
            screen.getByLabelText(/next due date/i),
            '2026-11-10',
        )

        await user.click(
            screen.getByRole('button', {
                name: /create schedule/i,
            }),
        )

        await waitFor(() => {
            expect(
                mocks.createAsync,
            ).toHaveBeenCalledWith(
                expect.objectContaining({
                    title: 'Netflix subscription',
                    category: 'SUBSCRIPTION',
                    frequency: 'MONTHLY',
                    nextDueDate: '2026-11-10',
                    active: true,
                }),
            )
        })

        expect(
            mocks.toastSuccess,
        ).toHaveBeenCalled()

        expect(onClose).toHaveBeenCalledTimes(1)
    })

    it('shows validation messages for an invalid form', async () => {
        const user = userEvent.setup()

        render(
            <RecurringExpenseFormDialog
                onClose={vi.fn()}
            />,
        )

        await user.clear(
            screen.getByLabelText(/next due date/i),
        )

        await user.click(
            screen.getByRole('button', {
                name: /create schedule/i,
            }),
        )

        expect(
            await screen.findAllByRole('alert'),
        ).not.toHaveLength(0)

        expect(
            mocks.createAsync,
        ).not.toHaveBeenCalled()
    })

    it('renders existing values in edit mode', () => {
        render(
            <RecurringExpenseFormDialog
                recurringExpense={{
                    id: 4,
                    title: 'Gym membership',
                    amount: 1200,
                    category: 'HEALTH',
                    frequency: 'MONTHLY',
                    nextDueDate: '2026-11-05',
                    active: false,
                }}
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('heading', {
                name: /edit recurring expense/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/title/i),
        ).toHaveValue('Gym membership')

        expect(
            screen.getByLabelText(/amount/i),
        ).toHaveValue(1200)

        expect(
            screen.getByRole('button', {
                name: /category health/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(/frequency/i),
        ).toHaveValue('MONTHLY')

        expect(
            screen.getByLabelText(/next due date/i),
        ).toHaveValue('2026-11-05')
    })

    it('updates an existing recurring expense', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <RecurringExpenseFormDialog
                recurringExpense={{
                    id: 4,
                    title: 'Gym membership',
                    amount: 1200,
                    category: 'HEALTH',
                    frequency: 'MONTHLY',
                    nextDueDate: '2026-11-05',
                    active: true,
                }}
                onClose={onClose}
            />,
        )

        const titleInput =
            screen.getByLabelText(/title/i)

        await user.clear(titleInput)
        await user.type(
            titleInput,
            'Updated gym membership',
        )

        await user.click(
            screen.getByRole('button', {
                name: /save changes/i,
            }),
        )

        await waitFor(() => {
            expect(
                mocks.updateAsync,
            ).toHaveBeenCalledWith({
                recurringExpenseId: 4,
                recurringExpense: expect.objectContaining({
                    title: 'Updated gym membership',
                    amount: expect.anything(),
                    category: 'HEALTH',
                    frequency: 'MONTHLY',
                    nextDueDate: '2026-11-05',
                    active: true,
                }),
            })
        })

        expect(
            mocks.toastSuccess,
        ).toHaveBeenCalled()

        expect(onClose).toHaveBeenCalledTimes(1)
    })

    it('shows a backend error and keeps the form open', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        mocks.createAsync.mockRejectedValue({
            response: {
                data: {
                    detail:
                        'A matching recurring expense already exists.',
                },
            },
        })

        render(
            <RecurringExpenseFormDialog
                onClose={onClose}
            />,
        )

        await user.type(
            screen.getByLabelText(/title/i),
            'Internet bill',
        )

        await user.type(
            screen.getByLabelText(/amount/i),
            '999',
        )

        await chooseCategory(
            user,
            'Bills',
        )

        await user.clear(
            screen.getByLabelText(/next due date/i),
        )

        await user.type(
            screen.getByLabelText(/next due date/i),
            '2026-11-15',
        )

        await user.click(
            screen.getByRole('button', {
                name: /create schedule/i,
            }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'A matching recurring expense already exists.',
        )

        expect(onClose).not.toHaveBeenCalled()
    })

    it('closes when Cancel is selected', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <RecurringExpenseFormDialog
                onClose={onClose}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /cancel/i,
            }),
        )

        expect(onClose).toHaveBeenCalledTimes(1)
    })
})