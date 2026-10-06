import {
    zodResolver,
} from '@hookform/resolvers/zod'
import {
    Controller,
    useForm,
} from 'react-hook-form'
import { useState } from 'react'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    RECURRING_FREQUENCY_OPTIONS,
} from '../../constants/finance'
import {
    useCreateRecurringExpense,
    useUpdateRecurringExpense,
} from '../../features/recurring-expenses/useRecurringExpenseQueries'
import {
    recurringExpenseSchema,
} from '../../features/finance/financeSchemas'
import {
    getLocalDateInputValue,
} from '../../utils/finance'
import { Button } from '../common/Button'
import { Input } from '../common/Input'
import { Modal } from '../common/Modal'
import {
    CategorySelect,
} from '../expenses/CategorySelect'

function getDefaultValues(
    recurringExpense,
) {
    if (recurringExpense) {
        return {
            title:
            recurringExpense.title,
            amount: String(
                recurringExpense.amount,
            ),
            category:
            recurringExpense.category,
            frequency:
            recurringExpense.frequency,
            nextDueDate:
            recurringExpense.nextDueDate,
            active: Boolean(
                recurringExpense.active,
            ),
        }
    }

    return {
        title: '',
        amount: '',
        category: '',
        frequency: 'MONTHLY',
        nextDueDate:
            getLocalDateInputValue(),
        active: true,
    }
}

export function RecurringExpenseFormDialog({
                                               recurringExpense,
                                               onClose,
                                           }) {
    const editing =
        Boolean(recurringExpense)

    const [submitError, setSubmitError] =
        useState(null)

    const createMutation =
        useCreateRecurringExpense()

    const updateMutation =
        useUpdateRecurringExpense()

    const {
        register,
        control,
        handleSubmit,
        formState: {
            errors,
        },
    } = useForm({
        resolver:
            zodResolver(
                recurringExpenseSchema,
            ),

        defaultValues:
            getDefaultValues(
                recurringExpense,
            ),
    })

    const saving =
        createMutation.isPending ||
        updateMutation.isPending

    async function submitRecurringExpense(
        values,
    ) {
        setSubmitError(null)

        try {
            if (editing) {
                await updateMutation
                    .mutateAsync({
                        recurringExpenseId:
                        recurringExpense.id,
                        recurringExpense:
                        values,
                    })

                toast.success(
                    'Recurring expense updated.',
                )
            } else {
                await createMutation
                    .mutateAsync(values)

                toast.success(
                    'Recurring expense created.',
                )
            }

            onClose()
        } catch (error) {
            setSubmitError(
                getApiErrorMessage(
                    error,
                    editing
                        ? 'Recurring expense could not be updated.'
                        : 'Recurring expense could not be created.',
                ),
            )
        }
    }

    return (
        <Modal
            title={
                editing
                    ? 'Edit recurring expense'
                    : 'Create recurring expense'
            }
            description={
                editing
                    ? 'Update the schedule and payment details.'
                    : 'Create a repeating payment schedule for a regular expense.'
            }
            onClose={onClose}
        >
            <form
                className="space-y-5"
                onSubmit={handleSubmit(
                    submitRecurringExpense,
                )}
                noValidate
            >
                <Input
                    label="Title"
                    type="text"
                    maxLength={120}
                    placeholder="Example: Internet bill"
                    required
                    disabled={saving}
                    error={
                        errors.title?.message
                    }
                    {...register('title')}
                />

                <div className="grid gap-5 sm:grid-cols-2">
                    <Input
                        label="Amount"
                        type="number"
                        inputMode="decimal"
                        min="0.01"
                        step="0.01"
                        placeholder="0.00"
                        required
                        disabled={saving}
                        error={
                            errors.amount
                                ?.message
                        }
                        {...register('amount')}
                    />

                    <Controller
                        name="category"
                        control={control}
                        render={({ field }) => (
                            <CategorySelect
                                label="Category"
                                required
                                value={
                                    field.value
                                }
                                onChange={
                                    field.onChange
                                }
                                onBlur={
                                    field.onBlur
                                }
                                error={
                                    errors.category
                                        ?.message
                                }
                                disabled={saving}
                            />
                        )}
                    />
                </div>

                <div className="grid gap-5 sm:grid-cols-2">
                    <div className="w-full">
                        <label
                            htmlFor="recurring-frequency"
                            className="mb-2 block text-sm font-bold text-heading"
                        >
                            Frequency

                            <span
                                aria-hidden="true"
                                className="ml-1 text-rose-500"
                            >
                                *
                            </span>
                        </label>

                        <select
                            id="recurring-frequency"
                            required
                            disabled={saving}
                            aria-invalid={
                                errors.frequency
                                    ? 'true'
                                    : undefined
                            }
                            className={`min-h-12 w-full rounded-2xl border bg-white/85 px-4 text-heading shadow-sm outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-100 ${
                                errors.frequency
                                    ? 'border-rose-400 focus:border-rose-400 focus:ring-rose-100'
                                    : 'border-slate-200'
                            }`}
                            {...register(
                                'frequency',
                            )}
                        >
                            {RECURRING_FREQUENCY_OPTIONS.map(
                                (option) => (
                                    <option
                                        key={
                                            option.value
                                        }
                                        value={
                                            option.value
                                        }
                                    >
                                        {
                                            option.label
                                        }
                                    </option>
                                ),
                            )}
                        </select>

                        {errors.frequency && (
                            <p
                                role="alert"
                                className="mt-2 text-sm font-semibold text-rose-600"
                            >
                                {
                                    errors.frequency
                                        .message
                                }
                            </p>
                        )}
                    </div>

                    <Input
                        label="Next due date"
                        type="date"
                        required
                        disabled={saving}
                        error={
                            errors.nextDueDate
                                ?.message
                        }
                        {...register(
                            'nextDueDate',
                        )}
                    />
                </div>

                <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-brand-100 bg-brand-50/60 p-4">
                    <input
                        type="checkbox"
                        disabled={saving}
                        className="mt-0.5 h-5 w-5 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                        {...register('active')}
                    />

                    <span>
                        <span className="block font-bold text-heading">
                            Active schedule
                        </span>

                        <span className="mt-1 block text-sm leading-6 text-slate-500">
                            Active schedules can record
                            payments and appear in upcoming
                            reminders.
                        </span>
                    </span>
                </label>

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
                        disabled={saving}
                        onClick={onClose}
                    >
                        Cancel
                    </Button>

                    <Button
                        type="submit"
                        loading={saving}
                    >
                        {editing
                            ? 'Save changes'
                            : 'Create schedule'}
                    </Button>
                </div>
            </form>
        </Modal>
    )
}