import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

const fetchMock = vi.fn(() =>
    Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ status: 'UP' }),
    }),
)

vi.stubGlobal('fetch', fetchMock)

Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(() => true),
    })),
})

class IntersectionObserverMock {
    constructor(callback, options = {}) {
        this.callback = callback
        this.root = options.root ?? null
        this.rootMargin = options.rootMargin ?? '0px'
        this.thresholds = [options.threshold ?? 0]
    }

    observe = vi.fn()
    unobserve = vi.fn()
    disconnect = vi.fn()
    takeRecords = vi.fn(() => [])
}

vi.stubGlobal('IntersectionObserver', IntersectionObserverMock)

afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    fetchMock.mockClear()
})