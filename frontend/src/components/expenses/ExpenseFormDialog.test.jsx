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
import { toast } from 'sonner'

import {
    useAiExpenseSuggestion,
} from '../../features/expenses/useAiExpenseSuggestion'
import {
    useCreateExpense,
    useUpdateExpense,
} from '../../features/expenses/useExpenseQueries'
import { ExpenseFormDialog } from './ExpenseFormDialog'

vi.mock(
    '../../features/expenses/useExpenseQueries',
    () => ({
        useCreateExpense: vi.fn(),
        useUpdateExpense: vi.fn(),
    }),
)

vi.mock(
    '../../features/expenses/useAiExpenseSuggestion',
    () => ({
        useAiExpenseSuggestion: vi.fn(),
    }),
)

vi.mock('sonner', () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
    },
}))

const createExpense = vi.fn()
const updateExpense = vi.fn()
const requestAiSuggestion = vi.fn()

function getAmountInput() {
    return screen.getByRole('spinbutton', {
        name: /amount/i,
    })
}

function getExpenseDateInput() {
    return screen.getByLabelText(
        /expense date/i,
    )
}

function getManualDescriptionInput() {
    return screen.getByRole('textbox', {
        name: /^description$/i,
    })
}

function getAiDescriptionInput() {
    return screen.getByLabelText(
        'Expense description for AI suggestion',
    )
}

async function chooseCategory(
    user,
    categoryName,
) {
    const trigger =
        screen
            .getByText('Select category')
            .closest('button')

    expect(trigger).not.toBeNull()

    await user.click(trigger)

    await user.click(
        screen.getByRole('option', {
            name: categoryName,
        }),
    )
}

async function completeManualForm(user) {
    await user.type(
        getAmountInput(),
        '640',
    )

    await chooseCategory(
        user,
        'Food & dining',
    )

    await user.type(
        getManualDescriptionInput(),
        'Team lunch',
    )
}

