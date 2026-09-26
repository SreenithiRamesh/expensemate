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
        expiresAt: Date.now() + expiresIn * 1000,
    }
}

export function getSession() {
    return session
}

export function subscribeToSession(listener) {
    listeners.add(listener)

    return () => {
        listeners.delete(listener)
    }
}

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

export function applyRefreshedTokens(
    refreshResponse,
    expectedRefreshToken,
) {
    // Ignore refresh results belonging to a previous session.
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