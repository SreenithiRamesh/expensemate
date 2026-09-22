export function getApiErrorMessage(
    error,
    fallbackMessage = 'Something went wrong. Please try again.',
) {
    const problem = error?.response?.data

    if (
        typeof problem?.detail === 'string' &&
        problem.detail.trim()
    ) {
        return problem.detail
    }

    if (
        typeof problem?.message === 'string' &&
        problem.message.trim()
    ) {
        return problem.message
    }

    if (
        typeof error?.message === 'string' &&
        error.message.trim()
    ) {
        return error.message
    }

    return fallbackMessage
}