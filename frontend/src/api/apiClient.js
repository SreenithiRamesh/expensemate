import axios from 'axios'

import { API_V1_URL } from '../constants/environment'
import {
    applyRefreshedTokens,
    clearSession,
    getSession,
} from '../features/auth/authSession'

// Each login creates a new user object in authSession.
// Token refresh preserves that object, so it identifies the session.
const sessionKeys = new WeakMap()
let nextSessionKey = 1

// Concurrent failures using the same refresh token share one request.
const pendingRefreshes = new Map()

function getSessionKey(session) {
    if (!session) {
        return null
    }

    if (!sessionKeys.has(session.user)) {
        sessionKeys.set(session.user, nextSessionKey++)
    }

    return sessionKeys.get(session.user)
}

function createCorrelationId() {
    if (
        typeof crypto !== 'undefined' &&
        typeof crypto.randomUUID === 'function'
    ) {
        return crypto.randomUUID()
    }

    return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const apiClient = axios.create({
    baseURL: API_V1_URL,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
})

// Separate client: refreshing cannot trigger another refresh interceptor.
// Do not import authApi here; authApi already imports apiClient.
const refreshClient = axios.create({
    baseURL: API_V1_URL,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
})

function refreshAccessToken(session) {
    const refreshToken = session.refreshToken

    const existingRequest = pendingRefreshes.get(refreshToken)

    if (existingRequest) {
        return existingRequest
    }

    const refreshRequest = refreshClient
        .post(
            '/auth/refresh',
            { refreshToken },
            {
                headers: {
                    'X-Correlation-ID': createCorrelationId(),
                },
            },
        )
        .then((response) => {
            const applied = applyRefreshedTokens(
                response.data,
                refreshToken,
            )

            if (!applied) {
                throw new axios.CanceledError(
                    'Authentication session changed during refresh',
                )
            }
        })
        .catch((error) => {
            const status = error.response?.status

            // Clear only the session whose refresh token was rejected.
            // Network errors and server failures do not prove that
            // the user's credentials are invalid.
            if (
                [400, 401, 403].includes(status) &&
                getSession()?.refreshToken === refreshToken
            ) {
                clearSession()
            }

            throw error
        })
        .finally(() => {
            pendingRefreshes.delete(refreshToken)
        })

    pendingRefreshes.set(refreshToken, refreshRequest)

    return refreshRequest
}

apiClient.interceptors.request.use((config) => {
    if (!config.headers.has('X-Correlation-ID')) {
        config.headers.set('X-Correlation-ID', createCorrelationId())
    }

    // Login/register/refresh/logout requests from authApi
    // explicitly opt out of automatic bearer authentication.
    if (config.skipAuth) {
        config.headers.delete('Authorization')
        return config
    }

    const session = getSession()
    const sessionKey = getSessionKey(session)

    // A logout or new login may occur while a retry is queued.
    if (
        config._authRetry &&
        config._authSessionKey !== sessionKey
    ) {
        throw new axios.CanceledError(
            'Authentication session changed before retry',
        )
    }

    config._authSessionKey = sessionKey
    config._authAccessToken = session?.accessToken ?? null

    if (session) {
        config.headers.set(
            'Authorization',
            `Bearer ${session.accessToken}`,
        )
    } else {
        config.headers.delete('Authorization')
    }

    return config
})

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const request = error.config

        if (
            !request ||
            error.response?.status !== 401 ||
            request.skipAuth ||
            request.skipAuthRefresh ||
            !request._authAccessToken
        ) {
            throw error
        }

        const session = getSession()

        // Never replay a previous session's request after a new login.
        if (
            !session ||
            getSessionKey(session) !== request._authSessionKey
        ) {
            throw error
        }

        // Retry an authenticated request at most once.
        if (request._authRetry) {
            if (session.accessToken === request._authAccessToken) {
                clearSession()
            }

            throw error
        }

        request._authRetry = true

        // Another request may already have refreshed the token.
        // If so, reuse that token without refreshing again.
        if (session.accessToken === request._authAccessToken) {
            await refreshAccessToken(session)
        }

        const currentSession = getSession()

        if (
            !currentSession ||
            getSessionKey(currentSession) !== request._authSessionKey
        ) {
            throw new axios.CanceledError(
                'Authentication session changed before retry',
            )
        }

        // Request interceptor attaches the latest access token.
        // Existing headers, including Idempotency-Key, are preserved.
        return apiClient.request(request)
    },
)