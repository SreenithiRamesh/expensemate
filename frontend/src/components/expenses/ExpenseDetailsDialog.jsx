import {
    CalendarDays,
    Clock3,
    Tag,
    Wallet,
} from 'lucide-react'

import {
    getCategoryLabel,
} from '../../constants/finance'
import {
    formatCurrency,
    formatDateTime,
    formatFinanceDate,
} from '../../utils/finance'
import { Modal } from '../common/Modal'

function DetailRow({
                       icon: Icon,
                       label,
                       value,
                   }) {
    return (
        <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white text-brand-700 shadow-sm">
                <Icon
                    size={17}
                    aria-hidden="true"
                />
            </span>

            <div>
                <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                    {label}
                </p>

                <p className="mt-1 font-bold text-heading">
                    {value}
                </p>
            </div>
        </div>
    )
}

export function ExpenseDetailsDialog({
                                         expense,
                                         onClose,
                                     }) {
    return (
        <Modal
            title="Expense details"
            description={`Transaction #${expense.id}`}
            onClose={onClose}
            className="max-w-xl"
        >
            <div className="rounded-3xl bg-linear-to-br from-brand-600 to-sky-500 p-6 text-white">
                <p className="text-sm font-semibold text-white/75">
                    Amount spent
                </p>

                <p className="mt-2 text-4xl font-black">
                    {formatCurrency(
                        expense.amount,
                    )}
                </p>

                <p className="mt-3 text-sm text-white/85">
                    {expense.description ||
                        'No description provided'}
                </p>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <DetailRow
                    icon={Tag}
                    label="Category"
                    value={getCategoryLabel(
                        expense.category,
                    )}
                />

                <DetailRow
                    icon={CalendarDays}
                    label="Expense date"
                    value={formatFinanceDate(
                        expense.expenseDate,
                    )}
                />

                <DetailRow
                    icon={Clock3}
                    label="Created"
                    value={formatDateTime(
                        expense.createdAt,
                    )}
                />

                <DetailRow
                    icon={Wallet}
                    label="Last updated"
                    value={formatDateTime(
                        expense.updatedAt,
                    )}
                />
            </div>
        </Modal>
    )
}