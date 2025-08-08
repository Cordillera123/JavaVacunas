package com.vacutrack.service;

import com.vacutrack.dao.UsuarioDAO;
import com.vacutrack.dao.PadreFamiliaDAO;
import com.vacutrack.dao.ProfesionalEnfermeriaDAO;
import com.vacutrack.model.Usuario;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.util.PasswordUtil;
import com.vacutrack.util.ValidationUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de autenticación y autorización
 * Maneja el registro, login, logout y verificación de permisos
 * Incluye seguridad de contraseñas y manejo de sesiones
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class AutenticacionService {

    private static final Logger logger = LoggerFactory.getLogger(AutenticacionService.class);
    
    // Instancia singleton
    private static AutenticacionService instance;
    
    // DAOs
    private final UsuarioDAO usuarioDAO;
    private final PadreFamiliaDAO padreFamiliaDAO;
    private final ProfesionalEnfermeriaDAO profesionalDAO;
    
    // Configuración de seguridad
    private static final int MAX_INTENTOS_LOGIN = 5;
    private static final int TIEMPO_BLOQUEO_MINUTOS = 30;
    private static final int SESION_TIMEOUT_MINUTOS = 60;
    
    /**
     * Constructor privado para patrón singleton
     */
    private AutenticacionService() {
        this.usuarioDAO = UsuarioDAO.getInstance();
        this.padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        this.profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
    }
    
    /**
     * Obtiene la instancia singleton del servicio
     * @return instancia de AutenticacionService
     */
    public static synchronized AutenticacionService getInstance() {
        if (instance == null) {
            instance = new AutenticacionService();
        }
        return instance;
    }
    
    /**
     * Autentica un usuario con email y contraseña
     * @param email email del usuario
     * @param password contraseña en texto plano
     * @return resultado de la autenticación
     */
    public AuthResult login(String email, String password) {
        logger.info("Intento de login para email: {}", email);
        
        // Validar parámetros
        if (email == null || email.trim().isEmpty()) {
            return AuthResult.error("El email es requerido");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return AuthResult.error("La contraseña es requerida");
        }
        
        try {
            // Buscar usuario por email
            Optional<Usuario> usuarioOpt = usuarioDAO.findByEmail(email.trim());
            
            if (usuarioOpt.isEmpty()) {
                logger.warn("Intento de login con email no registrado: {}", email);
                return AuthResult.error("Email o contraseña incorrectos");
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Verificar si el usuario está activo
            if (!usuario.getActivo()) {
                logger.warn("Intento de login con usuario inactivo: {}", email);
                return AuthResult.error("Usuario inactivo. Contacte al administrador");
            }
            
            // Verificar si el usuario está bloqueado (simplificado - sin campos adicionales)
            // En una implementación completa se podría usar una tabla separada para bloqueos
            
            // Verificar contraseña
            if (!PasswordUtil.verifyPassword(password, usuario.getPasswordHash())) {
                logger.warn("Contraseña incorrecta para usuario: {}", email);
                return AuthResult.error("Email o contraseña incorrectos");
            }
            
            // Login exitoso - actualizar último acceso
            actualizarUltimoAcceso(usuario);
            
            // Cargar información completa del usuario según su tipo
            AuthResult result = cargarUsuarioCompleto(usuario);
            
            logger.info("Login exitoso para usuario: {} ({})", email, usuario.getTipoUsuarioId());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error durante el login para email: " + email, e);
            return AuthResult.error("Error interno del sistema. Intente más tarde");
        }
    }
    
    /**
     * Registra un nuevo padre de familia
     * @param padreFamilia datos del padre
     * @param password contraseña en texto plano
     * @return resultado del registro
     */
    public AuthResult registrarPadreFamilia(PadreFamilia padreFamilia, String password) {
        logger.info("Registrando nuevo padre de familia: {}", padreFamilia.getEmail());
        
        try {
            // Validar datos del padre
            String validationError = validarDatosPadre(padreFamilia, password);
            if (validationError != null) {
                return AuthResult.error(validationError);
            }
            
            // Verificar que el email no esté registrado
            if (usuarioDAO.findByEmail(padreFamilia.getEmail()).isPresent()) {
                return AuthResult.error("El email ya está registrado en el sistema");
            }
            
            // Verificar que la cédula no esté registrada
            if (padreFamiliaDAO.existsByCedula(padreFamilia.getCedula())) {
                return AuthResult.error("La cédula ya está registrada en el sistema");
            }
            
            // Crear usuario base
            Usuario usuario = new Usuario();
            usuario.setCedula(padreFamilia.getCedula());
            usuario.setEmail(padreFamilia.getEmail());
            usuario.setPasswordHash(PasswordUtil.hashPassword(password));
            usuario.setTipoUsuarioId(2); // ID para PADRE_FAMILIA (según la BD)
            usuario.setActivo(true);
            usuario.setFechaRegistro(LocalDateTime.now());
            usuario.setUltimaSesion(LocalDateTime.now());
            
            // Insertar usuario
            Usuario usuarioCreado = usuarioDAO.insert(usuario);
            if (usuarioCreado == null) {
                return AuthResult.error("Error al crear la cuenta de usuario");
            }
            
            // Asociar ID del usuario al padre
            padreFamilia.setUsuarioId(usuarioCreado.getId());
            padreFamilia.setFechaCreacion(LocalDateTime.now());
            
            // Insertar padre de familia
            PadreFamilia padreCreado = padreFamiliaDAO.insert(padreFamilia);
            if (padreCreado == null) {
                // Rollback: eliminar usuario creado
                usuarioDAO.deleteById(usuarioCreado.getId());
                return AuthResult.error("Error al crear el perfil de padre de familia");
            }
            
            logger.info("Padre de familia registrado exitosamente: {}", padreFamilia.getEmail());
            
            // Retornar resultado exitoso con el usuario completo
            padreCreado.setUsuario(usuarioCreado);
            return AuthResult.success(usuarioCreado, padreCreado, null);
            
        } catch (Exception e) {
            logger.error("Error al registrar padre de familia: " + padreFamilia.getEmail(), e);
            return AuthResult.error("Error interno del sistema. Intente más tarde");
        }
    }
    
    /**
     * Registra un nuevo profesional de enfermería
     * @param profesional datos del profesional
     * @param password contraseña en texto plano
     * @return resultado del registro
     */
    public AuthResult registrarProfesional(ProfesionalEnfermeria profesional, String password) {
        logger.info("Registrando nuevo profesional: {}", profesional.getEmail());
        
        try {
            // Validar datos del profesional
            String validationError = validarDatosProfesional(profesional, password);
            if (validationError != null) {
                return AuthResult.error(validationError);
            }
            
            // Verificar que el email no esté registrado
            if (usuarioDAO.findByEmail(profesional.getEmail()).isPresent()) {
                return AuthResult.error("El email ya está registrado en el sistema");
            }
            
            // Verificar que la cédula no esté registrada
            if (profesionalDAO.existsByCedula(profesional.getCedula())) {
                return AuthResult.error("La cédula ya está registrada en el sistema");
            }
            
            // Verificar que el registro profesional no esté registrado
            if (profesionalDAO.existsByRegistroProfesional(profesional.getNumeroColegio())) {
                return AuthResult.error("El número de registro profesional ya está registrado");
            }
            
            // Crear usuario base
            Usuario usuario = new Usuario();
            usuario.setCedula(profesional.getCedula());
            usuario.setEmail(profesional.getEmail());
            usuario.setPasswordHash(PasswordUtil.hashPassword(password));
            usuario.setTipoUsuarioId(3); // ID para PROFESIONAL_ENFERMERIA
            usuario.setActivo(true);
            usuario.setFechaRegistro(LocalDateTime.now());
            usuario.setUltimaSesion(LocalDateTime.now());
            
            // Insertar usuario
            Usuario usuarioCreado = usuarioDAO.insert(usuario);
            if (usuarioCreado == null) {
                return AuthResult.error("Error al crear la cuenta de usuario");
            }
            
            // Asociar ID del usuario al profesional
            profesional.setUsuarioId(usuarioCreado.getId());
            profesional.setFechaCreacion(LocalDateTime.now());
            
            // Insertar profesional
            ProfesionalEnfermeria profesionalCreado = profesionalDAO.insert(profesional);
            if (profesionalCreado == null) {
                // Rollback: eliminar usuario creado
                usuarioDAO.deleteById(usuarioCreado.getId());
                return AuthResult.error("Error al crear el perfil profesional");
            }
            
            logger.info("Profesional registrado exitosamente: {}", profesional.getEmail());
            
            // Retornar resultado exitoso con el usuario completo
            profesionalCreado.setUsuario(usuarioCreado);
            return AuthResult.success(usuarioCreado, null, profesionalCreado);
            
        } catch (Exception e) {
            logger.error("Error al registrar profesional: " + profesional.getEmail(), e);
            return AuthResult.error("Error interno del sistema. Intente más tarde");
        }
    }
    
    /**
     * Cambia la contraseña de un usuario
     * @param usuarioId ID del usuario
     * @param passwordActual contraseña actual
     * @param passwordNueva nueva contraseña
     * @return true si se cambió exitosamente
     */
    public boolean cambiarPassword(Integer usuarioId, String passwordActual, String passwordNueva) {
        logger.info("Cambiando contraseña para usuario ID: {}", usuarioId);
        
        try {
            // Buscar usuario
            Optional<Usuario> usuarioOpt = usuarioDAO.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                logger.warn("Intento de cambio de contraseña para usuario inexistente: {}", usuarioId);
                return false;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Verificar contraseña actual
            if (!PasswordUtil.verifyPassword(passwordActual, usuario.getPasswordHash())) {
                logger.warn("Contraseña actual incorrecta para usuario: {}", usuarioId);
                return false;
            }
            
            // Validar nueva contraseña
            if (!ValidationUtil.isValidPassword(passwordNueva)) {
                logger.warn("Nueva contraseña no cumple criterios de seguridad para usuario: {}", usuarioId);
                return false;
            }
            
            // Actualizar contraseña
            usuario.setPasswordHash(PasswordUtil.hashPassword(passwordNueva));
            
            if (usuarioDAO.update(usuario)) {
                logger.info("Contraseña cambiada exitosamente para usuario: {}", usuarioId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error al cambiar contraseña para usuario: " + usuarioId, e);
            return false;
        }
    }
    
    /**
     * Verifica si un usuario tiene permisos para una acción específica
     * @param usuario usuario a verificar
     * @param permiso permiso requerido
     * @return true si tiene el permiso
     */
    public boolean verificarPermiso(Usuario usuario, String permiso) {
        if (usuario == null || permiso == null) {
            return false;
        }
        
        // Verificar permisos según tipo de usuario
        // 1 = PADRE_FAMILIA, 2 = PROFESIONAL_ENFERMERIA, 3 = ADMINISTRADOR
        switch (usuario.getTipoUsuarioId()) {
            case 3: // ADMINISTRADOR
                return true; // El administrador tiene todos los permisos
                
            case 2: // PROFESIONAL_ENFERMERIA
                return verificarPermisoProfesional(permiso);
                
            case 1: // PADRE_FAMILIA
                return verificarPermisoPadre(permiso);
                
            default:
                return false;
        }
    }
    
    /**
     * Cierra la sesión de un usuario
     * @param usuarioId ID del usuario
     */
    public void logout(Integer usuarioId) {
        logger.info("Cerrando sesión para usuario ID: {}", usuarioId);
        
        try {
            // Actualizar último acceso
            Optional<Usuario> usuarioOpt = usuarioDAO.findById(usuarioId);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                usuario.setUltimaSesion(LocalDateTime.now());
                usuarioDAO.update(usuario);
            }
            
            logger.info("Sesión cerrada para usuario ID: {}", usuarioId);
            
        } catch (Exception e) {
            logger.error("Error al cerrar sesión para usuario: " + usuarioId, e);
        }
    }
    
    // Métodos privados de utilidad
    
    /**
     * Actualiza el último acceso del usuario
     */
    private void actualizarUltimoAcceso(Usuario usuario) {
        try {
            usuario.setUltimaSesion(LocalDateTime.now());
            usuarioDAO.update(usuario);
        } catch (Exception e) {
            logger.error("Error al actualizar último acceso para usuario: " + usuario.getId(), e);
        }
    }
    
    /**
     * Carga la información completa del usuario según su tipo
     */
    private AuthResult cargarUsuarioCompleto(Usuario usuario) {
        try {
            switch (usuario.getTipoUsuarioId()) {
                case 1: // PADRE_FAMILIA
                    Optional<PadreFamilia> padreOpt = padreFamiliaDAO.findByUsuarioId(usuario.getId());
                    if (padreOpt.isPresent()) {
                        PadreFamilia padre = padreOpt.get();
                        padre.setUsuario(usuario);
                        return AuthResult.success(usuario, padre, null);
                    }
                    break;
                    
                case 3: // PROFESIONAL_ENFERMERIA
                    Optional<ProfesionalEnfermeria> profesionalOpt = profesionalDAO.findByUsuarioId(usuario.getId());
                    if (profesionalOpt.isPresent()) {
                        ProfesionalEnfermeria profesional = profesionalOpt.get();
                        profesional.setUsuario(usuario);
                        return AuthResult.success(usuario, null, profesional);
                    }
                    break;
                    
                case 2: // ADMINISTRADOR
                    return AuthResult.success(usuario, null, null);
            }
            
            // Si no se encontró información específica, retornar solo el usuario base
            return AuthResult.success(usuario, null, null);
            
        } catch (Exception e) {
            logger.error("Error al cargar usuario completo: " + usuario.getId(), e);
            return AuthResult.success(usuario, null, null);
        }
    }
    
    /**
     * Valida los datos de un padre de familia
     */
    private String validarDatosPadre(PadreFamilia padre, String password) {
        if (padre == null) {
            return "Datos del padre son requeridos";
        }
        
        if (!ValidationUtil.isValidEmail(padre.getEmail())) {
            return "El email no tiene un formato válido";
        }
        
        if (!ValidationUtil.isValidPassword(password)) {
            return "La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números";
        }
        
        if (!ValidationUtil.isValidCedula(padre.getCedula())) {
            return "La cédula no tiene un formato válido";
        }
        
        if (padre.getNombres() == null || padre.getNombres().trim().length() < 2) {
            return "Los nombres deben tener al menos 2 caracteres";
        }
        
        if (padre.getApellidos() == null || padre.getApellidos().trim().length() < 2) {
            return "Los apellidos deben tener al menos 2 caracteres";
        }
        
        return null; // Válido
    }
    
    /**
     * Valida los datos de un profesional de enfermería
     */
    private String validarDatosProfesional(ProfesionalEnfermeria profesional, String password) {
        if (profesional == null) {
            return "Datos del profesional son requeridos";
        }
        
        if (!ValidationUtil.isValidEmail(profesional.getEmail())) {
            return "El email no tiene un formato válido";
        }
        
        if (!ValidationUtil.isValidPassword(password)) {
            return "La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números";
        }
        
        if (!ValidationUtil.isValidCedula(profesional.getCedula())) {
            return "La cédula no tiene un formato válido";
        }
        
        if (profesional.getNombres() == null || profesional.getNombres().trim().length() < 2) {
            return "Los nombres deben tener al menos 2 caracteres";
        }
        
        if (profesional.getApellidos() == null || profesional.getApellidos().trim().length() < 2) {
            return "Los apellidos deben tener al menos 2 caracteres";
        }
        
        if (profesional.getRegistroProfesional() == null || profesional.getRegistroProfesional().trim().isEmpty()) {
            return "El número de registro profesional es requerido";
        }
        
        if (profesional.getInstitucionSalud() == null || profesional.getInstitucionSalud().trim().isEmpty()) {
            return "La institución de salud es requerida";
        }
        
        return null; // Válido
    }
    
    /**
     * Verifica permisos específicos para profesionales
     */
    private boolean verificarPermisoProfesional(String permiso) {
        switch (permiso) {
            case "APLICAR_VACUNA":
            case "VER_HISTORIAL_VACUNACION":
            case "GENERAR_CERTIFICADO":
            case "GESTIONAR_PACIENTES":
            case "VER_REPORTES":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Verifica permisos específicos para padres
     */
    private boolean verificarPermisoPadre(String permiso) {
        switch (permiso) {
            case "VER_HISTORIAL_HIJOS":
            case "GESTIONAR_HIJOS":
            case "VER_NOTIFICACIONES":
            case "DESCARGAR_CERTIFICADO":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Clase interna para resultados de autenticación
     */
    public static class AuthResult {
        private final boolean success;
        private final String errorMessage;
        private final Usuario usuario;
        private final PadreFamilia padreFamilia;
        private final ProfesionalEnfermeria profesional;
        
        private AuthResult(boolean success, String errorMessage, Usuario usuario, 
                          PadreFamilia padreFamilia, ProfesionalEnfermeria profesional) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.usuario = usuario;
            this.padreFamilia = padreFamilia;
            this.profesional = profesional;
        }
        
        public static AuthResult success(Usuario usuario, PadreFamilia padre, ProfesionalEnfermeria profesional) {
            return new AuthResult(true, null, usuario, padre, profesional);
        }
        
        public static AuthResult error(String message) {
            return new AuthResult(false, message, null, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Usuario getUsuario() { return usuario; }
        public PadreFamilia getPadreFamilia() { return padreFamilia; }
        public ProfesionalEnfermeria getProfesional() { return profesional; }
        
        public boolean isPadre() { return padreFamilia != null; }
        public boolean isProfesional() { return profesional != null; }
        public boolean isAdministrador() { 
            return usuario != null && Usuario.TIPO_ADMINISTRADOR.equals(usuario.getTipoUsuario()); 
        }
    }
}