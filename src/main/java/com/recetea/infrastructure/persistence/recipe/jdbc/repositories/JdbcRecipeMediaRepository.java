package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.media.IRecipeMediaRepository;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.RecipeMediaMapper;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * JDBC adapter for direct {@code recipe_media} reads / writes —
 * orthogonal to {@link RecipeMediaTableGateway} which only handles the
 * smart-sync path inside an aggregate update.
 *
 * <p>{@link #save(RecipeMedia)} returns the persisted record with the
 * generated id populated so the caller (typically
 * {@code AttachMediaUseCase}) can compensate by deleting the file when
 * the persistence step succeeds but a downstream step fails. This is
 * the contract the port specifies — the surrounding two-phase flow
 * depends on the id being known after the insert.
 *
 * <p><b>ES — </b>Adaptador JDBC para lecturas / escrituras directas
 * sobre {@code recipe_media} — ortogonal a
 * {@link RecipeMediaTableGateway}, que sólo gestiona la ruta de
 * smart-sync dentro de un update de agregado.
 *
 * <p>{@link #save(RecipeMedia)} devuelve el record persistido con el
 * id generado rellenado para que el llamador (típicamente
 * {@code AttachMediaUseCase}) pueda compensar borrando el archivo
 * cuando el paso de persistencia tiene éxito pero un paso posterior
 * falla. Es el contrato que especifica el puerto — el flujo de dos
 * fases que lo rodea depende de conocer el id tras el insert.
 */
public class JdbcRecipeMediaRepository extends BaseJdbcRepository implements IRecipeMediaRepository {

    private final RecipeMediaMapper mapper = new RecipeMediaMapper();

    private static final String INSERT =
            "INSERT INTO recipe_media (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID =
            "SELECT media_id, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
            "FROM recipe_media WHERE media_id = ?";
    private static final String SELECT_BY_RECIPE =
            "SELECT media_id, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
            "FROM recipe_media WHERE recipe_id = ? ORDER BY sort_order ASC";
    private static final String DELETE =
            "DELETE FROM recipe_media WHERE media_id = ?";

    public JdbcRecipeMediaRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public RecipeMedia save(RecipeMedia media) {
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, media.recipeId().value());
                ps.setString(2, media.storageKey());
                ps.setString(3, media.storageProvider());
                ps.setString(4, media.mimeType());
                ps.setLong(5, media.sizeBytes());
                ps.setBoolean(6, media.isMain());
                ps.setInt(7, media.sortOrder());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new RecipeMedia(
                                new RecipeMediaId(keys.getInt(1)),
                                media.recipeId(),
                                media.storageKey(),
                                media.storageProvider(),
                                media.mimeType(),
                                media.sizeBytes(),
                                media.isMain(),
                                media.sortOrder());
                    }
                }
            }
            throw new InfrastructureException("No generated key returned for recipe_media insert.", null);
        }, INSERT);
    }

    @Override
    public Optional<RecipeMedia> findById(RecipeMediaId id) {
        return queryForObject(SELECT_BY_ID, mapper, id.value());
    }

    @Override
    public List<RecipeMedia> findByRecipeId(RecipeId recipeId) {
        return queryForList(SELECT_BY_RECIPE, mapper, recipeId.value());
    }

    @Override
    public void delete(RecipeMediaId id) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setInt(1, id.value());
                ps.executeUpdate();
            }
            return null;
        }, DELETE);
    }

}
