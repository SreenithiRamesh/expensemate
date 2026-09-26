import { useEffect, useState } from 'react'
import { Coins, Sparkle } from 'lucide-react'

import WalletMascot from '../home/WalletMascot.jsx'

const MOOD_STYLES = {
    idle: {
        wrap: '',
        bubble: 'border-white/80 bg-white/95 text-heading',
    },
    curious: {
        wrap: 'animate-mascot-nod',
        bubble: 'border-brand-200 bg-white text-brand-900',
    },
    shy: {
        wrap: 'animate-mascot-peek',
        bubble: 'border-sky-200 bg-white text-sky-900',
    },
    happy: {
        wrap: 'animate-mascot-bounce',
        bubble:
            'border-emerald-200 bg-emerald-50 text-emerald-900',
    },
    worried: {
        wrap: 'animate-mascot-shiver',
        bubble: 'border-rose-200 bg-rose-50 text-rose-900',
    },
    celebrating: {
        wrap: 'animate-mascot-jump',
        bubble: 'border-amber-200 bg-amber-50 text-amber-900',
    },
}

const COIN_SLOTS = [
    {
        top: '4%',
        left: '8%',
        delay: '0s',
        size: 18,
    },
    {
        top: '14%',
        right: '10%',
        delay: '0.6s',
        size: 14,
    },
    {
        bottom: '18%',
        left: '4%',
        delay: '1.1s',
        size: 16,
    },
    {
        bottom: '6%',
        right: '14%',
        delay: '0.3s',
        size: 12,
    },
]

function prefersReducedMotion() {
    return (
        typeof window !== 'undefined' &&
        typeof window.matchMedia === 'function' &&
        window.matchMedia(
            '(prefers-reduced-motion: reduce)',
        ).matches
    )
}

