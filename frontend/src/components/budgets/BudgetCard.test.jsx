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

import { BudgetCard } from './BudgetCard'

function createBudget(overrides = {}) {
    return {
        id: 1,
        category: 'FOOD',
        monthlyLimit: 10000,
        month: 10,
        year: 2026,
        spent: 5000,
        remaining: 5000,
        percentageUsed: 50,
        createdAt:
            '2026-10-01T10:00:00',
        updatedAt:
            '2026-10-01T10:00:00',
        ...overrides,
    }
}

describe('BudgetCard', () => {
    it('renders an on-track budget', () => {
        render(
            <BudgetCard
                budget={createBudget()}
                onEdit={vi.fn()}
                onDelete={vi.fn()}
            />,
        )

        expect(
            screen.getByText(
                'Food & dining',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText('On track'),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'progressbar',
                {
                    name: /food & dining budget usage/i,
                },
            ),
        ).toHaveAttribute(
            'aria-valuenow',
            '50',
        )
    })

    it('renders a warning state', () => {
        render(
            <BudgetCard
                budget={createBudget({
                    spent: 8500,
                    remaining: 1500,
                    percentageUsed: 85,
                })}
                onEdit={vi.fn()}
                onDelete={vi.fn()}
            />,
        )

        expect(
            screen.getByText(
                'Approaching limit',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /1,500.*remaining/i,
            ),
        ).toBeInTheDocument()
    })

    it('renders an exceeded state', () => {
        render(
            <BudgetCard
                budget={createBudget({
                    spent: 12500,
                    remaining: -2500,
                    percentageUsed: 125,
                })}
                onEdit={vi.fn()}
                onDelete={vi.fn()}
            />,
        )

        expect(
            screen.getByText(
                'Budget exceeded',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /2,500.*over/i,
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'progressbar',
            ),
        ).toHaveAttribute(
            'aria-valuenow',
            '100',
        )

        expect(
            screen.getByRole(
                'progressbar',
            ),
        ).toHaveAttribute(
            'aria-valuetext',
            '125.0% used',
        )
    })

    it('calls edit and delete handlers', async () => {
        const user = userEvent.setup()
        const budget = createBudget()
        const onEdit = vi.fn()
        const onDelete = vi.fn()

        render(
            <BudgetCard
                budget={budget}
                onEdit={onEdit}
                onDelete={onDelete}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Edit',
            }),
        )

        expect(
            onEdit,
        ).toHaveBeenCalledWith(budget)

        await user.click(
            screen.getByRole('button', {
                name: 'Delete',
            }),
        )

        expect(
            onDelete,
        ).toHaveBeenCalledWith(budget)
    })
})