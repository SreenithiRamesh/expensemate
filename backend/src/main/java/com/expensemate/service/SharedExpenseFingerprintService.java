package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
public class SharedExpenseFingerprintService {

    public String fingerprint(
            Long groupId,
            SharedExpenseCreateRequest request
    ) {

        if (groupId == null || request == null) {
            throw new InvalidRequestException(
                    "Shared expense request is required"
            );
        }

        String canonicalRequest =
                buildCanonicalRequest(
                        groupId,
                        request
                );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            canonicalRequest.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            /*
             * SHA-256 is required by the Java platform,
             * so reaching this point indicates an
             * unexpected runtime configuration problem.
             */
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private String buildCanonicalRequest(
            Long groupId,
            SharedExpenseCreateRequest request
    ) {

        String title =
                request.getTitle() == null
                        ? ""
                        : request.getTitle().trim();

        String amount =
                canonicalDecimal(
                        request.getAmount()
                );

        String splitType =
                request.getSplitType() == null
                        ? ""
                        : request.getSplitType().name();

        String expenseDate =
                request.getExpenseDate() == null
                        ? ""
                        : request.getExpenseDate().toString();

        String paidByUserId =
                request.getPaidByUserId() == null
                        ? ""
                        : request.getPaidByUserId().toString();

        String splits =
                canonicalSplits(
                        request.getSplits()
                );

        return String.join(
                "|",
                groupId.toString(),
                title,
                amount,
                paidByUserId,
                splitType,
                expenseDate,
                splits
        );
    }

    private String canonicalSplits(
            List<SplitInputRequest> splits
    ) {

        if (splits == null) {
            return "";
        }

        /*
         * Sort by user ID so the same logical split request
         * produces the same fingerprint even if participants
         * arrive in a different JSON array order.
         */
        return splits.stream()
                .sorted(
                        Comparator.comparing(
                                SplitInputRequest::getUserId,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .map(
                        split ->
                                canonicalSplit(split)
                )
                .reduce(
                        (left, right) ->
                                left + ";" + right
                )
                .orElse("");
    }

    private String canonicalSplit(
            SplitInputRequest split
    ) {

        if (split == null) {
            return "<null>";
        }

        String userId =
                split.getUserId() == null
                        ? ""
                        : split.getUserId().toString();

        return userId
                + ":"
                + canonicalDecimal(
                split.getValue()
        );
    }

    private String canonicalDecimal(
            BigDecimal value
    ) {

        if (value == null) {
            return "";
        }

        /*
         * 100, 100.0 and 100.00 represent the same
         * logical monetary value for fingerprinting.
         */
        BigDecimal normalized =
                value.stripTrailingZeros();

        if (normalized.compareTo(
                BigDecimal.ZERO
        ) == 0) {
            normalized = BigDecimal.ZERO;
        }

        return normalized.toPlainString();
    }
}