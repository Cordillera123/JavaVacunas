package com.vacutrack.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clase modelo para PadreFamilia
 * Representa la información específica de padres de familia en el sistema
 * Extiende la información básica del Usuario
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class PadreFamilia {

    // Atributos de la clase
    private Integer id;
    private Integer usuarioId;
    private Usuario usuario; // Relación con Usuario
    private String nombres;
    private String apellidos;
    private String telefono;
    private String direccion;
    private LocalDateTime fechaCreacion;

    // Patrones para validación
    private static final Pattern PATRON_TELEFONO_ECUADOR = Pattern.compile(
            "^(02-\\d{7}|09\\d{8}|\\d{7,10})$");
    private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile(
            "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");

    // Constructor vacío (requerido para JavaBeans)
    public PadreFamilia() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public PadreFamilia(Integer usuarioId, String nombres, String apellidos) {
        this();
        this.usuarioId = usuarioId;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    // Constructor con Usuario
    public PadreFamilia(Usuario usuario, String nombres, String apellidos) {
        this();
        this.usuario = usuario;
        this.usuarioId = usuario != null ? usuario.getId() : null;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    // Constructor completo
    public PadreFamilia(Integer id, Integer usuarioId, String nombres, String apellidos,
                        String telefono, String direccion, LocalDateTime fechaCreacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        this.usuarioId = usuario != null ? usuario.getId() : null;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres != null ? normalizarTexto(nombres) : null;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos != null ? normalizarTexto(apellidos) : null;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        if (telefono != null) {
            // Limpiar y formatear teléfono
            this.telefono = telefono.trim().replaceAll("[^0-9-]", "");
        } else {
            this.telefono = null;
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion != null ? direccion.trim() : null;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Obtiene el nombre completo del padre
     * @return String con nombres y apellidos
     */
    public String obtenerNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder();

        if (this.nombres != null && !this.nombres.trim().isEmpty()) {
            nombreCompleto.append(this.nombres);
        }

        if (this.apellidos != null && !this.apellidos.trim().isEmpty()) {
            if (nombreCompleto.length() > 0) {
                nombreCompleto.append(" ");
            }
            nombreCompleto.append(this.apellidos);
        }

        return nombreCompleto.length() > 0 ? nombreCompleto.toString() : "Sin nombre";
    }

    /**
     * Obtiene las iniciales del padre
     * @return String con las iniciales (ej: "J.P.")
     */
    public String obtenerIniciales() {
        StringBuilder iniciales = new StringBuilder();

        if (this.nombres != null && !this.nombres.isEmpty()) {
            String[] partesNombres = this.nombres.trim().split("\\s+");
            for (String parte : partesNombres) {
                if (!parte.isEmpty()) {
                    iniciales.append(parte.charAt(0)).append(".");
                }
            }
        }

        if (this.apellidos != null && !this.apellidos.isEmpty()) {
            String[] partesApellidos = this.apellidos.trim().split("\\s+");
            for (String parte : partesApellidos) {
                if (!parte.isEmpty()) {
                    iniciales.append(parte.charAt(0)).append(".");
                }
            }
        }

        return iniciales.toString();
    }

    /**
     * Obtiene información de contacto completa
     * @return String con teléfono y dirección
     */
    public String obtenerContactoCompleto() {
        StringBuilder contacto = new StringBuilder();

        if (this.telefono != null && !this.telefono.isEmpty()) {
            contacto.append("Tel: ").append(this.telefono);
        }

        if (this.direccion != null && !this.direccion.isEmpty()) {
            if (contacto.length() > 0) {
                contacto.append(" - ");
            }
            contacto.append("Dir: ").append(this.direccion);
        }

        return contacto.length() > 0 ? contacto.toString() : "Sin información de contacto";
    }

    /**
     * Obtiene la cédula del usuario asociado
     * @return String con la cédula o null si no hay usuario
     */
    public String obtenerCedula() {
        return this.usuario != null ? this.usuario.getCedula() : null;
    }

    /**
     * Obtiene el email del usuario asociado
     * @return String con el email o null si no hay usuario
     */
    public String obtenerEmail() {
        return this.usuario != null ? this.usuario.getEmail() : null;
    }

    /**
     * Verifica si el padre está activo (basado en el usuario)
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.usuario != null && this.usuario.estaActivo();
    }

    /**
     * Verifica si tiene información de contacto
     * @return true si tiene teléfono o dirección
     */
    public boolean tieneContacto() {
        return (this.telefono != null && !this.telefono.isEmpty()) ||
                (this.direccion != null && !this.direccion.isEmpty());
    }

    /**
     * Obtiene información completa del padre para mostrar
     * @return String con toda la información disponible
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();

        info.append(obtenerNombreCompleto());

        String cedula = obtenerCedula();
        if (cedula != null) {
            info.append(" - CI: ").append(cedula);
        }

        String email = obtenerEmail();
        if (email != null) {
            info.append(" - ").append(email);
        }

        if (tieneContacto()) {
            info.append(" - ").append(obtenerContactoCompleto());
        }

        return info.toString();
    }

    /**
     * Calcula los días desde el registro
     * @return número de días desde el registro
     */
    public long diasDesdeRegistro() {
        if (this.fechaCreacion == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.fechaCreacion.toLocalDate(),
                LocalDateTime.now().toLocalDate());
    }

    /**
     * Normaliza texto (capitaliza primera letra de cada palabra)
     * @param texto texto a normalizar
     * @return texto normalizado
     */
    private String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        String textoLimpio = texto.trim().toLowerCase();
        StringBuilder resultado = new StringBuilder();
        boolean capitalizarSiguiente = true;

        for (char c : textoLimpio.toCharArray()) {
            if (Character.isLetter(c)) {
                if (capitalizarSiguiente) {
                    resultado.append(Character.toUpperCase(c));
                    capitalizarSiguiente = false;
                } else {
                    resultado.append(c);
                }
            } else if (Character.isWhitespace(c)) {
                resultado.append(c);
                capitalizarSiguiente = true;
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del padre son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.usuarioId != null &&
                validarNombres() && validarApellidos() &&
                (this.telefono == null || validarTelefono());
    }

    /**
     * Valida los nombres
     * @return true si los nombres son válidos
     */
    public boolean validarNombres() {
        return this.nombres != null &&
                !this.nombres.trim().isEmpty() &&
                this.nombres.length() >= 2 &&
                this.nombres.length() <= 100 &&
                PATRON_SOLO_LETRAS.matcher(this.nombres).matches();
    }

    /**
     * Valida los apellidos
     * @return true si los apellidos son válidos
     */
    public boolean validarApellidos() {
        return this.apellidos != null &&
                !this.apellidos.trim().isEmpty() &&
                this.apellidos.length() >= 2 &&
                this.apellidos.length() <= 100 &&
                PATRON_SOLO_LETRAS.matcher(this.apellidos).matches();
    }

    /**
     * Valida el teléfono
     * @return true si el teléfono es válido o null
     */
    public boolean validarTelefono() {
        if (this.telefono == null || this.telefono.isEmpty()) {
            return true; // Teléfono es opcional
        }
        return PATRON_TELEFONO_ECUADOR.matcher(this.telefono).matches();
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.usuarioId == null) {
            mensajes.append("El usuario es requerido. ");
        }

        if (!validarNombres()) {
            mensajes.append("Los nombres son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarApellidos()) {
            mensajes.append("Los apellidos son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarTelefono()) {
            mensajes.append("El formato del teléfono no es válido para Ecuador. ");
        }

        if (this.direccion != null && this.direccion.length() > 250) {
            mensajes.append("La dirección no puede exceder 250 caracteres. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PadreFamilia that = (PadreFamilia) obj;
        return Objects.equals(id, that.id) && Objects.equals(usuarioId, that.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioId);
    }

    @Override
    public String toString() {
        return "PadreFamilia{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", telefono='" + telefono + '\'' +
                ", direccion='" + direccion + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return obtenerNombreCompleto() + " (CI: " + obtenerCedula() + ")";
    }

    /**
     * Representación para listas y selectores
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append(obtenerNombreCompleto());

        String cedula = obtenerCedula();
        if (cedula != null && cedula.length() >= 4) {
            display.append(" - ****").append(cedula.substring(cedula.length() - 4));
        }

        return display.toString();
    }

    /**
     * Representación para reportes
     * @return String con información completa para reportes
     */
    public String toReportString() {
        StringBuilder report = new StringBuilder();
        report.append("Padre/Madre: ").append(obtenerNombreCompleto()).append("\n");

        String cedula = obtenerCedula();
        if (cedula != null) {
            report.append("Cédula: ").append(cedula).append("\n");
        }

        String email = obtenerEmail();
        if (email != null) {
            report.append("Email: ").append(email).append("\n");
        }

        if (this.telefono != null) {
            report.append("Teléfono: ").append(this.telefono).append("\n");
        }

        if (this.direccion != null) {
            report.append("Dirección: ").append(this.direccion).append("\n");
        }

        report.append("Registro: ").append(this.fechaCreacion != null ?
                this.fechaCreacion.toLocalDate() : "No disponible");

        return report.toString();
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un padre de familia con información mínima
     * @param usuario usuario asociado
     * @param nombres nombres del padre
     * @param apellidos apellidos del padre
     * @return PadreFamilia configurado
     */
    public static PadreFamilia crearBasico(Usuario usuario, String nombres, String apellidos) {
        PadreFamilia padre = new PadreFamilia();
        padre.setUsuario(usuario);
        padre.setNombres(nombres);
        padre.setApellidos(apellidos);
        return padre;
    }

    /**
     * Crea un padre de familia con información completa
     * @param usuario usuario asociado
     * @param nombres nombres del padre
     * @param apellidos apellidos del padre
     * @param telefono teléfono del padre
     * @param direccion dirección del padre
     * @return PadreFamilia configurado
     */
    public static PadreFamilia crearCompleto(Usuario usuario, String nombres, String apellidos,
                                             String telefono, String direccion) {
        PadreFamilia padre = crearBasico(usuario, nombres, apellidos);
        padre.setTelefono(telefono);
        padre.setDireccion(direccion);
        return padre;
    }

    /**
     * Crea un padre de familia de ejemplo para testing
     * @return PadreFamilia de ejemplo
     */
    public static PadreFamilia crearEjemplo() {
        PadreFamilia padre = new PadreFamilia();
        padre.setNombres("María Elena");
        padre.setApellidos("González López");
        padre.setTelefono("02-295-1234");
        padre.setDireccion("Av. 10 de Agosto N24-123 y Cordero, Quito");
        return padre;
    }
}