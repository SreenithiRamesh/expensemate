export const EXPENSE_CATEGORIES = Object.freeze([
    'FOOD',
    'TRAVEL',
    'SHOPPING',
    'BILLS',
    'ENTERTAINMENT',
    'HEALTH',
    'EDUCATION',
    'RENT',
    'SUBSCRIPTION',
    'OTHER',
])

export const EXPENSE_CATEGORY_OPTIONS = Object.freeze([
    { value: 'FOOD', label: 'Food & dining' },
    { value: 'TRAVEL', label: 'Travel' },
    { value: 'SHOPPING', label: 'Shopping' },
    { value: 'BILLS', label: 'Bills' },
    { value: 'ENTERTAINMENT', label: 'Entertainment' },
    { value: 'HEALTH', label: 'Health' },
    { value: 'EDUCATION', label: 'Education' },
    { value: 'RENT', label: 'Rent' },
    { value: 'SUBSCRIPTION', label: 'Subscription' },
    { value: 'OTHER', label: 'Other' },
])

export const RECURRING_FREQUENCIES = Object.freeze([
    'WEEKLY',
    'MONTHLY',
    'YEARLY',
])

export const RECURRING_FREQUENCY_OPTIONS = Object.freeze([
    { value: 'WEEKLY', label: 'Weekly' },
    { value: 'MONTHLY', label: 'Monthly' },
    { value: 'YEARLY', label: 'Yearly' },
])

export const DEFAULT_EXPENSE_PAGE_SIZE = 10
export const MAX_EXPENSE_PAGE_SIZE = 100

export function getCategoryLabel(category) {
    return (
        EXPENSE_CATEGORY_OPTIONS.find(
            (option) => option.value === category,
        )?.label ?? 'Other'
    )
}

export function getFrequencyLabel(frequency) {
    return (
        RECURRING_FREQUENCY_OPTIONS.find(
            (option) => option.value === frequency,
        )?.label ?? frequency
    )
}