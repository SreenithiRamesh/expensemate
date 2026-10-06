import { apiClient } from './apiClient'

function normalizeRecurringExpensePayload(
    recurringExpense,
) {
    return {
        title: recurringExpense.title.trim(),
        amount: recurringExpense.amount,
        category: recurringExpense.category,
        frequency: recurringExpense.frequency,
        nextDueDate:
        recurringExpense.nextDueDate,
        active:
            recurringExpense.active ?? true,
    }
}

export async function getRecurringExpenses() {
    const response = await apiClient.get(
        '/recurring-expenses',
    )

    return response.data
}

export async function getRecurringExpenseById(
    recurringExpenseId,
) {
    const response = await apiClient.get(
        `/recurring-expenses/${recurringExpenseId}`,
    )

    return response.data
}

export async function getUpcomingRecurringExpenses(
    days = 30,
) {
    const response = await apiClient.get(
        '/recurring-expenses/upcoming',
        {
            params: {
                days: Number(days),
            },
        },
    )

    return response.data
}

export async function createRecurringExpense(
    recurringExpense,
) {
    const response = await apiClient.post(
        '/recurring-expenses',
        normalizeRecurringExpensePayload(
            recurringExpense,
        ),
    )

    return response.data
}

export async function updateRecurringExpense({
                                                 recurringExpenseId,
                                                 recurringExpense,
                                             }) {
    const response = await apiClient.put(
        `/recurring-expenses/${recurringExpenseId}`,
        normalizeRecurringExpensePayload(
            recurringExpense,
        ),
    )

    return response.data
}

export async function setRecurringExpenseActive({
                                                    recurringExpense,
                                                    active,
                                                }) {
    return updateRecurringExpense({
        recurringExpenseId:
        recurringExpense.id,

        recurringExpense: {
            title: recurringExpense.title,
            amount: recurringExpense.amount,
            category: recurringExpense.category,
            frequency:
            recurringExpense.frequency,
            nextDueDate:
            recurringExpense.nextDueDate,
            active,
        },
    })
}

export async function recordRecurringPayment({
                                                 recurringExpenseId,
                                                 paymentDate,
                                             }) {
    const response = await apiClient.post(
        `/recurring-expenses/${recurringExpenseId}/record-payment`,
        {
            paymentDate,
        },
    )

    return response.data
}

export async function deleteRecurringExpense(
    recurringExpenseId,
) {
    await apiClient.delete(
        `/recurring-expenses/${recurringExpenseId}`,
    )
}