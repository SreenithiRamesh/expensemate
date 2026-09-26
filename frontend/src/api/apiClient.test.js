import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    apiClient,
    authRefreshClient,
} from './apiClient'
import {
    clearSession,
    getSession,
    startSession,
} from '../features/auth/authSession'
import {
    subscribeToSessionExpired,
} from '../features/auth/authEvents'

const originalApiAdapter =
    apiClient.defaults.adapter

const originalRefreshAdapter =
    authRefreshClient.defaults.adapter

function createLoginResponse(overrides = {}) {
    return {
        accessToken: 'access-token-1',
        refreshToken: 'refresh-token-1',
        expiresIn: 900,
        userId: 42,
        name: 'Sreenithi',
        email: 'sree@example.com',
        ...overrides,
    }
}

function createRefreshResponse(overrides = {}) {
    return {
        accessToken: 'access-token-2',
        refreshToken: 'refresh-token-2',
        expiresIn: 900,
        ...overrides,
    }
}

function createSuccessResponse(
    config,
    data = { success: true },
    status = 200,
) {
    return {
        data,
        status,
        statusText: 'OK',
        headers: {},
        config,
        request: {},
    }
}

function createHttpError(config, status) {
    const error = new Error(
        `Request failed with status code ${status}`,
    )

    error.config = config

    error.response = {
        data: {
            status,
            detail: `HTTP ${status}`,
        },
        status,
        statusText: String(status),
        headers: {},
        config,
        request: {},
    }

    return error
}

function rejectWithStatus(config, status) {
    return Promise.reject(
        createHttpError(config, status),
    )
}

