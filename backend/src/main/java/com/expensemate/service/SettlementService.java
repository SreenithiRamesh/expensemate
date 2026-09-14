package com.expensemate.service;

import com.expensemate.dto.debt.DebtSettlementSuggestion;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import com.expensemate.dto.settlement.SettlementCreateRequest;
import com.expensemate.dto.settlement.SettlementResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.enums.SettlementMode;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SettlementRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final DebtSimplificationService debtSimplificationService;

    // M13
    private final ActivityService activityService;

    public SettlementService(
            SettlementRepository settlementRepository,
            UserRepository userRepository,
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository,
            DebtSimplificationService debtSimplificationService,
            ActivityService activityService
    ) {
        this.settlementRepository = settlementRepository;
        this.userRepository = userRepository;
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.debtSimplificationService = debtSimplificationService;
        this.activityService = activityService;
    }

    @Transactional
    public SettlementResponse createSettlement(
            Long groupId,
            String email,
            String idempotencyKey,
            SettlementCreateRequest request
    ) {

        validateIdempotencyKey(idempotencyKey);

        User currentUser = getCurrentUser(email);

        /*
         * Idempotency check happens BEFORE creating anything.
         *
         * Therefore retrying the same request:
         * - does not create another settlement
         * - does not create another activity entry
         */
        Settlement existingSettlement =
                settlementRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingSettlement != null) {

            boolean sameUser =
                    existingSettlement
                            .getFromUser()
                            .getId()
                            .equals(currentUser.getId());

            boolean sameGroup =
                    existingSettlement
                            .getGroup()
                            .getId()
                            .equals(groupId);

            if (!sameUser || !sameGroup) {
                throw new InvalidRequestException(
                        "Idempotency key has already been used"
                );
            }

            return toResponse(existingSettlement);
        }

        ExpenseGroup group =
                getAccessibleGroup(
                        groupId,
                        currentUser
                );

        validateRequest(request);

        User receiver =
                userRepository
                        .findById(request.toUserId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        boolean receiverIsMember =
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                receiver.getId()
                        );

        if (!receiverIsMember) {
            throw new ResourceNotFoundException(
                    "Group member not found"
            );
        }

        if (currentUser
                .getId()
                .equals(receiver.getId())) {

            throw new InvalidRequestException(
                    "You cannot settle debt with yourself"
            );
        }

        DebtSettlementSuggestion currentDebt =
                findCurrentDebt(
                        groupId,
                        email,
                        currentUser.getId(),
                        receiver.getId()
                );

        BigDecimal settlementAmount =
                determineSettlementAmount(
                        request,
                        currentDebt.amount()
                );

        Settlement settlement =
                new Settlement(
                        group,
                        currentUser,
                        receiver,
                        settlementAmount,
                        request.mode(),
                        idempotencyKey,
                        currentUser
                );

        Settlement savedSettlement =
                settlementRepository.save(settlement);

        /*
         * M13 — Activity / Audit History
         *
         * This runs inside the same transaction.
         * If activity persistence fails, the settlement
         * transaction also rolls back.
         */
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

        return toResponse(savedSettlement);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlementHistory(
            Long groupId,
            String email
    ) {

        User currentUser =
                getCurrentUser(email);

        getAccessibleGroup(
                groupId,
                currentUser
        );

        return settlementRepository
                .findByGroup_IdOrderBySettledAtDesc(groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(
            Long groupId,
            Long settlementId,
            String email
    ) {

        User currentUser =
                getCurrentUser(email);

        getAccessibleGroup(
                groupId,
                currentUser
        );

        Settlement settlement =
                settlementRepository
                        .findByIdAndGroup_Id(
                                settlementId,
                                groupId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Settlement not found"
                                        )
                        );

        return toResponse(settlement);
    }

    private User getCurrentUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                );
    }

    private ExpenseGroup getAccessibleGroup(
            Long groupId,
            User currentUser
    ) {

        ExpenseGroup group =
                expenseGroupRepository
                        .findById(groupId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Group not found"
                                        )
                        );

        boolean member =
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                currentUser.getId()
                        );

        /*
         * We intentionally return "Group not found"
         * for a non-member instead of revealing that
         * the group exists.
         */
        if (!member) {
            throw new ResourceNotFoundException(
                    "Group not found"
            );
        }

        return group;
    }

    private DebtSettlementSuggestion findCurrentDebt(
            Long groupId,
            String email,
            Long fromUserId,
            Long toUserId
    ) {

        DebtSimplificationResponse debtResponse =
                debtSimplificationService
                        .simplifyGroupDebt(
                                groupId,
                                email
                        );

        return debtResponse
                .settlements()
                .stream()
                .filter(
                        settlement ->
                                settlement
                                        .fromUserId()
                                        .equals(fromUserId)
                                        &&
                                        settlement
                                                .toUserId()
                                                .equals(toUserId)
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new InvalidRequestException(
                                        "No outstanding debt exists for this settlement"
                                )
                );
    }

    private BigDecimal determineSettlementAmount(
            SettlementCreateRequest request,
            BigDecimal outstandingAmount
    ) {

        BigDecimal outstanding =
                normalize(outstandingAmount);

        if (request.mode() == SettlementMode.FULL) {
            return outstanding;
        }

        if (request.mode() == SettlementMode.PARTIAL) {

            if (request.amount() == null) {
                throw new InvalidRequestException(
                        "Amount is required for partial settlement"
                );
            }

            BigDecimal partialAmount =
                    normalize(request.amount());

            if (partialAmount.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                throw new InvalidRequestException(
                        "Settlement amount must be greater than zero"
                );
            }

            if (partialAmount.compareTo(
                    outstanding
            ) > 0) {

                throw new InvalidRequestException(
                        "Settlement amount cannot exceed outstanding debt"
                );
            }

            return partialAmount;
        }

        throw new InvalidRequestException(
                "Invalid settlement mode"
        );
    }

    private void validateRequest(
            SettlementCreateRequest request
    ) {

        if (request == null) {
            throw new InvalidRequestException(
                    "Settlement request is required"
            );
        }

        if (request.toUserId() == null) {
            throw new InvalidRequestException(
                    "Receiver user ID is required"
            );
        }

        if (request.mode() == null) {
            throw new InvalidRequestException(
                    "Settlement mode is required"
            );
        }

        /*
         * FULL settlement amount is calculated by
         * the backend from the current outstanding debt.
         */
        if (request.mode() == SettlementMode.FULL
                && request.amount() != null) {

            throw new InvalidRequestException(
                    "Amount must not be provided for full settlement"
            );
        }
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new InvalidRequestException(
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {
            throw new InvalidRequestException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }

    private BigDecimal normalize(
            BigDecimal amount
    ) {

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private SettlementResponse toResponse(
            Settlement settlement
    ) {

        return new SettlementResponse(
                settlement.getId(),
                settlement.getGroup().getId(),

                settlement.getFromUser().getId(),
                settlement.getFromUser().getName(),

                settlement.getToUser().getId(),
                settlement.getToUser().getName(),

                settlement.getAmount(),
                settlement.getSettlementMode(),
                settlement.getSettledAt()
        );
    }
}