import { zodResolver } from '@hookform/resolvers/zod'
import {
    Bot,
    Sparkles,
} from 'lucide-react'
import { useState } from 'react'
import {
    Controller,
    useForm,
} from 'react-hook-form'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../../api/apiErrors'
import {
    useAiExpenseSuggestion,
} from '../../features/expenses/useAiExpenseSuggestion'
import {
    useCreateExpense,
    useUpdateExpense,
} from '../../features/expenses/useExpenseQueries'
import {
    expenseSchema,
} from '../../features/finance/financeSchemas'
import {
    getLocalDateInputValue,
} from '../../utils/finance'
import { Button } from '../common/Button'
import { Input } from '../common/Input'
import { Modal } from '../common/Modal'
import { Textarea } from '../common/Textarea'
import {
    AiUnavailableState,
} from './AiUnavailableState'
import {
    CategorySelect,
} from './CategorySelect'

function getDefaultValues(expense) {
    if (expense) {
        return {
            amount: String(expense.amount),
            category: expense.category,
            expenseDate: expense.expenseDate,
            description:
                expense.description ?? '',
        }
    }

    return {
        amount: '',
        category: '',
        expenseDate:
            getLocalDateInputValue(),
        description: '',
    }
}

export function ExpenseFormDialog({
                                      expense,
                                      onClose,
                                  }) {
    const editing = Boolean(expense)

    const [aiText, setAiText] = useState(
        expense?.description ?? '',
    )

    const [aiError, setAiError] =
        useState(null)

    const [suggestion, setSuggestion] =
        useState(null)

    const [submitError, setSubmitError] =
        useState(null)

    const createMutation =
        useCreateExpense()

    const updateMutation =
        useUpdateExpense()

    const aiMutation =
        useAiExpenseSuggestion()

    const {
        register,
        control,
        handleSubmit,
        setValue,
        formState: {
            errors,
        },
    } = useForm({
        resolver:
            zodResolver(expenseSchema),

        defaultValues:
            getDefaultValues(expense),
    })

    const saving =
        createMutation.isPending ||
        updateMutation.isPending

    async function requestSuggestion() {
        setAiError(null)
        setSuggestion(null)

        try {
            const result =
                await aiMutation.mutateAsync(
                    aiText,
                )

            if (result.amount != null) {
                setValue(
                    'amount',
                    String(result.amount),
                    {
                        shouldValidate: true,
                        shouldDirty: true,
                    },
                )
            }

            if (result.category) {
                setValue(
                    'category',
                    result.category,
                    {
                        shouldValidate: true,
                        shouldDirty: true,
                    },
                )
            }

            if (result.expenseDate) {
                setValue(
                    'expenseDate',
                    result.expenseDate,
                    {
                        shouldValidate: true,
                        shouldDirty: true,
                    },
                )
            }

            if (result.description) {
                setValue(
                    'description',
                    result.description,
                    {
                        shouldValidate: true,
                        shouldDirty: true,
                    },
                )
            }

            setSuggestion(result)

            toast.success(
                'AI suggestion applied. Review it before saving.',
            )
        } catch (error) {
            setAiError(
                getApiErrorMessage(
                    error,
                    'Gemini could not generate a suggestion.',
                ),
            )
        }
    }

    async function submitExpense(values) {
        setSubmitError(null)

        try {
            if (editing) {
                await updateMutation.mutateAsync({
                    expenseId: expense.id,
                    expense: values,
                })

                toast.success(
                    'Expense updated successfully.',
                )
            } else {
                await createMutation.mutateAsync(
                    values,
                )

                toast.success(
                    'Expense added successfully.',
                )
            }

            onClose()
        } catch (error) {
            setSubmitError(
                getApiErrorMessage(
                    error,
                    editing
                        ? 'Expense could not be updated.'
                        : 'Expense could not be created.',
                ),
            )
        }
    }

    return (
        <Modal
            title={
                editing
                    ? 'Edit expense'
                    : 'Add expense'
            }
            description={
                editing
                    ? 'Update the transaction details below.'
                    : 'Record a personal expense manually or start with an AI suggestion.'
            }
            onClose={onClose}
        >
            <section className="rounded-2xl border border-brand-100 bg-linear-to-br from-brand-50 to-sky-50 p-4">
                <div className="flex items-start gap-3">
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-white text-brand-700 shadow-sm">
                        <Bot
                            size={20}
                            aria-hidden="true"
                        />
                    </span>

                    <div className="min-w-0 flex-1">
                        <h3 className="font-black text-heading">
                            AI expense assistant
                        </h3>

                        <p className="mt-1 text-sm leading-6 text-slate-500">
                            Example: “Paid ₹640 for
                            team lunch today.”
                        </p>
                    </div>
                </div>

                <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                    <input
                        type="text"
                        value={aiText}
                        maxLength={500}
                        aria-label="Expense description for AI suggestion"
                        placeholder="Describe the expense..."
                        onChange={(event) =>
                            setAiText(
                                event.target.value,
                            )
                        }
                        className="min-h-11 flex-1 rounded-2xl border border-brand-200 bg-white px-4 text-sm text-heading outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
                    />

                    <Button
                        type="button"
                        size="small"
                        loading={
                            aiMutation.isPending
                        }
                        disabled={
                            !aiText.trim() ||
                            aiText.trim().length >
                            500
                        }
                        onClick={
                            requestSuggestion
                        }
                    >
                        <Sparkles
                            size={16}
                            aria-hidden="true"
                        />
                        Suggest
                    </Button>
                </div>

                <p className="mt-2 text-right text-xs text-slate-400">
                    {aiText.length}/500
                </p>

                {suggestion && (
                    <div className="mt-3 rounded-2xl border border-emerald-100 bg-emerald-50 p-3 text-sm text-emerald-800">
                        <p className="font-bold">
                            Suggestion applied
                        </p>

                        <p className="mt-1">
                            {suggestion.requiresReview
                                ? 'Please review the suggested values carefully.'
                                : 'The suggested values are ready for your review.'}
                        </p>

                        <p className="mt-1 text-xs">
                            AI requests remaining:{' '}
                            {
                                suggestion.remainingRequests
                            }
                        </p>
                    </div>
                )}
            </section>

            {aiError && (
                <div className="mt-4">
                    <AiUnavailableState
                        message={aiError}
                        onRetry={
                            aiText.trim()
                                ? requestSuggestion
                                : undefined
                        }
                    />
                </div>
            )}

            <form
                className="mt-6 space-y-5"
                onSubmit={handleSubmit(
                    submitExpense,
                )}
                noValidate
            >
                <div className="grid gap-5 sm:grid-cols-2">
                    <Input
                        label="Amount"
                        type="number"
                        inputMode="decimal"
                        min="0.01"
                        step="0.01"
                        placeholder="0.00"
                        required
                        error={
                            errors.amount?.message
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
                                disabled={
                                    saving
                                }
                            />
                        )}
                    />
                </div>

                <Input
                    label="Expense date"
                    type="date"
                    required
                    error={
                        errors.expenseDate?.message
                    }
                    {...register('expenseDate')}
                />

                <Textarea
                    label="Description"
                    maxLength={255}
                    placeholder="Optional description"
                    error={
                        errors.description?.message
                    }
                    {...register('description')}
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
                            : 'Add expense'}
                    </Button>
                </div>
            </form>
        </Modal>
    )
}