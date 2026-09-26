const sessionExpiredListeners =
    new Set()

export function subscribeToSessionExpired(
    listener,
) {
    sessionExpiredListeners.add(listener)

    return () => {
        sessionExpiredListeners.delete(
            listener,
        )
    }
}

export function publishSessionExpired(
    reason = 'session-expired',
) {
    sessionExpiredListeners.forEach(
        (listener) => {
            listener({
                reason,
                occurredAt: new Date(),
            })
        },
    )
}