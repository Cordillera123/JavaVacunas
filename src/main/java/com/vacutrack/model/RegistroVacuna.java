package com.vacutrack.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;

/**
 * Clase modelo para RegistroVacuna
 * Representa el historial de vacunas aplicadas a los niños
 * Es la clase central del sistema que conecta niños, vacunas, profesionales y centros
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class RegistroVacuna {

    // Atributos de la clase
    private Integer id;
    private Integer ninoId;
    private Nino nino; // Relación con Nino
    private Integer vacunaId;
    private Vacuna vacuna; // Relación con Vacuna
    private Integer profesionalId;
    private ProfesionalEnfermeria profesional; // Relación con ProfesionalEnfermeria
    private Integer centroSaludId;
    private CentroSalud centroSalud; // Relación con CentroSalud
    private LocalDate fechaAplicacion;
    private Integer numeroDosis;
    private String loteVacuna;
    private LocalDate fechaVencimiento;
    private String observaciones;
    private Boolean reaccionAdversa;
    private String descripcionReaccion;
    private BigDecimal pesoAplicacion; // Peso del niño al momento de aplicar
    private BigDecimal tallaAplicacion; // Talla del niño al momento de aplicar
    private LocalDateTime fechaRegistro;

    // Campos adicionales para información de vacuna (no se almacenan en BD)
    private String vacunaNombre;
    private String vacunaDescripcion;
    private String vacunaCodigo;

    // Constantes para validación
    private static final int LOTE_MIN_LENGTH = 3;
    private static final int LOTE_MAX_LENGTH = 50;
    private static final int OBSERVACIONES_MAX_LENGTH = 1000;
    private static final int DESCRIPCION_REACCION_MAX_LENGTH = 500;

    // Constructor vacío (requerido para JavaBeans)
    public RegistroVacuna() {
        this.reaccionAdversa = false;
        this.fechaRegistro = LocalDateTime.now();
        this.fechaAplicacion = LocalDate.now();
        this.numeroDosis = 1;
    }

    // Constructor con parámetros básicos
    public RegistroVacuna(Integer ninoId, Integer vacunaId, Integer profesionalId,
                          LocalDate fechaAplicacion, Integer numeroDosis) {
        this();
        this.ninoId = ninoId;
        this.vacunaId = vacunaId;
        this.profesionalId = profesionalId;
        this.fechaAplicacion = fechaAplicacion;
        this.numeroDosis = numeroDosis;
    }

    // Constructor con objetos relacionados
    public RegistroVacuna(Nino nino, Vacuna vacuna, ProfesionalEnfermeria profesional,
                          LocalDate fechaAplicacion, Integer numeroDosis) {
        this();
        this.nino = nino;
        this.ninoId = nino != null ? nino.getId() : null;
        this.vacuna = vacuna;
        this.vacunaId = vacuna != null ? vacuna.getId() : null;
        this.profesional = profesional;
        this.profesionalId = profesional != null ? profesional.getId() : null;
        this.centroSalud = profesional != null ? profesional.getCentroSalud() : null;
        this.centroSaludId = this.centroSalud != null ? this.centroSalud.getId() : null;
        this.fechaAplicacion = fechaAplicacion;
        this.numeroDosis = numeroDosis;
    }

    // Constructor completo
    public RegistroVacuna(Integer id, Integer ninoId, Integer vacunaId, Integer profesionalId,
                          Integer centroSaludId, LocalDate fechaAplicacion, Integer numeroDosis,
                          String loteVacuna, LocalDate fechaVencimiento, String observaciones,
                          Boolean reaccionAdversa, String descripcionReaccion, BigDecimal pesoAplicacion,
                          BigDecimal tallaAplicacion, LocalDateTime fechaRegistro) {
        this.id = id;
        this.ninoId = ninoId;
        this.vacunaId = vacunaId;
        this.profesionalId = profesionalId;
        this.centroSaludId = centroSaludId;
        this.fechaAplicacion = fechaAplicacion;
        this.numeroDosis = numeroDosis;
        this.loteVacuna = loteVacuna;
        this.fechaVencimiento = fechaVencimiento;
        this.observaciones = observaciones;
        this.reaccionAdversa = reaccionAdversa;
        this.descripcionReaccion = descripcionReaccion;
        this.pesoAplicacion = pesoAplicacion;
        this.tallaAplicacion = tallaAplicacion;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNinoId() {
        return ninoId;
    }

    public void setNinoId(Integer ninoId) {
        this.ninoId = ninoId;
    }

    public Nino getNino() {
        return nino;
    }

    public void setNino(Nino nino) {
        this.nino = nino;
        this.ninoId = nino != null ? nino.getId() : null;
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

    public Integer getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Integer profesionalId) {
        this.profesionalId = profesionalId;
    }

    public ProfesionalEnfermeria getProfesional() {
        return profesional;
    }

    public void setProfesional(ProfesionalEnfermeria profesional) {
        this.profesional = profesional;
        this.profesionalId = profesional != null ? profesional.getId() : null;
    }

    public Integer getCentroSaludId() {
        return centroSaludId;
    }

    public void setCentroSaludId(Integer centroSaludId) {
        this.centroSaludId = centroSaludId;
    }

    public CentroSalud getCentroSalud() {
        return centroSalud;
    }

    public void setCentroSalud(CentroSalud centroSalud) {
        this.centroSalud = centroSalud;
        this.centroSaludId = centroSalud != null ? centroSalud.getId() : null;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public Integer getNumeroDosis() {
        return numeroDosis;
    }

    public void setNumeroDosis(Integer numeroDosis) {
        this.numeroDosis = numeroDosis != null && numeroDosis > 0 ? numeroDosis : 1;
    }

    public String getLoteVacuna() {
        return loteVacuna;
    }

    public void setLoteVacuna(String loteVacuna) {
        this.loteVacuna = loteVacuna != null ? loteVacuna.trim().toUpperCase() : null;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones != null ? observaciones.trim() : null;
    }

    public Boolean getReaccionAdversa() {
        return reaccionAdversa;
    }

    public void setReaccionAdversa(Boolean reaccionAdversa) {
        this.reaccionAdversa = reaccionAdversa != null ? reaccionAdversa : false;
    }

    public String getDescripcionReaccion() {
        return descripcionReaccion;
    }

    public void setDescripcionReaccion(String descripcionReaccion) {
        this.descripcionReaccion = descripcionReaccion != null ? descripcionReaccion.trim() : null;
    }

    public BigDecimal getPesoAplicacion() {
        return pesoAplicacion;
    }

    public void setPesoAplicacion(BigDecimal pesoAplicacion) {
        this.pesoAplicacion = pesoAplicacion;
    }

    public BigDecimal getTallaAplicacion() {
        return tallaAplicacion;
    }

    public void setTallaAplicacion(BigDecimal tallaAplicacion) {
        this.tallaAplicacion = tallaAplicacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
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
     * Verifica si tuvo reacción adversa
     * @return true si tuvo reacción adversa
     */
    public boolean tuvoReaccionAdversa() {
        return this.reaccionAdversa != null && this.reaccionAdversa;
    }

    /**
     * Obtiene el nombre del niño
     * @return String con el nombre del niño
     */
    public String obtenerNombreNino() {
        return this.nino != null ? this.nino.obtenerNombreCompleto() : "Niño no especificado";
    }

    /**
     * Obtiene el nombre de la vacuna
     * @return String con el nombre de la vacuna
     */
    public String obtenerNombreVacuna() {
        if (this.vacunaNombre != null) {
            return this.vacunaNombre;
        }
        return this.vacuna != null ? this.vacuna.obtenerNombreDisplay() : "Vacuna no especificada";
    }

    /**
     * Obtiene el código de la vacuna
     * @return String con el código de la vacuna
     */
    public String obtenerCodigoVacuna() {
        if (this.vacunaCodigo != null) {
            return this.vacunaCodigo;
        }
        return this.vacuna != null ? this.vacuna.getCodigo() : null;
    }

    /**
     * Obtiene el nombre del profesional
     * @return String con el nombre del profesional
     */
    public String obtenerNombreProfesional() {
        return this.profesional != null ? this.profesional.obtenerNombreCompleto() : "Profesional no especificado";
    }

    /**
     * Obtiene el nombre del centro de salud
     * @return String con el nombre del centro
     */
    public String obtenerNombreCentroSalud() {
        return this.centroSalud != null ? this.centroSalud.getNombre() : "Centro no especificado";
    }

    /**
     * Calcula la edad del niño al momento de la aplicación
     * @return Period con la edad en el momento de aplicación
     */
    public Period calcularEdadAlMomentoAplicacion() {
        if (this.nino == null || this.nino.getFechaNacimiento() == null || this.fechaAplicacion == null) {
            return Period.ZERO;
        }
        return Period.between(this.nino.getFechaNacimiento(), this.fechaAplicacion);
    }

    /**
     * Calcula la edad en días al momento de la aplicación
     * @return número de días de edad al aplicar
     */
    public long calcularEdadEnDiasAlAplicar() {
        if (this.nino == null || this.nino.getFechaNacimiento() == null || this.fechaAplicacion == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.nino.getFechaNacimiento(), this.fechaAplicacion);
    }

    /**
     * Obtiene la edad formateada al momento de aplicación
     * @return String con la edad formateada
     */
    public String obtenerEdadFormateadaAlAplicar() {
        Period edad = calcularEdadAlMomentoAplicacion();
        StringBuilder edadStr = new StringBuilder();

        if (edad.getYears() > 0) {
            edadStr.append(edad.getYears()).append(edad.getYears() == 1 ? " año" : " años");
        }

        if (edad.getMonths() > 0) {
            if (edadStr.length() > 0) edadStr.append(", ");
            edadStr.append(edad.getMonths()).append(edad.getMonths() == 1 ? " mes" : " meses");
        }

        if (edad.getDays() > 0 && edad.getYears() == 0) {
            if (edadStr.length() > 0) edadStr.append(", ");
            edadStr.append(edad.getDays()).append(edad.getDays() == 1 ? " día" : " días");
        }

        return edadStr.length() > 0 ? edadStr.toString() : "Recién nacido";
    }

    /**
     * Verifica si la vacuna está vencida al momento de aplicación
     * @return true si la vacuna estaba vencida
     */
    public boolean vacunaEstabaVencida() {
        return this.fechaVencimiento != null &&
                this.fechaAplicacion != null &&
                this.fechaAplicacion.isAfter(this.fechaVencimiento);
    }

    /**
     * Verifica si es la primera dosis de la vacuna
     * @return true si es la primera dosis
     */
    public boolean esPrimeraDosis() {
        return this.numeroDosis != null && this.numeroDosis == 1;
    }

    /**
     * Verifica si la aplicación fue tardía según el esquema
     * @param esquema esquema de vacunación correspondiente
     * @return true si fue aplicada tardíamente
     */
    public boolean fueAplicacionTardia(EsquemaVacunacion esquema) {
        if (esquema == null || this.nino == null || this.fechaAplicacion == null) {
            return false;
        }

        LocalDate fechaIdeal = esquema.calcularFechaProgramada(this.nino.getFechaNacimiento());
        if (fechaIdeal == null) {
            return false;
        }

        return this.fechaAplicacion.isAfter(fechaIdeal.plusDays(30)); // Más de 30 días de retraso
    }

    /**
     * Verifica si la aplicación fue temprana según el esquema
     * @param esquema esquema de vacunación correspondiente
     * @return true si fue aplicada muy tempranamente
     */
    public boolean fueAplicacionTemprana(EsquemaVacunacion esquema) {
        if (esquema == null || this.nino == null || this.fechaAplicacion == null) {
            return false;
        }

        LocalDate fechaMinima = esquema.calcularFechaMinima(this.nino.getFechaNacimiento());
        if (fechaMinima == null) {
            return false;
        }

        return this.fechaAplicacion.isBefore(fechaMinima);
    }

    /**
     * Obtiene información completa del registro
     * @return String con información completa
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();

        info.append("Vacuna: ").append(obtenerNombreVacuna())
                .append(" - Dosis ").append(this.numeroDosis);

        info.append("\nNiño: ").append(obtenerNombreNino())
                .append(" (Edad al aplicar: ").append(obtenerEdadFormateadaAlAplicar()).append(")");

        info.append("\nFecha aplicación: ").append(this.fechaAplicacion);

        info.append("\nProfesional: ").append(obtenerNombreProfesional());

        info.append("\nCentro: ").append(obtenerNombreCentroSalud());

        if (this.loteVacuna != null) {
            info.append("\nLote: ").append(this.loteVacuna);
        }

        if (this.pesoAplicacion != null) {
            info.append("\nPeso: ").append(this.pesoAplicacion).append(" kg");
        }

        if (this.tallaAplicacion != null) {
            info.append("\nTalla: ").append(this.tallaAplicacion).append(" cm");
        }

        if (tuvoReaccionAdversa()) {
            info.append("\n⚠️ REACCIÓN ADVERSA: ").append(this.descripcionReaccion);
        }

        if (this.observaciones != null && !this.observaciones.isEmpty()) {
            info.append("\nObservaciones: ").append(this.observaciones);
        }

        return info.toString();
    }

    /**
     * Obtiene información para certificado
     * @return String formateado para certificado
     */
    public String obtenerInformacionCertificado() {
        StringBuilder cert = new StringBuilder();

        cert.append(obtenerCodigoVacuna() != null ? obtenerCodigoVacuna() : obtenerNombreVacuna());
        cert.append(" - Dosis ").append(this.numeroDosis);
        cert.append(" - ").append(this.fechaAplicacion);

        if (this.loteVacuna != null) {
            cert.append(" (Lote: ").append(this.loteVacuna).append(")");
        }

        return cert.toString();
    }

    /**
     * Genera resumen para reporte médico
     * @return String con resumen médico
     */
    public String generarResumenMedico() {
        StringBuilder resumen = new StringBuilder();

        resumen.append("REGISTRO DE VACUNACIÓN\n");
        resumen.append("=====================\n");
        resumen.append("Paciente: ").append(obtenerNombreNino()).append("\n");
        resumen.append("Vacuna: ").append(obtenerNombreVacuna()).append(" (Dosis ").append(this.numeroDosis).append(")\n");
        resumen.append("Fecha aplicación: ").append(this.fechaAplicacion).append("\n");
        resumen.append("Edad al aplicar: ").append(obtenerEdadFormateadaAlAplicar()).append("\n");
        resumen.append("Profesional: ").append(obtenerNombreProfesional()).append("\n");
        resumen.append("Centro: ").append(obtenerNombreCentroSalud()).append("\n");

        if (this.loteVacuna != null) {
            resumen.append("Lote vacuna: ").append(this.loteVacuna).append("\n");
        }

        if (this.fechaVencimiento != null) {
            resumen.append("Vencimiento: ").append(this.fechaVencimiento);
            if (vacunaEstabaVencida()) {
                resumen.append(" ⚠️ VACUNA VENCIDA");
            }
            resumen.append("\n");
        }

        if (this.pesoAplicacion != null || this.tallaAplicacion != null) {
            resumen.append("Datos antropométricos: ");
            if (this.pesoAplicacion != null) {
                resumen.append("Peso ").append(this.pesoAplicacion).append(" kg ");
            }
            if (this.tallaAplicacion != null) {
                resumen.append("Talla ").append(this.tallaAplicacion).append(" cm");
            }
            resumen.append("\n");
        }

        if (tuvoReaccionAdversa()) {
            resumen.append("REACCIÓN ADVERSA: ").append(this.descripcionReaccion).append("\n");
        }

        if (this.observaciones != null && !this.observaciones.isEmpty()) {
            resumen.append("Observaciones: ").append(this.observaciones).append("\n");
        }

        resumen.append("Fecha registro: ").append(this.fechaRegistro).append("\n");

        return resumen.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del registro son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.ninoId != null &&
                this.vacunaId != null &&
                this.profesionalId != null &&
                this.fechaAplicacion != null &&
                this.numeroDosis != null && this.numeroDosis > 0 &&
                validarFechaAplicacion() &&
                validarLote() &&
                validarObservaciones() &&
                validarReaccionAdversa();
    }

    /**
     * Valida la fecha de aplicación
     * @return true si la fecha es válida
     */
    public boolean validarFechaAplicacion() {
        if (this.fechaAplicacion == null) {
            return false;
        }

        LocalDate hoy = LocalDate.now();

        // No puede ser fecha futura
        if (this.fechaAplicacion.isAfter(hoy)) {
            return false;
        }

        // Si hay niño, no puede ser antes del nacimiento
        if (this.nino != null && this.nino.getFechaNacimiento() != null) {
            return !this.fechaAplicacion.isBefore(this.nino.getFechaNacimiento());
        }

        return true;
    }

    /**
     * Valida el lote de la vacuna
     * @return true si el lote es válido
     */
    public boolean validarLote() {
        if (this.loteVacuna == null || this.loteVacuna.isEmpty()) {
            return true; // Es opcional
        }

        return this.loteVacuna.length() >= LOTE_MIN_LENGTH &&
                this.loteVacuna.length() <= LOTE_MAX_LENGTH;
    }

    /**
     * Valida las observaciones
     * @return true si las observaciones son válidas
     */
    public boolean validarObservaciones() {
        if (this.observaciones == null || this.observaciones.isEmpty()) {
            return true; // Son opcionales
        }

        return this.observaciones.length() <= OBSERVACIONES_MAX_LENGTH;
    }

    /**
     * Valida la información de reacción adversa
     * @return true si la información de reacción es válida
     */
    public boolean validarReaccionAdversa() {
        // Si no tuvo reacción, no debe haber descripción
        if (!tuvoReaccionAdversa()) {
            return this.descripcionReaccion == null || this.descripcionReaccion.isEmpty();
        }

        // Si tuvo reacción, debe tener descripción
        if (this.descripcionReaccion == null || this.descripcionReaccion.trim().isEmpty()) {
            return false;
        }

        return this.descripcionReaccion.length() <= DESCRIPCION_REACCION_MAX_LENGTH;
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.ninoId == null) {
            mensajes.append("El niño es requerido. ");
        }

        if (this.vacunaId == null) {
            mensajes.append("La vacuna es requerida. ");
        }

        if (this.profesionalId == null) {
            mensajes.append("El profesional es requerido. ");
        }

        if (this.numeroDosis == null || this.numeroDosis <= 0) {
            mensajes.append("El número de dosis debe ser mayor a 0. ");
        }

        if (!validarFechaAplicacion()) {
            mensajes.append("La fecha de aplicación no es válida. ");
        }

        if (!validarLote()) {
            mensajes.append("El lote debe tener entre 3-50 caracteres. ");
        }

        if (!validarObservaciones()) {
            mensajes.append("Las observaciones no pueden exceder 1000 caracteres. ");
        }

        if (!validarReaccionAdversa()) {
            mensajes.append("Si hubo reacción adversa, debe describirse (máximo 500 caracteres). ");
        }

        if (this.fechaVencimiento != null && this.fechaAplicacion != null &&
                this.fechaAplicacion.isAfter(this.fechaVencimiento)) {
            mensajes.append("⚠️ La vacuna estaba vencida al momento de aplicación. ");
        }

        if (this.pesoAplicacion != null && (this.pesoAplicacion.compareTo(BigDecimal.ZERO) <= 0 ||
                this.pesoAplicacion.compareTo(new BigDecimal("200")) > 0)) {
            mensajes.append("El peso debe estar entre 0.1 y 200 kg. ");
        }

        if (this.tallaAplicacion != null && (this.tallaAplicacion.compareTo(BigDecimal.ZERO) <= 0 ||
                this.tallaAplicacion.compareTo(new BigDecimal("250")) > 0)) {
            mensajes.append("La talla debe estar entre 1 y 250 cm. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RegistroVacuna that = (RegistroVacuna) obj;
        return Objects.equals(id, that.id) &&
                Objects.equals(ninoId, that.ninoId) &&
                Objects.equals(vacunaId, that.vacunaId) &&
                Objects.equals(numeroDosis, that.numeroDosis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ninoId, vacunaId, numeroDosis);
    }

    @Override
    public String toString() {
        return "RegistroVacuna{" +
                "id=" + id +
                ", ninoId=" + ninoId +
                ", vacunaId=" + vacunaId +
                ", numeroDosis=" + numeroDosis +
                ", fechaAplicacion=" + fechaAplicacion +
                ", reaccionAdversa=" + reaccionAdversa +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return obtenerNombreVacuna() + " (Dosis " + this.numeroDosis + ") - " + this.fechaAplicacion;
    }

    /**
     * Representación para listas
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append(obtenerCodigoVacuna() != null ? obtenerCodigoVacuna() : obtenerNombreVacuna());
        display.append(" - D").append(this.numeroDosis);
        display.append(" (").append(this.fechaAplicacion).append(")");

        if (tuvoReaccionAdversa()) {
            display.append(" ⚠️");
        }

        return display.toString();
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un registro básico de vacuna
     * @param nino niño vacunado
     * @param vacuna vacuna aplicada
     * @param profesional profesional que aplicó
     * @param fechaAplicacion fecha de aplicación
     * @param numeroDosis número de dosis
     * @return RegistroVacuna configurado
     */
    public static RegistroVacuna crearBasico(Nino nino, Vacuna vacuna, ProfesionalEnfermeria profesional,
                                             LocalDate fechaAplicacion, Integer numeroDosis) {
        RegistroVacuna registro = new RegistroVacuna();
        registro.setNino(nino);
        registro.setVacuna(vacuna);
        registro.setProfesional(profesional);
        registro.setFechaAplicacion(fechaAplicacion);
        registro.setNumeroDosis(numeroDosis);
        return registro;
    }

    /**
     * Crea un registro completo con todos los datos
     * @param nino niño vacunado
     * @param vacuna vacuna aplicada
     * @param profesional profesional que aplicó
     * @param fechaAplicacion fecha de aplicación
     * @param numeroDosis número de dosis
     * @param loteVacuna lote de la vacuna
     * @param pesoAplicacion peso del niño al aplicar
     * @param tallaAplicacion talla del niño al aplicar
     * @return RegistroVacuna configurado
     */
    public static RegistroVacuna crearCompleto(Nino nino, Vacuna vacuna, ProfesionalEnfermeria profesional,
                                               LocalDate fechaAplicacion, Integer numeroDosis, String loteVacuna,
                                               BigDecimal pesoAplicacion, BigDecimal tallaAplicacion) {
        RegistroVacuna registro = crearBasico(nino, vacuna, profesional, fechaAplicacion, numeroDosis);
        registro.setLoteVacuna(loteVacuna);
        registro.setPesoAplicacion(pesoAplicacion);
        registro.setTallaAplicacion(tallaAplicacion);
        registro.setCentroSalud(profesional.getCentroSalud());
        return registro;
    }

    /**
     * Crea un registro con reacción adversa
     * @param nino niño vacunado
     * @param vacuna vacuna aplicada
     * @param profesional profesional que aplicó
     * @param fechaAplicacion fecha de aplicación
     * @param numeroDosis número de dosis
     * @param descripcionReaccion descripción de la reacción adversa
     * @return RegistroVacuna configurado con reacción adversa
     */
    public static RegistroVacuna crearConReaccionAdversa(Nino nino, Vacuna vacuna, ProfesionalEnfermeria profesional,
                                                         LocalDate fechaAplicacion, Integer numeroDosis,
                                                         String descripcionReaccion) {
        RegistroVacuna registro = crearBasico(nino, vacuna, profesional, fechaAplicacion, numeroDosis);
        registro.setReaccionAdversa(true);
        registro.setDescripcionReaccion(descripcionReaccion);
        return registro;
    }

    /**
     * Crea un registro de BCG para recién nacido
     * @param nino niño recién nacido
     * @param vacunaBCG vacuna BCG
     * @param profesional profesional que aplicó
     * @return RegistroVacuna para BCG al nacer
     */
    public static RegistroVacuna crearBCGRecienNacido(Nino nino, Vacuna vacunaBCG, ProfesionalEnfermeria profesional) {
        RegistroVacuna registro = crearBasico(nino, vacunaBCG, profesional, LocalDate.now(), 1);
        registro.setObservaciones("Aplicada según esquema nacional - Al nacer");
        return registro;
    }

    /**
     * Crea un registro de ejemplo para testing
     * @return RegistroVacuna de ejemplo
     */
    public static RegistroVacuna crearEjemplo() {
        RegistroVacuna registro = new RegistroVacuna();
        registro.setFechaAplicacion(LocalDate.now().minusDays(30));
        registro.setNumeroDosis(1);
        registro.setLoteVacuna("VAC-2024-001");
        registro.setFechaVencimiento(LocalDate.now().plusYears(1));
        registro.setPesoAplicacion(new BigDecimal("7.2"));
        registro.setTallaAplicacion(new BigDecimal("68"));
        registro.setReaccionAdversa(false);
        registro.setObservaciones("Aplicación sin complicaciones. Niño toleró bien el procedimiento.");
        return registro;
    }
}