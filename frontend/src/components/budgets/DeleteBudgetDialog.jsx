import {
    AlertTriangle,
    Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    getCategoryLabel,
} from '../../constants/finance'
import {
    useDeleteBudget,
} from '../../features/budgets/useBudgetQueries'
import { Button } from '../common/Button'
import { Modal } from '../common/Modal'

function formatBudgetPeriod(month, year) {
    const monthNumber = Number(month)
    const yearNumber = Number(year)

    if (
        !Number.isInteger(monthNumber) ||
        monthNumber < 1 ||
        monthNumber > 12 ||
        !Number.isInteger(yearNumber)
    ) {
        return 'the selected period'
    }

    return new Date(
        yearNumber,
        monthNumber - 1,
        1,
    ).toLocaleDateString('en-IN', {
        month: 'long',
        year: 'numeric',
    })
}

export function DeleteBudgetDialog({
                                       budget,
                                       onClose,
                                   }) {
    const [deleteError, setDeleteError] =
        useState(null)

    const deleteMutation =
        useDeleteBudget()

    async function handleDelete() {
        setDeleteError(null)

        try {
            await deleteMutation.mutateAsync(
                budget.id,
            )

            toast.success(
                'Budget deleted successfully.',
            )

            onClose()
        } catch (error) {
            setDeleteError(
                getApiErrorMessage(
                    error,
                    'Budget could not be deleted.',
                ),
            )
        }
    }

    return (
        <Modal
            title="Delete budget?"
            description="This action permanently removes the budget limit."
            onClose={onClose}
            className="max-w-lg"
        >
            <div className="rounded-2xl border border-rose-100 bg-rose-50/80 p-4">
                <div className="flex items-start gap-3">
                    <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-white text-rose-600 shadow-sm">
                        <AlertTriangle
                            size={22}
                            aria-hidden="true"
                        />
                    </span>

                    <div>
                        <p className="font-black text-heading">
                            {getCategoryLabel(
                                budget.category,
                            )}
                        </p>

                        <p className="mt-1 text-sm leading-6 text-slate-600">
                            The budget for{' '}
                            {formatBudgetPeriod(
                                budget.month,
                                budget.year,
                            )}{' '}
                            will be removed. Existing
                            expenses will not be deleted.
                        </p>
                    </div>
                </div>
            </div>

            {deleteError && (
                <p
                    role="alert"
                    className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 p-3 text-sm font-semibold text-rose-700"
                >
                    {deleteError}
                </p>
            )}

            <div className="mt-6 flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end">
                <Button
                    type="button"
                    variant="secondary"
                    disabled={
                        deleteMutation.isPending
                    }
                    onClick={onClose}
                >
                    Keep budget
                </Button>

                <Button
                    type="button"
                    variant="danger"
                    loading={
                        deleteMutation.isPending
                    }
                    onClick={handleDelete}
                >
                    <Trash2
                        size={17}
                        aria-hidden="true"
                    />
                    Delete budget
                </Button>
            </div>
        </Modal>
    )
}