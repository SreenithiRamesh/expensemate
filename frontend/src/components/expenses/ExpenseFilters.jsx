import {
    ChevronDown,
    RotateCcw,
    Search,
} from 'lucide-react'
import {
    useEffect,
    useId,
    useRef,
    useState,
} from 'react'

import { Input } from '../common/Input'
import {
    CategorySelect,
} from './CategorySelect'

const PAGE_SIZE_OPTIONS = [
    { value: 10, label: '10' },
    { value: 20, label: '20' },
    { value: 50, label: '50' },
]

function PageSizeDropdown({
                              value,
                              onChange,
                          }) {
    const dropdownId = useId()
    const containerRef = useRef(null)
    const [isOpen, setIsOpen] = useState(false)

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

    function selectSize(size) {
        onChange(size)
        setIsOpen(false)
    }

    return (
        <div
            ref={containerRef}
            className="relative w-20"
        >
            <button
                type="button"
                id={`${dropdownId}-button`}
                aria-label="Rows per page"
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                aria-controls={`${dropdownId}-options`}
                onClick={() =>
                    setIsOpen((current) => !current)
                }
                className="flex min-h-10 w-full items-center justify-between rounded-xl border border-brand-200 bg-white px-3 text-sm font-bold text-heading shadow-sm outline-none transition hover:border-brand-400 focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
            >
                {value}

                <ChevronDown
                    size={16}
                    aria-hidden="true"
                    className={`text-brand-600 transition-transform ${
                        isOpen
                            ? 'rotate-180'
                            : ''
                    }`}
                />
            </button>

            {isOpen && (
                <div
                    id={`${dropdownId}-options`}
                    role="listbox"
                    aria-label="Rows per page"
                    className="absolute right-0 left-0 z-60 mt-2 rounded-xl border border-brand-100 bg-white p-1.5 shadow-xl shadow-brand-900/15"
                >
                    {PAGE_SIZE_OPTIONS.map(
                        (option) => {
                            const selected =
                                option.value ===
                                value

                            return (
                                <button
                                    key={
                                        option.value
                                    }
                                    type="button"
                                    role="option"
                                    aria-selected={
                                        selected
                                    }
                                    onClick={() =>
                                        selectSize(
                                            option.value,
                                        )
                                    }
                                    className={`w-full rounded-lg px-3 py-2 text-left text-sm font-bold transition ${
                                        selected
                                            ? 'bg-brand-100 text-brand-900'
                                            : 'text-slate-600 hover:bg-brand-50 hover:text-brand-800'
                                    }`}
                                >
                                    {
                                        option.label
                                    }
                                </button>
                            )
                        },
                    )}
                </div>
            )}
        </div>
    )
}

export function ExpenseFilters({
                                   filters,
                                   onChange,
                                   onReset,
                               }) {
    return (
        <section
            aria-label="Expense filters"
            className="relative z-30 overflow-visible rounded-3xl border border-white/80 bg-white/80 p-5 shadow-sm backdrop-blur-xl"
        >
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-[1.4fr_1fr_1fr_1fr_auto]">
                <div className="relative">
                    <Search
                        aria-hidden="true"
                        size={18}
                        className="pointer-events-none absolute top-[2.75rem] left-4 z-10 text-slate-400"
                    />

                    <Input
                        label="Search"
                        type="search"
                        value={filters.search}
                        placeholder="Search descriptions..."
                        className="pl-11"
                        onChange={(event) =>
                            onChange(
                                'search',
                                event.target.value,
                            )
                        }
                    />
                </div>

                <CategorySelect
                    label="Category"
                    includeAll
                    value={filters.category}
                    onChange={(category) =>
                        onChange(
                            'category',
                            category,
                        )
                    }
                />

                <Input
                    label="From date"
                    type="date"
                    value={filters.startDate}
                    max={
                        filters.endDate ||
                        undefined
                    }
                    onChange={(event) =>
                        onChange(
                            'startDate',
                            event.target.value,
                        )
                    }
                />

                <Input
                    label="To date"
                    type="date"
                    value={filters.endDate}
                    min={
                        filters.startDate ||
                        undefined
                    }
                    onChange={(event) =>
                        onChange(
                            'endDate',
                            event.target.value,
                        )
                    }
                />

                <div className="flex items-end">
                    <button
                        type="button"
                        onClick={onReset}
                        className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl border-[1.5px] border-brand-200 bg-white px-5 font-bold text-brand-700 transition-colors duration-150 hover:border-brand-400 hover:bg-brand-50 active:bg-brand-100 xl:w-auto"
                    >
                        <RotateCcw
                            size={17}
                            aria-hidden="true"
                        />
                        Reset
                    </button>
                </div>
            </div>

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
                <p className="text-xs font-medium text-slate-500">
                    Results are ordered by expense date,
                    newest first.
                </p>

                <div className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                    <span>Rows per page</span>

                    <PageSizeDropdown
                        value={filters.size}
                        onChange={(size) =>
                            onChange(
                                'size',
                                size,
                            )
                        }
                    />
                </div>
            </div>
        </section>
    )
}