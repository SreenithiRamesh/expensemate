import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from 'react'

import { API_V1_URL } from '../../constants/environment'
import { SERVER_HEALTH } from './serverHealth'

const HEALTH_URL = `${API_V1_URL}/health`

const REQUEST_TIMEOUT_MS = 8_000
const POLL_INTERVAL_MS = 10_000
const WAKING_FAILURE_LIMIT = 2

export function useServerHealth() {
    const [status, setStatus] = useState(
        SERVER_HEALTH.CHECKING,
    )

    const [lastCheckedAt, setLastCheckedAt] =
        useState(null)

    const failureCountRef = useRef(0)
    const isMountedRef = useRef(false)

    const checkHealth = useCallback(async () => {
        const controller =
            new AbortController()

        const timeoutId =
            window.setTimeout(
                () => {
                    controller.abort()
                },
                REQUEST_TIMEOUT_MS,
            )

        try {
            const response = await fetch(
                HEALTH_URL,
                {
                    method: 'GET',

                    headers: {
                        Accept: 'application/json',
                    },

                    signal: controller.signal,
                    cache: 'no-store',
                },
            )

            if (!response.ok) {
                throw new Error(
                    `Health request failed with status ${response.status}`,
                )
            }

            const health =
                await response.json()

            if (
                typeof health?.status ===
                'string' &&
                health.status.toUpperCase() !==
                'UP'
            ) {
                throw new Error(
                    'Backend health status is not UP',
                )
            }

            if (!isMountedRef.current) {
                return
            }

            failureCountRef.current = 0

            setStatus(
                SERVER_HEALTH.AVAILABLE,
            )
        } catch {
            if (!isMountedRef.current) {
                return
            }

            failureCountRef.current += 1

            if (
                failureCountRef.current <=
                WAKING_FAILURE_LIMIT
            ) {
                setStatus(
                    SERVER_HEALTH.WAKING,
                )
            } else {
                setStatus(
                    SERVER_HEALTH.UNAVAILABLE,
                )
            }
        } finally {
            window.clearTimeout(timeoutId)

            if (isMountedRef.current) {
                setLastCheckedAt(
                    new Date(),
                )
            }
        }
    }, [])

    useEffect(() => {
        isMountedRef.current = true

        /*
         * Queue the initial check instead of invoking it
         * synchronously inside the effect. This satisfies
         * React's set-state-in-effect lint rule.
         */
        const initialCheckId =
            window.setTimeout(
                () => {
                    void checkHealth()
                },
                0,
            )

        const intervalId =
            window.setInterval(
                () => {
                    void checkHealth()
                },
                POLL_INTERVAL_MS,
            )

        return () => {
            isMountedRef.current = false

            window.clearTimeout(
                initialCheckId,
            )

            window.clearInterval(
                intervalId,
            )
        }
    }, [checkHealth])

    return {
        status,
        lastCheckedAt,
        retry: checkHealth,
    }
}