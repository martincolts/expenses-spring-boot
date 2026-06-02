package com.tincho.expenses.services;

import com.tincho.expenses.controllers.dto.CategoryExpenseTotal;
import com.tincho.expenses.controllers.dto.DashboardResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardResponse getForTenant(Long tenantId, int year, int month) {
        var dateFrom = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var dateTo = dateFrom.plusMonths(1).minusNanos(1);

        var params = Map.of("tenantId", tenantId, "dateFrom", dateFrom, "dateTo", dateTo);

        List<CategoryExpenseTotal> byCategory = jdbc.query(
                """
                SELECT c.id AS category_id,
                       COALESCE(c.name, 'Uncategorized') AS category_name,
                       SUM(e.amount) AS total,
                       c.color_id
                FROM expense e
                LEFT JOIN category c ON e.category_id = c.id
                WHERE e.tenant_id = :tenantId
                  AND e.incurred_at >= :dateFrom
                  AND e.incurred_at <= :dateTo
                GROUP BY c.id, c.name
                ORDER BY total DESC
                """,
                params,
                (rs, i) -> new CategoryExpenseTotal(
                        rs.getLong("category_id"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("total"),
                        rs.getString("color_id")));

        BigDecimal totalExpenses = byCategory.stream()
                .map(CategoryExpenseTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(amount), 0)
                FROM income
                WHERE tenant_id = :tenantId
                  AND created_at >= :dateFrom
                  AND created_at <= :dateTo
                """,
                params,
                BigDecimal.class);

        BigDecimal balance = (totalIncome != null ? totalIncome : BigDecimal.ZERO).subtract(totalExpenses);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate today = LocalDate.now();
        int daysUntilEnd = 0;
        if (today.getYear() == year && today.getMonthValue() == month) {
            daysUntilEnd = ym.lengthOfMonth() - today.getDayOfMonth() + 1;
        }

        BigDecimal dailyRemainingBudget = daysUntilEnd > 0 && balance.compareTo(BigDecimal.ZERO) > 0
                ? balance.divide(BigDecimal.valueOf(daysUntilEnd), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new DashboardResponse(
                totalExpenses,
                totalIncome != null ? totalIncome : BigDecimal.ZERO,
                balance,
                daysUntilEnd,
                dailyRemainingBudget,
                byCategory);
    }
}
