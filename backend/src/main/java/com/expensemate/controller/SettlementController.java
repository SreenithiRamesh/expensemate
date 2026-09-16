package com.expensemate.controller;

import com.expensemate.dto.settlement.SettlementCreateRequest;
import com.expensemate.dto.settlement.SettlementResponse;
import com.expensemate.service.SettlementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/groups/{groupId}/settlements"
)
public class SettlementController {

    private final SettlementService
            settlementService;

    public SettlementController(
            SettlementService settlementService
    ) {

        this.settlementService =
                settlementService;
    }

    @PostMapping
    @ResponseStatus(
            HttpStatus.CREATED
    )
    public SettlementResponse createSettlement(
            @PathVariable Long groupId,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,
            @Valid @RequestBody SettlementCreateRequest request,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        return settlementService
                .createSettlement(
                        groupId,
                        email,
                        idempotencyKey,
                        request
                );
    }

    @GetMapping
    public List<SettlementResponse>
    getSettlementHistory(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        return settlementService
                .getSettlementHistory(
                        groupId,
                        email
                );
    }

    @GetMapping("/{settlementId}")
    public SettlementResponse getSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        return settlementService
                .getSettlement(
                        groupId,
                        settlementId,
                        email
                );
    }
}