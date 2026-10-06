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
    useCreateBudget,
    useUpdateBudget,
} from '../../features/budgets/useBudgetQueries'
import { BudgetFormDialog } from './BudgetFormDialog'

vi.mock(
    '../../features/budgets/useBudgetQueries',
    () => ({
        useCreateBudget: vi.fn(),
        useUpdateBudget: vi.fn(),
    }),
)

vi.mock('sonner', () => ({
    toast: {
        success: vi.fn(),
    },
}))

function buildMutation(overrides = {}) {
    return {
        isPending: false,
        mutateAsync: vi.fn(),
        ...overrides,
    }
}

const sampleBudget = {
    id: 7,
    category: 'TRAVEL',
    monthlyLimit: 5000,
    month: 10,
    year: 2026,
    spent: 2500,
    remaining: 2500,
    percentageUsed: 50,
}

describe('BudgetFormDialog', () => {
    let createBudgetMutation
    let updateBudgetMutation

    beforeEach(() => {
        vi.clearAllMocks()

        createBudgetMutation =
            buildMutation()

        updateBudgetMutation =
            buildMutation()

        vi.mocked(
            useCreateBudget,
        ).mockReturnValue(
            createBudgetMutation,
        )

        vi.mocked(
            useUpdateBudget,
        ).mockReturnValue(
            updateBudgetMutation,
        )
    })

    it('renders the create-budget form', () => {
        render(
            <BudgetFormDialog
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('heading', {
                name: 'Create budget',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'spinbutton',
                {
                    name: /monthly limit/i,
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'combobox',
                {
                    name: /^month/i,
                },
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'spinbutton',
                {
                    name: /^year/i,
                },
            ),
        ).toBeInTheDocument()
    })

    it('shows validation errors for an empty submission', async () => {
        const user = userEvent.setup()

        render(
            <BudgetFormDialog
                onClose={vi.fn()}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Create budget',
            }),
        )

        expect(
            await screen.findByText(
                'Please select a category.',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Please enter an amount.',
            ),
        ).toBeInTheDocument()

        expect(
            createBudgetMutation
                .mutateAsync,
        ).not.toHaveBeenCalled()
    })

    it('creates a budget', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        createBudgetMutation
            .mutateAsync
            .mockResolvedValue({
                id: 1,
            })

        render(
            <BudgetFormDialog
                onClose={onClose}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        )

        await user.click(
            screen.getByRole('option', {
                name: 'Food & dining',
            }),
        )

        await user.type(
            screen.getByRole(
                'spinbutton',
                {
                    name: /monthly limit/i,
                },
            ),
            '8000',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Create budget',
            }),
        )

        await waitFor(() => {
            expect(
                createBudgetMutation
                    .mutateAsync,
            ).toHaveBeenCalledWith(
                expect.objectContaining({
                    category: 'FOOD',
                    monthlyLimit: '8000',
                    month:
                        expect.any(Number),
                    year:
                        expect.any(Number),
                }),
            )
        })

        expect(
            onClose,
        ).toHaveBeenCalledTimes(1)
    })

    it('renders existing values and updates a budget', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        updateBudgetMutation
            .mutateAsync
            .mockResolvedValue(
                sampleBudget,
            )

        render(
            <BudgetFormDialog
                budget={sampleBudget}
                onClose={onClose}
            />,
        )

        expect(
            screen.getByRole('heading', {
                name: 'Edit budget',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole(
                'spinbutton',
                {
                    name: /monthly limit/i,
                },
            ),
        ).toHaveValue(5000)

        expect(
            screen.getByRole(
                'combobox',
                {
                    name: /^month/i,
                },
            ),
        ).toHaveValue('10')

        expect(
            screen.getByRole(
                'spinbutton',
                {
                    name: /^year/i,
                },
            ),
        ).toHaveValue(2026)

        await user.click(
            screen.getByRole('button', {
                name: 'Save changes',
            }),
        )

        await waitFor(() => {
            expect(
                updateBudgetMutation
                    .mutateAsync,
            ).toHaveBeenCalledWith({
                budgetId: 7,
                budget: {
                    category: 'TRAVEL',
                    monthlyLimit: '5000',
                    month: 10,
                    year: 2026,
                },
            })
        })

        expect(
            onClose,
        ).toHaveBeenCalledTimes(1)
    })

    it('displays backend validation errors', async () => {
        const user = userEvent.setup()

        createBudgetMutation
            .mutateAsync
            .mockRejectedValue({
                response: {
                    data: {
                        detail:
                            'Budget already exists for this category and month',
                    },
                },
            })

        render(
            <BudgetFormDialog
                onClose={vi.fn()}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        )

        await user.click(
            screen.getByRole('option', {
                name: 'Food & dining',
            }),
        )

        await user.type(
            screen.getByRole(
                'spinbutton',
                {
                    name: /monthly limit/i,
                },
            ),
            '8000',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Create budget',
            }),
        )

        expect(
            await screen.findByRole(
                'alert',
            ),
        ).toHaveTextContent(
            'Budget already exists for this category and month',
        )
    })

    it('disables form actions while saving', () => {
        vi.mocked(
            useCreateBudget,
        ).mockReturnValue(
            buildMutation({
                isPending: true,
            }),
        )

        render(
            <BudgetFormDialog
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('button', {
                name: 'Cancel',
            }),
        ).toBeDisabled()

        expect(
            screen.getByRole('button', {
                name: 'Create budget',
            }),
        ).toBeDisabled()

        expect(
            screen.getByRole('button', {
                name: /category select category/i,
            }),
        ).toBeDisabled()
    })
})