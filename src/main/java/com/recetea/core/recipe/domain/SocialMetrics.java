package com.recetea.core.recipe.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure value of a recipe's denormalized rating aggregates. Computing the metrics
 * is a stateless pure function over the current ratings list, so it lives outside
 * the {@link Recipe} aggregate — Recipe stays focused on state consistency and
 * invariant enforcement, not arithmetic.
 *
 * The {@code averageScore} is always normalized to scale 2 (HALF_UP) to match
 * the {@code numeric(3,2)} column in PostgreSQL's {@code recipes} table.
 */
public record SocialMetrics(BigDecimal averageScore, int totalRatings) {

    /** Empty-list canonical form: zero ratings, zero average (correctly scaled to 2 decimal places). */
    private static final SocialMetrics EMPTY = new SocialMetrics(BigDecimal.ZERO.setScale(2), 0);

    /** Builds the metrics for a snapshot of ratings. Null/empty input returns the canonical zero value. */
    public static SocialMetrics from(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) return EMPTY;
        int sum = ratings.stream().mapToInt(r -> r.score().value()).sum();
        BigDecimal avg = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        return new SocialMetrics(avg, ratings.size());
    }
}
