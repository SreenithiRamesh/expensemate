import { apiClient } from './apiClient'

export async function suggestExpenseDetails(
    text,
) {
    const normalizedText = text.trim()

    if (!normalizedText) {
        throw new Error(
            'Expense text is required.',
        )
    }

    if (normalizedText.length > 500) {
        throw new Error(
            'Expense text must not exceed 500 characters.',
        )
    }

    const response = await apiClient.post(
        '/ai/expenses/categorize',
        {
            text: normalizedText,
        },
    )

    return response.data
}