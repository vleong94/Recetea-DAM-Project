package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.core.user.domain.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

public class Rating {

    private final UserId userId;
    private final Score score;
    private final String comment;
    private final LocalDateTime createdAt;

    public Rating(UserId userId, Score score, String comment, LocalDateTime createdAt) {
        this.userId = Objects.requireNonNull(userId, "userId es obligatorio.");
        this.score = Objects.requireNonNull(score, "score es obligatorio.");
        this.comment = Objects.requireNonNull(comment, "comment es obligatorio.");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio.");
    }

    public UserId getUserId() { return userId; }
    public Score getScore() { return score; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
