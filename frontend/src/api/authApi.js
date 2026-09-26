import { apiClient } from './apiClient'

// The authentication interceptor we add next must respect these flags.
// Authentication requests must not recursively trigger token refresh.
const AUTH_REQUEST_CONFIG = {
    skipAuth: true,
    skipAuthRefresh: true,
}

export async function registerUser({ name, email, password }) {
    const response = await apiClient.post(
        '/auth/register',
        {
            name: name.trim(),
            email: email.trim(),
            password,
        },
        AUTH_REQUEST_CONFIG,
    )

    return response.data
}

export async function loginUser({ email, password }) {
    const response = await apiClient.post(
        '/auth/login',
        {
            email: email.trim(),
            password,
        },
        AUTH_REQUEST_CONFIG,
    )

    return response.data
}

export async function refreshSession(refreshToken) {
    const response = await apiClient.post(
        '/auth/refresh',
        { refreshToken },
        AUTH_REQUEST_CONFIG,
    )

    return response.data
}

export async function logoutUser(refreshToken) {
    await apiClient.post(
        '/auth/logout',
        { refreshToken },
        AUTH_REQUEST_CONFIG,
    )
}