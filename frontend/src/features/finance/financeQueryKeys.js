function normalizeFilters(filters = {}) {
    return {
        category: filters.category || null,
        startDate: filters.startDate || null,
        endDate: filters.endDate || null,
        search: filters.search?.trim() || null,
        page: Number(filters.page ?? 0),
        size: Number(filters.size ?? 10),
    }
}

export const financeQueryKeys = {
    all: ['finance'],

    dashboardRoot: () => [
        ...financeQueryKeys.all,
        'dashboard',
    ],

    dashboard: (month) => [
        ...financeQueryKeys.dashboardRoot(),
        month || null,
    ],

    expensesRoot: () => [
        ...financeQueryKeys.all,
        'expenses',
    ],

    expenseLists: () => [
        ...financeQueryKeys.expensesRoot(),
        'list',
    ],

    expenseList: (filters) => [
        ...financeQueryKeys.expenseLists(),
        normalizeFilters(filters),
    ],

    expenseDetails: () => [
        ...financeQueryKeys.expensesRoot(),
        'detail',
    ],

    expenseDetail: (expenseId) => [
        ...financeQueryKeys.expenseDetails(),
        Number(expenseId),
    ],

    budgetsRoot: () => [
        ...financeQueryKeys.all,
        'budgets',
    ],

    budgetList: (month, year) => [
        ...financeQueryKeys.budgetsRoot(),
        'list',
        {
            month: month ?? null,
            year: year ?? null,
        },
    ],

    budgetDetail: (budgetId) => [
        ...financeQueryKeys.budgetsRoot(),
        'detail',
        Number(budgetId),
    ],

    recurringRoot: () => [
        ...financeQueryKeys.all,
        'recurring-expenses',
    ],

    recurringList: () => [
        ...financeQueryKeys.recurringRoot(),
        'list',
    ],

    recurringDetail: (recurringExpenseId) => [
        ...financeQueryKeys.recurringRoot(),
        'detail',
        Number(recurringExpenseId),
    ],

    upcomingRecurring: (days) => [
        ...financeQueryKeys.recurringRoot(),
        'upcoming',
        Number(days ?? 30),
    ],
}