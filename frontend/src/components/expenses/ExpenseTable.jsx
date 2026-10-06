import {
    CalendarDays,
    Eye,
    Pencil,
    ReceiptText,
    Trash2,
} from 'lucide-react'

import {
    getCategoryLabel,
} from '../../constants/finance'
import {
    formatCurrency,
    formatFinanceDate,
} from '../../utils/finance'
import { cn } from '../../utils/cn'

const categoryStyles = {
    FOOD: 'bg-teal-50 text-teal-700',
    TRAVEL: 'bg-sky-50 text-sky-700',
    SHOPPING: 'bg-amber-50 text-amber-700',
    BILLS: 'bg-emerald-50 text-emerald-700',
    ENTERTAINMENT: 'bg-violet-50 text-violet-700',
    HEALTH: 'bg-rose-50 text-rose-700',
    EDUCATION: 'bg-indigo-50 text-indigo-700',
    RENT: 'bg-orange-50 text-orange-700',
    SUBSCRIPTION: 'bg-cyan-50 text-cyan-700',
    OTHER: 'bg-slate-100 text-slate-700',
}

function CategoryBadge({ category }) {
    return (
        <span
            className={cn(
                'inline-flex rounded-full px-2.5 py-1 text-xs font-bold',
                categoryStyles[category] ??
                categoryStyles.OTHER,
            )}
        >
            {getCategoryLabel(category)}
        </span>
    )
}

function ExpenseActions({
                            expense,
                            onView,
                            onEdit,
                            onDelete,
                        }) {
    if (!onView && !onEdit && !onDelete) {
        return null
    }

    return (
        <div className="flex items-center justify-end gap-1">
            {onView && (
                <button
                    type="button"
                    aria-label={`View ${expense.description || 'expense'}`}
                    onClick={() => onView(expense)}
                    className="grid h-9 w-9 place-items-center rounded-xl text-slate-500 transition hover:bg-brand-50 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                >
                    <Eye
                        size={17}
                        aria-hidden="true"
                    />
                </button>
            )}

            {onEdit && (
                <button
                    type="button"
                    aria-label={`Edit ${expense.description || 'expense'}`}
                    onClick={() => onEdit(expense)}
                    className="grid h-9 w-9 place-items-center rounded-xl text-slate-500 transition hover:bg-sky-50 hover:text-sky-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-sky-100"
                >
                    <Pencil
                        size={17}
                        aria-hidden="true"
                    />
                </button>
            )}

            {onDelete && (
                <button
                    type="button"
                    aria-label={`Delete ${expense.description || 'expense'}`}
                    onClick={() => onDelete(expense)}
                    className="grid h-9 w-9 place-items-center rounded-xl text-slate-500 transition hover:bg-rose-50 hover:text-rose-600 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-rose-100"
                >
                    <Trash2
                        size={17}
                        aria-hidden="true"
                    />
                </button>
            )}
        </div>
    )
}

export function ExpenseTable({
                                 expenses,
                                 onView,
                                 onEdit,
                                 onDelete,
                             }) {
    return (
        <>
            <div className="hidden overflow-x-auto md:block">
                <table className="w-full border-collapse">
                    <thead>
                    <tr className="border-b border-slate-100 text-left text-xs uppercase tracking-wider text-slate-400">
                        <th className="px-4 py-4 font-bold">
                            Expense
                        </th>

                        <th className="px-4 py-4 font-bold">
                            Category
                        </th>

                        <th className="px-4 py-4 font-bold">
                            Date
                        </th>

                        <th className="px-4 py-4 text-right font-bold">
                            Amount
                        </th>

                        {(onView ||
                            onEdit ||
                            onDelete) && (
                            <th className="px-4 py-4 text-right font-bold">
                                Actions
                            </th>
                        )}
                    </tr>
                    </thead>

                    <tbody>
                    {expenses.map((expense) => (
                        <tr
                            key={expense.id}
                            className="border-b border-slate-100/80 transition last:border-0 hover:bg-brand-50/40"
                        >
                            <td className="px-4 py-4">
                                <div className="flex items-center gap-3">
                                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-brand-50 text-brand-700">
                                            <ReceiptText
                                                size={18}
                                                aria-hidden="true"
                                            />
                                        </span>

                                    <div>
                                        <p className="max-w-xs truncate font-bold text-heading">
                                            {expense.description ||
                                                'Untitled expense'}
                                        </p>

                                        <p className="text-xs text-slate-400">
                                            Expense #{expense.id}
                                        </p>
                                    </div>
                                </div>
                            </td>

                            <td className="px-4 py-4">
                                <CategoryBadge
                                    category={
                                        expense.category
                                    }
                                />
                            </td>

                            <td className="px-4 py-4 text-sm font-medium text-slate-600">
                                {formatFinanceDate(
                                    expense.expenseDate,
                                )}
                            </td>

                            <td className="px-4 py-4 text-right font-black text-heading">
                                {formatCurrency(
                                    expense.amount,
                                )}
                            </td>

                            {(onView ||
                                onEdit ||
                                onDelete) && (
                                <td className="px-4 py-4">
                                    <ExpenseActions
                                        expense={expense}
                                        onView={onView}
                                        onEdit={onEdit}
                                        onDelete={
                                            onDelete
                                        }
                                    />
                                </td>
                            )}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            <div className="grid gap-3 md:hidden">
                {expenses.map((expense) => (
                    <article
                        key={expense.id}
                        className="rounded-2xl border border-slate-100 bg-white/70 p-4"
                    >
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex min-w-0 items-center gap-3">
                                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-brand-50 text-brand-700">
                                    <ReceiptText
                                        size={18}
                                        aria-hidden="true"
                                    />
                                </span>

                                <div className="min-w-0">
                                    <h3 className="truncate font-bold text-heading">
                                        {expense.description ||
                                            'Untitled expense'}
                                    </h3>

                                    <p className="mt-1 inline-flex items-center gap-1 text-xs text-slate-500">
                                        <CalendarDays
                                            size={13}
                                            aria-hidden="true"
                                        />

                                        {formatFinanceDate(
                                            expense.expenseDate,
                                        )}
                                    </p>
                                </div>
                            </div>

                            <p className="shrink-0 font-black text-heading">
                                {formatCurrency(
                                    expense.amount,
                                )}
                            </p>
                        </div>

                        <div className="mt-4 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
                            <CategoryBadge
                                category={expense.category}
                            />

                            <ExpenseActions
                                expense={expense}
                                onView={onView}
                                onEdit={onEdit}
                                onDelete={onDelete}
                            />
                        </div>
                    </article>
                ))}
            </div>
        </>
    )
}