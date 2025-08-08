package com.vacutrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Clase modelo para Notificacion
 * Representa las alertas y recordatorios de vacunación para padres de familia
 * Maneja diferentes tipos de notificaciones y estados
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class Notificacion {

    // Atributos de la clase
    private Integer id;
    private Integer ninoId;
    private Nino nino; // Relación con Nino
    private Integer vacunaId;
    private Vacuna vacuna; // Relación con Vacuna
    private Integer numeroDosis;
    private LocalDate fechaProgramada;
    private String tipoNotificacion; // PROXIMA, VENCIDA, RECORDATORIO
    private String estado; // PENDIENTE, ENVIADA, LEIDA, APLICADA
    private String mensaje;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaCreacion;
    private String vacunaNombre;

    public String getVacunaNombre() {
        return vacunaNombre;
    }

    public void setVacunaNombre(String vacunaNombre) {
        this.vacunaNombre = vacunaNombre;
    }

    public String getNinoNombreCompleto() {
        return ninoNombreCompleto;
    }

    public void setNinoNombreCompleto(String ninoNombreCompleto) {
        this.ninoNombreCompleto = ninoNombreCompleto;
    }

    private String ninoNombreCompleto;

    // Constantes para tipos de notificación
    public static final String TIPO_PROXIMA = "PROXIMA";
    public static final String TIPO_VENCIDA = "VENCIDA";
    public static final String TIPO_RECORDATORIO = "RECORDATORIO";
    public static final String TIPO_URGENTE = "URGENTE";

    // Constantes para estados
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ENVIADA = "ENVIADA";
    public static final String ESTADO_LEIDA = "LEIDA";
    public static final String ESTADO_APLICADA = "APLICADA";
    public static final String ESTADO_CANCELADA = "CANCELADA";

    // Constantes para períodos de notificación
    public static final int DIAS_NOTIFICACION_PROXIMA = 7; // 1 semana antes
    public static final int DIAS_NOTIFICACION_RECORDATORIO = 3; // 3 días antes
    public static final int DIAS_NOTIFICACION_URGENTE = 0; // El mismo día
    public static final int DIAS_VENCIMIENTO_ALERTA = 30; // 30 días después de vencida

    // Constructor vacío (requerido para JavaBeans)
    public Notificacion() {
        this.tipoNotificacion = TIPO_RECORDATORIO;
        this.estado = ESTADO_PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
        this.numeroDosis = 1;
    }

    // Constructor con parámetros básicos
    public Notificacion(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                        LocalDate fechaProgramada, String tipoNotificacion) {
        this();
        this.ninoId = ninoId;
        this.vacunaId = vacunaId;
        this.numeroDosis = numeroDosis;
        this.fechaProgramada = fechaProgramada;
        this.tipoNotificacion = tipoNotificacion;
        this.mensaje = generarMensajeAutomatico();
    }

    // Constructor con objetos relacionados
    public Notificacion(Nino nino, Vacuna vacuna, Integer numeroDosis,
                        LocalDate fechaProgramada, String tipoNotificacion) {
        this();
        this.nino = nino;
        this.ninoId = nino != null ? nino.getId() : null;
        this.vacuna = vacuna;
        this.vacunaId = vacuna != null ? vacuna.getId() : null;
        this.numeroDosis = numeroDosis;
        this.fechaProgramada = fechaProgramada;
        this.tipoNotificacion = tipoNotificacion;
        this.mensaje = generarMensajeAutomatico();
    }

    // Constructor completo
    public Notificacion(Integer id, Integer ninoId, Integer vacunaId, Integer numeroDosis,
                        LocalDate fechaProgramada, String tipoNotificacion, String estado,
                        String mensaje, LocalDateTime fechaEnvio, LocalDateTime fechaCreacion) {
        this.id = id;
        this.ninoId = ninoId;
        this.vacunaId = vacunaId;
        this.numeroDosis = numeroDosis;
        this.fechaProgramada = fechaProgramada;
        this.tipoNotificacion = tipoNotificacion;
        this.estado = estado;
        this.mensaje = mensaje;
        this.fechaEnvio = fechaEnvio;
        this.fechaCreacion = fechaCreacion;
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

    public Integer getNumeroDosis() {
        return numeroDosis;
    }

    public void setNumeroDosis(Integer numeroDosis) {
        this.numeroDosis = numeroDosis != null && numeroDosis > 0 ? numeroDosis : 1;
    }

    public LocalDate getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDate fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public String getTipoNotificacion() {
        return tipoNotificacion;
    }

    public void setTipoNotificacion(String tipoNotificacion) {
        this.tipoNotificacion = tipoNotificacion != null ? tipoNotificacion.toUpperCase() : TIPO_RECORDATORIO;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado != null ? estado.toUpperCase() : ESTADO_PENDIENTE;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje != null ? mensaje.trim() : null;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Verifica si es una notificación próxima
     * @return true si es próxima
     */
    public boolean esProxima() {
        return TIPO_PROXIMA.equals(this.tipoNotificacion);
    }

    /**
     * Verifica si es una notificación vencida
     * @return true si es vencida
     */
    public boolean esVencida() {
        return TIPO_VENCIDA.equals(this.tipoNotificacion);
    }

    /**
     * Verifica si es un recordatorio
     * @return true si es recordatorio
     */
    public boolean esRecordatorio() {
        return TIPO_RECORDATORIO.equals(this.tipoNotificacion);
    }

    /**
     * Verifica si es urgente
     * @return true si es urgente
     */
    public boolean esUrgente() {
        return TIPO_URGENTE.equals(this.tipoNotificacion);
    }

    /**
     * Verifica si está pendiente
     * @return true si está pendiente
     */
    public boolean estaPendiente() {
        return ESTADO_PENDIENTE.equals(this.estado);
    }

    /**
     * Verifica si fue enviada
     * @return true si fue enviada
     */
    public boolean fueEnviada() {
        return ESTADO_ENVIADA.equals(this.estado) || ESTADO_LEIDA.equals(this.estado) || ESTADO_APLICADA.equals(this.estado);
    }

    /**
     * Verifica si fue leída
     * @return true si fue leída
     */
    public boolean fueLeida() {
        return ESTADO_LEIDA.equals(this.estado) || ESTADO_APLICADA.equals(this.estado);
    }

    /**
     * Verifica si la vacuna fue aplicada
     * @return true si fue aplicada
     */
    public boolean fueAplicada() {
        return ESTADO_APLICADA.equals(this.estado);
    }

    /**
     * Verifica si está cancelada
     * @return true si está cancelada
     */
    public boolean estaCancelada() {
        return ESTADO_CANCELADA.equals(this.estado);
    }

    /**
     * Obtiene el nombre del niño
     * @return String con el nombre del niño
     */
    public String obtenerNombreNino() {
        // Priorizar el campo auxiliar del JOIN, luego el objeto
        if (this.ninoNombreCompleto != null) {
            return this.ninoNombreCompleto;
        }
        return this.nino != null ? this.nino.obtenerNombreCompleto() : "Niño no especificado";
    }

    /**
     * Obtiene el nombre de la vacuna
     * @return String con el nombre de la vacuna
     */
    public String obtenerNombreVacuna() {
        // Priorizar el campo auxiliar del JOIN, luego el objeto
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
        return this.vacuna != null ? this.vacuna.getCodigo() : null;
    }

    /**
     * Calcula los días hasta la fecha programada
     * @return número de días (negativo si ya pasó)
     */
    public long calcularDiasHastaFecha() {
        if (this.fechaProgramada == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), this.fechaProgramada);
    }

    /**
     * Calcula los días desde la fecha programada
     * @return número de días desde la fecha (0 si no ha llegado)
     */
    public long calcularDiasDesdeFecha() {
        if (this.fechaProgramada == null) {
            return 0;
        }

        long dias = ChronoUnit.DAYS.between(this.fechaProgramada, LocalDate.now());
        return Math.max(0, dias);
    }

    /**
     * Obtiene la edad del niño en la fecha programada
     * @return String con la edad formateada
     */
    public String obtenerEdadEnFechaProgramada() {
        if (this.nino == null || this.nino.getFechaNacimiento() == null || this.fechaProgramada == null) {
            return "Edad no disponible";
        }

        long diasEdad = this.nino.calcularEdadEnDias(this.fechaProgramada);

        if (diasEdad < 30) {
            return diasEdad + (diasEdad == 1 ? " día" : " días");
        } else if (diasEdad < 365) {
            long meses = diasEdad / 30;
            return meses + (meses == 1 ? " mes" : " meses");
        } else {
            long anos = diasEdad / 365;
            long mesesRestantes = (diasEdad % 365) / 30;
            StringBuilder edad = new StringBuilder();
            edad.append(anos).append(anos == 1 ? " año" : " años");
            if (mesesRestantes > 0) {
                edad.append(", ").append(mesesRestantes).append(mesesRestantes == 1 ? " mes" : " meses");
            }
            return edad.toString();
        }
    }

    /**
     * Genera mensaje automático basado en el tipo y estado
     * @return String con el mensaje generado
     */
    public String generarMensajeAutomatico() {
        if (this.nino == null || this.vacuna == null) {
            return "Recordatorio de vacunación pendiente";
        }

        StringBuilder mensaje = new StringBuilder();
        String nombreNino = this.nino.obtenerNombreCompleto();
        String nombreVacuna = this.vacuna.obtenerNombreDisplay();

        switch (this.tipoNotificacion != null ? this.tipoNotificacion : TIPO_RECORDATORIO) {
            case TIPO_PROXIMA:
                mensaje.append("🔔 Próxima vacuna para ").append(nombreNino);
                mensaje.append(": ").append(nombreVacuna);
                if (this.numeroDosis != null && this.numeroDosis > 1) {
                    mensaje.append(" (Dosis ").append(this.numeroDosis).append(")");
                }
                mensaje.append(" programada para el ").append(this.fechaProgramada);
                break;

            case TIPO_VENCIDA:
                mensaje.append("⚠️ Vacuna VENCIDA para ").append(nombreNino);
                mensaje.append(": ").append(nombreVacuna);
                if (this.numeroDosis != null && this.numeroDosis > 1) {
                    mensaje.append(" (Dosis ").append(this.numeroDosis).append(")");
                }
                mensaje.append(" debió aplicarse el ").append(this.fechaProgramada);
                mensaje.append(". ¡Agende su cita lo antes posible!");
                break;

            case TIPO_URGENTE:
                mensaje.append("🚨 URGENTE: Vacuna para ").append(nombreNino);
                mensaje.append(": ").append(nombreVacuna);
                if (this.numeroDosis != null && this.numeroDosis > 1) {
                    mensaje.append(" (Dosis ").append(this.numeroDosis).append(")");
                }
                mensaje.append(" debe aplicarse HOY (").append(this.fechaProgramada).append(")");
                break;

            default: // TIPO_RECORDATORIO
                mensaje.append("📅 Recordatorio para ").append(nombreNino);
                mensaje.append(": ").append(nombreVacuna);
                if (this.numeroDosis != null && this.numeroDosis > 1) {
                    mensaje.append(" (Dosis ").append(this.numeroDosis).append(")");
                }
                mensaje.append(" programada para el ").append(this.fechaProgramada);
                mensaje.append(". Edad recomendada: ").append(obtenerEdadEnFechaProgramada());
                break;
        }

        return mensaje.toString();
    }

    /**
     * Actualiza el mensaje automáticamente
     */
    public void actualizarMensaje() {
        this.mensaje = generarMensajeAutomatico();
    }

    /**
     * Marca la notificación como enviada
     */
    public void marcarComoEnviada() {
        this.estado = ESTADO_ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
    }

    /**
     * Marca la notificación como leída
     */
    public void marcarComoLeida() {
        if (fueEnviada()) {
            this.estado = ESTADO_LEIDA;
        }
    }

    /**
     * Marca la notificación como aplicada (vacuna completada)
     */
    public void marcarComoAplicada() {
        this.estado = ESTADO_APLICADA;
    }

    /**
     * Cancela la notificación
     */
    public void cancelar() {
        this.estado = ESTADO_CANCELADA;
    }

    /**
     * Obtiene el nivel de prioridad de la notificación
     * @return int con el nivel (1=baja, 2=media, 3=alta, 4=crítica)
     */
    public int obtenerNivelPrioridad() {
        switch (this.tipoNotificacion != null ? this.tipoNotificacion : TIPO_RECORDATORIO) {
            case TIPO_URGENTE:
                return 4; // Crítica
            case TIPO_VENCIDA:
                return 3; // Alta
            case TIPO_PROXIMA:
                return 2; // Media
            default: // RECORDATORIO
                return 1; // Baja
        }
    }

    /**
     * Obtiene el texto de prioridad
     * @return String con la prioridad
     */
    public String obtenerTextoPrioridad() {
        switch (obtenerNivelPrioridad()) {
            case 4: return "CRÍTICA";
            case 3: return "ALTA";
            case 2: return "MEDIA";
            default: return "BAJA";
        }
    }

    /**
     * Obtiene el color para mostrar en UI
     * @return String con código de color CSS
     */
    public String obtenerColorUI() {
        switch (this.tipoNotificacion != null ? this.tipoNotificacion : TIPO_RECORDATORIO) {
            case TIPO_URGENTE:
                return "#dc3545"; // Rojo crítico
            case TIPO_VENCIDA:
                return "#fd7e14"; // Naranja alerta
            case TIPO_PROXIMA:
                return "#0d6efd"; // Azul información
            default: // RECORDATORIO
                return "#198754"; // Verde normal
        }
    }

    /**
     * Obtiene el ícono para mostrar en UI
     * @return String con el ícono
     */
    public String obtenerIconoUI() {
        switch (this.tipoNotificacion != null ? this.tipoNotificacion : TIPO_RECORDATORIO) {
            case TIPO_URGENTE:
                return "🚨";
            case TIPO_VENCIDA:
                return "⚠️";
            case TIPO_PROXIMA:
                return "🔔";
            default: // RECORDATORIO
                return "📅";
        }
    }

    /**
     * Verifica si la notificación debe ser mostrada
     * @return true si debe mostrarse
     */
    public boolean debeSerMostrada() {
        return !estaCancelada() && !fueAplicada();
    }

    /**
     * Verifica si la notificación está activa
     * @return true si está activa
     */
    public boolean estaActiva() {
        return debeSerMostrada() && (estaPendiente() || fueEnviada() || fueLeida());
    }

    /**
     * Obtiene información completa de la notificación
     * @return String con toda la información
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();

        info.append("Notificación ID: ").append(this.id).append("\n");
        info.append("Niño: ").append(obtenerNombreNino()).append("\n");
        info.append("Vacuna: ").append(obtenerNombreVacuna());
        if (this.numeroDosis != null && this.numeroDosis > 1) {
            info.append(" (Dosis ").append(this.numeroDosis).append(")");
        }
        info.append("\n");
        info.append("Fecha programada: ").append(this.fechaProgramada).append("\n");
        info.append("Tipo: ").append(this.tipoNotificacion).append("\n");
        info.append("Estado: ").append(this.estado).append("\n");
        info.append("Prioridad: ").append(obtenerTextoPrioridad()).append("\n");

        long diasHasta = calcularDiasHastaFecha();
        if (diasHasta > 0) {
            info.append("Días restantes: ").append(diasHasta).append("\n");
        } else if (diasHasta < 0) {
            info.append("Días de retraso: ").append(Math.abs(diasHasta)).append("\n");
        } else {
            info.append("¡Es hoy!\n");
        }

        info.append("Creada: ").append(this.fechaCreacion).append("\n");

        if (this.fechaEnvio != null) {
            info.append("Enviada: ").append(this.fechaEnvio).append("\n");
        }

        if (this.mensaje != null) {
            info.append("Mensaje: ").append(this.mensaje);
        }

        return info.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos de la notificación son válidos
     * @return true si los datos son válidos
     */
    public boolean esValida() {
        return this.ninoId != null &&
                this.vacunaId != null &&
                this.numeroDosis != null && this.numeroDosis > 0 &&
                this.fechaProgramada != null &&
                validarTipoNotificacion() &&
                validarEstado() &&
                validarMensaje();
    }

    /**
     * Valida el tipo de notificación
     * @return true si el tipo es válido
     */
    public boolean validarTipoNotificacion() {
        return TIPO_PROXIMA.equals(this.tipoNotificacion) ||
                TIPO_VENCIDA.equals(this.tipoNotificacion) ||
                TIPO_RECORDATORIO.equals(this.tipoNotificacion) ||
                TIPO_URGENTE.equals(this.tipoNotificacion);
    }

    /**
     * Valida el estado de la notificación
     * @return true si el estado es válido
     */
    public boolean validarEstado() {
        return ESTADO_PENDIENTE.equals(this.estado) ||
                ESTADO_ENVIADA.equals(this.estado) ||
                ESTADO_LEIDA.equals(this.estado) ||
                ESTADO_APLICADA.equals(this.estado) ||
                ESTADO_CANCELADA.equals(this.estado);
    }

    /**
     * Valida el mensaje
     * @return true si el mensaje es válido
     */
    public boolean validarMensaje() {
        return this.mensaje != null && !this.mensaje.trim().isEmpty() && this.mensaje.length() <= 1000;
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válida
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.ninoId == null) {
            mensajes.append("El niño es requerido. ");
        }

        if (this.vacunaId == null) {
            mensajes.append("La vacuna es requerida. ");
        }

        if (this.numeroDosis == null || this.numeroDosis <= 0) {
            mensajes.append("El número de dosis debe ser mayor a 0. ");
        }

        if (this.fechaProgramada == null) {
            mensajes.append("La fecha programada es requerida. ");
        }

        if (!validarTipoNotificacion()) {
            mensajes.append("El tipo de notificación no es válido. ");
        }

        if (!validarEstado()) {
            mensajes.append("El estado de la notificación no es válido. ");
        }

        if (!validarMensaje()) {
            mensajes.append("El mensaje es requerido y no puede exceder 1000 caracteres. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Notificacion that = (Notificacion) obj;
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
        return "Notificacion{" +
                "id=" + id +
                ", ninoId=" + ninoId +
                ", vacunaId=" + vacunaId +
                ", numeroDosis=" + numeroDosis +
                ", fechaProgramada=" + fechaProgramada +
                ", tipoNotificacion='" + tipoNotificacion + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return obtenerIconoUI() + " " + obtenerNombreVacuna() +
                " (Dosis " + this.numeroDosis + ") - " + this.fechaProgramada;
    }

    /**
     * Representación para listas de notificaciones
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append(obtenerIconoUI()).append(" ");
        display.append(obtenerNombreNino()).append(": ");
        display.append(obtenerNombreVacuna());

        if (this.numeroDosis != null && this.numeroDosis > 1) {
            display.append(" (D").append(this.numeroDosis).append(")");
        }

        display.append(" - ").append(this.fechaProgramada);

        long diasHasta = calcularDiasHastaFecha();
        if (diasHasta > 0) {
            display.append(" (en ").append(diasHasta).append(" días)");
        } else if (diasHasta < 0) {
            display.append(" (").append(Math.abs(diasHasta)).append(" días de retraso)");
        } else {
            display.append(" (HOY)");
        }

        return display.toString();
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea una notificación próxima
     * @param nino niño que recibirá la vacuna
     * @param vacuna vacuna a aplicar
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha programada
     * @return Notificacion configurada como próxima
     */
    public static Notificacion crearProxima(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaProgramada) {
        return new Notificacion(nino, vacuna, numeroDosis, fechaProgramada, TIPO_PROXIMA);
    }

    /**
     * Crea una notificación de recordatorio
     * @param nino niño que recibirá la vacuna
     * @param vacuna vacuna a aplicar
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha programada
     * @return Notificacion configurada como recordatorio
     */
    public static Notificacion crearRecordatorio(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaProgramada) {
        return new Notificacion(nino, vacuna, numeroDosis, fechaProgramada, TIPO_RECORDATORIO);
    }

    /**
     * Crea una notificación vencida
     * @param nino niño que debió recibir la vacuna
     * @param vacuna vacuna que debió aplicarse
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha que ya pasó
     * @return Notificacion configurada como vencida
     */
    public static Notificacion crearVencida(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaProgramada) {
        return new Notificacion(nino, vacuna, numeroDosis, fechaProgramada, TIPO_VENCIDA);
    }

    /**
     * Crea una notificación urgente
     * @param nino niño que debe recibir la vacuna hoy
     * @param vacuna vacuna a aplicar
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha de hoy
     * @return Notificacion configurada como urgente
     */
    public static Notificacion crearUrgente(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaProgramada) {
        return new Notificacion(nino, vacuna, numeroDosis, fechaProgramada, TIPO_URGENTE);
    }

    /**
     * Crea una notificación automática basada en la fecha programada
     * @param nino niño que recibirá la vacuna
     * @param vacuna vacuna a aplicar
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha programada
     * @return Notificacion con tipo automático según la fecha
     */
    public static Notificacion crearAutomatica(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaProgramada) {
        long diasHasta = ChronoUnit.DAYS.between(LocalDate.now(), fechaProgramada);

        String tipo;
        if (diasHasta < 0) {
            // Ya pasó la fecha
            tipo = TIPO_VENCIDA;
        } else if (diasHasta == 0) {
            // Es hoy
            tipo = TIPO_URGENTE;
        } else if (diasHasta <= DIAS_NOTIFICACION_RECORDATORIO) {
            // Faltan pocos días
            tipo = TIPO_PROXIMA;
        } else {
            // Aún falta tiempo
            tipo = TIPO_RECORDATORIO;
        }

        return new Notificacion(nino, vacuna, numeroDosis, fechaProgramada, tipo);
    }

    /**
     * Crea notificaciones para todo el esquema de un niño
     * @param nino niño para el que crear notificaciones
     * @param esquemas lista de esquemas de vacunación
     * @return Lista de notificaciones creadas
     */
    public static java.util.List<Notificacion> crearParaEsquemaCompleto(Nino nino, java.util.List<EsquemaVacunacion> esquemas) {
        java.util.List<Notificacion> notificaciones = new java.util.ArrayList<>();

        if (nino == null || nino.getFechaNacimiento() == null || esquemas == null) {
            return notificaciones;
        }

        for (EsquemaVacunacion esquema : esquemas) {
            if (esquema.estaActivo()) {
                LocalDate fechaProgramada = esquema.calcularFechaProgramada(nino.getFechaNacimiento());
                if (fechaProgramada != null) {
                    Notificacion notificacion = crearAutomatica(
                            nino,
                            esquema.getVacuna(),
                            esquema.getNumeroDosis(),
                            fechaProgramada
                    );
                    notificaciones.add(notificacion);
                }
            }
        }

        return notificaciones;
    }

    /**
     * Crea una notificación de ejemplo para testing
     * @return Notificacion de ejemplo
     */
    public static Notificacion crearEjemplo() {
        Notificacion notificacion = new Notificacion();
        notificacion.setNumeroDosis(1);
        notificacion.setFechaProgramada(LocalDate.now().plusDays(7));
        notificacion.setTipoNotificacion(TIPO_PROXIMA);
        notificacion.setEstado(ESTADO_PENDIENTE);
        notificacion.setMensaje("Recordatorio de vacunación - Segunda dosis de Pentavalente programada para la próxima semana.");
        return notificacion;
    }

    /**
     * Actualiza automáticamente el tipo de notificación según la fecha actual
     */
    public void actualizarTipoAutomatico() {
        if (this.fechaProgramada == null) {
            return;
        }

        long diasHasta = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaProgramada);
        String tipoAnterior = this.tipoNotificacion;

        if (diasHasta < 0) {
            this.tipoNotificacion = TIPO_VENCIDA;
        } else if (diasHasta == 0) {
            this.tipoNotificacion = TIPO_URGENTE;
        } else if (diasHasta <= DIAS_NOTIFICACION_RECORDATORIO) {
            this.tipoNotificacion = TIPO_PROXIMA;
        } else if (diasHasta <= DIAS_NOTIFICACION_PROXIMA) {
            this.tipoNotificacion = TIPO_RECORDATORIO;
        }

        // Si cambió el tipo, regenerar mensaje
        if (!Objects.equals(tipoAnterior, this.tipoNotificacion)) {
            actualizarMensaje();
        }
    }
}