function TypewriterText({
                            text,
                            speedMs = 14,
                        }) {
    /*
     * TypewriterText is rendered with key={message}.
     * A new message therefore creates a fresh component with
     * fresh state, so no synchronous reset is needed in the effect.
     */
    const [shown, setShown] = useState(() =>
        prefersReducedMotion() ? text : '',
    )

    useEffect(() => {
        if (!text || prefersReducedMotion()) {
            return undefined
        }

        let index = 0
        let timeoutId

        function typeNextCharacter() {
            index += 1
            setShown(text.slice(0, index))

            if (index < text.length) {
                timeoutId = window.setTimeout(
                    typeNextCharacter,
                    speedMs,
                )
            }
        }

        timeoutId = window.setTimeout(
            typeNextCharacter,
            speedMs,
        )

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [text, speedMs])

    return (
        <>
            {/*
             * Screen readers receive the complete message once,
             * rather than announcing every typed character.
             */}
            <span className="sr-only">{text}</span>

            <span aria-hidden="true">
                {shown}

                {shown.length < text.length && (
                    <span className="ml-0.5 inline-block h-3 w-0.5 animate-pulse bg-current align-middle motion-reduce:hidden" />
                )}
            </span>
        </>
    )
}

function FloatingCoins({ burst }) {
    return (
        <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 overflow-hidden"
        >
            {COIN_SLOTS.map((slot, index) => (
                <span
                    key={`${slot.top ?? slot.bottom}-${index}`}
                    className={`absolute grid place-items-center rounded-full bg-amber-300/90 text-amber-800 shadow-sm ${
                        burst
                            ? 'animate-coin-burst'
                            : 'animate-coin-float'
                    }`}
                    style={{
                        top: slot.top,
                        left: slot.left,
                        right: slot.right,
                        bottom: slot.bottom,
                        width: slot.size,
                        height: slot.size,
                        animationDelay: slot.delay,
                    }}
                >
                    <Coins
                        size={Math.round(slot.size * 0.6)}
                        strokeWidth={2.5}
                    />
                </span>
            ))}
        </div>
    )
}

export default function MoneyMascotCompanion({
                                                 mood = 'idle',
                                                 message = '',
                                                 celebrating = false,
                                                 className = '',
                                             }) {
    const moodStyle =
        MOOD_STYLES[mood] ?? MOOD_STYLES.idle

    return (
        <div
            className={`relative flex flex-col items-center ${className}`}
        >
            <style>
                {`
                    @keyframes mascotNod {
                        0%, 100% {
                            transform: rotate(0deg);
                        }

                        50% {
                            transform: rotate(-4deg);
                        }
                    }

                    @keyframes mascotPeek {
                        0%, 100% {
                            transform: translateY(0) scale(1);
                        }

                        50% {
                            transform: translateY(2px) scale(0.98);
                        }
                    }

                    @keyframes mascotBounce {
                        0%, 100% {
                            transform: translateY(0);
                        }

                        30% {
                            transform: translateY(-10px);
                        }

                        55% {
                            transform: translateY(0);
                        }

                        70% {
                            transform: translateY(-4px);
                        }
                    }

                    @keyframes mascotShiver {
                        0%, 100% {
                            transform: translateX(0) rotate(0deg);
                        }

                        25% {
                            transform: translateX(-2px) rotate(-2deg);
                        }

                        75% {
                            transform: translateX(2px) rotate(2deg);
                        }
                    }

                    @keyframes mascotJump {
                        0%, 100% {
                            transform: translateY(0) rotate(0deg);
                        }

                        20% {
                            transform: translateY(-16px) rotate(-6deg);
                        }

                        40% {
                            transform: translateY(0) rotate(4deg);
                        }

                        60% {
                            transform: translateY(-10px) rotate(-3deg);
                        }

                        80% {
                            transform: translateY(0);
                        }
                    }

                    @keyframes coinFloat {
                        0%, 100% {
                            transform: translateY(0) rotate(0deg);
                            opacity: 0.85;
                        }

                        50% {
                            transform: translateY(-8px) rotate(12deg);
                            opacity: 1;
                        }
                    }

                    @keyframes coinBurst {
                        0% {
                            transform: translateY(0) scale(0.8);
                            opacity: 1;
                        }

                        100% {
                            transform: translateY(-46px) scale(1.15);
                            opacity: 0;
                        }
                    }

                    @keyframes bubbleIn {
                        0% {
                            opacity: 0;
                            transform: translateY(6px) scale(0.96);
                        }

                        100% {
                            opacity: 1;
                            transform: translateY(0) scale(1);
                        }
                    }

                    .animate-mascot-nod {
                        animation: mascotNod 2.4s ease-in-out infinite;
                    }

                    .animate-mascot-peek {
                        animation: mascotPeek 2.2s ease-in-out infinite;
                    }

                    .animate-mascot-bounce {
                        animation: mascotBounce 1.1s ease-in-out infinite;
                    }

                    .animate-mascot-shiver {
                        animation: mascotShiver 0.35s ease-in-out 2;
                    }

                    .animate-mascot-jump {
                        animation: mascotJump 1.4s ease-in-out infinite;
                    }

                    .animate-coin-float {
                        animation: coinFloat 3.2s ease-in-out infinite;
                    }

                    .animate-coin-burst {
                        animation: coinBurst 0.9s ease-out forwards;
                    }

                    .animate-bubble-in {
                        animation: bubbleIn 0.22s ease-out;
                    }

                    @media (prefers-reduced-motion: reduce) {
                        .animate-mascot-nod,
                        .animate-mascot-peek,
                        .animate-mascot-bounce,
                        .animate-mascot-shiver,
                        .animate-mascot-jump,
                        .animate-coin-float,
                        .animate-coin-burst,
                        .animate-bubble-in {
                            animation: none;
                        }
                    }
                `}
            </style>

            <div className="relative h-32 w-32">
                <FloatingCoins burst={celebrating} />

                <div
                    className={`h-full w-full ${moodStyle.wrap}`}
                >
                    <WalletMascot
                        className="h-full w-full"
                        animated
                    />
                </div>
            </div>

            {message && (
                <div
                    key={message}
                    role="status"
                    aria-live="polite"
                    aria-atomic="true"
                    className={`animate-bubble-in relative mt-3 max-w-[15rem] rounded-2xl border px-3.5 py-2 text-center text-xs font-medium leading-5 shadow-sm ${moodStyle.bubble}`}
                >
                    <span
                        aria-hidden="true"
                        className={`absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 border-l border-t ${moodStyle.bubble}`}
                    />

                    <TypewriterText
                        key={message}
                        text={message}
                    />

                    {celebrating && (
                        <Sparkle
                            size={12}
                            className="ml-1 inline animate-pulse text-amber-500 motion-reduce:animate-none"
                            aria-hidden="true"
                        />
                    )}
                </div>
            )}
        </div>
    )
}