/**
 * Lightweight cartoon wallet mascot rendered as inline SVG.
 * Used as the hero illustration fallback (if /animations/expensemate-hero.webp
 * is unavailable) and beside the final CTA — keeps the page from ever
 * appearing broken while staying on-brand with the teal gradient system.
 */
function WalletMascot({ className = '', animated = true }) {
    return (
        <svg
            viewBox="0 0 220 200"
            className={className}
            role="img"
            aria-label="ExpenseMate wallet mascot smiling next to a stack of coins"
        >
            <defs>
                <linearGradient id="walletBody" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" stopColor="#2BC1B9" />
                    <stop offset="55%" stopColor="#24B5B5" />
                    <stop offset="100%" stopColor="#159FA4" />
                </linearGradient>
                <linearGradient id="coinBody" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#FFE8A3" />
                    <stop offset="100%" stopColor="#F0B94D" />
                </linearGradient>
            </defs>

            <ellipse cx="110" cy="182" rx="70" ry="10" fill="#159FA4" opacity="0.12" />

            <g>
                <ellipse cx="46" cy="150" rx="26" ry="9" fill="#E9B949" />
                <rect x="20" y="132" width="52" height="18" rx="9" fill="url(#coinBody)" />
                <ellipse cx="46" cy="132" rx="26" ry="9" fill="#FFE8A3" />
                <ellipse cx="46" cy="132" rx="18" ry="6" fill="#F0B94D" opacity="0.6" />
            </g>

            <rect x="60" y="46" width="120" height="96" rx="22" fill="url(#walletBody)" />
            <rect x="60" y="80" width="120" height="14" fill="#0f7d82" opacity="0.25" />
            <circle cx="150" cy="87" r="10" fill="#FDF9F3" />
            <circle cx="150" cy="87" r="4" fill="#159FA4" />

            <circle cx="102" cy="104" r="5" fill="#252936" />
            <circle cx="126" cy="104" r="5" fill="#252936" />
            <path
                d="M100 118c6 8 22 8 28 0"
                stroke="#252936"
                strokeWidth="4"
                strokeLinecap="round"
                fill="none"
            />

            <g fill="#F0B94D" className={animated ? 'animate-coin-spin' : ''} style={{ transformOrigin: '182px 44px' }}>
                <path d="M182 40l3 8 8 3-8 3-3 8-3-8-8-3 8-3z" />
            </g>
            <path d="M40 60l2 5 5 2-5 2-2 5-2-5-5-2 5-2z" fill="#F0B94D" opacity="0.8" />
        </svg>
    )
}

export default WalletMascot