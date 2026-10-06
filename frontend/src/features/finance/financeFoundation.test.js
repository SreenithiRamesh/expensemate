import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { apiClient } from '../../api/apiClient'
import {
    createExpense,
    getExpenses,
    updateExpense,
} from '../../api/expensesApi'
import {
    createBudget,
    getBudgets,
} from '../../api/budgetsApi'
import {
    createRecurringExpense,
    recordRecurringPayment,
    setRecurringExpenseActive,
} from '../../api/recurringExpensesApi'
import { getDashboard } from '../../api/dashboardApi'
import { suggestExpenseDetails } from '../../api/aiExpenseApi'
import {
    EXPENSE_CATEGORIES,
    getCategoryLabel,
    getFrequencyLabel,
} from '../../constants/finance'
import {
    aiExpenseTextSchema,
    budgetSchema,
    expenseSchema,
    recurringExpenseSchema,
} from './financeSchemas'
import { financeQueryKeys } from './financeQueryKeys'
import {
    useCreateExpense,
    useDeleteExpense,
    useExpense,
    useExpenses,
    useUpdateExpense,
} from '../expenses/useExpenseQueries'
import { useAiExpenseSuggestion } from '../expenses/useAiExpenseSuggestion'
import {
    useBudget,
    useBudgets,
    useCreateBudget,
    useDeleteBudget,
    useUpdateBudget,
} from '../budgets/useBudgetQueries'
import {
    useCreateRecurringExpense,
    useDeleteRecurringExpense,
    useRecordRecurringPayment,
    useRecurringExpense,
    useRecurringExpenses,
    useSetRecurringExpenseActive,
    useUpcomingRecurringExpenses,
    useUpdateRecurringExpense,
} from '../recurring-expenses/useRecurringExpenseQueries'
import { useDashboardQuery } from '../dashboard/useDashboardQuery'

vi.mock('../../api/apiClient', () => ({
    apiClient: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}))

describe('finance constants', () => {
    it('contains the categories accepted by the backend', () => {
        expect(EXPENSE_CATEGORIES).toEqual([
            'FOOD',
            'TRAVEL',
            'SHOPPING',
            'BILLS',
            'ENTERTAINMENT',
            'HEALTH',
            'EDUCATION',
            'RENT',
            'SUBSCRIPTION',
            'OTHER',
        ])
    })

    it('formats category and frequency labels', () => {
        expect(
            getCategoryLabel('FOOD'),
        ).toBe('Food & dining')

        expect(
            getFrequencyLabel('MONTHLY'),
        ).toBe('Monthly')
    })
})

describe('finance schemas', () => {
    it('accepts a valid personal expense', () => {
        const result = expenseSchema.safeParse({
            amount: '1250.50',
            category: 'FOOD',
            expenseDate: '2026-09-27',
            description: 'Team lunch',
        })

        expect(result.success).toBe(true)
    })

    it('rejects invalid money values', () => {
        const result = expenseSchema.safeParse({
            amount: '100.999',
            category: 'FOOD',
            expenseDate: '2026-09-27',
            description: '',
        })

        expect(result.success).toBe(false)
    })

    it('rejects unsupported categories', () => {
        const result = expenseSchema.safeParse({
            amount: '100',
            category: 'COFFEE',
            expenseDate: '2026-09-27',
            description: '',
        })

        expect(result.success).toBe(false)
    })

    it('coerces budget month and year to numbers', () => {
        const result = budgetSchema.parse({
            category: 'BILLS',
            monthlyLimit: '6000',
            month: '9',
            year: '2026',
        })

        expect(result.month).toBe(9)
        expect(result.year).toBe(2026)
    })

    it('accepts a valid recurring expense', () => {
        const result =
            recurringExpenseSchema.safeParse({
                title: 'Internet bill',
                amount: '999',
                category: 'BILLS',
                frequency: 'MONTHLY',
                nextDueDate: '2026-10-01',
                active: true,
            })

        expect(result.success).toBe(true)
    })

    it('enforces the AI request length limit', () => {
        expect(
            aiExpenseTextSchema.safeParse({
                text: 'Lunch for ₹640',
            }).success,
        ).toBe(true)

        expect(
            aiExpenseTextSchema.safeParse({
                text: 'x'.repeat(501),
            }).success,
        ).toBe(false)
    })
})

describe('finance query keys', () => {
    it('separates expense lists from expense details', () => {
        expect(
            financeQueryKeys.expenseList({
                page: 0,
                size: 10,
            }),
        ).toEqual([
            'finance',
            'expenses',
            'list',
            {
                category: null,
                startDate: null,
                endDate: null,
                search: null,
                page: 0,
                size: 10,
            },
        ])

        expect(
            financeQueryKeys.expenseDetail(12),
        ).toEqual([
            'finance',
            'expenses',
            'detail',
            12,
        ])
    })

    it('includes the selected month in dashboard keys', () => {
        expect(
            financeQueryKeys.dashboard(
                '2026-09',
            ),
        ).toEqual([
            'finance',
            'dashboard',
            '2026-09',
        ])
    })
})

