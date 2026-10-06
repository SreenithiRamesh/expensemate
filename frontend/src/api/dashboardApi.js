import { apiClient } from './apiClient'

export async function getDashboard(
    month,
) {
    const response = await apiClient.get(
        '/dashboard',
        {
            params: month
                ? { month }
                : undefined,
        },
    )

    return response.data
}