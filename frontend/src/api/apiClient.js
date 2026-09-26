import axios from 'axios'

import { API_V1_URL } from '../constants/environment'
import {
    applyRefreshedTokens,
    clearSession,
    getSession,
} from '../features/auth/authSession'
import { publishSessionExpired } from '../features/auth/authEvents'

// Each login creates a new user object in authSession.
// Refresh preserves the user object, so it identifies the session.
const sessionKeys = new WeakMap()
let nextSessionKey = 1

// Concurrent failures using the same refresh token share one request.
const pendingRefreshes = new Map()

function getSessionKey(session) {
    if (!session) {
        return null
    }

    if (!sessionKeys.has(session.user)) {
        sessionKeys.set(session.user, nextSessionKey)
        nextSessionKey += 1
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

// This separate client does not have the normal response interceptor.
// Therefore, a failed refresh cannot recursively trigger another refresh.
export const authRefreshClient = axios.create({
    baseURL: API_V1_URL,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
})

function expireSession(expectedRefreshToken, reason) {
    const currentSession = getSession()

    // A refresh from an older session must never log out a newer session.
    if (
        !currentSession ||
        currentSession.refreshToken !== expectedRefreshToken
    ) {
        return false
    }

    clearSession()

    publishSessionExpired(
        reason)

    return true
}

function refreshAccessToken(session) {
    const refreshToken = session.refreshToken
    const existingRequest = pendingRefreshes.get(refreshToken)

    if (existingRequest) {
        return existingRequest
    }

    const refreshRequest = authRefreshClient
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

            // These responses mean that the refresh token is no longer valid.
            // Network errors and backend 5xx errors do not prove that the
            // user session is invalid, so the local session is preserved.
            if ([400, 401, 403].includes(status)) {
                expireSession(
                    refreshToken,
                    'refresh-token-rejected',
                )
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
        config.headers.set(
            'X-Correlation-ID',
            createCorrelationId(),
        )
    }

    // Login, registration and logout requests explicitly opt out of
    // bearer authentication and automatic refresh.
    if (config.skipAuth) {
        config.headers.delete('Authorization')
        return config
    }

    const session = getSession()
    const sessionKey = getSessionKey(session)

    // A logout or new login may occur while a retried request is queued.
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

        // Do not replay a request belonging to an older login session.
        if (
            !session ||
            getSessionKey(session) !== request._authSessionKey
        ) {
            throw error
        }

        // An authenticated request may only be retried once.
        if (request._authRetry) {
            if (
                session.accessToken === request._authAccessToken
            ) {
                expireSession(
                    session.refreshToken,
                    'retried-request-rejected',
                )
            }

            throw error
        }

        request._authRetry = true

        // Another failed request might already have refreshed the token.
        // If the access token changed, reuse it instead of refreshing again.
        if (
            session.accessToken === request._authAccessToken
        ) {
            await refreshAccessToken(session)
        }

        const currentSession = getSession()

        if (
            !currentSession ||
            getSessionKey(currentSession) !==
            request._authSessionKey
        ) {
            throw new axios.CanceledError(
                'Authentication session changed before retry',
            )
        }

        // The request interceptor attaches the latest access token.
        // Other headers, including Idempotency-Key, are preserved.
        return apiClient.request(request)
    },
)