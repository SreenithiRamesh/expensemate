package com.expensemate.service.ai;

import com.expensemate.repository.MonthlyInsightCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.YearMonth;
import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyInsightCacheServiceImplTest {

    @Mock
    private MonthlyInsightCacheRepository cacheRepository;

    private MonthlyInsightCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new MonthlyInsightCacheServiceImpl(
                        cacheRepository
                );
    }

    @Test
    void shouldInvalidateExpenseMonthAndFollowingMonth() {

        service.invalidateForExpenseChange(
                1L,
                LocalDate.of(2026, 8, 20)
        );

        verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-08"
                );

        verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldInvalidateThreeUniqueMonthsWhenExpenseMovesToNextMonth() {

        service.invalidateForExpenseChange(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 9, 5)
        );

        InOrder inOrder =
                inOrder(cacheRepository);

        inOrder.verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-08"
                );

        inOrder.verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        inOrder.verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-10"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldNotDeleteDuplicateMonthsWhenExpenseRemainsInSameMonth() {

        service.invalidateForExpenseChange(
                1L,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 25)
        );

        verify(cacheRepository, times(1))
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-08"
                );

        verify(cacheRepository, times(1))
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldHandleYearBoundaryCorrectly() {

        service.invalidateForExpenseChange(
                1L,
                LocalDate.of(2026, 12, 31)
        );

        verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-12"
                );

        verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2027-01"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldDoNothingWhenUserIdIsNull() {

        service.invalidateForExpenseChange(
                null,
                LocalDate.of(2026, 9, 15)
        );

        verifyNoInteractions(cacheRepository);
    }

    @Test
    void shouldDoNothingWhenExpenseDateIsNull() {

        service.invalidateForExpenseChange(
                1L,
                (LocalDate) null
        );

        verifyNoInteractions(cacheRepository);
    }
    @Test
    void shouldInvalidateOnlyBudgetMonth() {

        service.invalidateForBudgetChange(
                1L,
                YearMonth.of(2026, 9)
        );

        verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldInvalidateOldAndNewMonthsWhenBudgetMoves() {

        service.invalidateForBudgetChange(
                1L,
                YearMonth.of(2026, 8),
                YearMonth.of(2026, 9)
        );

        InOrder inOrder =
                inOrder(cacheRepository);

        inOrder.verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-08"
                );

        inOrder.verify(cacheRepository)
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldNotDeleteBudgetMonthTwiceWhenPeriodDoesNotChange() {

        service.invalidateForBudgetChange(
                1L,
                YearMonth.of(2026, 9),
                YearMonth.of(2026, 9)
        );

        verify(cacheRepository, times(1))
                .deleteByUserIdAndInsightMonth(
                        1L,
                        "2026-09"
                );

        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void shouldDoNothingWhenBudgetMonthIsNull() {

        service.invalidateForBudgetChange(
                1L,
                (YearMonth) null
        );

        verifyNoInteractions(cacheRepository);
    }
}