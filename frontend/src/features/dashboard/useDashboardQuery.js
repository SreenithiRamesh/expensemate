import { useQuery } from '@tanstack/react-query'

import { getDashboard } from '../../api/dashboardApi'
import { financeQueryKeys } from '../finance/financeQueryKeys'

export function useDashboardQuery(
    month,
) {
    return useQuery({
        queryKey:
            financeQueryKeys.dashboard(
                month,
            ),

        queryFn: () =>
            getDashboard(month),

        staleTime: 30_000,
    })
}