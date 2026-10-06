import { useMutation } from '@tanstack/react-query'

import { suggestExpenseDetails } from '../../api/aiExpenseApi'

export function useAiExpenseSuggestion() {
    return useMutation({
        mutationFn: suggestExpenseDetails,
    })
}