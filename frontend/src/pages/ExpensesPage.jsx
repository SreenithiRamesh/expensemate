import {
    AlertTriangle,
    ChevronLeft,
    ChevronRight,
    Plus,
    ReceiptText,
    RefreshCw,
} from 'lucide-react'
import {
    useDeferredValue,
    useState,
} from 'react'

import { getApiErrorMessage } from '../api/apiErrors'
import { Button } from '../components/common/Button'
import {
    Card,
    CardDescription,
    CardHeader,
    CardTitle,
} from '../components/common/Card'
import {
    DeleteExpenseDialog,
} from '../components/expenses/DeleteExpenseDialog'
import {
    ExpenseDetailsDialog,
} from '../components/expenses/ExpenseDetailsDialog'
import {
    ExpenseFilters,
} from '../components/expenses/ExpenseFilters'
import {
    ExpenseFormDialog,
} from '../components/expenses/ExpenseFormDialog'
import {
    ExpenseTable,
} from '../components/expenses/ExpenseTable'
import {
    DEFAULT_EXPENSE_PAGE_SIZE,
} from '../constants/finance'
import {
    useExpenses,
} from '../features/expenses/useExpenseQueries'

const initialFilters = {
    search: '',
    category: '',
    startDate: '',
    endDate: '',
    size: DEFAULT_EXPENSE_PAGE_SIZE,
}

function ExpensesLoadingState() {
    return (
        <div
            role="status"
            aria-label="Loading expenses"
            className="space-y-3"
        >
            {Array.from({ length: 5 }).map(
                (_, index) => (
                    <div
                        key={index}
                        className="h-16 animate-pulse rounded-2xl bg-linear-to-r from-slate-100 via-brand-50/60 to-slate-100 motion-reduce:animate-none"
                    />
                ),
            )}
        </div>
    )
}

function ExpensesEmptyState({
                                filtered,
                                onReset,
                                onAdd,
                            }) {
    return (
        <div className="flex min-h-72 flex-col items-center justify-center px-5 text-center">
            <span className="grid h-16 w-16 place-items-center rounded-3xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/20">
                <ReceiptText
                    size={30}
                    aria-hidden="true"
                />
            </span>

            <h2 className="mt-5 font-display text-xl font-bold text-heading">
                {filtered
                    ? 'No matching expenses'
                    : 'No expenses yet'}
            </h2>

            <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                {filtered
                    ? 'Try changing or resetting the current filters.'
                    : 'Add your first personal expense to start tracking your spending.'}
            </p>

            <div className="mt-5 flex flex-wrap justify-center gap-3">
                {filtered ? (
                    <Button
                        variant="secondary"
                        onClick={onReset}
                    >
                        Reset filters
                    </Button>
                ) : (
                    <Button
                        onClick={onAdd}
                    >
                        <Plus
                            size={17}
                            aria-hidden="true"
                        />
                        Add first expense
                    </Button>
                )}
            </div>
        </div>
    )
}