describe('finance API contracts', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('sends supported expense filters', async () => {
        apiClient.get.mockResolvedValue({
            data: {
                content: [],
            },
        })

        await getExpenses({
            category: 'FOOD',
            startDate: '2026-09-01',
            endDate: '2026-09-30',
            search: ' lunch ',
            page: 1,
            size: 20,
        })

        expect(apiClient.get).toHaveBeenCalledWith(
            '/expenses',
            {
                params: {
                    category: 'FOOD',
                    startDate: '2026-09-01',
                    endDate: '2026-09-30',
                    search: 'lunch',
                    page: 1,
                    size: 20,
                },
            },
        )
    })

    it('normalizes an expense payload', async () => {
        apiClient.post.mockResolvedValue({
            data: {
                id: 1,
            },
        })

        await createExpense({
            amount: '640',
            category: 'FOOD',
            expenseDate: '2026-09-27',
            description: '  ',
        })

        expect(apiClient.post).toHaveBeenCalledWith(
            '/expenses',
            {
                amount: '640',
                category: 'FOOD',
                expenseDate: '2026-09-27',
                description: null,
            },
        )
    })

    it('updates the selected expense', async () => {
        apiClient.put.mockResolvedValue({
            data: {
                id: 9,
            },
        })

        await updateExpense({
            expenseId: 9,
            expense: {
                amount: '450',
                category: 'TRAVEL',
                expenseDate: '2026-09-26',
                description: 'Metro',
            },
        })

        expect(apiClient.put).toHaveBeenCalledWith(
            '/expenses/9',
            {
                amount: '450',
                category: 'TRAVEL',
                expenseDate: '2026-09-26',
                description: 'Metro',
            },
        )
    })

    it('sends optional budget filters', async () => {
        apiClient.get.mockResolvedValue({
            data: [],
        })

        await getBudgets({
            month: '9',
            year: '2026',
        })

        expect(apiClient.get).toHaveBeenCalledWith(
            '/budgets',
            {
                params: {
                    month: 9,
                    year: 2026,
                },
            },
        )
    })

    it('normalizes the budget payload', async () => {
        apiClient.post.mockResolvedValue({
            data: {
                id: 3,
            },
        })

        await createBudget({
            category: 'FOOD',
            monthlyLimit: '8000',
            month: '9',
            year: '2026',
        })

        expect(apiClient.post).toHaveBeenCalledWith(
            '/budgets',
            {
                category: 'FOOD',
                monthlyLimit: '8000',
                month: 9,
                year: 2026,
            },
        )
    })

    it('normalizes recurring-expense creation', async () => {
        apiClient.post.mockResolvedValue({
            data: {
                id: 4,
            },
        })

        await createRecurringExpense({
            title: ' Internet ',
            amount: '999',
            category: 'BILLS',
            frequency: 'MONTHLY',
            nextDueDate: '2026-10-01',
            active: true,
        })

        expect(apiClient.post).toHaveBeenCalledWith(
            '/recurring-expenses',
            {
                title: 'Internet',
                amount: '999',
                category: 'BILLS',
                frequency: 'MONTHLY',
                nextDueDate: '2026-10-01',
                active: true,
            },
        )
    })

    it('uses the complete recurring object when toggling active state', async () => {
        apiClient.put.mockResolvedValue({
            data: {
                id: 4,
                active: false,
            },
        })

        await setRecurringExpenseActive({
            recurringExpense: {
                id: 4,
                title: 'Internet',
                amount: 999,
                category: 'BILLS',
                frequency: 'MONTHLY',
                nextDueDate: '2026-10-01',
                active: true,
            },
            active: false,
        })

        expect(apiClient.put).toHaveBeenCalledWith(
            '/recurring-expenses/4',
            {
                title: 'Internet',
                amount: 999,
                category: 'BILLS',
                frequency: 'MONTHLY',
                nextDueDate: '2026-10-01',
                active: false,
            },
        )
    })

    it('records a recurring payment date', async () => {
        apiClient.post.mockResolvedValue({
            data: {
                id: 4,
            },
        })

        await recordRecurringPayment({
            recurringExpenseId: 4,
            paymentDate: '2026-09-27',
        })

        expect(apiClient.post).toHaveBeenCalledWith(
            '/recurring-expenses/4/record-payment',
            {
                paymentDate: '2026-09-27',
            },
        )
    })

    it('requests the selected dashboard month', async () => {
        apiClient.get.mockResolvedValue({
            data: {
                month: '2026-09',
            },
        })

        await getDashboard('2026-09')

        expect(apiClient.get).toHaveBeenCalledWith(
            '/dashboard',
            {
                params: {
                    month: '2026-09',
                },
            },
        )
    })

    it('validates and trims AI categorization text', async () => {
        apiClient.post.mockResolvedValue({
            data: {
                category: 'FOOD',
            },
        })

        await suggestExpenseDetails(
            '  Lunch ₹640  ',
        )

        expect(apiClient.post).toHaveBeenCalledWith(
            '/ai/expenses/categorize',
            {
                text: 'Lunch ₹640',
            },
        )

        await expect(
            suggestExpenseDetails('x'.repeat(501)),
        ).rejects.toThrow(
            'Expense text must not exceed 500 characters.',
        )
    })
})

describe('finance hooks', () => {
    it('exports all query and mutation hooks', () => {
        const hooks = [
            useExpenses,
            useExpense,
            useCreateExpense,
            useUpdateExpense,
            useDeleteExpense,
            useAiExpenseSuggestion,
            useBudgets,
            useBudget,
            useCreateBudget,
            useUpdateBudget,
            useDeleteBudget,
            useRecurringExpenses,
            useRecurringExpense,
            useUpcomingRecurringExpenses,
            useCreateRecurringExpense,
            useUpdateRecurringExpense,
            useSetRecurringExpenseActive,
            useRecordRecurringPayment,
            useDeleteRecurringExpense,
            useDashboardQuery,
        ]

        hooks.forEach((hook) => {
            expect(hook).toBeTypeOf('function')
        })
    })
})