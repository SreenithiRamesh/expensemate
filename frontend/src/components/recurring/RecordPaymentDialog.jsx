import {
    zodResolver,
} from '@hookform/resolvers/zod'
import {
    useForm,
} from 'react-hook-form'
import { useState } from 'react'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    useRecordRecurringPayment,
} from '../../features/recurring-expenses/useRecurringExpenseQueries'
import {
    recurringPaymentSchema,
} from '../../features/finance/financeSchemas'
import {
    getLocalDateInputValue,
} from '../../utils/finance'
import { Button } from '../common/Button'
import { Input } from '../common/Input'
import { Modal } from '../common/Modal'

export function RecordPaymentDialog({
                                        recurringExpense,
                                        onClose,
                                    }) {
    const [submitError, setSubmitError] =
        useState(null)

    const recordMutation =
        useRecordRecurringPayment()

    const {
        register,
        handleSubmit,
        formState: {
            errors,
        },
    } = useForm({
        resolver:
            zodResolver(
                recurringPaymentSchema,
            ),

        defaultValues: {
            paymentDate:
                getLocalDateInputValue(),
        },
    })

    async function submitPayment(values) {
        setSubmitError(null)

        try {
            await recordMutation.mutateAsync({
                recurringExpenseId:
                recurringExpense.id,
                paymentDate:
                values.paymentDate,
            })

            toast.success(
                'Payment recorded and next due date updated.',
            )

            onClose()
        } catch (error) {
            setSubmitError(
                getApiErrorMessage(
                    error,
                    'Payment could not be recorded.',
                ),
            )
        }
    }

    return (
        <Modal
            title="Record payment"
            description={`Record the latest payment for ${recurringExpense.title}.`}
            onClose={onClose}
            className="max-w-lg"
        >
            <div className="rounded-2xl border border-brand-100 bg-brand-50/60 p-4 text-sm leading-6 text-slate-600">
                Recording this payment creates a
                personal expense and automatically
                advances the schedule&rsquo;s next due
                date.
            </div>

            <form
                className="mt-5 space-y-5"
                onSubmit={handleSubmit(
                    submitPayment,
                )}
                noValidate
            >
                <Input
                    label="Payment date"
                    type="date"
                    required
                    disabled={
                        recordMutation.isPending
                    }
                    error={
                        errors.paymentDate
                            ?.message
                    }
                    {...register(
                        'paymentDate',
                    )}
                />

                {submitError && (
                    <p
                        role="alert"
                        className="rounded-2xl border border-rose-200 bg-rose-50 p-3 text-sm font-semibold text-rose-700"
                    >
                        {submitError}
                    </p>
                )}

                <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end">
                    <Button
                        type="button"
                        variant="secondary"
                        disabled={
                            recordMutation
                                .isPending
                        }
                        onClick={onClose}
                    >
                        Cancel
                    </Button>

                    <Button
                        type="submit"
                        loading={
                            recordMutation
                                .isPending
                        }
                    >
                        Record payment
                    </Button>
                </div>
            </form>
        </Modal>
    )
}