describe('ExpenseFormDialog', () => {
    beforeEach(() => {
        createExpense.mockReset()
        updateExpense.mockReset()
        requestAiSuggestion.mockReset()

        vi.mocked(
            toast.success,
        ).mockClear()

        vi.mocked(
            toast.error,
        ).mockClear()

        createExpense.mockResolvedValue({
            id: 1,
        })

        updateExpense.mockResolvedValue({
            id: 7,
        })

        requestAiSuggestion.mockResolvedValue({
            amount: 640,
            category: 'FOOD',
            expenseDate: '2026-10-06',
            description: 'Team lunch',
            requiresReview: false,
            remainingRequests: 4,
        })

        vi.mocked(
            useCreateExpense,
        ).mockReturnValue({
            mutateAsync: createExpense,
            isPending: false,
        })

        vi.mocked(
            useUpdateExpense,
        ).mockReturnValue({
            mutateAsync: updateExpense,
            isPending: false,
        })

        vi.mocked(
            useAiExpenseSuggestion,
        ).mockReturnValue({
            mutateAsync:
            requestAiSuggestion,
            isPending: false,
        })
    })

    it('renders the create-expense form', () => {
        render(
            <ExpenseFormDialog
                onClose={vi.fn()}
            />,
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Add expense',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('heading', {
                name: 'Add expense',
            }),
        ).toBeInTheDocument()

        expect(
            getAmountInput(),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Select category',
            ),
        ).toBeInTheDocument()

        expect(
            getExpenseDateInput(),
        ).toBeInTheDocument()

        expect(
            getManualDescriptionInput(),
        ).toBeInTheDocument()

        expect(
            getAiDescriptionInput(),
        ).toBeInTheDocument()
    })

    it('creates an expense using manually entered values', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <ExpenseFormDialog
                onClose={onClose}
            />,
        )

        await completeManualForm(user)

        await user.click(
            screen.getByRole('button', {
                name: 'Add expense',
            }),
        )

        await waitFor(() => {
            expect(
                createExpense,
            ).toHaveBeenCalledTimes(1)
        })

        const submittedExpense =
            createExpense.mock.calls[0][0]

        expect(
            Number(submittedExpense.amount),
        ).toBe(640)

        expect(
            submittedExpense,
        ).toEqual(
            expect.objectContaining({
                category: 'FOOD',
                description: 'Team lunch',
            }),
        )

        expect(
            toast.success,
        ).toHaveBeenCalledWith(
            'Expense added successfully.',
        )

        expect(onClose).toHaveBeenCalledTimes(1)
    })

    it('applies an AI expense suggestion to the form', async () => {
        const user = userEvent.setup()

        render(
            <ExpenseFormDialog
                onClose={vi.fn()}
            />,
        )

        await user.type(
            getAiDescriptionInput(),
            'Paid for team lunch',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Suggest',
            }),
        )

        await waitFor(() => {
            expect(
                requestAiSuggestion,
            ).toHaveBeenCalledWith(
                'Paid for team lunch',
            )
        })

        await waitFor(() => {
            expect(
                getAmountInput(),
            ).toHaveValue(640)

            expect(
                getExpenseDateInput(),
            ).toHaveValue('2026-10-06')

            expect(
                getManualDescriptionInput(),
            ).toHaveValue('Team lunch')
        })

        expect(
            screen.getByRole('button', {
                name: /category food & dining/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Suggestion applied',
            ),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'AI requests remaining: 4',
            ),
        ).toBeInTheDocument()

        expect(
            toast.success,
        ).toHaveBeenCalledWith(
            'AI suggestion applied. Review it before saving.',
        )
    })

    it('shows AI unavailable state without blocking manual creation', async () => {
        const user = userEvent.setup()

        requestAiSuggestion.mockRejectedValueOnce(
            new Error(
                'Gemini is temporarily unavailable.',
            ),
        )

        render(
            <ExpenseFormDialog
                onClose={vi.fn()}
            />,
        )

        await user.type(
            getAiDescriptionInput(),
            'Dinner',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Suggest',
            }),
        )

        await waitFor(() => {
            expect(
                requestAiSuggestion,
            ).toHaveBeenCalledWith(
                'Dinner',
            )
        })

        expect(
            await screen.findByText(
                /Gemini is temporarily unavailable/i,
            ),
        ).toBeInTheDocument()

        await completeManualForm(user)

        await user.click(
            screen.getByRole('button', {
                name: 'Add expense',
            }),
        )

        await waitFor(() => {
            expect(
                createExpense,
            ).toHaveBeenCalledTimes(1)
        })

        expect(
            createExpense,
        ).toHaveBeenCalledWith(
            expect.objectContaining({
                category: 'FOOD',
                description: 'Team lunch',
            }),
        )
    })

    it('renders existing values and updates an expense', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        const expense = {
            id: 7,
            amount: 1200,
            category: 'TRAVEL',
            expenseDate: '2026-10-01',
            description: 'Train ticket',
        }

        render(
            <ExpenseFormDialog
                expense={expense}
                onClose={onClose}
            />,
        )

        expect(
            screen.getByRole('dialog', {
                name: 'Edit expense',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('heading', {
                name: 'Edit expense',
            }),
        ).toBeInTheDocument()

        expect(
            getAmountInput(),
        ).toHaveValue(1200)

        expect(
            screen.getByRole('button', {
                name: /category travel/i,
            }),
        ).toBeInTheDocument()

        expect(
            getExpenseDateInput(),
        ).toHaveValue('2026-10-01')

        expect(
            getManualDescriptionInput(),
        ).toHaveValue('Train ticket')

        const description =
            getManualDescriptionInput()

        await user.clear(description)

        await user.type(
            description,
            'Updated train ticket',
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Save changes',
            }),
        )

        await waitFor(() => {
            expect(
                updateExpense,
            ).toHaveBeenCalledTimes(1)
        })

        expect(
            updateExpense,
        ).toHaveBeenCalledWith({
            expenseId: 7,
            expense: expect.objectContaining({
                category: 'TRAVEL',
                description:
                    'Updated train ticket',
            }),
        })

        expect(
            toast.success,
        ).toHaveBeenCalledWith(
            'Expense updated successfully.',
        )

        expect(onClose).toHaveBeenCalledTimes(1)
    })

    it('shows backend submission errors and keeps the dialog open', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        createExpense.mockRejectedValueOnce({
            response: {
                data: {
                    detail:
                        'The expense could not be saved.',
                },
            },
        })

        render(
            <ExpenseFormDialog
                onClose={onClose}
            />,
        )

        await completeManualForm(user)

        await user.click(
            screen.getByRole('button', {
                name: 'Add expense',
            }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'The expense could not be saved.',
        )

        expect(
            createExpense,
        ).toHaveBeenCalledTimes(1)

        expect(onClose).not.toHaveBeenCalled()

        expect(
            screen.getByRole('dialog', {
                name: 'Add expense',
            }),
        ).toBeInTheDocument()
    })

    it('closes when Cancel is selected', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()

        render(
            <ExpenseFormDialog
                onClose={onClose}
            />,
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Cancel',
            }),
        )

        expect(onClose).toHaveBeenCalledTimes(1)
    })
})