describe('apiClient authentication interceptors', () => {
    beforeEach(() => {
        clearSession()

        apiClient.defaults.adapter =
            originalApiAdapter

        authRefreshClient.defaults.adapter =
            originalRefreshAdapter
    })

    afterEach(() => {
        clearSession()

        apiClient.defaults.adapter =
            originalApiAdapter

        authRefreshClient.defaults.adapter =
            originalRefreshAdapter

        vi.restoreAllMocks()
    })

    it('attaches the access token and correlation ID', async () => {
        startSession(createLoginResponse())

        const adapter = vi.fn(async (config) =>
            createSuccessResponse(config),
        )

        apiClient.defaults.adapter = adapter

        await apiClient.get('/expenses')

        expect(adapter).toHaveBeenCalledTimes(1)

        const config = adapter.mock.calls[0][0]

        expect(
            config.headers.get('Authorization'),
        ).toBe('Bearer access-token-1')

        expect(
            config.headers.get('X-Correlation-ID'),
        ).toEqual(expect.any(String))
    })

    it('does not attach authorization when skipAuth is enabled', async () => {
        startSession(createLoginResponse())

        const adapter = vi.fn(async (config) =>
            createSuccessResponse(config),
        )

        apiClient.defaults.adapter = adapter

        await apiClient.post(
            '/auth/login',
            {
                email: 'sree@example.com',
                password: 'password123',
            },
            {
                skipAuth: true,
                skipAuthRefresh: true,
            },
        )

        const config = adapter.mock.calls[0][0]

        expect(
            config.headers.has('Authorization'),
        ).toBe(false)
    })

    it('refreshes the session and retries the original request', async () => {
        startSession(createLoginResponse())

        const apiAdapter = vi.fn((config) => {
            if (!config._authRetry) {
                return rejectWithStatus(config, 401)
            }

            return Promise.resolve(
                createSuccessResponse(config, {
                    expenses: [],
                }),
            )
        })

        const refreshAdapter = vi.fn(
            async (config) =>
                createSuccessResponse(
                    config,
                    createRefreshResponse(),
                ),
        )

        apiClient.defaults.adapter = apiAdapter
        authRefreshClient.defaults.adapter =
            refreshAdapter

        const response =
            await apiClient.get('/expenses')

        expect(response.data).toEqual({
            expenses: [],
        })

        expect(refreshAdapter).toHaveBeenCalledTimes(1)
        expect(apiAdapter).toHaveBeenCalledTimes(2)

        expect(getSession()).toMatchObject({
            accessToken: 'access-token-2',
            refreshToken: 'refresh-token-2',
        })

        const retriedConfig =
            apiAdapter.mock.calls[1][0]

        expect(
            retriedConfig.headers.get('Authorization'),
        ).toBe('Bearer access-token-2')
    })

    it('uses one refresh request for concurrent 401 responses', async () => {
        startSession(createLoginResponse())

        const requestSnapshots = []

        const apiAdapter = vi.fn((config) => {
            const snapshot = {
                url: config.url,
                isRetry: Boolean(config._authRetry),
                authorization:
                    config.headers.get('Authorization'),
            }

            requestSnapshots.push(snapshot)

            if (!snapshot.isRetry) {
                return rejectWithStatus(config, 401)
            }

            return Promise.resolve(
                createSuccessResponse(config, {
                    path: config.url,
                }),
            )
        })

        const refreshAdapter = vi.fn(
            async (config) =>
                createSuccessResponse(
                    config,
                    createRefreshResponse(),
                ),
        )

        apiClient.defaults.adapter = apiAdapter
        authRefreshClient.defaults.adapter =
            refreshAdapter

        const [expenses, budgets] =
            await Promise.all([
                apiClient.get('/expenses'),
                apiClient.get('/budgets'),
            ])

        expect(expenses.data.path).toBe('/expenses')
        expect(budgets.data.path).toBe('/budgets')

        expect(refreshAdapter).toHaveBeenCalledTimes(1)
        expect(apiAdapter).toHaveBeenCalledTimes(4)

        const initialRequests =
            requestSnapshots.filter(
                (request) => !request.isRetry,
            )

        const retriedRequests =
            requestSnapshots.filter(
                (request) => request.isRetry,
            )

        expect(initialRequests).toHaveLength(2)
        expect(retriedRequests).toHaveLength(2)

        initialRequests.forEach((request) => {
            expect(request.authorization).toBe(
                'Bearer access-token-1',
            )
        })

        retriedRequests.forEach((request) => {
            expect(request.authorization).toBe(
                'Bearer access-token-2',
            )
        })

        expect(
            retriedRequests.map(
                (request) => request.url,
            ),
        ).toEqual(
            expect.arrayContaining([
                '/expenses',
                '/budgets',
            ]),
        )
    })

    it('preserves custom headers when retrying a request', async () => {
        startSession(createLoginResponse())

        const apiAdapter = vi.fn((config) => {
            if (!config._authRetry) {
                return rejectWithStatus(config, 401)
            }

            return Promise.resolve(
                createSuccessResponse(config),
            )
        })

        authRefreshClient.defaults.adapter =
            vi.fn(async (config) =>
                createSuccessResponse(
                    config,
                    createRefreshResponse(),
                ),
            )

        apiClient.defaults.adapter = apiAdapter

        await apiClient.post(
            '/settlements',
            { amount: 500 },
            {
                headers: {
                    'Idempotency-Key':
                        'settlement-request-123',
                },
            },
        )

        const retriedConfig =
            apiAdapter.mock.calls[1][0]

        expect(
            retriedConfig.headers.get(
                'Idempotency-Key',
            ),
        ).toBe('settlement-request-123')
    })

    it('clears the session when the refresh token is rejected', async () => {
        startSession(createLoginResponse())

        const sessionExpiredListener = vi.fn()

        const unsubscribe =
            subscribeToSessionExpired(
                sessionExpiredListener,
            )

        apiClient.defaults.adapter = vi.fn(
            (config) =>
                rejectWithStatus(config, 401),
        )

        authRefreshClient.defaults.adapter =
            vi.fn((config) =>
                rejectWithStatus(config, 401),
            )

        await expect(
            apiClient.get('/expenses'),
        ).rejects.toMatchObject({
            response: {
                status: 401,
            },
        })

        expect(getSession()).toBeNull()

        expect(
            sessionExpiredListener,
        ).toHaveBeenCalledTimes(1)

        expect(
            sessionExpiredListener,
        ).toHaveBeenCalledWith(
            expect.objectContaining({
                reason:
                    'refresh-token-rejected',
                occurredAt: expect.any(Date),
            }),
        )

        unsubscribe()
    })

    it('preserves the session for a temporary refresh server failure', async () => {
        startSession(createLoginResponse())

        apiClient.defaults.adapter = vi.fn(
            (config) =>
                rejectWithStatus(config, 401),
        )

        authRefreshClient.defaults.adapter =
            vi.fn((config) =>
                rejectWithStatus(config, 503),
            )

        await expect(
            apiClient.get('/expenses'),
        ).rejects.toMatchObject({
            response: {
                status: 503,
            },
        })

        expect(getSession()).not.toBeNull()

        expect(getSession()).toMatchObject({
            accessToken: 'access-token-1',
            refreshToken: 'refresh-token-1',
        })
    })

    it('does not enter an infinite refresh loop when the retry returns 401', async () => {
        startSession(createLoginResponse())

        const sessionExpiredListener = vi.fn()

        const unsubscribe =
            subscribeToSessionExpired(
                sessionExpiredListener,
            )

        const apiAdapter = vi.fn(
            (config) =>
                rejectWithStatus(config, 401),
        )

        const refreshAdapter = vi.fn(
            async (config) =>
                createSuccessResponse(
                    config,
                    createRefreshResponse(),
                ),
        )

        apiClient.defaults.adapter = apiAdapter
        authRefreshClient.defaults.adapter =
            refreshAdapter

        await expect(
            apiClient.get('/expenses'),
        ).rejects.toMatchObject({
            response: {
                status: 401,
            },
        })

        expect(refreshAdapter).toHaveBeenCalledTimes(1)
        expect(apiAdapter).toHaveBeenCalledTimes(2)
        expect(getSession()).toBeNull()

        expect(
            sessionExpiredListener,
        ).toHaveBeenCalledWith(
            expect.objectContaining({
                reason:
                    'retried-request-rejected',
            }),
        )

        unsubscribe()
    })
})