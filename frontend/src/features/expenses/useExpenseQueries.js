import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    createExpense,
    deleteExpense,
    getExpenseById,
    getExpenses,
    updateExpense,
} from '../../api/expensesApi'
import { financeQueryKeys } from '../finance/financeQueryKeys'

function refreshExpenseData(queryClient) {
    return Promise.all([
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

export function useExpenses(
    filters,
) {
    return useQuery({
        queryKey:
            financeQueryKeys.expenseList(filters),

        queryFn: () =>
            getExpenses(filters),

        placeholderData: (previousData) =>
            previousData,

        staleTime: 30_000,
    })
}

export function useExpense(
    expenseId,
) {
    const hasExpenseId =
        expenseId !== undefined &&
        expenseId !== null &&
        expenseId !== ''

    return useQuery({
        queryKey:
            financeQueryKeys.expenseDetail(
                expenseId,
            ),

        queryFn: () =>
            getExpenseById(expenseId),

        enabled: hasExpenseId,

        staleTime: 30_000,
    })
}

export function useCreateExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: createExpense,

        onSuccess: () =>
            refreshExpenseData(queryClient),
    })
}

export function useUpdateExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: updateExpense,

        onSuccess: (
            updatedExpense,
            variables,
        ) => {
            queryClient.setQueryData(
                financeQueryKeys.expenseDetail(
                    variables.expenseId,
                ),
                updatedExpense,
            )

            return refreshExpenseData(
                queryClient,
            )
        },
    })
}

export function useDeleteExpense() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: deleteExpense,

        onSuccess: (
            _data,
            expenseId,
        ) => {
            queryClient.removeQueries({
                queryKey:
                    financeQueryKeys.expenseDetail(
                        expenseId,
                    ),
            })

            return refreshExpenseData(
                queryClient,
            )
        },
    })
}