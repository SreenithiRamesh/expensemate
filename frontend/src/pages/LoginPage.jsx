import { useState } from 'react'
import axios from 'axios'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import {
    Link,
    Navigate,
    useLocation,
} from 'react-router-dom'
import {
    ArrowLeft,
    Eye,
    EyeOff,
    LoaderCircle,
    ShieldCheck,
    Sparkles,
    Users2,
    Wallet,
} from 'lucide-react'

import WalletMascot from '../components/home/WalletMascot.jsx'
import { useAuth } from '../hooks/useAuth'
import { loginSchema } from '../schemas/authSchemas'

function getLoginErrorMessage(error) {
    if (!axios.isAxiosError(error)) {
        return 'Unable to sign in. Please try again.'
    }

    if (!error.response) {
        return 'Unable to reach ExpenseMate. Check your connection and try again.'
    }

    switch (error.response.status) {
        case 400:
            return 'Please check your email and password.'
        case 401:
            return 'The email or password is incorrect.'
        case 403:
            return 'Sign-in was denied. Please try again later.'
        case 423:
            return 'Your account is temporarily locked. Please try again later.'
        case 429:
            return 'Too many attempts. Please wait before trying again.'
        default:
            return 'Sign-in is currently unavailable. Please try again shortly.'
    }
}

function getRedirectPath(locationState) {
    const requestedLocation = locationState?.from

    if (
        typeof requestedLocation?.pathname !== 'string' ||
        !requestedLocation.pathname.startsWith('/app')
    ) {
        return '/app/dashboard'
    }

    return `${requestedLocation.pathname}${
        requestedLocation.search ?? ''
    }${requestedLocation.hash ?? ''}`
}

const trustItems = [
    {
        icon: ShieldCheck,
        label: 'Secure authentication',
    },
    {
        icon: Users2,
        label: 'Accurate shared balances',
    },
    {
        icon: Sparkles,
        label: 'AI-assisted insights',
    },
]

