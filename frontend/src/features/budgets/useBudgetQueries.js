import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    createBudget,
    deleteBudget,
    getBudgetById,
    getBudgets,
    updateBudget,
} from '../../api/budgetsApi'
import { financeQueryKeys } from '../finance/financeQueryKeys'

function refreshBudgetData(queryClient) {
    return Promise.all([
        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.budgetsRoot(),
        }),

        queryClient.invalidateQueries({
            queryKey:
                financeQueryKeys.dashboardRoot(),
        }),
    ])
}

export function useBudgets({
                               month,
                               year,
                           } = {}) {
    return useQuery({
        queryKey:
            financeQueryKeys.budgetList(
                month,
                year,
            ),

        queryFn: () =>
            getBudgets({
                month,
                year,
            }),

        staleTime: 30_000,
    })
}

export function useBudget(
    budgetId,
) {
    const hasBudgetId =
        budgetId !== undefined &&
        budgetId !== null &&
        budgetId !== ''

    return useQuery({
        queryKey:
            financeQueryKeys.budgetDetail(
                budgetId,
            ),

        queryFn: () =>
            getBudgetById(budgetId),

        enabled: hasBudgetId,

        staleTime: 30_000,
    })
}

export function useCreateBudget() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: createBudget,

        onSuccess: () =>
            refreshBudgetData(queryClient),
    })
}

export function useUpdateBudget() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: updateBudget,

        onSuccess: (
            updatedBudget,
            variables,
        ) => {
            queryClient.setQueryData(
                financeQueryKeys.budgetDetail(
                    variables.budgetId,
                ),
                updatedBudget,
            )

            return refreshBudgetData(
                queryClient,
            )
        },
    })
}

export function useDeleteBudget() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: deleteBudget,

        onSuccess: (
            _data,
            budgetId,
        ) => {
            queryClient.removeQueries({
                queryKey:
                    financeQueryKeys.budgetDetail(
                        budgetId,
                    ),
            })

            return refreshBudgetData(
                queryClient,
            )
        },
    })
}