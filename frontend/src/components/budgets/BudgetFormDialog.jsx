import {
    zodResolver,
} from '@hookform/resolvers/zod'
import {
    Controller,
    useForm,
} from 'react-hook-form'
import { toast } from 'sonner'
import { useState } from 'react'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    useCreateBudget,
    useUpdateBudget,
} from '../../features/budgets/useBudgetQueries'
import {
    budgetSchema,
} from '../../features/finance/financeSchemas'
import { Button } from '../common/Button'
import { Input } from '../common/Input'
import { Modal } from '../common/Modal'
import {
    CategorySelect,
} from '../expenses/CategorySelect'

const MONTH_OPTIONS = Object.freeze(
    Array.from(
        { length: 12 },
        (_, index) => ({
            value: index + 1,
            label: new Date(
                2000,
                index,
                1,
            ).toLocaleDateString(
                'en-IN',
                {
                    month: 'long',
                },
            ),
        }),
    ),
)

function getDefaultValues(budget) {
    const today = new Date()

    if (budget) {
        return {
            category: budget.category,
            monthlyLimit: String(
                budget.monthlyLimit,
            ),
            month: Number(budget.month),
            year: Number(budget.year),
        }
    }

    return {
        category: '',
        monthlyLimit: '',
        month: today.getMonth() + 1,
        year: today.getFullYear(),
    }
}

export function BudgetFormDialog({
                                     budget,
                                     onClose,
                                 }) {
    const editing = Boolean(budget)

    const [submitError, setSubmitError] =
        useState(null)

    const createMutation =
        useCreateBudget()

    const updateMutation =
        useUpdateBudget()

    const {
        register,
        control,
        handleSubmit,
        formState: {
            errors,
        },
    } = useForm({
        resolver:
            zodResolver(budgetSchema),

        defaultValues:
            getDefaultValues(budget),
    })

    const saving =
        createMutation.isPending ||
        updateMutation.isPending

    async function submitBudget(values) {
        setSubmitError(null)

        try {
            if (editing) {
                await updateMutation.mutateAsync({
                    budgetId: budget.id,
                    budget: values,
                })

                toast.success(
                    'Budget updated successfully.',
                )
            } else {
                await createMutation.mutateAsync(
                    values,
                )

                toast.success(
                    'Budget created successfully.',
                )
            }

            onClose()
        } catch (error) {
            setSubmitError(
                getApiErrorMessage(
                    error,
                    editing
                        ? 'Budget could not be updated.'
                        : 'Budget could not be created.',
                ),
            )
        }
    }

    return (
        <Modal
            title={
                editing
                    ? 'Edit budget'
                    : 'Create budget'
            }
            description={
                editing
                    ? 'Update the category limit or budget period.'
                    : 'Set a monthly spending limit for one expense category.'
            }
            onClose={onClose}
        >
            <form
                className="space-y-5"
                onSubmit={handleSubmit(
                    submitBudget,
                )}
                noValidate
            >
                <Controller
                    name="category"
                    control={control}
                    render={({ field }) => (
                        <CategorySelect
                            label="Category"
                            required
                            value={field.value}
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

                <Input
                    label="Monthly limit"
                    type="number"
                    inputMode="decimal"
                    min="0.01"
                    step="0.01"
                    placeholder="0.00"
                    helperText="Enter the maximum amount you want to spend in this category."
                    required
                    disabled={saving}
                    error={
                        errors.monthlyLimit
                            ?.message
                    }
                    {...register(
                        'monthlyLimit',
                    )}
                />

                <div className="grid gap-5 sm:grid-cols-2">
                    <div className="w-full">
                        <label
                            htmlFor="budget-month"
                            className="mb-2 block text-sm font-bold text-heading"
                        >
                            Month
                            <span
                                aria-hidden="true"
                                className="ml-1 text-rose-500"
                            >
                                *
                            </span>
                        </label>

                        <select
                            id="budget-month"
                            required
                            disabled={saving}
                            aria-invalid={
                                errors.month
                                    ? 'true'
                                    : undefined
                            }
                            className={`min-h-12 w-full rounded-2xl border bg-white/85 px-4 text-heading shadow-sm outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-100 ${
                                errors.month
                                    ? 'border-rose-400 focus:border-rose-400 focus:ring-rose-100'
                                    : 'border-slate-200'
                            }`}
                            {...register(
                                'month',
                            )}
                        >
                            {MONTH_OPTIONS.map(
                                (month) => (
                                    <option
                                        key={
                                            month.value
                                        }
                                        value={
                                            month.value
                                        }
                                    >
                                        {
                                            month.label
                                        }
                                    </option>
                                ),
                            )}
                        </select>

                        {errors.month && (
                            <p
                                role="alert"
                                className="mt-2 text-sm font-semibold text-rose-600"
                            >
                                {
                                    errors.month
                                        .message
                                }
                            </p>
                        )}
                    </div>

                    <Input
                        label="Year"
                        type="number"
                        min="2000"
                        step="1"
                        required
                        disabled={saving}
                        error={
                            errors.year?.message
                        }
                        {...register('year')}
                    />
                </div>

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
                            : 'Create budget'}
                    </Button>
                </div>
            </form>
        </Modal>
    )
}