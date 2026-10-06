import { apiClient } from './apiClient'

function compactParams(params) {
    return Object.fromEntries(
        Object.entries(params).filter(
            ([, value]) =>
                value !== undefined &&
                value !== null &&
                value !== '',
        ),
    )
}

function normalizeExpensePayload(expense) {
    return {
        amount: expense.amount,
        category: expense.category,
        expenseDate: expense.expenseDate,
        description:
            expense.description?.trim() || null,
    }
}

export async function getExpenses(
    filters = {},
) {
    const response = await apiClient.get(
        '/expenses',
        {
            params: compactParams({
                category: filters.category,
                startDate: filters.startDate,
                endDate: filters.endDate,
                search: filters.search?.trim(),
                page: filters.page ?? 0,
                size: filters.size ?? 10,
            }),
        },
    )

    return response.data
}

export async function getExpenseById(
    expenseId,
) {
    const response = await apiClient.get(
        `/expenses/${expenseId}`,
    )

    return response.data
}

export async function createExpense(
    expense,
) {
    const response = await apiClient.post(
        '/expenses',
        normalizeExpensePayload(expense),
    )

    return response.data
}

export async function updateExpense({
                                        expenseId,
                                        expense,
                                    }) {
    const response = await apiClient.put(
        `/expenses/${expenseId}`,
        normalizeExpensePayload(expense),
    )

    return response.data
}

export async function deleteExpense(
    expenseId,
) {
    await apiClient.delete(
        `/expenses/${expenseId}`,
    )
}