export default function LoginPage() {
    const { login, isAuthenticated } = useAuth()
    const location = useLocation()
    const [showPassword, setShowPassword] = useState(false)

    const accountCreated =
        location.state?.accountCreated === true

    const redirectPath = getRedirectPath(
        location.state,
    )

    const {
        register,
        handleSubmit,
        setError,
        clearErrors,
        formState: {
            errors,
            isSubmitting,
        },
    } = useForm({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            email: '',
            password: '',
        },
    })

    async function onSubmit(values) {
        clearErrors('root')

        try {
            await login(values)
        } catch (error) {
            if (axios.isCancel(error)) {
                return
            }

            setError('root.server', {
                type: 'server',
                message: getLoginErrorMessage(error),
            })
        }
    }

    if (isAuthenticated) {
        return (
            <Navigate
                to={redirectPath}
                replace
            />
        )
    }

    const inputClassName =
        'mt-2 min-h-12 w-full rounded-2xl border border-slate-300 ' +
        'bg-white px-4 py-3 text-heading outline-none transition ' +
        'placeholder:text-slate-400 focus:border-brand-500 ' +
        'focus:ring-4 focus:ring-brand-100 ' +
        'aria-invalid:border-rose-500 disabled:opacity-60'

    return (
        <main className="relative isolate min-h-screen overflow-hidden">
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -top-40 -left-40 h-96 w-96 rounded-full bg-brand-300/30 blur-3xl"
            />

            <div
                aria-hidden="true"
                className="pointer-events-none absolute right-[-8rem] bottom-[-10rem] h-[30rem] w-[30rem] rounded-full bg-sky-300/20 blur-3xl"
            />

            <div className="relative mx-auto grid min-h-screen max-w-6xl items-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_1.05fr] lg:gap-16 lg:py-12">
                <div className="relative hidden lg:block">
                    <Link
                        to="/"
                        className="mb-8 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-brand-800 transition hover:-translate-x-0.5 motion-reduce:transform-none motion-reduce:transition-none"
                    >
                        <ArrowLeft
                            size={18}
                            aria-hidden="true"
                        />
                        Back to home
                    </Link>

                    <div className="relative overflow-hidden rounded-[2.5rem] border border-white/80 bg-linear-to-br from-brand-700 via-brand-600 to-[#239db7] p-10 text-white shadow-xl shadow-brand-700/25">
                        <div
                            aria-hidden="true"
                            className="pointer-events-none absolute -top-16 -right-10 h-64 w-64 rounded-full bg-white/10 blur-3xl"
                        />

                        <p className="relative inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-2 text-sm font-semibold">
                            <Sparkles
                                size={16}
                                aria-hidden="true"
                            />
                            Smarter money. Better together.
                        </p>

                        <h2 className="relative mt-6 font-display text-3xl font-extrabold tracking-tight sm:text-4xl">
                            Welcome back to your money, organized.
                        </h2>

                        <p className="relative mt-4 max-w-sm text-white/85">
                            Sign back in to pick up right where you
                            left off — budgets, shared balances, and
                            AI insights all in one place.
                        </p>

                        <ul className="relative mt-8 space-y-3">
                            {trustItems.map((item) => {
                                const Icon = item.icon

                                return (
                                    <li
                                        key={item.label}
                                        className="flex items-center gap-3 text-sm font-medium text-white/90"
                                    >
                                        <span className="grid h-8 w-8 place-items-center rounded-xl bg-white/15">
                                            <Icon
                                                size={16}
                                                aria-hidden="true"
                                            />
                                        </span>

                                        {item.label}
                                    </li>
                                )
                            })}
                        </ul>

                        <div
                            aria-hidden="true"
                            className="relative mt-10 flex items-center justify-center"
                        >
                            <WalletMascot
                                className="h-40 w-40 animate-mascot-breathe motion-reduce:animate-none"
                                animated
                            />
                        </div>
                    </div>

                    <div className="absolute -bottom-6 left-10 w-48 animate-float-c rounded-2xl border border-white/80 bg-white/95 p-3.5 shadow-lg shadow-brand-900/10 backdrop-blur-xl motion-reduce:animate-none">
                        <p className="text-[11px] font-semibold text-slate-500">
                            Example balance
                        </p>

                        <p className="mt-1 text-sm text-slate-600">
                            Roommates owe you
                        </p>

                        <p className="mt-1 text-lg font-bold text-[#2F9E6F]">
                            ₹1,250
                        </p>
                    </div>
                </div>

                <div className="mx-auto w-full max-w-md">
                    <Link
                        to="/"
                        className="mb-6 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-brand-800 hover:underline lg:hidden"
                    >
                        <ArrowLeft
                            size={18}
                            aria-hidden="true"
                        />
                        Back to home
                    </Link>

                    <section
                        aria-labelledby="login-heading"
                        className="relative overflow-hidden rounded-3xl border border-white/80 bg-white/90 p-6 shadow-card backdrop-blur-xl transition-shadow duration-300 hover:shadow-xl motion-reduce:transition-none sm:p-8"
                    >
                        <div
                            aria-hidden="true"
                            className="absolute inset-x-0 top-0 h-1 bg-linear-to-r from-brand-500 via-cyan-400 to-sky-400"
                        />

                        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-button">
                            <Wallet
                                size={28}
                                aria-hidden="true"
                            />
                        </div>

                        <p className="mt-6 text-sm font-bold text-brand-700">
                            ExpenseMate
                        </p>

                        <h1
                            id="login-heading"
                            className="mt-2 font-display text-3xl font-extrabold tracking-tight text-heading"
                        >
                            Welcome back
                        </h1>

                        <p className="mt-3 text-sm leading-6 text-slate-600">
                            Sign in to track your expenses, manage
                            budgets, and keep shared balances
                            organized.
                        </p>

                        {accountCreated && (
                            <div
                                role="status"
                                className="mt-6 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-800"
                            >
                                Account created successfully. Sign in
                                with your email and password.
                            </div>
                        )}

                        <form
                            className="mt-7 space-y-5"
                            onSubmit={handleSubmit(onSubmit)}
                            noValidate
                            aria-busy={isSubmitting}
                        >
                            {errors.root?.server && (
                                <div
                                    role="alert"
                                    className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm leading-6 text-rose-800"
                                >
                                    {errors.root.server.message}
                                </div>
                            )}

                            <div>
                                <label
                                    htmlFor="login-email"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Email address
                                </label>

                                <input
                                    {...register('email')}
                                    id="login-email"
                                    type="email"
                                    autoComplete="username"
                                    autoCapitalize="none"
                                    spellCheck={false}
                                    placeholder="you@example.com"
                                    disabled={isSubmitting}
                                    aria-invalid={Boolean(
                                        errors.email,
                                    )}
                                    aria-describedby={
                                        errors.email
                                            ? 'login-email-error'
                                            : undefined
                                    }
                                    className={inputClassName}
                                />

                                {errors.email && (
                                    <p
                                        id="login-email-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.email.message}
                                    </p>
                                )}
                            </div>

                            <div>
                                <label
                                    htmlFor="login-password"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Password
                                </label>

                                <div className="relative">
                                    <input
                                        {...register('password')}
                                        id="login-password"
                                        type={
                                            showPassword
                                                ? 'text'
                                                : 'password'
                                        }
                                        autoComplete="current-password"
                                        placeholder="Enter your password"
                                        disabled={isSubmitting}
                                        aria-invalid={Boolean(
                                            errors.password,
                                        )}
                                        aria-describedby={
                                            errors.password
                                                ? 'login-password-error'
                                                : undefined
                                        }
                                        className={`${inputClassName} pr-14`}
                                    />

                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowPassword(
                                                (visible) =>
                                                    !visible,
                                            )
                                        }}
                                        disabled={isSubmitting}
                                        aria-label={
                                            showPassword
                                                ? 'Hide password'
                                                : 'Show password'
                                        }
                                        aria-controls="login-password"
                                        className="absolute right-1 bottom-1 flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition hover:bg-brand-50 hover:text-brand-800 disabled:opacity-50"
                                    >
                                        {showPassword ? (
                                            <EyeOff
                                                size={20}
                                                aria-hidden="true"
                                            />
                                        ) : (
                                            <Eye
                                                size={20}
                                                aria-hidden="true"
                                            />
                                        )}
                                    </button>
                                </div>

                                {errors.password && (
                                    <p
                                        id="login-password-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.password.message}
                                    </p>
                                )}
                            </div>

                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-600/25 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0 motion-reduce:transform-none motion-reduce:transition-none"
                            >
                                {isSubmitting && (
                                    <LoaderCircle
                                        size={20}
                                        aria-hidden="true"
                                        className="animate-spin motion-reduce:animate-none"
                                    />
                                )}

                                {isSubmitting
                                    ? 'Signing in…'
                                    : 'Sign in'}
                            </button>
                        </form>

                        <p className="mt-6 text-center text-sm text-slate-500">
                            New to ExpenseMate?{' '}
                            <Link
                                to="/register"
                                className="font-bold text-brand-700 hover:underline"
                            >
                                Create an account
                            </Link>
                        </p>

                        <p className="mt-3 text-center text-xs leading-5 text-slate-500">
                            For this version, refreshing the browser
                            signs you out.
                        </p>
                    </section>
                </div>
            </div>
        </main>
    )
}