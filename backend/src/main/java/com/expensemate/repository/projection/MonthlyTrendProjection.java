package com.expensemate.repository.projection;

import java.math.BigDecimal;

public interface MonthlyTrendProjection {

    Integer getYear();

    Integer getMonth();

    BigDecimal getAmount();
}