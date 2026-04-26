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
    // Cached display field — populated during DB hydration (see RecipeMapper).
    // Null when the rating is created in-memory via Recipe.addRating().
    private final String username;

    // Used by Recipe.addRating() — no username available at write time.
    public Rating(UserId userId, Score score, String comment, LocalDateTime createdAt) {
        this(userId, score, comment, createdAt, null);
    }

    // Used by RecipeMapper when loading from the database with the joined username.
    public Rating(UserId userId, Score score, String comment, LocalDateTime createdAt, String username) {
        this.userId = Objects.requireNonNull(userId, "userId es obligatorio.");
        this.score = Objects.requireNonNull(score, "score es obligatorio.");
        Objects.requireNonNull(comment, "comment es obligatorio.");
        if (comment.length() > 1000)
            throw new IllegalArgumentException("El comentario no puede superar los 1000 caracteres.");
        this.comment = comment;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio.");
        this.username = username;
    }

    public UserId getUserId() { return userId; }
    public Score getScore() { return score; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUsername() { return username; }
}
