package com.expensemate.specification;

import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.PersonalExpense;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class PersonalExpenseSpecification {

    private PersonalExpenseSpecification() {
    }

    public static Specification<PersonalExpense> belongsToUser(
            Long userId
    ) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("user").get("id"),
                        userId
                );
    }

    public static Specification<PersonalExpense> hasCategory(
            ExpenseCategory category
    ) {

        return (root, query, criteriaBuilder) -> {

            if (category == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("category"),
                    category
            );
        };
    }

    public static Specification<PersonalExpense> dateFrom(
            LocalDate startDate
    ) {

        return (root, query, criteriaBuilder) -> {

            if (startDate == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("expenseDate"),
                    startDate
            );
        };
    }

    public static Specification<PersonalExpense> dateTo(
            LocalDate endDate
    ) {

        return (root, query, criteriaBuilder) -> {

            if (endDate == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    root.get("expenseDate"),
                    endDate
            );
        };
    }

    public static Specification<PersonalExpense> descriptionContains(
            String search
    ) {

        return (root, query, criteriaBuilder) -> {

            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern =
                    "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.get("description")
                    ),
                    searchPattern
            );
        };
    }
}