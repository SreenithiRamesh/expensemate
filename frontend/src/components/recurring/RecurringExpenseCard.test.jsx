import {
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { RecurringExpenseCard } from './RecurringExpenseCard'

const activeRecurringExpense = {
    id: 12,
    title: 'Netflix subscription',
    amount: 649,
    category: 'SUBSCRIPTION',
    frequency: 'MONTHLY',
    nextDueDate: '2026-11-10',
    active: true,
    createdAt: '2026-10-01T10:00:00',
    updatedAt: '2026-10-01T10:00:00',
}

function renderCard(overrides = {}) {
    const props = {
        recurringExpense: {
            ...activeRecurringExpense,
            ...overrides,
        },
        onEdit: vi.fn(),
        onDelete: vi.fn(),
        onRecordPayment: vi.fn(),
        onToggleActive: vi.fn(),
    }

    render(
        <RecurringExpenseCard
            {...props}
        />,
    )

    return props
}

describe('RecurringExpenseCard', () => {
    it('renders recurring-expense information', () => {
        renderCard()

        expect(
            screen.getByText(
                'Netflix subscription',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText('Subscription'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('Monthly'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('₹649.00'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('Upcoming'),
        ).toBeInTheDocument()
    })

    it('calls the edit handler', async () => {
        const user = userEvent.setup()
        const props = renderCard()

        await user.click(
            screen.getByRole('button', {
                name: 'Edit Netflix subscription',
            }),
        )

        expect(
            props.onEdit,
        ).toHaveBeenCalledTimes(1)
    })

    it('calls the record-payment handler', async () => {
        const user = userEvent.setup()
        const props = renderCard()

        await user.click(
            screen.getByRole('button', {
                name:
                    'Record payment for Netflix subscription',
            }),
        )

        expect(
            props.onRecordPayment,
        ).toHaveBeenCalledTimes(1)
    })

    it('calls the pause handler for an active schedule', async () => {
        const user = userEvent.setup()
        const props = renderCard()

        await user.click(
            screen.getByRole('button', {
                name:
                    'Pause Netflix subscription',
            }),
        )

        expect(
            props.onToggleActive,
        ).toHaveBeenCalledTimes(1)
    })

    it('calls the activate handler for a paused schedule', async () => {
        const user = userEvent.setup()

        const props = renderCard({
            active: false,
        })

        await user.click(
            screen.getByRole('button', {
                name:
                    'Activate Netflix subscription',
            }),
        )

        expect(
            props.onToggleActive,
        ).toHaveBeenCalledTimes(1)
    })

    it('disables payment recording for a paused schedule', () => {
        renderCard({
            active: false,
        })

        expect(
            screen.getByRole('button', {
                name:
                    'Record payment for Netflix subscription',
            }),
        ).toBeDisabled()
    })

    it('calls the delete handler', async () => {
        const user = userEvent.setup()
        const props = renderCard()

        await user.click(
            screen.getByRole('button', {
                name:
                    'Delete Netflix subscription',
            }),
        )

        expect(
            props.onDelete,
        ).toHaveBeenCalledTimes(1)
    })
})