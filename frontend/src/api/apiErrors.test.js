import {
    describe,
    expect,
    it,
} from 'vitest'

import { getApiErrorMessage } from './apiErrors'

describe('getApiErrorMessage', () => {
    it('returns the RFC 7807 detail message', () => {
        const error = {
            response: {
                data: {
                    type: 'https://expensemate.dev/problems/invalid-request',
                    title: 'Invalid request',
                    status: 400,
                    detail: 'The expense amount must be greater than zero.',
                },
            },
        }

        expect(getApiErrorMessage(error)).toBe(
            'The expense amount must be greater than zero.',
        )
    })

    it('prioritizes RFC 7807 detail over a generic message', () => {
        const error = {
            response: {
                data: {
                    detail: 'Your account is temporarily locked.',
                    message: 'Authentication failed.',
                },
            },
        }

        expect(getApiErrorMessage(error)).toBe(
            'Your account is temporarily locked.',
        )
    })

    it('returns the backend message when detail is unavailable', () => {
        const error = {
            response: {
                data: {
                    message: 'Email address is already registered.',
                },
            },
        }

        expect(getApiErrorMessage(error)).toBe(
            'Email address is already registered.',
        )
    })

    it('returns the JavaScript error message for a network failure', () => {
        const error = new Error('Network Error')

        expect(getApiErrorMessage(error)).toBe('Network Error')
    })

    it('returns the supplied fallback when no message exists', () => {
        expect(
            getApiErrorMessage(
                {},
                'Unable to complete the request.',
            ),
        ).toBe('Unable to complete the request.')
    })

    it('ignores blank backend messages', () => {
        const error = {
            response: {
                data: {
                    detail: '   ',
                    message: '',
                },
            },
        }

        expect(getApiErrorMessage(error)).toBe(
            'Something went wrong. Please try again.',
        )
    })
})