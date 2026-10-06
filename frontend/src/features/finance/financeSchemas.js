import { z } from 'zod'

import {
    EXPENSE_CATEGORIES,
    RECURRING_FREQUENCIES,
} from '../../constants/finance'

const decimalPattern =
    /^\d{1,10}(\.\d{1,2})?$/

function isValidDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return false
    }

    const date = new Date(`${value}T00:00:00`)

    return !Number.isNaN(date.getTime())
}

export const moneySchema = z
    .string()
    .trim()
    .min(1, 'Please enter an amount.')
    .regex(
        decimalPattern,
        'Enter a valid amount with up to 2 decimal places.',
    )
    .refine(
        (value) => Number(value) > 0,
        'Amount must be greater than zero.',
    )

export const categorySchema = z.enum(
    EXPENSE_CATEGORIES,
    {
        message: 'Please select a category.',
    },
)

export const financeDateSchema = z
    .string()
    .trim()
    .min(1, 'Please select a date.')
    .refine(
        isValidDate,
        'Please select a valid date.',
    )

export const expenseSchema = z.object({
    amount: moneySchema,

    category: categorySchema,

    expenseDate: financeDateSchema,

    description: z
        .string()
        .trim()
        .max(
            255,
            'Description must not exceed 255 characters.',
        ),
})

export const budgetSchema = z.object({
    category: categorySchema,

    monthlyLimit: moneySchema,

    month: z.coerce
        .number()
        .int()
        .min(1, 'Month must be between 1 and 12.')
        .max(12, 'Month must be between 1 and 12.'),

    year: z.coerce
        .number()
        .int()
        .min(2000, 'Year must be 2000 or later.'),
})

export const recurringExpenseSchema = z.object({
    title: z
        .string()
        .trim()
        .min(1, 'Please enter a title.')
        .max(
            120,
            'Title must not exceed 120 characters.',
        ),

    amount: moneySchema,

    category: categorySchema,

    frequency: z.enum(
        RECURRING_FREQUENCIES,
        {
            message: 'Please select a frequency.',
        },
    ),

    nextDueDate: financeDateSchema,

    active: z.boolean(),
})

export const recurringPaymentSchema = z.object({
    paymentDate: financeDateSchema,
})

export const aiExpenseTextSchema = z.object({
    text: z
        .string()
        .trim()
        .min(
            1,
            'Describe the expense before requesting a suggestion.',
        )
        .max(
            500,
            'Expense text must not exceed 500 characters.',
        ),
})