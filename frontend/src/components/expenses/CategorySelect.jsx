import {
    Check,
    ChevronDown,
    CircleDollarSign,
    Clapperboard,
    GraduationCap,
    HeartPulse,
    House,
    ListFilter,
    Plane,
    ReceiptText,
    RefreshCcw,
    Shapes,
    ShoppingBag,
    Utensils,
} from 'lucide-react'
import {
    useEffect,
    useId,
    useRef,
    useState,
} from 'react'

import {
    EXPENSE_CATEGORY_OPTIONS,
} from '../../constants/finance'

const CATEGORY_STYLES = {
    FOOD: {
        icon: Utensils,
        iconClass:
            'bg-emerald-100 text-emerald-700',
    },
    TRAVEL: {
        icon: Plane,
        iconClass:
            'bg-sky-100 text-sky-700',
    },
    SHOPPING: {
        icon: ShoppingBag,
        iconClass:
            'bg-amber-100 text-amber-700',
    },
    BILLS: {
        icon: ReceiptText,
        iconClass:
            'bg-violet-100 text-violet-700',
    },
    ENTERTAINMENT: {
        icon: Clapperboard,
        iconClass:
            'bg-pink-100 text-pink-700',
    },
    HEALTH: {
        icon: HeartPulse,
        iconClass:
            'bg-rose-100 text-rose-700',
    },
    EDUCATION: {
        icon: GraduationCap,
        iconClass:
            'bg-indigo-100 text-indigo-700',
    },
    RENT: {
        icon: House,
        iconClass:
            'bg-orange-100 text-orange-700',
    },
    SUBSCRIPTION: {
        icon: RefreshCcw,
        iconClass:
            'bg-cyan-100 text-cyan-700',
    },
    OTHER: {
        icon: Shapes,
        iconClass:
            'bg-slate-100 text-slate-700',
    },
}

const DEFAULT_STYLE = {
    icon: CircleDollarSign,
    iconClass: 'bg-brand-100 text-brand-700',
}

const ALL_CATEGORIES_STYLE = {
    icon: ListFilter,
    iconClass: 'bg-brand-100 text-brand-700',
}

