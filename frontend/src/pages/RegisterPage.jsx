import { useState } from 'react'
import axios from 'axios'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useNavigate } from 'react-router-dom'
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

import { useAuth } from '../hooks/useAuth'
import { registerSchema } from '../schemas/authSchemas'
import WalletMascot from '../components/home/WalletMascot.jsx'

function getRegisterErrorMessage(error) {
    if (!axios.isAxiosError(error)) {
        return 'Unable to create your account. Please try again.'
    }

    if (!error.response) {
        return 'Unable to reach ExpenseMate. Check your connection and try again.'
    }

    switch (error.response.status) {
        case 400:
            return 'Please check the details you entered.'
        case 409:
            return 'An account with this email already exists.'
        case 422:
            return 'Please check the details you entered.'
        case 429:
            return 'Too many attempts. Please wait before trying again.'
        default:
            return 'Registration is currently unavailable. Please try again shortly.'
    }
}

const trustItems = [
    { icon: ShieldCheck, label: 'Secure authentication' },
    { icon: Users2, label: 'Accurate shared balances' },
    { icon: Sparkles, label: 'AI-assisted insights' },
]

export default function RegisterPage() {
    const { register: registerUser, isAuthenticated } = useAuth()
    const navigate = useNavigate()
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)

    const {
        register,
        handleSubmit,
        setError,
        clearErrors,
        formState: { errors, isSubmitting },
    } = useForm({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            name: '',
            email: '',
            password: '',
            confirmPassword: '',
        },
    })

    async function onSubmit(values) {
        clearErrors('root')

        try {
            // Only name, email, and password are sent to the backend —
            // confirmPassword exists purely for client-side validation.
            await registerUser({
                name: values.name,
                email: values.email,
                password: values.password,
            })

            navigate('/login', {
                replace: true,
                state: { accountCreated: true },
            })
        } catch (error) {
            if (axios.isCancel(error)) {
                return
            }

            setError('root.server', {
                type: 'server',
                message: getRegisterErrorMessage(error),
            })
        }
    }

    // AuthProvider updates after registration auto-signs the user in, if it does.
    if (isAuthenticated) {
        return <Navigate to="/app/dashboard" replace />
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

            <div className="relative mx-auto grid min-h-screen max-w-6xl items-center gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[1fr_1.05fr] lg:gap-16 lg:py-16">
                {/* Illustration panel — height follows its content, no forced fixed
                    height, so nothing sits in empty space. Hidden on small screens. */}
                <div className="relative hidden pb-10 lg:block">
                    <Link
                        to="/"
                        className="mb-8 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-brand-800 transition hover:-translate-x-0.5"
                    >
                        <ArrowLeft size={18} aria-hidden="true" />
                        Back to home
                    </Link>

                    <div className="relative overflow-hidden rounded-[2.5rem] border border-white/80 bg-linear-to-br from-brand-700 via-brand-600 to-[#239db7] px-10 py-12 text-white shadow-xl shadow-brand-700/25">
                        <div
                            aria-hidden="true"
                            className="pointer-events-none absolute -top-16 -right-10 h-64 w-64 rounded-full bg-white/10 blur-3xl"
                        />
                        <div
                            aria-hidden="true"
                            className="pointer-events-none absolute -bottom-24 -left-14 h-56 w-56 rounded-full bg-white/10 blur-3xl"
                        />

                        <p className="relative inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-2 text-sm font-semibold">
                            <Sparkles size={16} />
                            Smarter money. Better together.
                        </p>

                        <h1 className="relative mt-7 font-display text-3xl leading-[1.15] font-extrabold tracking-tight sm:text-4xl">
                            Join ExpenseMate and get organized.
                        </h1>

                        <p className="relative mt-5 max-w-sm text-white/85">
                            Create an account to track spending, split costs
                            with roommates, and get AI-assisted insights on
                            where your money goes.
                        </p>

                        <div className="relative mt-12 flex justify-center">
                            <WalletMascot
                                className="h-36 w-36 animate-mascot-breathe"
                                animated
                            />
                        </div>

                        <ul className="relative mt-10 flex flex-wrap justify-center gap-3">
                            {trustItems.map((item) => {
                                const Icon = item.icon
                                return (
                                    <li
                                        key={item.label}
                                        className="inline-flex items-center gap-2 rounded-full bg-white/12 px-3.5 py-2 text-xs font-semibold text-white/90"
                                    >
                                        <Icon size={14} />
                                        {item.label}
                                    </li>
                                )
                            })}
                        </ul>
                    </div>

                    {/* Two balanced floating cards instead of one, so the panel
                        reads as a full composition rather than trailing off. */}
                    <div className="absolute -bottom-5 left-10 w-44 animate-float-a rounded-2xl border border-white/80 bg-white/95 p-3.5 shadow-lg shadow-brand-900/10 backdrop-blur-xl">
                        <p className="text-[11px] font-semibold text-slate-500">Monthly budget</p>
                        <p className="mt-1 text-base font-bold text-heading">
                            ₹18,400 <span className="text-xs font-semibold text-slate-400">/ ₹25,000</span>
                        </p>
                        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-brand-100">
                            <div className="h-full w-[74%] rounded-full bg-linear-to-r from-brand-500 to-brand-400" />
                        </div>
                    </div>

                    <div className="absolute -bottom-5 right-10 w-40 animate-float-b rounded-2xl border border-white/80 bg-white/95 p-3.5 shadow-lg shadow-brand-900/10 backdrop-blur-xl">
                        <p className="text-[11px] font-semibold text-slate-500">Roommates owe you</p>
                        <p className="mt-1 text-lg font-bold text-[#2F9E6F]">₹1,250</p>
                    </div>
                </div>

                {/* Form panel */}
                <div className="mx-auto w-full max-w-md">
                    <Link
                        to="/"
                        className="mb-6 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-brand-800 hover:underline lg:hidden"
                    >
                        <ArrowLeft size={18} aria-hidden="true" />
                        Back to home
                    </Link>

                    <section
                        aria-labelledby="register-heading"
                        className="relative overflow-hidden rounded-3xl border border-white/80 bg-white/90 p-7 shadow-card backdrop-blur-xl transition-shadow duration-300 hover:shadow-xl sm:p-10"
                    >
                        <div
                            aria-hidden="true"
                            className="absolute inset-x-0 top-0 h-1 bg-linear-to-r from-brand-500 via-cyan-400 to-sky-400"
                        />

                        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-button">
                            <Wallet size={28} aria-hidden="true" />
                        </div>

                        <p className="mt-7 text-sm font-bold text-brand-700">
                            ExpenseMate
                        </p>

                        <h1
                            id="register-heading"
                            className="mt-2 font-display text-3xl font-extrabold tracking-tight text-heading"
                        >
                            Create your account
                        </h1>

                        <p className="mt-3 text-sm leading-6 text-slate-600">
                            It takes less than a minute to start tracking
                            expenses and splitting costs.
                        </p>

                        <form
                            className="mt-8 space-y-6"
                            onSubmit={handleSubmit(onSubmit)}
                            noValidate
                            aria-busy={isSubmitting}
                        >
                            {errors.root?.server && (
                                <div
                                    role="alert"
                                    className="animate-fade-in-up rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm leading-6 text-rose-800"
                                >
                                    {errors.root.server.message}
                                </div>
                            )}

                            <div>
                                <label
                                    htmlFor="register-name"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Full name
                                </label>

                                <input
                                    {...register('name')}
                                    id="register-name"
                                    type="text"
                                    autoComplete="name"
                                    placeholder="Jordan Lee"
                                    disabled={isSubmitting}
                                    aria-invalid={Boolean(errors.name)}
                                    aria-describedby={
                                        errors.name
                                            ? 'register-name-error'
                                            : undefined
                                    }
                                    className={inputClassName}
                                />

                                {errors.name && (
                                    <p
                                        id="register-name-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.name.message}
                                    </p>
                                )}
                            </div>

                            <div>
                                <label
                                    htmlFor="register-email"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Email address
                                </label>

                                <input
                                    {...register('email')}
                                    id="register-email"
                                    type="email"
                                    autoComplete="username"
                                    autoCapitalize="none"
                                    spellCheck={false}
                                    placeholder="you@example.com"
                                    disabled={isSubmitting}
                                    aria-invalid={Boolean(errors.email)}
                                    aria-describedby={
                                        errors.email
                                            ? 'register-email-error'
                                            : undefined
                                    }
                                    className={inputClassName}
                                />

                                {errors.email && (
                                    <p
                                        id="register-email-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.email.message}
                                    </p>
                                )}
                            </div>

                            <div>
                                <label
                                    htmlFor="register-password"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Password
                                </label>

                                <div className="relative">
                                    <input
                                        {...register('password')}
                                        id="register-password"
                                        type={showPassword ? 'text' : 'password'}
                                        autoComplete="new-password"
                                        placeholder="Create a password"
                                        disabled={isSubmitting}
                                        aria-invalid={Boolean(errors.password)}
                                        aria-describedby={
                                            errors.password
                                                ? 'register-password-error'
                                                : 'register-password-hint'
                                        }
                                        className={`${inputClassName} pr-14`}
                                    />

                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowPassword((visible) => !visible)
                                        }}
                                        disabled={isSubmitting}
                                        aria-label={
                                            showPassword
                                                ? 'Hide password'
                                                : 'Show password'
                                        }
                                        aria-controls="register-password"
                                        className="absolute right-1 bottom-1 flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition hover:bg-brand-50 hover:text-brand-800 disabled:opacity-50"
                                    >
                                        <span className="grid animate-fade-in-up">
                                            {showPassword ? (
                                                <EyeOff size={20} aria-hidden="true" />
                                            ) : (
                                                <Eye size={20} aria-hidden="true" />
                                            )}
                                        </span>
                                    </button>
                                </div>

                                {errors.password ? (
                                    <p
                                        id="register-password-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.password.message}
                                    </p>
                                ) : (
                                    <p id="register-password-hint" className="mt-1.5 text-xs text-slate-400">
                                        Use at least 8 characters.
                                    </p>
                                )}
                            </div>

                            <div>
                                <label
                                    htmlFor="register-confirm-password"
                                    className="text-sm font-semibold text-heading"
                                >
                                    Confirm password
                                </label>

                                <div className="relative">
                                    <input
                                        {...register('confirmPassword')}
                                        id="register-confirm-password"
                                        type={
                                            showConfirmPassword
                                                ? 'text'
                                                : 'password'
                                        }
                                        autoComplete="new-password"
                                        placeholder="Re-enter your password"
                                        disabled={isSubmitting}
                                        aria-invalid={Boolean(
                                            errors.confirmPassword,
                                        )}
                                        aria-describedby={
                                            errors.confirmPassword
                                                ? 'register-confirm-password-error'
                                                : undefined
                                        }
                                        className={`${inputClassName} pr-14`}
                                    />

                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowConfirmPassword(
                                                (visible) => !visible,
                                            )
                                        }}
                                        disabled={isSubmitting}
                                        aria-label={
                                            showConfirmPassword
                                                ? 'Hide password'
                                                : 'Show password'
                                        }
                                        aria-controls="register-confirm-password"
                                        className="absolute right-1 bottom-1 flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition hover:bg-brand-50 hover:text-brand-800 disabled:opacity-50"
                                    >
                                        <span className="grid animate-fade-in-up">
                                            {showConfirmPassword ? (
                                                <EyeOff size={20} aria-hidden="true" />
                                            ) : (
                                                <Eye size={20} aria-hidden="true" />
                                            )}
                                        </span>
                                    </button>
                                </div>

                                {errors.confirmPassword && (
                                    <p
                                        id="register-confirm-password-error"
                                        role="alert"
                                        className="mt-2 text-sm text-rose-700"
                                    >
                                        {errors.confirmPassword.message}
                                    </p>
                                )}
                            </div>

                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="group inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-600/25 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                            >
                                {isSubmitting && (
                                    <LoaderCircle
                                        size={20}
                                        aria-hidden="true"
                                        className="animate-spin motion-reduce:animate-none"
                                    />
                                )}

                                {isSubmitting
                                    ? 'Creating account…'
                                    : 'Create account'}
                            </button>
                        </form>

                        <p className="mt-7 text-center text-sm text-slate-500">
                            Already have an account?{' '}
                            <Link
                                to="/login"
                                className="font-bold text-brand-700 hover:underline"
                            >
                                Sign in
                            </Link>
                        </p>
                    </section>
                </div>
            </div>
        </main>
    )
}