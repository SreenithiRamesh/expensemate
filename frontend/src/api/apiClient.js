import axios from 'axios'

import { API_V1_URL } from '../constants/environment'

function createCorrelationId() {
    if (
        typeof crypto !== 'undefined' &&
        typeof crypto.randomUUID === 'function'
    ) {
        return crypto.randomUUID()
    }

    return `web-${Date.now()}-${Math.random()
        .toString(16)
        .slice(2)}`
}

export const apiClient = axios.create({
    baseURL: API_V1_URL,
    timeout: 15_000,

    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
})

apiClient.interceptors.request.use(
    (config) => {
        config.headers.set(
            'X-Correlation-ID',
            createCorrelationId(),
        )

        return config
    },
    (error) => Promise.reject(error),
)

apiClient.interceptors.response.use(
    (response) => response,
    (error) => Promise.reject(error),
)