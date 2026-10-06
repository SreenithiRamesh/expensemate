import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    createRecurringExpense,
    deleteRecurringExpense,
    getRecurringExpenseById,
    getRecurringExpenses,
    getUpcomingRecurringExpenses,
    recordRecurringPayment,
    setRecurringExpenseActive,
    updateRecurringExpense,
} from '../../api/recurringExpensesApi'
import { financeQueryKeys } from '../finance/financeQueryKeys'

function refreshRecurringData(
    queryClient,
) {
    return Promise.all([
        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.recurringRoot(),
        }),

        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.expenseLists(),
        }),

        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.dashboardRoot(),
        }),

        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.budgetsRoot(),
        }),
    ])
}

export function useRecurringExpenses() {
    return useQuery({
        queryKey:
            financeQueryKeys.recurringList(),

        queryFn:
        getRecurringExpenses,

        staleTime: 30_000,
    })
}

export function useRecurringExpense(
    recurringExpenseId,
) {
    const hasId =
        recurringExpenseId !== undefined &&
        recurringExpenseId !== null &&
        recurringExpenseId !== ''

    return useQuery({
        queryKey:
            financeQueryKeys.recurringDetail(
                recurringExpenseId,
            ),

        queryFn: () =>
            getRecurringExpenseById(
                recurringExpenseId,
            ),

        enabled: hasId,

        staleTime: 30_000,
    })
}

export function useUpcomingRecurringExpenses(
    days = 30,
) {
    return useQuery({
        queryKey:
            financeQueryKeys.upcomingRecurring(
                days,
            ),

        queryFn: () =>
            getUpcomingRecurringExpenses(
                days,
            ),

        staleTime: 30_000,
    })
}

export function useCreateRecurringExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn:
        createRecurringExpense,

        onSuccess: () =>
            refreshRecurringData(
                queryClient,
            ),
    })
}

export function useUpdateRecurringExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn:
        updateRecurringExpense,

        onSuccess: (
            updatedRecurringExpense,
            variables,
        ) => {
            queryClient.setQueryData(
                financeQueryKeys.recurringDetail(
                    variables.recurringExpenseId,
                ),
                updatedRecurringExpense,
            )

            return refreshRecurringData(
                queryClient,
            )
        },
    })
}

export function useSetRecurringExpenseActive() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn:
        setRecurringExpenseActive,

        onSuccess: (
            updatedRecurringExpense,
        ) => {
            queryClient.setQueryData(
                financeQueryKeys.recurringDetail(
                    updatedRecurringExpense.id,
                ),
                updatedRecurringExpense,
            )

            return refreshRecurringData(
                queryClient,
            )
        },
    })
}

export function useRecordRecurringPayment() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn:
        recordRecurringPayment,

        onSuccess: (
            updatedRecurringExpense,
        ) => {
            queryClient.setQueryData(
                financeQueryKeys.recurringDetail(
                    updatedRecurringExpense.id,
                ),
                updatedRecurringExpense,
            )

            return refreshRecurringData(
                queryClient,
            )
        },
    })
}

export function useDeleteRecurringExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn:
        deleteRecurringExpense,

        onSuccess: (
            _data,
            recurringExpenseId,
        ) => {
            queryClient.removeQueries({
                queryKey:
                    financeQueryKeys.recurringDetail(
                        recurringExpenseId,
                    ),
            })

            return refreshRecurringData(
                queryClient,
            )
        },
    })
}