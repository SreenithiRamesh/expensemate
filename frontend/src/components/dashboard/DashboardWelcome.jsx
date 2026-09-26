import { Plus, Users2 } from 'lucide-react'
import { useRef, useState, useSyncExternalStore } from 'react'

import { DashboardMascot } from './DashboardMascot'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

function getMotionQuery() {
    if (
        typeof window === 'undefined' ||
        typeof window.matchMedia !== 'function'
    ) {
        return null
    }

    return window.matchMedia(REDUCED_MOTION_QUERY)
}

function subscribeToReducedMotion(onChange) {
    const query = getMotionQuery()

    if (!query) {
        return () => {}
    }

    query.addEventListener('change', onChange)

    return () => {
        query.removeEventListener('change', onChange)
    }
}

function getReducedMotionSnapshot() {
    return getMotionQuery()?.matches ?? true
}

function getServerSnapshot() {
    return true
}

function useReducedMotion() {
    return useSyncExternalStore(
        subscribeToReducedMotion,
        getReducedMotionSnapshot,
        getServerSnapshot,
    )
}

function getGreeting(date) {
    const hour = date.getHours()

    if (hour < 12) return 'Good morning'
    if (hour < 17) return 'Good afternoon'

    return 'Good evening'
}

export function DashboardWelcome({
                                     name = 'Sree',
                                     onAddExpense,
                                     onCreateGroup,
                                 }) {
    const reducedMotion = useReducedMotion()
    const heroRef = useRef(null)

    const [offset, setOffset] = useState({ x: 0, y: 0 })
    const [imageFailed, setImageFailed] = useState(false)

    const motionOffset = reducedMotion ? { x: 0, y: 0 } : offset

    const now = new Date()
    const today = now.toLocaleDateString('en-IN', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
    })

    function handlePointerMove(event) {
        if (
            reducedMotion ||
            event.pointerType !== 'mouse' ||
            !heroRef.current
        ) {
            return
        }

        const rect = heroRef.current.getBoundingClientRect()

        if (rect.width <= 0 || rect.height <= 0) {
            return
        }

        const relativeX = Math.max(
            -0.5,
            Math.min(0.5, (event.clientX - rect.left) / rect.width - 0.5),
        )

        const relativeY = Math.max(
            -0.5,
            Math.min(0.5, (event.clientY - rect.top) / rect.height - 0.5),
        )

        setOffset({
            x: relativeX * 12,
            y: relativeY * 10,
        })
    }

    function resetOffset() {
        setOffset({ x: 0, y: 0 })
    }

    return (
        <section
            ref={heroRef}
            onPointerMove={handlePointerMove}
            onPointerLeave={resetOffset}
            onPointerCancel={resetOffset}
            className="relative overflow-hidden rounded-3xl border border-white/80 bg-linear-to-br from-white via-brand-50/60 to-sky-50 p-6 shadow-sm sm:p-8"
        >
            <span
                aria-hidden="true"
                className="absolute top-8 right-24 hidden h-3 w-3 rounded-full bg-amber-400/70 sm:block"
                style={{
                    transform: `translate(${motionOffset.x}px, ${motionOffset.y}px)`,
                }}
            />

            <span
                aria-hidden="true"
                className="absolute top-20 right-40 hidden h-2 w-2 rounded-full bg-brand-400/70 sm:block"
                style={{
                    transform: `translate(${motionOffset.x * 1.6}px, ${motionOffset.y * 1.6}px)`,
                }}
            />

            <span
                aria-hidden="true"
                className="absolute right-16 bottom-10 hidden h-4 w-4 rounded-full bg-sky-300/60 sm:block"
                style={{
                    transform: `translate(${motionOffset.x * -1.2}px, ${motionOffset.y * -1.2}px)`,
                }}
            />

            <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                <div className="max-w-xl">
                    <p className="text-sm font-semibold text-slate-500">
                        {today}
                    </p>

                    <h1 className="mt-2 text-3xl font-black tracking-tight text-heading sm:text-4xl">
                        {getGreeting(now)}, {name}{' '}
                        <span aria-hidden="true">👋</span>
                    </h1>

                    <p className="mt-2 text-slate-500">
                        Here&rsquo;s what&rsquo;s happening with your money today.
                    </p>

                    <div className="mt-6 flex flex-wrap gap-3">
                        <button
                            type="button"
                            onClick={onAddExpense}
                            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 motion-reduce:transition-none motion-reduce:hover:translate-y-0"
                        >
                            <Plus size={18} aria-hidden="true" />
                            Add expense
                        </button>

                        <button
                            type="button"
                            onClick={onCreateGroup}
                            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl border border-brand-200 bg-white px-5 py-3 font-bold text-brand-700 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 motion-reduce:transition-none motion-reduce:hover:translate-y-0"
                        >
                            <Users2 size={18} aria-hidden="true" />
                            Create group
                        </button>
                    </div>
                </div>

                <div
                    aria-hidden="true"
                    className="shrink-0 self-center transition-transform duration-200 ease-out motion-reduce:transition-none"
                    style={{
                        transform: `translate(${motionOffset.x * -0.6}px, ${motionOffset.y * -0.6}px)`,
                    }}
                >
                    {imageFailed || reducedMotion ? (
                        <DashboardMascot variant="wallet" />
                    ) : (
                        <img
                            src="/animations/dashboard-welcome.webp"
                            alt=""
                            width={144}
                            height={112}
                            className="h-28 w-36 object-contain"
                            onError={() => setImageFailed(true)}
                        />
                    )}
                </div>
            </div>
        </section>
    )
}