import {
    LogIn,
    ShieldAlert,
} from 'lucide-react'
import {
    useEffect,
    useRef,
    useState,
} from 'react'
import { useNavigate } from 'react-router-dom'

import {
    subscribeToSessionExpired,
} from '../../features/auth/authEvents'

export default function SessionExpiredDialog() {
    const navigate = useNavigate()
    const buttonRef = useRef(null)

    const [
        isOpen,
        setIsOpen,
    ] = useState(false)

    useEffect(() => {
        return subscribeToSessionExpired(
            () => {
                setIsOpen(true)
            },
        )
    }, [])

    useEffect(() => {
        if (isOpen) {
            buttonRef.current?.focus()
        }
    }, [isOpen])

    function handleSignIn() {
        setIsOpen(false)

        navigate(
            '/login',
            {
                replace: true,
                state: {
                    sessionExpired: true,
                },
            },
        )
    }

    if (!isOpen) {
        return null
    }

    return (
        <div
            className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/45 px-4 backdrop-blur-sm"
            role="presentation"
        >
            <section
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="session-expired-title"
                aria-describedby="session-expired-description"
                className="w-full max-w-md rounded-3xl border border-white/80 bg-white p-7 shadow-2xl"
            >
                <div className="grid h-14 w-14 place-items-center rounded-2xl bg-amber-50 text-amber-600">
                    <ShieldAlert
                        size={28}
                        aria-hidden="true"
                    />
                </div>

                <h2
                    id="session-expired-title"
                    className="mt-5 font-display text-2xl font-extrabold text-heading"
                >
                    Your session has expired
                </h2>

                <p
                    id="session-expired-description"
                    className="mt-3 text-sm leading-6 text-slate-600"
                >
                    For your security, ExpenseMate signed you out.
                    Sign in again to continue where you left off.
                </p>

                <button
                    ref={buttonRef}
                    type="button"
                    onClick={handleSignIn}
                    className="mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 motion-reduce:transform-none"
                >
                    <LogIn
                        size={19}
                        aria-hidden="true"
                    />

                    Sign in again
                </button>
            </section>
        </div>
    )
}