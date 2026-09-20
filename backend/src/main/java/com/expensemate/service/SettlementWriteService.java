package com.expensemate.service;

import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.enums.SettlementMode;
import com.expensemate.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SettlementWriteService {

    private final SettlementRepository settlementRepository;
    private final ActivityService activityService;

    public SettlementWriteService(
            SettlementRepository settlementRepository,
            ActivityService activityService
    ) {
        this.settlementRepository = settlementRepository;
        this.activityService = activityService;
    }

    /*
     * A separate transaction is essential for concurrent idempotency.
     *
     * If two requests use the same idempotency key:
     *
     * 1. One transaction successfully commits.
     * 2. The other transaction violates the unique constraint.
     * 3. The failed transaction is completely rolled back.
     * 4. SettlementService can safely query and return the winner.
     *
     * The activity entry participates in this transaction, ensuring
     * that either both records are committed or neither is committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Settlement create(
            ExpenseGroup group,
            User currentUser,
            User receiver,
            BigDecimal settlementAmount,
            SettlementMode settlementMode,
            String idempotencyKey
    ) {

        Settlement settlement =
                new Settlement(
                        group,
                        currentUser,
                        receiver,
                        settlementAmount,
                        settlementMode,
                        idempotencyKey,
                        currentUser
                );

        /*
         * saveAndFlush forces the database unique constraint to be
         * evaluated inside this transaction rather than later.
         */
        Settlement savedSettlement =
                settlementRepository.saveAndFlush(
                        settlement
                );

        activityService.record(
                group,
                currentUser,
                ActivityType.SETTLEMENT_CREATED,
                currentUser.getName()
                        + " settled ₹"
                        + savedSettlement
                        .getAmount()
                        .toPlainString()
                        + " with "
                        + receiver.getName(),
                savedSettlement.getId()
        );

        return savedSettlement;
    }
}