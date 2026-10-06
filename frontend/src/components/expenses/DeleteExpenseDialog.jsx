import {
    AlertTriangle,
} from 'lucide-react'
import {
    useState,
} from 'react'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    useDeleteExpense,
} from '../../features/expenses/useExpenseQueries'
import {
    formatCurrency,
} from '../../utils/finance'
import { Button } from '../common/Button'
import { Modal } from '../common/Modal'

export function DeleteExpenseDialog({
                                        expense,
                                        onClose,
                                    }) {
    const [errorMessage, setErrorMessage] =
        useState(null)

    const deleteMutation =
        useDeleteExpense()

    async function confirmDelete() {
        setErrorMessage(null)

        try {
            await deleteMutation.mutateAsync(
                expense.id,
            )

            toast.success(
                'Expense deleted successfully.',
            )

            onClose()
        } catch (error) {
            setErrorMessage(
                getApiErrorMessage(
                    error,
                    'Expense could not be deleted.',
                ),
            )
        }
    }

    return (
        <Modal
            title="Delete expense?"
            description="This action cannot be undone."
            onClose={onClose}
            className="max-w-lg"
        >
            <div className="flex items-start gap-4 rounded-2xl border border-rose-200 bg-rose-50 p-4">
                <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-rose-100 text-rose-700">
                    <AlertTriangle
                        size={21}
                        aria-hidden="true"
                    />
                </span>

                <div>
                    <p className="font-black text-rose-950">
                        {expense.description ||
                            'Untitled expense'}
                    </p>

                    <p className="mt-1 text-sm text-rose-800">
                        {formatCurrency(
                            expense.amount,
                        )}
                    </p>
                </div>
            </div>

            {errorMessage && (
                <p
                    role="alert"
                    className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 p-3 text-sm font-semibold text-rose-700"
                >
                    {errorMessage}
                </p>
            )}

            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <Button
                    type="button"
                    variant="secondary"
                    disabled={
                        deleteMutation.isPending
                    }
                    onClick={onClose}
                >
                    Cancel
                </Button>

                <Button
                    type="button"
                    variant="danger"
                    loading={
                        deleteMutation.isPending
                    }
                    onClick={confirmDelete}
                >
                    Delete expense
                </Button>
            </div>
        </Modal>
    )
}