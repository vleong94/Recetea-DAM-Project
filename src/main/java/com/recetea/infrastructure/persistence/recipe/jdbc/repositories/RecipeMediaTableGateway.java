package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart-sync gateway for the {@code recipe_media} table. The most
 * elaborate of the three child-table gateways because of the partial
 * unique index on {@code (recipe_id) WHERE is_main = true}.
 *
 * <p><b>Two-pass UPDATE.</b> When the user re-promotes which item is
 * the cover image, both rows would briefly hold {@code is_main = true}
 * if a single UPDATE statement re-applied the new state — the unique
 * index would reject the change. The fix: pass 1 issues
 * {@code SET is_main = false} on every row that's losing main status,
 * pass 2 applies the new state. Since both passes run inside the same
 * transaction, the index never sees a violating intermediate.
 *
 * <p>{@code newItems} (rows with null ids) are batched at the end via
 * INSERT — they correspond to items added in the form before save and
 * have no PK yet.
 *
 * <p>Package-private — {@link JdbcRecipeRepository} is the only caller.
 *
 * <p><b>ES — </b>Gateway de smart-sync para la tabla
 * {@code recipe_media}. El más elaborado de los tres gateways de
 * tabla hija debido al índice único parcial sobre
 * {@code (recipe_id) WHERE is_main = true}.
 *
 * <p><b>UPDATE en dos pasadas.</b> Cuando el usuario re-promueve qué
 * elemento es la imagen de portada, ambas filas tendrían brevemente
 * {@code is_main = true} si una única sentencia UPDATE volviese a
 * aplicar el estado nuevo — el índice único rechazaría el cambio.
 * Solución: la pasada 1 emite {@code SET is_main = false} sobre cada
 * fila que pierde la condición de principal, y la pasada 2 aplica
 * el nuevo estado. Como ambas pasadas corren dentro de la misma
 * transacción, el índice nunca ve un estado intermedio inválido.
 *
 * <p>{@code newItems} (filas con id nulo) se ponen en batch al final
 * vía INSERT — corresponden a elementos añadidos en el formulario
 * antes de guardar y aún no tienen PK.
 *
 * <p>Package-private — {@link JdbcRecipeRepository} es el único
 * llamador.
 */
class RecipeMediaTableGateway extends BaseJdbcRepository {

    private static final String SELECT_MEDIA_FOR_DIFF =
            "SELECT media_id, is_main, sort_order FROM recipe_media WHERE recipe_id = ?";
    private static final String INSERT_MEDIA =
            "INSERT INTO recipe_media (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_MEDIA =
            "UPDATE recipe_media SET is_main = ?, sort_order = ? WHERE media_id = ?";
    private static final String CLEAR_MAIN_MEDIA =
            "UPDATE recipe_media SET is_main = false WHERE media_id = ?";
    private static final String DELETE_MEDIA_ITEM =
            "DELETE FROM recipe_media WHERE media_id = ?";

    RecipeMediaTableGateway(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    void insertMedia(Recipe recipe, RecipeId id) {
        if (recipe.getMediaItems().isEmpty()) return;
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_MEDIA)) {
                for (RecipeMedia m : recipe.getMediaItems()) {
                    ps.setInt(1, id.value());
                    ps.setString(2, m.storageKey());
                    ps.setString(3, m.storageProvider());
                    ps.setString(4, m.mimeType());
                    ps.setLong(5, m.sizeBytes());
                    ps.setBoolean(6, m.isMain());
                    ps.setInt(7, m.sortOrder());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        }, "insert media for recipe id=" + id.value());
    }

    void syncMedia(Recipe recipe) {
        withConnection(conn -> {
            doSync(conn, recipe);
            return null;
        }, "sync media for recipe id=" + recipe.getId().value());
    }

    private void doSync(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, DbMediaRow> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_MEDIA_FOR_DIFF)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int mediaId = rs.getInt("media_id");
                    existing.put(mediaId, new DbMediaRow(mediaId, rs.getBoolean("is_main"), rs.getInt("sort_order")));
                }
            }
        }

        Map<Integer, RecipeMedia> incomingById = new LinkedHashMap<>();
        List<RecipeMedia> newItems = new ArrayList<>();
        for (RecipeMedia m : recipe.getMediaItems()) {
            if (m.id() != null) incomingById.put(m.id().value(), m);
            else newItems.add(m);
        }

        // DELETE items removed from the aggregate
        try (PreparedStatement ps = conn.prepareStatement(DELETE_MEDIA_ITEM)) {
            for (int mediaId : existing.keySet()) {
                if (!incomingById.containsKey(mediaId)) {
                    ps.setInt(1, mediaId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // UPDATE pass 1: clear is_main on items losing main status first to avoid
        // violating the partial unique index (only one is_main=true per recipe)
        try (PreparedStatement ps = conn.prepareStatement(CLEAR_MAIN_MEDIA)) {
            for (Map.Entry<Integer, RecipeMedia> entry : incomingById.entrySet()) {
                DbMediaRow row = existing.get(entry.getKey());
                if (row != null && row.isMain() && !entry.getValue().isMain()) {
                    ps.setInt(1, entry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // UPDATE pass 2: apply full changes (gains is_main=true, sort_order changes)
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_MEDIA)) {
            for (Map.Entry<Integer, RecipeMedia> entry : incomingById.entrySet()) {
                DbMediaRow row = existing.get(entry.getKey());
                if (row != null && row.isDifferentFrom(entry.getValue())) {
                    ps.setBoolean(1, entry.getValue().isMain());
                    ps.setInt(2, entry.getValue().sortOrder());
                    ps.setInt(3, entry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // INSERT new items (null id means not yet persisted)
        if (!newItems.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_MEDIA)) {
                for (RecipeMedia m : newItems) {
                    ps.setInt(1, recipeId);
                    ps.setString(2, m.storageKey());
                    ps.setString(3, m.storageProvider());
                    ps.setString(4, m.mimeType());
                    ps.setLong(5, m.sizeBytes());
                    ps.setBoolean(6, m.isMain());
                    ps.setInt(7, m.sortOrder());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private record DbMediaRow(int id, boolean isMain, int sortOrder) {
        boolean isDifferentFrom(RecipeMedia m) {
            return isMain != m.isMain() || sortOrder != m.sortOrder();
        }
    }
}
