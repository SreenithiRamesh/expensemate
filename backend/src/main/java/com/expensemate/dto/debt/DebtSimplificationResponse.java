package com.expensemate.dto.debt;

import java.util.List;

public record DebtSimplificationResponse(
        Long groupId,
        List<DebtSettlementSuggestion> settlements,
        List<DebtGraphNode> nodes,
        List<DebtGraphEdge> edges
) {
}