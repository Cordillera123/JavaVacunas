package com.vacutrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase modelo para EsquemaVacunacion
 * Representa el cronograma oficial de vacunación infantil
 * Define cuándo y cómo se debe aplicar cada dosis de cada vacuna
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class EsquemaVacunacion {

    // Atributos de la clase
    private Integer id;
    private Integer vacunaId;
    private Vacuna vacuna; // Relación con Vacuna
    private Integer numeroDosis;
    private Integer edadAplicacionDias; // Edad en días desde nacimiento
    private Integer edadMinimaDias;
    private Integer edadMaximaDias;
    private String descripcionEdad; // Ej: "Al nacer", "2 meses", "4 meses"
    private Boolean esRefuerzo;
    private Integer intervaloDosisAnterior; // Días mínimos desde dosis anterior
    private String observaciones;
    private Boolean activo;

    // Campos adicionales para información de vacuna (no se almacenan en BD)
    private String vacunaNombre;
    private String vacunaDescripcion;
    private String vacunaCodigo;

    // Constantes para edades comunes
    public static final int EDAD_AL_NACER = 0;
    public static final int EDAD_2_MESES = 60;
    public static final int EDAD_4_MESES = 120;
    public static final int EDAD_6_MESES = 180;
    public static final int EDAD_7_MESES = 210;
    public static final int EDAD_12_MESES = 365;
    public static final int EDAD_18_MESES = 540;
    public static final int EDAD_24_MESES = 730;

    // Constantes para intervalos
    public static final int INTERVALO_ESTANDAR_DOSIS = 60; // 2 meses
    public static final int INTERVALO_MINIMO_DOSIS = 30; // 1 mes
    public static final int INTERVALO_INFLUENZA = 30; // 1 mes para influenza

    // Constructor vacío (requerido para JavaBeans)
    public EsquemaVacunacion() {
        this.activo = true;
        this.esRefuerzo = false;
        this.numeroDosis = 1;
    }

    // Constructor con parámetros básicos
    public EsquemaVacunacion(Integer vacunaId, Integer numeroDosis, Integer edadAplicacionDias, String descripcionEdad) {
        this();
        this.vacunaId = vacunaId;
        this.numeroDosis = numeroDosis;
        this.edadAplicacionDias = edadAplicacionDias;
        this.descripcionEdad = descripcionEdad;
    }

    // Constructor con Vacuna
    public EsquemaVacunacion(Vacuna vacuna, Integer numeroDosis, Integer edadAplicacionDias, String descripcionEdad) {
        this();
        this.vacuna = vacuna;
        this.vacunaId = vacuna != null ? vacuna.getId() : null;
        this.numeroDosis = numeroDosis;
        this.edadAplicacionDias = edadAplicacionDias;
        this.descripcionEdad = descripcionEdad;
    }

    // Constructor completo
    public EsquemaVacunacion(Integer id, Integer vacunaId, Integer numeroDosis, Integer edadAplicacionDias,
                             Integer edadMinimaDias, Integer edadMaximaDias, String descripcionEdad,
                             Boolean esRefuerzo, Integer intervaloDosisAnterior, String observaciones, Boolean activo) {
        this.id = id;
        this.vacunaId = vacunaId;
        this.numeroDosis = numeroDosis;
        this.edadAplicacionDias = edadAplicacionDias;
        this.edadMinimaDias = edadMinimaDias;
        this.edadMaximaDias = edadMaximaDias;
        this.descripcionEdad = descripcionEdad;
        this.esRefuerzo = esRefuerzo;
        this.intervaloDosisAnterior = intervaloDosisAnterior;
        this.observaciones = observaciones;
        this.activo = activo;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVacunaId() {
        return vacunaId;
    }

    public void setVacunaId(Integer vacunaId) {
        this.vacunaId = vacunaId;
    }

    public Vacuna getVacuna() {
        return vacuna;
    }

    public void setVacuna(Vacuna vacuna) {
        this.vacuna = vacuna;
        this.vacunaId = vacuna != null ? vacuna.getId() : null;
    }

    public Integer getNumeroDosis() {
        return numeroDosis;
    }

    public void setNumeroDosis(Integer numeroDosis) {
        this.numeroDosis = numeroDosis != null && numeroDosis > 0 ? numeroDosis : 1;
    }

    public Integer getEdadAplicacionDias() {
        return edadAplicacionDias;
    }

    public void setEdadAplicacionDias(Integer edadAplicacionDias) {
        this.edadAplicacionDias = edadAplicacionDias != null && edadAplicacionDias >= 0 ? edadAplicacionDias : 0;
    }

    public Integer getEdadMinimaDias() {
        return edadMinimaDias;
    }

    public void setEdadMinimaDias(Integer edadMinimaDias) {
        this.edadMinimaDias = edadMinimaDias;
    }

    public Integer getEdadMaximaDias() {
        return edadMaximaDias;
    }

    public void setEdadMaximaDias(Integer edadMaximaDias) {
        this.edadMaximaDias = edadMaximaDias;
    }

    public String getDescripcionEdad() {
        return descripcionEdad;
    }

    public void setDescripcionEdad(String descripcionEdad) {
        this.descripcionEdad = descripcionEdad != null ? descripcionEdad.trim() : null;
    }

    public Boolean getEsRefuerzo() {
        return esRefuerzo;
    }

    public void setEsRefuerzo(Boolean esRefuerzo) {
        this.esRefuerzo = esRefuerzo != null ? esRefuerzo : false;
    }

    public Integer getIntervaloDosisAnterior() {
        return intervaloDosisAnterior;
    }

    public void setIntervaloDosisAnterior(Integer intervaloDosisAnterior) {
        this.intervaloDosisAnterior = intervaloDosisAnterior;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones != null ? observaciones.trim() : null;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    // Getters y setters para campos adicionales de vacuna
    public String getVacunaNombre() {
        return vacunaNombre;
    }

    public void setVacunaNombre(String vacunaNombre) {
        this.vacunaNombre = vacunaNombre;
    }

    public String getVacunaDescripcion() {
        return vacunaDescripcion;
    }

    public void setVacunaDescripcion(String vacunaDescripcion) {
        this.vacunaDescripcion = vacunaDescripcion;
    }

    public String getVacunaCodigo() {
        return vacunaCodigo;
    }

    public void setVacunaCodigo(String vacunaCodigo) {
        this.vacunaCodigo = vacunaCodigo;
    }

    // Métodos utilitarios

    /**
     * Verifica si el esquema está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo;
    }

    /**
     * Verifica si es un refuerzo
     * @return true si es refuerzo
     */
    public boolean esRefuerzo() {
        return this.esRefuerzo != null && this.esRefuerzo;
    }

    /**
     * Verifica si es la primera dosis
     * @return true si es la primera dosis
     */
    public boolean esPrimeraDosis() {
        return this.numeroDosis != null && this.numeroDosis == 1;
    }

    /**
     * Calcula la fecha programada para un niño específico
     * @param fechaNacimiento fecha de nacimiento del niño
     * @return LocalDate con la fecha programada
     */
    public LocalDate calcularFechaProgramada(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null || this.edadAplicacionDias == null) {
            return null;
        }
        return fechaNacimiento.plusDays(this.edadAplicacionDias);
    }

    /**
     * Calcula la edad mínima permitida para aplicar
     * @param fechaNacimiento fecha de nacimiento del niño
     * @return LocalDate con la fecha mínima permitida
     */
    public LocalDate calcularFechaMinima(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return null;
        }

        int diasMinimos = this.edadMinimaDias != null ? this.edadMinimaDias : this.edadAplicacionDias;
        return fechaNacimiento.plusDays(diasMinimos);
    }

    /**
     * Calcula la edad máxima recomendada para aplicar
     * @param fechaNacimiento fecha de nacimiento del niño
     * @return LocalDate con la fecha máxima recomendada o null si no hay límite
     */
    public LocalDate calcularFechaMaxima(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null || this.edadMaximaDias == null) {
            return null;
        }
        return fechaNacimiento.plusDays(this.edadMaximaDias);
    }

    /**
     * Verifica si una fecha está en el rango válido para aplicar
     * @param fechaNacimiento fecha de nacimiento del niño
     * @param fechaAplicacion fecha propuesta para aplicar
     * @return true si está en rango válido
     */
    public boolean estaEnRangoValido(LocalDate fechaNacimiento, LocalDate fechaAplicacion) {
        if (fechaNacimiento == null || fechaAplicacion == null) {
            return false;
        }

        LocalDate fechaMinima = calcularFechaMinima(fechaNacimiento);
        LocalDate fechaMaxima = calcularFechaMaxima(fechaNacimiento);

        boolean despuesMinima = fechaAplicacion.isAfter(fechaMinima) || fechaAplicacion.isEqual(fechaMinima);
        boolean antesMaxima = fechaMaxima == null || fechaAplicacion.isBefore(fechaMaxima) || fechaAplicacion.isEqual(fechaMaxima);

        return despuesMinima && antesMaxima;
    }

    /**
     * Verifica si la vacuna está vencida
     * @param fechaNacimiento fecha de nacimiento del niño
     * @param fechaActual fecha actual
     * @return true si está vencida
     */
    public boolean estaVencida(LocalDate fechaNacimiento, LocalDate fechaActual) {
        if (fechaNacimiento == null || fechaActual == null || this.edadMaximaDias == null) {
            return false;
        }

        LocalDate fechaMaxima = calcularFechaMaxima(fechaNacimiento);
        return fechaMaxima != null && fechaActual.isAfter(fechaMaxima);
    }

    /**
     * Verifica si la vacuna está próxima a aplicar (dentro de 30 días)
     * @param fechaNacimiento fecha de nacimiento del niño
     * @param fechaActual fecha actual
     * @return true si está próxima
     */
    public boolean estaProxima(LocalDate fechaNacimiento, LocalDate fechaActual) {
        LocalDate fechaProgramada = calcularFechaProgramada(fechaNacimiento);
        if (fechaProgramada == null) {
            return false;
        }

        return !fechaActual.isAfter(fechaProgramada) &&
                fechaActual.plusDays(30).isAfter(fechaProgramada);
    }

    /**
     * Obtiene el nombre de la vacuna
     * @return String con el nombre de la vacuna
     */
    public String obtenerNombreVacuna() {
        if (this.vacuna != null) {
            return this.vacuna.obtenerNombreDisplay();
        }
        return "Vacuna no especificada";
    }

    /**
     * Obtiene el código de la vacuna
     * @return String con el código de la vacuna
     */
    public String obtenerCodigoVacuna() {
        if (this.vacuna != null) {
            return this.vacuna.getCodigo();
        }
        return null;
    }

    /**
     * Obtiene información completa del esquema
     * @return String con información del esquema
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();

        info.append(obtenerNombreVacuna());
        info.append(" - Dosis ").append(this.numeroDosis);

        if (this.descripcionEdad != null) {
            info.append(" (").append(this.descripcionEdad).append(")");
        }

        if (this.esRefuerzo()) {
            info.append(" - REFUERZO");
        }

        return info.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del esquema son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.vacunaId != null &&
                this.numeroDosis != null && this.numeroDosis > 0 &&
                this.edadAplicacionDias != null && this.edadAplicacionDias >= 0 &&
                this.descripcionEdad != null && !this.descripcionEdad.trim().isEmpty();
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.vacunaId == null) {
            mensajes.append("La vacuna es requerida. ");
        }

        if (this.numeroDosis == null || this.numeroDosis <= 0) {
            mensajes.append("El número de dosis debe ser mayor a 0. ");
        }

        if (this.edadAplicacionDias == null || this.edadAplicacionDias < 0) {
            mensajes.append("La edad de aplicación debe ser mayor o igual a 0. ");
        }

        if (this.descripcionEdad == null || this.descripcionEdad.trim().isEmpty()) {
            mensajes.append("La descripción de edad es requerida. ");
        }

        if (this.edadMinimaDias != null && this.edadAplicacionDias != null &&
                this.edadMinimaDias > this.edadAplicacionDias) {
            mensajes.append("La edad mínima no puede ser mayor que la edad de aplicación. ");
        }

        if (this.edadMaximaDias != null && this.edadAplicacionDias != null &&
                this.edadMaximaDias < this.edadAplicacionDias) {
            mensajes.append("La edad máxima no puede ser menor que la edad de aplicación. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EsquemaVacunacion that = (EsquemaVacunacion) obj;
        return Objects.equals(id, that.id) &&
                Objects.equals(vacunaId, that.vacunaId) &&
                Objects.equals(numeroDosis, that.numeroDosis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vacunaId, numeroDosis);
    }

    @Override
    public String toString() {
        return "EsquemaVacunacion{" +
                "id=" + id +
                ", vacunaId=" + vacunaId +
                ", numeroDosis=" + numeroDosis +
                ", edadAplicacionDias=" + edadAplicacionDias +
                ", descripcionEdad='" + descripcionEdad + '\'' +
                ", esRefuerzo=" + esRefuerzo +
                ", activo=" + activo +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return obtenerNombreVacuna() + " - Dosis " + this.numeroDosis + " (" + this.descripcionEdad + ")";
    }

    // Métodos estáticos para crear esquemas predefinidos

    /**
     * Crea esquema para BCG al nacer
     * @param vacunaBCG vacuna BCG
     * @return EsquemaVacunacion configurado
     */
    public static EsquemaVacunacion crearBCGAlNacer(Vacuna vacunaBCG) {
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setVacuna(vacunaBCG);
        esquema.setNumeroDosis(1);
        esquema.setEdadAplicacionDias(EDAD_AL_NACER);
        esquema.setEdadMinimaDias(0);
        esquema.setEdadMaximaDias(30); // Máximo 30 días
        esquema.setDescripcionEdad("Al nacer");
        esquema.setIntervaloDosisAnterior(null);
        esquema.setObservaciones("Aplicar en las primeras 24-48 horas de vida");
        return esquema;
    }

    /**
     * Crea esquema para vacunas de 2 meses
     * @param vacuna vacuna correspondiente
     * @param numeroDosis número de dosis
     * @return EsquemaVacunacion configurado
     */
    public static EsquemaVacunacion crear2Meses(Vacuna vacuna, Integer numeroDosis) {
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setVacuna(vacuna);
        esquema.setNumeroDosis(numeroDosis);
        esquema.setEdadAplicacionDias(EDAD_2_MESES);
        esquema.setEdadMinimaDias(45); // Mínimo 45 días
        esquema.setEdadMaximaDias(90); // Máximo 3 meses
        esquema.setDescripcionEdad("2 meses");
        esquema.setIntervaloDosisAnterior(INTERVALO_ESTANDAR_DOSIS);
        return esquema;
    }

    /**
     * Crea esquema para SRP a los 12 meses
     * @param vacunaSRP vacuna SRP
     * @param numeroDosis número de dosis (1 o 2)
     * @return EsquemaVacunacion configurado
     */
    public static EsquemaVacunacion crearSRP(Vacuna vacunaSRP, Integer numeroDosis) {
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setVacuna(vacunaSRP);
        esquema.setNumeroDosis(numeroDosis);

        if (numeroDosis == 1) {
            esquema.setEdadAplicacionDias(EDAD_12_MESES);
            esquema.setDescripcionEdad("12 meses");
        } else {
            esquema.setEdadAplicacionDias(EDAD_18_MESES);
            esquema.setDescripcionEdad("18 meses");
            esquema.setIntervaloDosisAnterior(180); // 6 meses entre dosis
        }

        esquema.setObservaciones("No aplicar antes de los 12 meses por interferencia con anticuerpos maternos");
        return esquema;
    }
}