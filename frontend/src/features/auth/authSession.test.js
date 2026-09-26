import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    applyRefreshedTokens,
    clearSession,
    getSession,
    startSession,
    subscribeToSession,
} from './authSession'

const validLoginResponse = {
    accessToken: 'access-token-one',
    refreshToken: 'refresh-token-one',
    expiresIn: 3600,
    userId: 1,
    name: 'Sreenithi',
    email: 'sreenithi@example.com',
}

describe('authSession', () => {
    beforeEach(() => {
        clearSession()

        vi.spyOn(
            Date,
            'now',
        ).mockReturnValue(1_000_000)
    })

    afterEach(() => {
        clearSession()
        vi.restoreAllMocks()
    })

    it('starts a valid authenticated session', () => {
        const session =
            startSession(validLoginResponse)

        expect(session).toEqual({
            accessToken: 'access-token-one',
            refreshToken: 'refresh-token-one',
            expiresAt: 4_600_000,
            user: {
                id: 1,
                name: 'Sreenithi',
                email: 'sreenithi@example.com',
            },
        })

        expect(getSession()).toBe(session)
        expect(Object.isFrozen(session)).toBe(true)
        expect(Object.isFrozen(session.user)).toBe(true)
    })

    it('rejects a response without an access token', () => {
        expect(() => {
            startSession({
                ...validLoginResponse,
                accessToken: '',
            })
        }).toThrow(
            'Invalid authentication response: accessToken',
        )

        expect(getSession()).toBeNull()
    })

    it('rejects a response without a refresh token', () => {
        expect(() => {
            startSession({
                ...validLoginResponse,
                refreshToken: '',
            })
        }).toThrow(
            'Invalid authentication response: refreshToken',
        )

        expect(getSession()).toBeNull()
    })

    it('rejects an invalid expiration duration', () => {
        expect(() => {
            startSession({
                ...validLoginResponse,
                expiresIn: 0,
            })
        }).toThrow(
            'Invalid authentication response: expiresIn',
        )

        expect(getSession()).toBeNull()
    })

    it('rejects a response without user details', () => {
        expect(() => {
            startSession({
                ...validLoginResponse,
                name: '',
            })
        }).toThrow(
            'Invalid authentication response: name',
        )

        expect(getSession()).toBeNull()
    })

    it('rotates both tokens while preserving the user', () => {
        const originalSession =
            startSession(validLoginResponse)

        const updated =
            applyRefreshedTokens(
                {
                    accessToken: 'access-token-two',
                    refreshToken: 'refresh-token-two',
                    expiresIn: 7200,
                },
                'refresh-token-one',
            )

        const refreshedSession =
            getSession()

        expect(updated).toBe(true)

        expect(
            refreshedSession.accessToken,
        ).toBe('access-token-two')

        expect(
            refreshedSession.refreshToken,
        ).toBe('refresh-token-two')

        expect(
            refreshedSession.user,
        ).toBe(originalSession.user)
    })

    it('ignores a stale refresh response', () => {
        const originalSession =
            startSession(validLoginResponse)

        const updated =
            applyRefreshedTokens(
                {
                    accessToken: 'stale-access-token',
                    refreshToken: 'stale-refresh-token',
                    expiresIn: 7200,
                },
                'incorrect-refresh-token',
            )

        expect(updated).toBe(false)
        expect(getSession()).toBe(originalSession)
    })

    it('does not restore a cleared session from a refresh response', () => {
        startSession(validLoginResponse)
        clearSession()

        const updated =
            applyRefreshedTokens(
                {
                    accessToken: 'new-access-token',
                    refreshToken: 'new-refresh-token',
                    expiresIn: 7200,
                },
                'refresh-token-one',
            )

        expect(updated).toBe(false)
        expect(getSession()).toBeNull()
    })

    it('notifies subscribers when the session changes', () => {
        const listener = vi.fn()

        const unsubscribe =
            subscribeToSession(listener)

        startSession(validLoginResponse)

        expect(listener).toHaveBeenCalledTimes(1)

        clearSession()

        expect(listener).toHaveBeenCalledTimes(2)

        unsubscribe()

        startSession(validLoginResponse)

        expect(listener).toHaveBeenCalledTimes(2)
    })

    it('clears an authenticated session', () => {
        startSession(validLoginResponse)

        clearSession()

        expect(getSession()).toBeNull()
    })
})