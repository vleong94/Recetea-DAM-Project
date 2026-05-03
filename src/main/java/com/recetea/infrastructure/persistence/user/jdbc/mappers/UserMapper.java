package com.recetea.infrastructure.persistence.user.jdbc.mappers;

import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code users} row into a {@link User} aggregate.
 * Reads {@code password_hash} into the {@link PasswordHash} VO whose
 * compact constructor enforces the {@code $2} BCrypt prefix — a
 * malformed hash rejects at the boundary rather than at login time.
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code users} en un
 * agregado {@link User}. Lee {@code password_hash} en el VO
 * {@link PasswordHash}, cuyo constructor compacto aplica el prefijo
 * BCrypt {@code $2} — un hash malformado se rechaza en la frontera
 * en lugar de en el momento de login.
 */
public class UserMapper implements RowMapper<User> {

    @Override
    public User map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                new UserId(rs.getInt("user_id")),
                new Username(rs.getString("username")),
                new Email(rs.getString("email")),
                new PasswordHash(rs.getString("password_hash")));
    }
}
