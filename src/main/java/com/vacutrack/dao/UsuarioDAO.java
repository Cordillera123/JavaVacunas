package com.vacutrack.dao;

import com.vacutrack.model.Usuario;
import com.vacutrack.model.TipoUsuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad Usuario
 * Maneja operaciones de base de datos relacionadas con usuarios del sistema
 * Incluye métodos específicos para autenticación y gestión de sesiones
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class UsuarioDAO extends BaseDAO<Usuario, Integer> {

    // Instancia singleton
    private static UsuarioDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO usuarios (cedula, email, password_hash, tipo_usuario_id, activo, fecha_registro) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE usuarios SET cedula = ?, email = ?, password_hash = ?, tipo_usuario_id = ?, " +
                    "activo = ?, ultima_sesion = ? WHERE id = ?";

    private static final String FIND_BY_CEDULA_SQL =
            "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                    "FROM usuarios u " +
                    "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                    "WHERE u.cedula = ? AND u.activo = true";

    private static final String FIND_BY_EMAIL_SQL =
            "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                    "FROM usuarios u " +
                    "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                    "WHERE u.email = ? AND u.activo = true";

    private static final String FIND_BY_CEDULA_AND_PASSWORD_SQL =
            "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                    "FROM usuarios u " +
                    "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                    "WHERE u.cedula = ? AND u.password_hash = ? AND u.activo = true";

    private static final String UPDATE_LAST_SESSION_SQL =
            "UPDATE usuarios SET ultima_sesion = ? WHERE id = ?";

    private static final String FIND_BY_TYPE_SQL =
            "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                    "FROM usuarios u " +
                    "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                    "WHERE u.tipo_usuario_id = ? AND u.activo = true " +
                    "ORDER BY u.fecha_registro DESC";

    private static final String FIND_ACTIVE_USERS_SQL =
            "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                    "FROM usuarios u " +
                    "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                    "WHERE u.activo = true " +
                    "ORDER BY u.fecha_registro DESC";

    private static final String COUNT_BY_TYPE_SQL =
            "SELECT COUNT(*) FROM usuarios WHERE tipo_usuario_id = ? AND activo = true";

    private static final String DEACTIVATE_USER_SQL =
            "UPDATE usuarios SET activo = false WHERE id = ?";

    private static final String ACTIVATE_USER_SQL =
            "UPDATE usuarios SET activo = true WHERE id = ?";

    /**
     * Constructor privado para patrón singleton
     */
    private UsuarioDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de UsuarioDAO
     */
    public static synchronized UsuarioDAO getInstance() {
        if (instance == null) {
            instance = new UsuarioDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "usuarios";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected Usuario mapResultSetToEntity(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();

        // Datos básicos del usuario
        usuario.setId(getInteger(rs, "id"));
        usuario.setCedula(getString(rs, "cedula"));
        usuario.setEmail(getString(rs, "email"));
        usuario.setPasswordHash(getString(rs, "password_hash"));
        usuario.setTipoUsuarioId(getInteger(rs, "tipo_usuario_id"));
        usuario.setActivo(getBoolean(rs, "activo"));
        usuario.setFechaRegistro(toLocalDateTime(rs.getTimestamp("fecha_registro")));
        usuario.setUltimaSesion(toLocalDateTime(rs.getTimestamp("ultima_sesion")));

        // Mapear TipoUsuario si está disponible en el ResultSet
        try {
            String tipoNombre = getString(rs, "tipo_nombre");
            String tipoDescripcion = getString(rs, "tipo_descripcion");

            if (tipoNombre != null) {
                TipoUsuario tipoUsuario = new TipoUsuario();
                tipoUsuario.setId(usuario.getTipoUsuarioId());
                tipoUsuario.setNombre(tipoNombre);
                tipoUsuario.setDescripcion(tipoDescripcion);
                tipoUsuario.setActivo(true);
                usuario.setTipoUsuario(tipoUsuario);
            }
        } catch (SQLException e) {
            // Las columnas de tipo_usuario no están disponibles en esta consulta, ignorar
            logger.debug("Información de TipoUsuario no disponible en el ResultSet");
        }

        return usuario;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Usuario usuario) throws SQLException {
        ps.setString(1, usuario.getCedula());
        ps.setString(2, usuario.getEmail());
        ps.setString(3, usuario.getPasswordHash());
        ps.setInt(4, usuario.getTipoUsuarioId());
        ps.setBoolean(5, usuario.getActivo() != null ? usuario.getActivo() : true);
        ps.setTimestamp(6, java.sql.Timestamp.valueOf(
                usuario.getFechaRegistro() != null ? usuario.getFechaRegistro() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Usuario usuario) throws SQLException {
        ps.setString(1, usuario.getCedula());
        ps.setString(2, usuario.getEmail());
        ps.setString(3, usuario.getPasswordHash());
        ps.setInt(4, usuario.getTipoUsuarioId());
        ps.setBoolean(5, usuario.getActivo() != null ? usuario.getActivo() : true);

        if (usuario.getUltimaSesion() != null) {
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(usuario.getUltimaSesion()));
        } else {
            ps.setNull(6, java.sql.Types.TIMESTAMP);
        }

        ps.setInt(7, usuario.getId());
    }

    @Override
    protected String buildInsertSql() {
        return INSERT_SQL;
    }

    @Override
    protected String buildUpdateSql() {
        return UPDATE_SQL;
    }

    @Override
    protected void assignGeneratedId(Usuario usuario, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            usuario.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para autenticación

    /**
     * Busca un usuario por cédula
     * @param cedula cédula del usuario
     * @return Optional con el usuario encontrado
     */
    public Optional<Usuario> findByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CEDULA_SQL, cedula);
        Usuario usuario = executeQuerySingle(FIND_BY_CEDULA_SQL, cedula.trim());

        if (usuario != null) {
            logger.debug("Usuario encontrado por cédula: {}", cedula);
        } else {
            logger.debug("No se encontró usuario con cédula: {}", cedula);
        }

        return Optional.ofNullable(usuario);
    }

    /**
     * Busca un usuario por email
     * @param email email del usuario
     * @return Optional con el usuario encontrado
     */
    public Optional<Usuario> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_EMAIL_SQL, email);
        Usuario usuario = executeQuerySingle(FIND_BY_EMAIL_SQL, email.trim().toLowerCase());

        if (usuario != null) {
            logger.debug("Usuario encontrado por email: {}", email);
        } else {
            logger.debug("No se encontró usuario con email: {}", email);
        }

        return Optional.ofNullable(usuario);
    }

    /**
     * Autentica un usuario por cédula y contraseña
     * @param cedula cédula del usuario
     * @param passwordHash hash de la contraseña
     * @return Optional con el usuario autenticado
     */
    public Optional<Usuario> authenticate(String cedula, String passwordHash) {
        if (cedula == null || passwordHash == null ||
                cedula.trim().isEmpty() || passwordHash.trim().isEmpty()) {
            logger.warn("Intento de autenticación con credenciales vacías");
            return Optional.empty();
        }

        logQuery(FIND_BY_CEDULA_AND_PASSWORD_SQL, cedula, "***");
        Usuario usuario = executeQuerySingle(FIND_BY_CEDULA_AND_PASSWORD_SQL, cedula.trim(), passwordHash);

        if (usuario != null) {
            logger.info("Autenticación exitosa para usuario: {}", cedula);
            // Actualizar última sesión
            updateLastSession(usuario.getId());
            usuario.setUltimaSesion(LocalDateTime.now());
        } else {
            logger.warn("Autenticación fallida para usuario: {}", cedula);
        }

        return Optional.ofNullable(usuario);
    }

    /**
     * Actualiza la última sesión de un usuario
     * @param userId ID del usuario
     * @return true si se actualizó correctamente
     */
    public boolean updateLastSession(Integer userId) {
        if (userId == null) {
            return false;
        }

        try {
            int affectedRows = executeUpdate(UPDATE_LAST_SESSION_SQL, LocalDateTime.now(), userId);
            boolean updated = affectedRows > 0;

            if (updated) {
                logger.debug("Última sesión actualizada para usuario ID: {}", userId);
            } else {
                logger.warn("No se pudo actualizar la última sesión para usuario ID: {}", userId);
            }

            return updated;

        } catch (Exception e) {
            logger.error("Error al actualizar última sesión para usuario ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Busca usuarios por tipo
     * @param tipoUsuarioId ID del tipo de usuario
     * @return lista de usuarios del tipo especificado
     */
    public List<Usuario> findByTipo(Integer tipoUsuarioId) {
        if (tipoUsuarioId == null) {
            return List.of();
        }

        logQuery(FIND_BY_TYPE_SQL, tipoUsuarioId);
        List<Usuario> usuarios = executeQuery(FIND_BY_TYPE_SQL, tipoUsuarioId);

        logger.debug("Encontrados {} usuarios del tipo ID: {}", usuarios.size(), tipoUsuarioId);
        return usuarios;
    }

    /**
     * Obtiene todos los usuarios activos
     * @return lista de usuarios activos
     */
    public List<Usuario> findActiveUsers() {
        logQuery(FIND_ACTIVE_USERS_SQL);
        List<Usuario> usuarios = executeQuery(FIND_ACTIVE_USERS_SQL);

        logger.debug("Encontrados {} usuarios activos", usuarios.size());
        return usuarios;
    }

    /**
     * Cuenta usuarios por tipo
     * @param tipoUsuarioId ID del tipo de usuario
     * @return número de usuarios del tipo especificado
     */
    public long countByTipo(Integer tipoUsuarioId) {
        if (tipoUsuarioId == null) {
            return 0;
        }

        logQuery(COUNT_BY_TYPE_SQL, tipoUsuarioId);
        Object result = executeScalar(COUNT_BY_TYPE_SQL, tipoUsuarioId);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de usuarios tipo ID {}: {}", tipoUsuarioId, count);

        return count;
    }

    /**
     * Desactiva un usuario (soft delete)
     * @param userId ID del usuario a desactivar
     * @return true si se desactivó correctamente
     */
    public boolean deactivateUser(Integer userId) {
        if (userId == null) {
            return false;
        }

        try {
            int affectedRows = executeUpdate(DEACTIVATE_USER_SQL, userId);
            boolean deactivated = affectedRows > 0;

            if (deactivated) {
                logger.info("Usuario desactivado ID: {}", userId);
            } else {
                logger.warn("No se pudo desactivar el usuario ID: {}", userId);
            }

            return deactivated;

        } catch (Exception e) {
            logger.error("Error al desactivar usuario ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Activa un usuario
     * @param userId ID del usuario a activar
     * @return true si se activó correctamente
     */
    public boolean activateUser(Integer userId) {
        if (userId == null) {
            return false;
        }

        try {
            int affectedRows = executeUpdate(ACTIVATE_USER_SQL, userId);
            boolean activated = affectedRows > 0;

            if (activated) {
                logger.info("Usuario activado ID: {}", userId);
            } else {
                logger.warn("No se pudo activar el usuario ID: {}", userId);
            }

            return activated;

        } catch (Exception e) {
            logger.error("Error al activar usuario ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Verifica si una cédula ya está registrada
     * @param cedula cédula a verificar
     * @return true si ya existe
     */
    public boolean existsByCedula(String cedula) {
        return findByCedula(cedula).isPresent();
    }

    /**
     * Verifica si un email ya está registrado
     * @param email email a verificar
     * @return true si ya existe
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * Verifica si una cédula ya está registrada excluyendo un usuario específico
     * @param cedula cédula a verificar
     * @param excludeUserId ID del usuario a excluir de la verificación
     * @return true si ya existe en otro usuario
     */
    public boolean existsByCedulaExcluding(String cedula, Integer excludeUserId) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM usuarios WHERE cedula = ? AND id != ? AND activo = true";
        logQuery(sql, cedula, excludeUserId);

        Object result = executeScalar(sql, cedula.trim(), excludeUserId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Verifica si un email ya está registrado excluyendo un usuario específico
     * @param email email a verificar
     * @param excludeUserId ID del usuario a excluir de la verificación
     * @return true si ya existe en otro usuario
     */
    public boolean existsByEmailExcluding(String email, Integer excludeUserId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM usuarios WHERE email = ? AND id != ? AND activo = true";
        logQuery(sql, email, excludeUserId);

        Object result = executeScalar(sql, email.trim().toLowerCase(), excludeUserId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Actualiza solo la contraseña de un usuario
     * @param userId ID del usuario
     * @param newPasswordHash nuevo hash de contraseña
     * @return true si se actualizó correctamente
     */
    public boolean updatePassword(Integer userId, String newPasswordHash) {
        if (userId == null || newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE usuarios SET password_hash = ? WHERE id = ? AND activo = true";

        try {
            int affectedRows = executeUpdate(sql, newPasswordHash, userId);
            boolean updated = affectedRows > 0;

            if (updated) {
                logger.info("Contraseña actualizada para usuario ID: {}", userId);
            } else {
                logger.warn("No se pudo actualizar la contraseña para usuario ID: {}", userId);
            }

            return updated;

        } catch (Exception e) {
            logger.error("Error al actualizar contraseña para usuario ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Busca usuarios que no han iniciado sesión en los últimos días especificados
     * @param days número de días
     * @return lista de usuarios inactivos
     */
    public List<Usuario> findInactiveUsers(int days) {
        String sql = "SELECT u.*, tu.nombre as tipo_nombre, tu.descripcion as tipo_descripcion " +
                "FROM usuarios u " +
                "LEFT JOIN tipos_usuario tu ON u.tipo_usuario_id = tu.id " +
                "WHERE u.activo = true AND " +
                "(u.ultima_sesion IS NULL OR u.ultima_sesion < DATE_SUB(NOW(), INTERVAL ? DAY)) " +
                "ORDER BY u.ultima_sesion ASC";

        logQuery(sql, days);
        List<Usuario> usuarios = executeQuery(sql, days);

        logger.debug("Encontrados {} usuarios inactivos (más de {} días)", usuarios.size(), days);
        return usuarios;
    }
}