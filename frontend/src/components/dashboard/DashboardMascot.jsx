import { cn } from '../../utils/cn'

/**
 * Inline SVG mascot family used across the dashboard.
 * These exist so the dashboard never depends on external image
 * assets being present — every "variant" below is a complete,
 * self-contained illustration drawn with the ExpenseMate palette.
 *
 * variant:
 *  - 'wallet'  : friendly winking wallet, used in the welcome hero
 *  - 'piggy'   : savings jar / piggy bank, used in budget progress
 *  - 'sparkle' : small AI sparkle burst, used in the insight card
 *  - 'receipt' : empty clipboard/receipt, used in empty states
 */
export function DashboardMascot({ variant = 'wallet', className, ...rest }) {
    const shared = {
        role: 'img',
        'aria-hidden': true,
        focusable: 'false',
    }

    if (variant === 'piggy') {
        return (
            <svg
                {...shared}
                viewBox="0 0 120 100"
                className={cn('h-24 w-28', className)}
                {...rest}
            >
                <ellipse cx="60" cy="60" rx="46" ry="34" fill="#27B8B5" opacity="0.16" />
                <path
                    d="M34 55c0-16 13-27 29-27 12 0 22 6 27 16 6 1 10 5 10 10 0 5-4 9-10 10-3 11-14 19-27 19-16 0-29-11-29-27Z"
                    fill="#159FA4"
                />
                <circle cx="80" cy="50" r="3.4" fill="#0F3B3C" />
                <path
                    d="M55 30c3-6 10-9 16-7"
                    stroke="#0F7C80"
                    strokeWidth="3"
                    strokeLinecap="round"
                    fill="none"
                />
                <rect x="55" y="20" width="10" height="7" rx="3" fill="#E9B949" />
                <path
                    d="M40 78c-1 5-1 9 1 12M76 78c1 5 1 9-1 12"
                    stroke="#0F7C80"
                    strokeWidth="5"
                    strokeLinecap="round"
                    fill="none"
                />
                <circle cx="46" cy="46" r="3" fill="#0F3B3C" />
            </svg>
        )
    }

    if (variant === 'sparkle') {
        return (
            <svg
                {...shared}
                viewBox="0 0 64 64"
                className={cn('h-10 w-10', className)}
                {...rest}
            >
                <path
                    d="M32 8l4.5 13.5L50 26l-13.5 4.5L32 44l-4.5-13.5L14 26l13.5-4.5L32 8Z"
                    fill="url(#sparkleGrad)"
                />
                <path
                    d="M50 42l2 6 6 2-6 2-2 6-2-6-6-2 6-2 2-6Z"
                    fill="#27B8B5"
                    opacity="0.85"
                />
                <defs>
                    <linearGradient id="sparkleGrad" x1="14" y1="8" x2="50" y2="44" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#159FA4" />
                        <stop offset="1" stopColor="#3E8EF7" />
                    </linearGradient>
                </defs>
            </svg>
        )
    }

    if (variant === 'receipt') {
        return (
            <svg
                {...shared}
                viewBox="0 0 100 110"
                className={cn('h-24 w-20', className)}
                {...rest}
            >
                <path
                    d="M22 6h56v92l-8-6-8 6-8-6-8 6-8-6-8 6-8-6V6Z"
                    fill="#FFFFFF"
                    stroke="#CFE3E4"
                    strokeWidth="2"
                />
                <rect x="32" y="24" width="36" height="4" rx="2" fill="#D8E7E8" />
                <rect x="32" y="34" width="36" height="4" rx="2" fill="#D8E7E8" />
                <rect x="32" y="44" width="24" height="4" rx="2" fill="#D8E7E8" />
                <circle cx="70" cy="70" r="18" fill="#E7F2F3" stroke="#27B8B5" strokeWidth="2.5" />
                <path
                    d="M63 70l5 5 9-11"
                    stroke="#159FA4"
                    strokeWidth="3.4"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    fill="none"
                />
            </svg>
        )
    }

    // default: wallet
    return (
        <svg
            {...shared}
            viewBox="0 0 160 130"
            className={cn('h-28 w-36', className)}
            {...rest}
        >
            <ellipse cx="80" cy="112" rx="58" ry="10" fill="#159FA4" opacity="0.08" />
            <rect x="18" y="34" width="124" height="72" rx="18" fill="url(#walletGrad)" />
            <rect x="18" y="34" width="124" height="26" rx="13" fill="#ffffff" opacity="0.14" />
            <circle cx="120" cy="70" r="12" fill="#E9B949" />
            <circle cx="120" cy="70" r="4" fill="#8A5B00" opacity="0.5" />
            <path
                d="M30 46c14-14 34-14 48 0"
                stroke="#ffffff"
                strokeOpacity="0.55"
                strokeWidth="4"
                strokeLinecap="round"
                fill="none"
            />
            <circle cx="46" cy="30" r="5" fill="#27B8B5" />
            <circle cx="58" cy="22" r="3.4" fill="#E9B949" />
            <circle cx="68" cy="32" r="4" fill="#2F9E6F" />
            <defs>
                <linearGradient id="walletGrad" x1="18" y1="34" x2="142" y2="106" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#159FA4" />
                    <stop offset="1" stopColor="#0F7C80" />
                </linearGradient>
            </defs>
        </svg>
    )
}