export default function ExpensesPage() {
    const [filters, setFilters] =
        useState(initialFilters)

    const [page, setPage] =
        useState(0)

    const [dialog, setDialog] =
        useState(null)

    const deferredSearch =
        useDeferredValue(filters.search)

    const queryFilters = {
        ...filters,
        search: deferredSearch,
        page,
    }

    const {
        data,
        error,
        isError,
        isFetching,
        isLoading,
        refetch,
    } = useExpenses(queryFilters)

    const expenses = data?.content ?? []
    const currentPage = data?.number ?? page
    const totalPages = data?.totalPages ?? 0
    const totalElements =
        data?.totalElements ?? 0

    const hasFilters = Boolean(
        filters.search ||
        filters.category ||
        filters.startDate ||
        filters.endDate,
    )

    function handleFilterChange(
        field,
        value,
    ) {
        setFilters((current) => ({
            ...current,
            [field]: value,
        }))

        setPage(0)
    }

    function handleResetFilters() {
        setFilters(initialFilters)
        setPage(0)
    }

    function openCreateDialog() {
        setDialog({
            type: 'create',
        })
    }

    function openDetailsDialog(expense) {
        setDialog({
            type: 'details',
            expense,
        })
    }

    function openEditDialog(expense) {
        setDialog({
            type: 'edit',
            expense,
        })
    }

    function openDeleteDialog(expense) {
        setDialog({
            type: 'delete',
            expense,
        })
    }

    function closeDialog() {
        setDialog(null)
    }

    return (
        <>
            <div className="space-y-6">
                <header className="flex flex-col justify-between gap-4 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl sm:flex-row sm:items-center">
                    <div className="flex items-start gap-4">
                        <span className="hidden h-12 w-12 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/20 sm:grid">
                            <ReceiptText
                                size={22}
                                aria-hidden="true"
                            />
                        </span>

                        <div>
                            <p className="text-sm font-bold text-brand-700">
                                Personal finance
                            </p>

                            <h1 className="mt-1 font-display text-3xl font-extrabold tracking-tight text-heading">
                                Expenses
                            </h1>

                            <p className="mt-2 text-sm text-slate-500">
                                Search, filter and manage
                                your personal spending.
                            </p>
                        </div>
                    </div>

                    <Button
                        onClick={openCreateDialog}
                    >
                        <Plus
                            size={18}
                            aria-hidden="true"
                        />
                        Add expense
                    </Button>
                </header>

                <ExpenseFilters
                    filters={filters}
                    onChange={handleFilterChange}
                    onReset={
                        handleResetFilters
                    }
                />

                <Card>
                    <CardHeader className="items-center">
                        <div>
                            <CardTitle>
                                Expense history
                            </CardTitle>

                            <CardDescription>
                                {totalElements === 1
                                    ? '1 expense'
                                    : `${totalElements} expenses`}
                            </CardDescription>
                        </div>

                        {isFetching &&
                            !isLoading && (
                                <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
                                    <RefreshCw
                                        size={13}
                                        className="animate-spin motion-reduce:animate-none"
                                        aria-hidden="true"
                                    />
                                    Updating
                                </span>
                            )}
                    </CardHeader>

                    {isLoading && (
                        <ExpensesLoadingState />
                    )}

                    {isError && (
                        <div
                            role="alert"
                            className="flex min-h-64 flex-col items-center justify-center px-5 text-center"
                        >
                            <span className="grid h-14 w-14 place-items-center rounded-2xl bg-[#E66A6A]/10 text-[#E66A6A]">
                                <AlertTriangle
                                    size={26}
                                    aria-hidden="true"
                                />
                            </span>

                            <h2 className="mt-4 font-display text-xl font-bold text-heading">
                                Expenses could not be
                                loaded
                            </h2>

                            <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                                {getApiErrorMessage(
                                    error,
                                    'Please check the backend connection and try again.',
                                )}
                            </p>

                            <Button
                                variant="secondary"
                                className="mt-5"
                                onClick={() =>
                                    refetch()
                                }
                            >
                                <RefreshCw
                                    size={17}
                                    aria-hidden="true"
                                />
                                Try again
                            </Button>
                        </div>
                    )}

                    {!isLoading &&
                        !isError &&
                        expenses.length === 0 && (
                            <ExpensesEmptyState
                                filtered={
                                    hasFilters
                                }
                                onReset={
                                    handleResetFilters
                                }
                                onAdd={
                                    openCreateDialog
                                }
                            />
                        )}

                    {!isLoading &&
                        !isError &&
                        expenses.length > 0 && (
                            <>
                                <ExpenseTable
                                    expenses={
                                        expenses
                                    }
                                    onView={
                                        openDetailsDialog
                                    }
                                    onEdit={
                                        openEditDialog
                                    }
                                    onDelete={
                                        openDeleteDialog
                                    }
                                />

                                <div className="mt-6 flex flex-col items-center justify-between gap-4 border-t border-slate-100 pt-5 sm:flex-row">
                                    <p className="text-sm font-medium text-slate-500">
                                        Page{' '}
                                        {totalPages === 0
                                            ? 0
                                            : currentPage +
                                            1}{' '}
                                        of {totalPages}
                                    </p>

                                    <div className="flex gap-2">
                                        <Button
                                            variant="secondary"
                                            size="small"
                                            disabled={
                                                data?.first ??
                                                (currentPage ===
                                                    0)
                                            }
                                            onClick={() =>
                                                setPage(
                                                    (
                                                        current,
                                                    ) =>
                                                        Math.max(
                                                            current -
                                                            1,
                                                            0,
                                                        ),
                                                )
                                            }
                                        >
                                            <ChevronLeft
                                                size={
                                                    17
                                                }
                                                aria-hidden="true"
                                            />
                                            Previous
                                        </Button>

                                        <Button
                                            variant="secondary"
                                            size="small"
                                            disabled={
                                                data?.last ??
                                                (currentPage >=
                                                    totalPages -
                                                    1)
                                            }
                                            onClick={() =>
                                                setPage(
                                                    (
                                                        current,
                                                    ) =>
                                                        current +
                                                        1,
                                                )
                                            }
                                        >
                                            Next
                                            <ChevronRight
                                                size={
                                                    17
                                                }
                                                aria-hidden="true"
                                            />
                                        </Button>
                                    </div>
                                </div>
                            </>
                        )}
                </Card>
            </div>

            {dialog?.type === 'create' && (
                <ExpenseFormDialog
                    key="create-expense"
                    onClose={closeDialog}
                />
            )}

            {dialog?.type === 'edit' && (
                <ExpenseFormDialog
                    key={`edit-expense-${dialog.expense.id}`}
                    expense={dialog.expense}
                    onClose={closeDialog}
                />
            )}

            {dialog?.type === 'details' && (
                <ExpenseDetailsDialog
                    expense={dialog.expense}
                    onClose={closeDialog}
                />
            )}

            {dialog?.type === 'delete' && (
                <DeleteExpenseDialog
                    expense={dialog.expense}
                    onClose={closeDialog}
                />
            )}
        </>
    )
}