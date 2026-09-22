import '@testing-library/jest-dom/vitest'
import {
    afterEach,
    vi,
} from 'vitest'

const fetchMock = vi.fn(() =>
    Promise.resolve({
        ok: true,

        json: () =>
            Promise.resolve({
                status: 'UP',
            }),
    }),
)

vi.stubGlobal(
    'fetch',
    fetchMock,
)

afterEach(() => {
    fetchMock.mockClear()
})