package com.vacutrack.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase modelo para TipoUsuario
 * Representa los diferentes tipos de usuarios del sistema VACU-TRACK
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class TipoUsuario {

    // Atributos de la clase
    private Integer id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    // Constantes para los tipos de usuario
    public static final String PADRE_FAMILIA = "PADRE_FAMILIA";
    public static final String PROFESIONAL_ENFERMERIA = "PROFESIONAL_ENFERMERIA";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    // Constructor vacío (requerido para JavaBeans)
    public TipoUsuario() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros principales
    public TipoUsuario(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Constructor completo
    public TipoUsuario(Integer id, String nombre, String descripcion, Boolean activo, LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim().toUpperCase() : null;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion != null ? descripcion.trim() : null;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Verifica si el tipo de usuario es Padre de Familia
     * @return true si es padre de familia
     */
    public boolean esPadreFamilia() {
        return PADRE_FAMILIA.equals(this.nombre);
    }

    /**
     * Verifica si el tipo de usuario es Profesional de Enfermería
     * @return true si es profesional de enfermería
     */
    public boolean esProfesionalEnfermeria() {
        return PROFESIONAL_ENFERMERIA.equals(this.nombre);
    }

    /**
     * Verifica si el tipo de usuario es Administrador
     * @return true si es administrador
     */
    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(this.nombre);
    }

    /**
     * Verifica si el tipo de usuario está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo;
    }

    // Métodos para validación

    /**
     * Valida si los datos del tipo de usuario son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.nombre != null && !this.nombre.trim().isEmpty() &&
                this.descripcion != null && !this.descripcion.trim().isEmpty();
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.nombre == null || this.nombre.trim().isEmpty()) {
            mensajes.append("El nombre es requerido. ");
        }

        if (this.descripcion == null || this.descripcion.trim().isEmpty()) {
            mensajes.append("La descripción es requerida. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TipoUsuario that = (TipoUsuario) obj;
        return Objects.equals(id, that.id) && Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre);
    }

    @Override
    public String toString() {
        return "TipoUsuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", activo=" + activo +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return this.descripcion != null ? this.descripcion : this.nombre;
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un tipo de usuario Padre de Familia
     * @return TipoUsuario configurado como Padre de Familia
     */
    public static TipoUsuario crearPadreFamilia() {
        return new TipoUsuario(PADRE_FAMILIA, "Padre o madre de familia");
    }

    /**
     * Crea un tipo de usuario Profesional de Enfermería
     * @return TipoUsuario configurado como Profesional de Enfermería
     */
    public static TipoUsuario crearProfesionalEnfermeria() {
        return new TipoUsuario(PROFESIONAL_ENFERMERIA, "Profesional de enfermería autorizado");
    }

    /**
     * Crea un tipo de usuario Administrador
     * @return TipoUsuario configurado como Administrador
     */
    public static TipoUsuario crearAdministrador() {
        return new TipoUsuario(ADMINISTRADOR, "Administrador del sistema");
    }
}