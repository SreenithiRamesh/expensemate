// Authentication state lives in memory.
// Never log tokens or include them in URLs.

const listeners = new Set()

let session = null

function notifyListeners() {
    listeners.forEach((listener) => listener())
}

function requireNonEmptyString(value, field) {
    if (typeof value !== 'string' || value.trim().length === 0) {
        throw new Error(`Invalid authentication response: ${field}`)
    }

    return value
}

function readTokens(response) {
    if (!response || typeof response !== 'object') {
        throw new Error('Invalid authentication response')
    }

    const accessToken = requireNonEmptyString(
        response.accessToken,
        'accessToken',
    )

    const refreshToken = requireNonEmptyString(
        response.refreshToken,
        'refreshToken',
    )

    const expiresIn = Number(response.expiresIn)

    if (!Number.isFinite(expiresIn) || expiresIn <= 0) {
        throw new Error('Invalid authentication response: expiresIn')
    }

    return {
        accessToken,
        refreshToken,
        // The backend returns expiresIn in seconds.
        expiresAt: Date.now() + expiresIn * 1000,
    }
}

// Returns the same object until the session changes.
// This is suitable for React's useSyncExternalStore.
export function getSession() {
    return session
}

export function subscribeToSession(listener) {
    listeners.add(listener)

    return () => {
        listeners.delete(listener)
    }
}

// Call only after a successful login.
// Registration does not return tokens in our backend.
export function startSession(loginResponse) {
    const tokens = readTokens(loginResponse)

    if (loginResponse.userId == null) {
        throw new Error('Invalid authentication response: userId')
    }

    const user = Object.freeze({
        id: loginResponse.userId,
        name: requireNonEmptyString(loginResponse.name, 'name'),
        email: requireNonEmptyString(loginResponse.email, 'email'),
    })

    session = Object.freeze({
        ...tokens,
        user,
    })

    notifyListeners()

    return session
}

// Refresh responses contain tokens but no user details.
// Preserve the existing user and replace both rotated tokens.
//
// Checking the original refresh token prevents an old request
// from restoring a logged-out session or replacing a newer login.
export function applyRefreshedTokens(
    refreshResponse,
    expectedRefreshToken,
) {
    if (
        !session ||
        session.refreshToken !== expectedRefreshToken
    ) {
        return false
    }

    const tokens = readTokens(refreshResponse)

    session = Object.freeze({
        ...tokens,
        user: session.user,
    })

    notifyListeners()

    return true
}

export function clearSession() {
    if (session === null) {
        return
    }

    session = null
    notifyListeners()
}