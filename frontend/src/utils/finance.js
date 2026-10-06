export function formatCurrency(
    value,
    options = {},
) {
    const numericValue = Number(value ?? 0)

    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
        ...options,
    }).format(
        Number.isFinite(numericValue)
            ? numericValue
            : 0,
    )
}

export function formatFinanceDate(
    value,
    options = {},
) {
    if (!value) {
        return '—'
    }

    const date = new Date(`${value}T00:00:00`)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        ...options,
    }).format(date)
}

export function formatDateTime(
    value,
    options = {},
) {
    if (!value) {
        return '—'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        ...options,
    }).format(date)
}

export function getLocalDateInputValue(
    date = new Date(),
) {
    const year = date.getFullYear()
    const month = String(
        date.getMonth() + 1,
    ).padStart(2, '0')
    const day = String(
        date.getDate(),
    ).padStart(2, '0')

    return `${year}-${month}-${day}`
}

export function getCurrentMonthValue(
    date = new Date(),
) {
    const year = date.getFullYear()
    const month = String(
        date.getMonth() + 1,
    ).padStart(2, '0')

    return `${year}-${month}`
}

export function toFiniteNumber(
    value,
    fallback = 0,
) {
    const number = Number(value)

    return Number.isFinite(number)
        ? number
        : fallback
}