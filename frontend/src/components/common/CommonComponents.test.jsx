import {
    fireEvent,
    render,
    screen,
} from '@testing-library/react'
import {
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { ApiErrorState } from './ApiErrorState'
import { Button } from './Button'
import { EmptyState } from './EmptyState'
import { PageLoader } from './PageLoader'

describe('common components', () => {
    it('renders a reusable primary button', () => {
        render(
            <Button>
                Add expense
            </Button>,
        )

        expect(
            screen.getByRole('button', {
                name: /add expense/i,
            }),
        ).toBeEnabled()
    })

    it('disables the button while loading', () => {
        render(
            <Button loading>
                Save expense
            </Button>,
        )

        expect(
            screen.getByRole('button', {
                name: /save expense/i,
            }),
        ).toBeDisabled()
    })

    it('runs the empty-state action', () => {
        const handleAction = vi.fn()

        render(
            <EmptyState
                title="No expenses"
                actionLabel="Create expense"
                onAction={handleAction}
            />,
        )

        fireEvent.click(
            screen.getByRole('button', {
                name: /create expense/i,
            }),
        )

        expect(handleAction).toHaveBeenCalledOnce()
    })

    it('runs the API retry action', () => {
        const handleRetry = vi.fn()

        render(
            <ApiErrorState
                onRetry={handleRetry}
            />,
        )

        fireEvent.click(
            screen.getByRole('button', {
                name: /try again/i,
            }),
        )

        expect(handleRetry).toHaveBeenCalledOnce()
    })

    it('announces the page loader', () => {
        render(
            <PageLoader message="Loading dashboard" />,
        )

        expect(
            screen.getByRole('status'),
        ).toHaveTextContent(
            'Loading dashboard',
        )
    })
})