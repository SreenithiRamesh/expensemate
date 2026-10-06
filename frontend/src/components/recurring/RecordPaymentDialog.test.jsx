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

import { RecordPaymentDialog } from './RecordPaymentDialog'

const mocks = vi.hoisted(() => ({
    recordPaymentAsync: vi.fn(),
    reset: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock(
    '../../features/recurring-expenses/useRecurringExpenseQueries',
    () => ({
        useRecordRecurringPayment: () => ({
            mutateAsync:
            mocks.recordPaymentAsync,
            reset: mocks.reset,
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

const recurringExpense = {
    id: 9,
    title: 'Internet bill',
    amount: 999,
    category: 'BILLS',
    frequency: 'MONTHLY',
    nextDueDate: '2026-10-20',
    active: true,
}

describe('RecordPaymentDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks()

        mocks.recordPaymentAsync.mockResolvedValue({
            id: 9,
            nextDueDate: '2026-11-20',
        })
    })

    it('renders the recurring-expense payment form', () => {
        render(
            <RecordPaymentDialog
                recurringExpense={
                    recurringExpense
                }
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('heading', {
                name: /record payment/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /record the latest payment for Internet bill/i,
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /creates a personal expense/i,
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText(
                /payment date/i,
            ),
        ).toBeInTheDocument()
    })

    it('records a recurring payment', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <RecordPaymentDialog
                recurringExpense={
                    recurringExpense
                }
                onClose={onClose}
            />,
        )

        const paymentDateInput =
            screen.getByLabelText(
                /payment date/i,
            )

        await user.clear(
            paymentDateInput,
        )

        await user.type(
            paymentDateInput,
            '2026-10-06',
        )

        await user.click(
            screen.getByRole('button', {
                name: /^record payment$/i,
            }),
        )

        await waitFor(() => {
            expect(
                mocks.recordPaymentAsync,
            ).toHaveBeenCalledWith({
                recurringExpenseId: 9,
                paymentDate: '2026-10-06',
            })
        })

        expect(
            mocks.toastSuccess,
        ).toHaveBeenCalled()

        expect(
            onClose,
        ).toHaveBeenCalledTimes(1)
    })

    it('requires a payment date', async () => {
        const user = userEvent.setup()

        render(
            <RecordPaymentDialog
                recurringExpense={
                    recurringExpense
                }
                onClose={vi.fn()}
            />,
        )

        await user.clear(
            screen.getByLabelText(
                /payment date/i,
            ),
        )

        await user.click(
            screen.getByRole('button', {
                name: /^record payment$/i,
            }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toBeInTheDocument()

        expect(
            mocks.recordPaymentAsync,
        ).not.toHaveBeenCalled()
    })

    it('shows backend errors without closing the dialog', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        mocks.recordPaymentAsync.mockRejectedValue({
            response: {
                data: {
                    detail:
                        'Inactive recurring expenses cannot record payments.',
                },
            },
        })

        render(
            <RecordPaymentDialog
                recurringExpense={
                    recurringExpense
                }
                onClose={onClose}
            />,
        )

        const paymentDateInput =
            screen.getByLabelText(
                /payment date/i,
            )

        await user.clear(
            paymentDateInput,
        )

        await user.type(
            paymentDateInput,
            '2026-10-06',
        )

        await user.click(
            screen.getByRole('button', {
                name: /^record payment$/i,
            }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'Inactive recurring expenses cannot record payments.',
        )

        expect(
            onClose,
        ).not.toHaveBeenCalled()
    })

    it('closes when Cancel is selected', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <RecordPaymentDialog
                recurringExpense={
                    recurringExpense
                }
                onClose={onClose}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /cancel/i,
            }),
        )

        expect(
            onClose,
        ).toHaveBeenCalledTimes(1)
    })
})