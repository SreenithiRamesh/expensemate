package com.expensemate.service.ai;

import com.expensemate.dto.ai.MonthlyInsightData;
import com.expensemate.dto.ai.MonthlyInsightResponse;
import com.expensemate.dto.dashboard.BudgetStatusResponse;
import com.expensemate.dto.dashboard.CategorySpendingResponse;
import com.expensemate.dto.dashboard.DashboardResponse;
import com.expensemate.dto.dashboard.MonthlySpendingResponse;
import com.expensemate.dto.dashboard.MonthlyTrendResponse;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.MonthlyInsightCache;
import com.expensemate.entity.User;
import com.expensemate.exception.AiServiceException;
import com.expensemate.repository.MonthlyInsightCacheRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyInsightServiceImplTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private InsightNarrator insightNarrator;

    @Mock
    private MonthlyInsightCacheRepository cacheRepository;

    @Mock
    private UserRepository userRepository;

    private MonthlyInsightServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {

        service =
                new MonthlyInsightServiceImpl(
                        dashboardService,
                        insightNarrator,
                        cacheRepository,
                        userRepository
                );

        user = mock(User.class);

        lenient()
                .when(user.getId())
                .thenReturn(1L);
    }

    @Test
    void shouldReturnCachedInsightWithoutCallingDashboardOrNarrator() {

        YearMonth month =
                YearMonth.of(2026, 9);

        MonthlyInsightCache cache =
                mock(MonthlyInsightCache.class);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cache.getInsight())
                .thenReturn(
                        "Cached monthly insight"
                );

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.of(cache)
        );

        MonthlyInsightResponse response =
                service.getMonthlyInsight(
                        "user@example.com",
                        month
                );

        assertEquals(
                month,
                response.month()
        );

        assertEquals(
                "Cached monthly insight",
                response.insight()
        );

        assertTrue(
                response.cached()
        );

        verifyNoInteractions(
                dashboardService
        );

        verifyNoInteractions(
                insightNarrator
        );

        verify(
                cacheRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldGenerateNarrationAndCacheItOnCacheMiss() {

        YearMonth month =
                YearMonth.of(2026, 9);

        DashboardResponse dashboard =
                createDashboard();

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                dashboardService.getDashboard(
                        "user@example.com",
                        month
                )
        ).thenReturn(
                dashboard
        );

        when(
                insightNarrator.narrate(any())
        ).thenReturn(
                "  Food was your largest spending category this month.  "
        );

        MonthlyInsightResponse response =
                service.getMonthlyInsight(
                        "user@example.com",
                        month
                );

        assertEquals(
                month,
                response.month()
        );

        assertEquals(
                "Food was your largest spending category this month.",
                response.insight()
        );

        assertFalse(
                response.cached()
        );

        ArgumentCaptor<MonthlyInsightCache> cacheCaptor =
                ArgumentCaptor.forClass(
                        MonthlyInsightCache.class
                );

        verify(cacheRepository)
                .save(
                        cacheCaptor.capture()
                );

        MonthlyInsightCache savedCache =
                cacheCaptor.getValue();

        assertSame(
                user,
                savedCache.getUser()
        );

        assertEquals(
                "2026-09",
                savedCache.getInsightMonth()
        );

        assertEquals(
                "Food was your largest spending category this month.",
                savedCache.getInsight()
        );

        verify(insightNarrator)
                .narrate(
                        any(MonthlyInsightData.class)
                );
    }

    @Test
    void shouldCalculatePreviousMonthAndSpendingChangeInJava() {

        YearMonth month =
                YearMonth.of(2026, 9);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                dashboardService.getDashboard(
                        "user@example.com",
                        month
                )
        ).thenReturn(
                createDashboard()
        );

        when(
                insightNarrator.narrate(any())
        ).thenReturn(
                "Monthly insight"
        );

        service.getMonthlyInsight(
                "user@example.com",
                month
        );

        ArgumentCaptor<MonthlyInsightData> captor =
                ArgumentCaptor.forClass(
                        MonthlyInsightData.class
                );

        verify(insightNarrator)
                .narrate(
                        captor.capture()
                );

        MonthlyInsightData data =
                captor.getValue();

        assertEquals(
                month,
                data.month()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                data.totalSpending()
        );

        assertEquals(
                new BigDecimal("800.00"),
                data.previousMonthSpending()
        );

        assertEquals(
                new BigDecimal("200.00"),
                data.spendingChange()
        );
    }

    @Test
    void shouldPassComputedCategoryAndBudgetFactsToNarrator() {

        YearMonth month =
                YearMonth.of(2026, 9);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                dashboardService.getDashboard(
                        "user@example.com",
                        month
                )
        ).thenReturn(
                createDashboard()
        );

        when(
                insightNarrator.narrate(any())
        ).thenReturn(
                "Monthly insight"
        );

        service.getMonthlyInsight(
                "user@example.com",
                month
        );

        ArgumentCaptor<MonthlyInsightData> captor =
                ArgumentCaptor.forClass(
                        MonthlyInsightData.class
                );

        verify(insightNarrator)
                .narrate(
                        captor.capture()
                );

        MonthlyInsightData data =
                captor.getValue();

        assertEquals(
                2,
                data.categorySpending().size()
        );

        assertEquals(
                ExpenseCategory.FOOD,
                data.categorySpending()
                        .get(0)
                        .category()
        );

        assertEquals(
                new BigDecimal("600.00"),
                data.categorySpending()
                        .get(0)
                        .amount()
        );

        assertEquals(
                new BigDecimal("60.00"),
                data.categorySpending()
                        .get(0)
                        .percentage()
        );

        assertEquals(
                ExpenseCategory.TRAVEL,
                data.categorySpending()
                        .get(1)
                        .category()
        );

        assertEquals(
                new BigDecimal("400.00"),
                data.categorySpending()
                        .get(1)
                        .amount()
        );

        assertEquals(
                new BigDecimal("40.00"),
                data.categorySpending()
                        .get(1)
                        .percentage()
        );

        assertEquals(
                1,
                data.budgets().size()
        );

        assertEquals(
                ExpenseCategory.FOOD,
                data.budgets()
                        .get(0)
                        .category()
        );

        assertEquals(
                new BigDecimal("800.00"),
                data.budgets()
                        .get(0)
                        .monthlyLimit()
        );

        assertEquals(
                new BigDecimal("600.00"),
                data.budgets()
                        .get(0)
                        .spent()
        );

        assertEquals(
                new BigDecimal("200.00"),
                data.budgets()
                        .get(0)
                        .remaining()
        );

        assertEquals(
                new BigDecimal("75.00"),
                data.budgets()
                        .get(0)
                        .percentageUsed()
        );

        assertEquals(
                "SAFE",
                data.budgets()
                        .get(0)
                        .status()
        );
    }

    @Test
    void shouldNotCacheWhenNarratorFails() {

        YearMonth month =
                YearMonth.of(2026, 9);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                dashboardService.getDashboard(
                        "user@example.com",
                        month
                )
        ).thenReturn(
                createDashboard()
        );

        when(
                insightNarrator.narrate(any())
        ).thenThrow(
                new AiServiceException(
                        "AI monthly insight is temporarily unavailable"
                )
        );

        assertThrows(
                AiServiceException.class,
                () ->
                        service.getMonthlyInsight(
                                "user@example.com",
                                month
                        )
        );

        verify(
                cacheRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldNotCacheEmptyNarration() {

        YearMonth month =
                YearMonth.of(2026, 9);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(
                cacheRepository
                        .findByUserIdAndInsightMonth(
                                1L,
                                "2026-09"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                dashboardService.getDashboard(
                        "user@example.com",
                        month
                )
        ).thenReturn(
                createDashboard()
        );

        when(
                insightNarrator.narrate(any())
        ).thenReturn(
                "   "
        );

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () ->
                                service.getMonthlyInsight(
                                        "user@example.com",
                                        month
                                )
                );

        assertEquals(
                "AI monthly insight returned an empty response",
                exception.getMessage()
        );

        verify(
                cacheRepository,
                never()
        ).save(any());
    }

    private DashboardResponse createDashboard() {

        return new DashboardResponse(

                "2026-09",

                new MonthlySpendingResponse(
                        new BigDecimal("1000.00"),
                        4
                ),

                List.of(
                        new CategorySpendingResponse(
                                ExpenseCategory.FOOD,
                                new BigDecimal("600.00"),
                                new BigDecimal("60.00")
                        ),

                        new CategorySpendingResponse(
                                ExpenseCategory.TRAVEL,
                                new BigDecimal("400.00"),
                                new BigDecimal("40.00")
                        )
                ),

                List.of(
                        new MonthlyTrendResponse(
                                "2026-08",
                                new BigDecimal("800.00")
                        ),

                        new MonthlyTrendResponse(
                                "2026-09",
                                new BigDecimal("1000.00")
                        )
                ),

                List.of(
                        new BudgetStatusResponse(
                                ExpenseCategory.FOOD,
                                new BigDecimal("800.00"),
                                new BigDecimal("600.00"),
                                new BigDecimal("200.00"),
                                new BigDecimal("75.00"),
                                "SAFE"
                        )
                ),

                List.of(),

                List.of()
        );
    }
}