import {
    act,
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    MemoryRouter,
    useLocation,
} from 'react-router-dom'
import {
    describe,
    expect,
    it,
} from 'vitest'

import {
    publishSessionExpired,
} from '../../features/auth/authEvents'
import SessionExpiredDialog from './SessionExpiredDialog'

function LocationProbe() {
    const location = useLocation()

    return (
        <output data-testid="location">
            {JSON.stringify({
                pathname: location.pathname,
                state: location.state,
            })}
        </output>
    )
}

function renderDialog() {
    return render(
        <MemoryRouter initialEntries={['/app/dashboard']}>
            <SessionExpiredDialog />
            <LocationProbe />
        </MemoryRouter>,
    )
}

describe('SessionExpiredDialog', () => {
    it('is hidden before a session-expired event', () => {
        renderDialog()

        expect(
            screen.queryByRole('alertdialog'),
        ).not.toBeInTheDocument()
    })

    it('opens when the authentication session expires', () => {
        renderDialog()

        act(() => {
            publishSessionExpired(
                'refresh-token-rejected',
            )
        })

        expect(
            screen.getByRole('alertdialog'),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('heading', {
                name: 'Your session has expired',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                /expensemate signed you out/i,
            ),
        ).toBeInTheDocument()
    })

    it('focuses the sign-in button when opened', () => {
        renderDialog()

        act(() => {
            publishSessionExpired(
                'retried-request-rejected',
            )
        })

        expect(
            screen.getByRole('button', {
                name: 'Sign in again',
            }),
        ).toHaveFocus()
    })

    it('navigates to login with session-expired state', async () => {
        const user = userEvent.setup()

        renderDialog()

        act(() => {
            publishSessionExpired(
                'refresh-token-rejected',
            )
        })

        await user.click(
            screen.getByRole('button', {
                name: 'Sign in again',
            }),
        )

        const location = JSON.parse(
            screen.getByTestId('location').textContent,
        )

        expect(location).toEqual({
            pathname: '/login',
            state: {
                sessionExpired: true,
            },
        })

        expect(
            screen.queryByRole('alertdialog'),
        ).not.toBeInTheDocument()
    })

    it('unsubscribes when the dialog is unmounted', () => {
        const view = renderDialog()

        view.unmount()

        expect(() => {
            act(() => {
                publishSessionExpired(
                    'refresh-token-rejected',
                )
            })
        }).not.toThrow()
    })
})