export function CategorySelect({
                                   label = 'Category',
                                   value,
                                   onChange,
                                   onBlur,
                                   error,
                                   required = false,
                                   disabled = false,
                                   includeAll = false,
                                   allLabel = 'All categories',
                                   className = '',
                               }) {
    const selectId = useId()
    const containerRef = useRef(null)

    const [isOpen, setIsOpen] = useState(false)
    const [activeIndex, setActiveIndex] = useState(0)

    const options = includeAll
        ? [
            {
                value: '',
                label: allLabel,
                isAllOption: true,
            },
            ...EXPENSE_CATEGORY_OPTIONS,
        ]
        : EXPENSE_CATEGORY_OPTIONS

    const selectedIndex = options.findIndex(
        (category) =>
            category.value === value,
    )

    const selectedCategory =
        selectedIndex >= 0
            ? options[selectedIndex]
            : null

    useEffect(() => {
        function handlePointerDown(event) {
            if (
                containerRef.current &&
                !containerRef.current.contains(
                    event.target,
                )
            ) {
                setIsOpen(false)
            }
        }

        document.addEventListener(
            'pointerdown',
            handlePointerDown,
        )

        return () => {
            document.removeEventListener(
                'pointerdown',
                handlePointerDown,
            )
        }
    }, [])

    function getCategoryStyle(category) {
        if (category?.isAllOption) {
            return ALL_CATEGORIES_STYLE
        }

        return (
            CATEGORY_STYLES[category?.value] ??
            DEFAULT_STYLE
        )
    }

    function openDropdown() {
        setActiveIndex(
            selectedIndex >= 0
                ? selectedIndex
                : 0,
        )
        setIsOpen(true)
    }

    function toggleDropdown() {
        if (disabled) {
            return
        }

        if (isOpen) {
            setIsOpen(false)
            return
        }

        openDropdown()
    }

    function selectCategory(category) {
        onChange(category.value)
        onBlur?.()
        setIsOpen(false)
    }

    function handleKeyDown(event) {
        if (disabled) {
            return
        }

        if (event.key === 'Escape') {
            event.preventDefault()
            setIsOpen(false)
            return
        }

        if (event.key === 'ArrowDown') {
            event.preventDefault()

            if (!isOpen) {
                openDropdown()
                return
            }

            setActiveIndex((current) =>
                Math.min(
                    current + 1,
                    options.length - 1,
                ),
            )
            return
        }

        if (event.key === 'ArrowUp') {
            event.preventDefault()

            if (!isOpen) {
                openDropdown()
                return
            }

            setActiveIndex((current) =>
                Math.max(current - 1, 0),
            )
            return
        }

        if (
            isOpen &&
            (event.key === 'Enter' ||
                event.key === ' ')
        ) {
            event.preventDefault()
            selectCategory(
                options[activeIndex],
            )
        }
    }

    const selectedStyle =
        getCategoryStyle(selectedCategory)

    const SelectedIcon =
        selectedStyle.icon

    return (
        <div
            ref={containerRef}
            className={`relative w-full ${className}`}
        >
            <span
                id={`${selectId}-label`}
                className="mb-2 block text-sm font-bold text-heading"
            >
                {label}

                {required && (
                    <span
                        aria-hidden="true"
                        className="ml-1 text-rose-500"
                    >
                        *
                    </span>
                )}
            </span>

            <button
                type="button"
                id={`${selectId}-button`}
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                aria-required={required}
                aria-labelledby={`${selectId}-label ${selectId}-button`}
                aria-controls={`${selectId}-options`}
                aria-invalid={
                    error ? 'true' : undefined
                }
                aria-describedby={
                    error
                        ? `${selectId}-error`
                        : undefined
                }
                disabled={disabled}
                onClick={toggleDropdown}
                onKeyDown={handleKeyDown}
                className={`flex min-h-12 w-full items-center justify-between gap-3 rounded-2xl border bg-white/90 px-3.5 text-left shadow-sm outline-none transition ${
                    error
                        ? 'border-rose-400 focus:border-rose-400 focus:ring-4 focus:ring-rose-100'
                        : 'border-brand-200 hover:border-brand-400 focus:border-brand-500 focus:ring-4 focus:ring-brand-100'
                } disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500`}
            >
                <span className="flex min-w-0 items-center gap-3">
                    <span
                        className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl ${selectedStyle.iconClass}`}
                    >
                        <SelectedIcon
                            size={16}
                            aria-hidden="true"
                        />
                    </span>

                    <span
                        className={`truncate font-semibold ${
                            selectedCategory
                                ? 'text-heading'
                                : 'text-slate-400'
                        }`}
                    >
                        {selectedCategory
                            ? selectedCategory.label
                            : 'Select category'}
                    </span>
                </span>

                <ChevronDown
                    size={18}
                    aria-hidden="true"
                    className={`shrink-0 text-brand-600 transition-transform duration-200 ${
                        isOpen
                            ? 'rotate-180'
                            : ''
                    }`}
                />
            </button>

            {isOpen && (
                <div
                    id={`${selectId}-options`}
                    role="listbox"
                    aria-labelledby={`${selectId}-label`}
                    className="absolute right-0 left-0 z-60 mt-2 max-h-72 overflow-y-auto rounded-2xl border border-brand-100 bg-white p-2 shadow-2xl shadow-brand-900/15"
                >
                    {options.map(
                        (category, index) => {
                            const presentation =
                                getCategoryStyle(
                                    category,
                                )

                            const CategoryIcon =
                                presentation.icon

                            const isSelected =
                                category.value ===
                                value

                            const isActive =
                                activeIndex ===
                                index

                            return (
                                <button
                                    key={
                                        category.value ||
                                        'all'
                                    }
                                    type="button"
                                    role="option"
                                    aria-selected={
                                        isSelected
                                    }
                                    onMouseEnter={() =>
                                        setActiveIndex(
                                            index,
                                        )
                                    }
                                    onClick={() =>
                                        selectCategory(
                                            category,
                                        )
                                    }
                                    className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors ${
                                        isSelected
                                            ? 'bg-brand-100 text-brand-900'
                                            : isActive
                                                ? 'bg-brand-50 text-brand-800'
                                                : 'text-slate-600 hover:bg-brand-50 hover:text-brand-800'
                                    }`}
                                >
                                    <span
                                        className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${presentation.iconClass}`}
                                    >
                                        <CategoryIcon
                                            size={17}
                                            aria-hidden="true"
                                        />
                                    </span>

                                    <span className="min-w-0 flex-1 font-semibold">
                                        {
                                            category.label
                                        }
                                    </span>

                                    {isSelected && (
                                        <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-white text-brand-700 shadow-sm">
                                            <Check
                                                size={
                                                    16
                                                }
                                                aria-hidden="true"
                                            />
                                        </span>
                                    )}
                                </button>
                            )
                        },
                    )}
                </div>
            )}

            {error && (
                <p
                    id={`${selectId}-error`}
                    role="alert"
                    className="mt-2 text-sm font-semibold text-rose-600"
                >
                    {error}
                </p>
            )}
        </div>
    )
}