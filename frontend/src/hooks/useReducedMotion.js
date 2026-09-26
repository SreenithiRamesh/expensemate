import { useSyncExternalStore } from 'react'

const QUERY = '(prefers-reduced-motion: reduce)'

function getMediaQuery() {
    if (
        typeof window === 'undefined' ||
        typeof window.matchMedia !== 'function'
    ) {
        return null
    }

    return window.matchMedia(QUERY)
}

function subscribe(onChange) {
    const query = getMediaQuery()

    if (!query) {
        return () => {}
    }

    query.addEventListener('change', onChange)

    return () => {
        query.removeEventListener('change', onChange)
    }
}

function getSnapshot() {
    return getMediaQuery()?.matches ?? true
}

function getServerSnapshot() {
    return true
}

export function useReducedMotion() {
    return useSyncExternalStore(
        subscribe,
        getSnapshot,
        getServerSnapshot,
    )
}