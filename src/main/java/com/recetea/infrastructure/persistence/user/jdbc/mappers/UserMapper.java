package com.recetea.infrastructure.persistence.user.jdbc.mappers;

import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {

    @Override
    public User map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static User mapRow(ResultSet rs) throws SQLException {
        User user = new User(
                new Username(rs.getString("username")),
                new Email(rs.getString("email")),
                new PasswordHash(rs.getString("password_hash")));
        user.setId(new UserId(rs.getInt("id_user")));
        return user;
    }
}
