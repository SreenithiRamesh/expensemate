const defaultApiUrl =
    'http://localhost:8080'

function removeTrailingSlash(value) {
    return value.replace(/\/+$/, '')
}

export const API_BASE_URL =
    removeTrailingSlash(
        import.meta.env.VITE_API_BASE_URL ||
        defaultApiUrl,
    )

export const API_V1_URL =
    `${API_BASE_URL}/api/v1`