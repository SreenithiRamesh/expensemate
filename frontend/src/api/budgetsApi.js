import { apiClient } from './apiClient'

function normalizeBudgetPayload(budget) {
    return {
        category: budget.category,
        monthlyLimit: budget.monthlyLimit,
        month: Number(budget.month),
        year: Number(budget.year),
    }
}

export async function getBudgets({
                                     month,
                                     year,
                                 } = {}) {
    const response = await apiClient.get(
        '/budgets',
        {
            params: {
                ...(month
                    ? { month: Number(month) }
                    : {}),
                ...(year
                    ? { year: Number(year) }
                    : {}),
            },
        },
    )

    return response.data
}

export async function getBudgetById(
    budgetId,
) {
    const response = await apiClient.get(
        `/budgets/${budgetId}`,
    )

    return response.data
}

export async function createBudget(
    budget,
) {
    const response = await apiClient.post(
        '/budgets',
        normalizeBudgetPayload(budget),
    )

    return response.data
}

export async function updateBudget({
                                       budgetId,
                                       budget,
                                   }) {
    const response = await apiClient.put(
        `/budgets/${budgetId}`,
        normalizeBudgetPayload(budget),
    )

    return response.data
}

export async function deleteBudget(
    budgetId,
) {
    await apiClient.delete(
        `/budgets/${budgetId}`,
    )
}