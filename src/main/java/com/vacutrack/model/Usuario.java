package com.vacutrack.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clase modelo para Usuario
 * Representa la tabla base de usuarios del sistema VACU-TRACK
 * Maneja autenticación y relación con tipos de usuario
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class Usuario {

    // Atributos de la clase
    private Integer id;
    private String cedula;
    private String email;
    private String passwordHash;
    private Integer tipoUsuarioId;
    private TipoUsuario tipoUsuario; // Relación con TipoUsuario
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimaSesion;

    // Patrones para validación
    private static final Pattern PATRON_EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PATRON_CEDULA_ECUATORIANA = Pattern.compile(
            "^[0-9]{10}$");

    // Constructor vacío (requerido para JavaBeans)
    public Usuario() {
        this.activo = true;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public Usuario(String cedula, String email, String passwordHash, Integer tipoUsuarioId) {
        this();
        this.cedula = cedula;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tipoUsuarioId = tipoUsuarioId;
    }

    // Constructor con TipoUsuario
    public Usuario(String cedula, String email, String passwordHash, TipoUsuario tipoUsuario) {
        this();
        this.cedula = cedula;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tipoUsuario = tipoUsuario;
        this.tipoUsuarioId = tipoUsuario != null ? tipoUsuario.getId() : null;
    }

    // Constructor completo
    public Usuario(Integer id, String cedula, String email, String passwordHash,
                   Integer tipoUsuarioId, Boolean activo, LocalDateTime fechaRegistro,
                   LocalDateTime ultimaSesion) {
        this.id = id;
        this.cedula = cedula;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tipoUsuarioId = tipoUsuarioId;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
        this.ultimaSesion = ultimaSesion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        // Limpiar y validar cédula
        if (cedula != null) {
            this.cedula = cedula.trim().replaceAll("[^0-9]", "");
            if (this.cedula.length() != 10) {
                this.cedula = null; // Si no es válida, la dejamos nula
            }
        } else {
            this.cedula = null;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getTipoUsuarioId() {
        return tipoUsuarioId;
    }

    public void setTipoUsuarioId(Integer tipoUsuarioId) {
        this.tipoUsuarioId = tipoUsuarioId;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
        this.tipoUsuarioId = tipoUsuario != null ? tipoUsuario.getId() : null;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getUltimaSesion() {
        return ultimaSesion;
    }

    public void setUltimaSesion(LocalDateTime ultimaSesion) {
        this.ultimaSesion = ultimaSesion;
    }

    // Métodos utilitarios

    /**
     * Verifica si el usuario está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo;
    }

    /**
     * Verifica si el usuario es padre de familia
     * @return true si es padre de familia
     */
    public boolean esPadreFamilia() {
        return this.tipoUsuario != null && this.tipoUsuario.esPadreFamilia();
    }

    /**
     * Verifica si el usuario es profesional de enfermería
     * @return true si es profesional de enfermería
     */
    public boolean esProfesionalEnfermeria() {
        return this.tipoUsuario != null && this.tipoUsuario.esProfesionalEnfermeria();
    }

    /**
     * Verifica si el usuario es administrador
     * @return true si es administrador
     */
    public boolean esAdministrador() {
        return this.tipoUsuario != null && this.tipoUsuario.esAdministrador();
    }

    /**
     * Actualiza la última sesión a la fecha/hora actual
     */
    public void actualizarUltimaSesion() {
        this.ultimaSesion = LocalDateTime.now();
    }

    /**
     * Obtiene el nombre del tipo de usuario
     * @return String con el tipo de usuario o "No definido"
     */
    public String obtenerNombreTipoUsuario() {
        if (this.tipoUsuario != null) {
            return this.tipoUsuario.toDisplayString();
        }
        return "No definido";
    }

    /**
     * Verifica si tiene una sesión reciente (últimas 24 horas)
     * @return true si la última sesión fue en las últimas 24 horas
     */
    public boolean tieneSesionReciente() {
        if (this.ultimaSesion == null) {
            return false;
        }
        return this.ultimaSesion.isAfter(LocalDateTime.now().minusDays(1));
    }

    /**
     * Calcula los días desde el registro
     * @return número de días desde el registro
     */
    public long diasDesdeRegistro() {
        if (this.fechaRegistro == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.fechaRegistro.toLocalDate(),
                LocalDateTime.now().toLocalDate());
    }

    // Métodos para validación

    /**
     * Valida si los datos del usuario son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return validarCedula() && validarEmail() &&
                this.passwordHash != null && !this.passwordHash.trim().isEmpty() &&
                this.tipoUsuarioId != null;
    }

    /**
     * Valida la cédula ecuatoriana
     * @return true si la cédula es válida
     */
    public boolean validarCedula() {
        if (this.cedula == null || !PATRON_CEDULA_ECUATORIANA.matcher(this.cedula).matches()) {
            return false;
        }

        // Validación del dígito verificador de cédula ecuatoriana
        try {
            int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
            int suma = 0;
            int digitoVerificador = Integer.parseInt(this.cedula.substring(9, 10));

            for (int i = 0; i < 9; i++) {
                int digito = Integer.parseInt(this.cedula.substring(i, i + 1));
                int producto = digito * coeficientes[i];
                if (producto >= 10) {
                    producto -= 9;
                }
                suma += producto;
            }

            int residuo = suma % 10;
            int digitoEsperado = residuo == 0 ? 0 : 10 - residuo;

            return digitoEsperado == digitoVerificador;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valida el formato del email
     * @return true si el email es válido
     */
    public boolean validarEmail() {
        return this.email != null && PATRON_EMAIL.matcher(this.email).matches();
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (!validarCedula()) {
            mensajes.append("La cédula no es válida. ");
        }

        if (!validarEmail()) {
            mensajes.append("El formato del email no es válido. ");
        }

        if (this.passwordHash == null || this.passwordHash.trim().isEmpty()) {
            mensajes.append("La contraseña es requerida. ");
        }

        if (this.tipoUsuarioId == null) {
            mensajes.append("El tipo de usuario es requerido. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos para manejo de contraseñas

    /**
     * Verifica si la contraseña tiene un formato seguro
     * @param password contraseña en texto plano
     * @return true si cumple los requisitos mínimos de seguridad
     */
    public static boolean esPasswordSegura(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        boolean tieneMinuscula = password.matches(".*[a-z].*");
        boolean tieneMayuscula = password.matches(".*[A-Z].*");
        boolean tieneNumero = password.matches(".*[0-9].*");

        return tieneMinuscula && (tieneMayuscula || tieneNumero);
    }

    /**
     * Obtiene los requisitos de contraseña para mostrar al usuario
     * @return String con los requisitos
     */
    public static String obtenerRequisitosPassword() {
        return "La contraseña debe tener al menos 6 caracteres, " +
                "incluir minúsculas y al menos una mayúscula o número.";
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Usuario usuario = (Usuario) obj;
        return Objects.equals(id, usuario.id) && Objects.equals(cedula, usuario.cedula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cedula);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", cedula='" + cedula + '\'' +
                ", email='" + email + '\'' +
                ", tipoUsuarioId=" + tipoUsuarioId +
                ", activo=" + activo +
                ", fechaRegistro=" + fechaRegistro +
                ", ultimaSesion=" + ultimaSesion +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append("Cédula: ").append(this.cedula);
        if (this.email != null) {
            display.append(" - ").append(this.email);
        }
        display.append(" (").append(obtenerNombreTipoUsuario()).append(")");
        return display.toString();
    }

    /**
     * Representación para logs de seguridad (sin datos sensibles)
     * @return String seguro para logs
     */
    public String toSecureLogString() {
        return "Usuario{" +
                "id=" + id +
                ", cedula='" + (cedula != null ? cedula.substring(0, 4) + "******" : null) + '\'' +
                ", tipoUsuario='" + obtenerNombreTipoUsuario() + '\'' +
                ", activo=" + activo +
                '}';
    }

    // Métodos estáticos para crear usuarios predefinidos

    /**
     * Crea un usuario administrador por defecto
     * @param cedula cédula del administrador
     * @param email email del administrador
     * @param passwordHash contraseña ya encriptada
     * @return Usuario administrador configurado
     */
    public static Usuario crearAdministrador(String cedula, String email, String passwordHash) {
        Usuario admin = new Usuario();
        admin.setCedula(cedula);
        admin.setEmail(email);
        admin.setPasswordHash(passwordHash);
        admin.setTipoUsuario(TipoUsuario.crearAdministrador());
        return admin;
    }

    /**
     * Crea un usuario padre de familia
     * @param cedula cédula del padre
     * @param email email del padre
     * @param passwordHash contraseña ya encriptada
     * @return Usuario padre configurado
     */
    public static Usuario crearPadreFamilia(String cedula, String email, String passwordHash) {
        Usuario padre = new Usuario();
        padre.setCedula(cedula);
        padre.setEmail(email);
        padre.setPasswordHash(passwordHash);
        padre.setTipoUsuario(TipoUsuario.crearPadreFamilia());
        return padre;
    }

    /**
     * Crea un usuario profesional de enfermería
     * @param cedula cédula del profesional
     * @param email email del profesional
     * @param passwordHash contraseña ya encriptada
     * @return Usuario profesional configurado
     */
    public static Usuario crearProfesionalEnfermeria(String cedula, String email, String passwordHash) {
        Usuario profesional = new Usuario();
        profesional.setCedula(cedula);
        profesional.setEmail(email);
        profesional.setPasswordHash(passwordHash);
        profesional.setTipoUsuario(TipoUsuario.crearProfesionalEnfermeria());
        return profesional;
    }
}