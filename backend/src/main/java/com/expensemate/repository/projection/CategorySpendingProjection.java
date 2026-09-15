package com.expensemate.repository.projection;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;

public interface CategorySpendingProjection {

    ExpenseCategory getCategory();

    BigDecimal getAmount();
}