package com.vacutrack.service;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de notificaciones simplificado para VACU-TRACK
 * Maneja recordatorios básicos de vacunación para padres de familia
 * Versión estudiantil - funcionalidades esenciales
 *
 * @author VACU-TRACK Team
 * @version 1.0 - Simplificado
 */
public class NotificacionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionService.class);

    // Instancia singleton
    private static NotificacionService instance;

    // DAOs necesarios
    private final NotificacionDAO notificacionDAO;
    private final NinoDAO ninoDAO;
    private final VacunaDAO vacunaDAO;
    private final EsquemaVacunacionDAO esquemaDAO;

    // Configuración básica de días para notificaciones
    private static final int DIAS_RECORDATORIO_PREVIO = 7;  // 1 semana antes
    private static final int DIAS_RECORDATORIO_URGENTE = 1; // 1 día antes

    /**
     * Constructor privado para patrón singleton
     */
    private NotificacionService() {
        this.notificacionDAO = NotificacionDAO.getInstance();
        this.ninoDAO = NinoDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.esquemaDAO = EsquemaVacunacionDAO.getInstance();
    }

    /**
     * Obtiene la instancia singleton del servicio
     */
    public static synchronized NotificacionService getInstance() {
        if (instance == null) {
            instance = new NotificacionService();
        }
        return instance;
    }

    /**
     * Procesa notificaciones automáticas básicas
     * Genera recordatorios y alertas de vacunas vencidas
     */
    public int procesarNotificacionesAutomaticas() {
        logger.info("Iniciando procesamiento automático de notificaciones");

        int notificacionesCreadas = 0;

        try {
            // 1. Procesar recordatorios de vacunas próximas
            notificacionesCreadas += procesarRecordatoriosProximos();

            // 2. Procesar alertas de vacunas vencidas
            notificacionesCreadas += procesarAlertasVencidas();

            // 3. Actualizar estados automáticamente
            notificacionDAO.procesarNotificacionesAutomaticas();

            logger.info("Procesamiento completado: {} notificaciones creadas", notificacionesCreadas);

        } catch (Exception e) {
            logger.error("Error en procesamiento automático de notificaciones", e);
        }

        return notificacionesCreadas;
    }

    /**
     * Obtiene notificaciones activas para un niño específico
     */
    public List<Notificacion> getNotificacionesPorNino(Integer ninoId) {
        if (ninoId == null) {
            return new ArrayList<>();
        }

        try {
            return notificacionDAO.findByNino(ninoId);
        } catch (Exception e) {
            logger.error("Error al obtener notificaciones para niño: " + ninoId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Crea una notificación de recordatorio manual
     */
    public Notificacion crearRecordatorio(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                                          LocalDate fechaProgramada) {
        if (ninoId == null || vacunaId == null || fechaProgramada == null) {
            logger.warn("Parámetros inválidos para crear recordatorio");
            return null;
        }

        try {
            // Verificar que no existe ya una notificación similar
            if (notificacionDAO.existsNotificacion(ninoId, vacunaId, numeroDosis)) {
                logger.debug("Ya existe notificación para niño {} - vacuna {}", ninoId, vacunaId);
                return null;
            }

            return notificacionDAO.crearRecordatorioVacuna(ninoId, vacunaId, numeroDosis, fechaProgramada);

        } catch (Exception e) {
            logger.error("Error al crear recordatorio", e);
            return null;
        }
    }

    /**
     * Crea una notificación de vacuna vencida
     */
    public Notificacion crearAlertaVencida(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                                           LocalDate fechaVencida) {
        if (ninoId == null || vacunaId == null || fechaVencida == null) {
            logger.warn("Parámetros inválidos para crear alerta vencida");
            return null;
        }

        try {
            return notificacionDAO.crearAlertaVacunaVencida(ninoId, vacunaId, numeroDosis, fechaVencida);

        } catch (Exception e) {
            logger.error("Error al crear alerta vencida", e);
            return null;
        }
    }

    /**
     * Marca una notificación como leída
     */
    public boolean marcarComoLeida(Integer notificacionId) {
        if (notificacionId == null) {
            return false;
        }

        try {
            return notificacionDAO.markAsRead(notificacionId);
        } catch (Exception e) {
            logger.error("Error al marcar notificación como leída: " + notificacionId, e);
            return false;
        }
    }

    /**
     * Marca una notificación como aplicada (vacuna completada)
     */
    public boolean marcarComoAplicada(Integer notificacionId) {
        if (notificacionId == null) {
            return false;
        }

        try {
            return notificacionDAO.markAsAplicada(notificacionId);
        } catch (Exception e) {
            logger.error("Error al marcar notificación como aplicada: " + notificacionId, e);
            return false;
        }
    }

    /**
     * Obtiene notificaciones para enviar hoy
     */
    public List<Notificacion> getNotificacionesParaEnviarHoy() {
        try {
            return notificacionDAO.findPendientes(LocalDate.now());
        } catch (Exception e) {
            logger.error("Error al obtener notificaciones para enviar hoy", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene estadísticas básicas de notificaciones
     */
    public Map<String, Integer> getEstadisticasBasicas() {
        Map<String, Integer> stats = new HashMap<>();

        try {
            stats.put("pendientes", (int) notificacionDAO.countByEstado(NotificacionDAO.ESTADO_PENDIENTE));
            stats.put("enviadas", (int) notificacionDAO.countByEstado(NotificacionDAO.ESTADO_ENVIADA));
            stats.put("leidas", (int) notificacionDAO.countByEstado(NotificacionDAO.ESTADO_LEIDA));
            stats.put("aplicadas", (int) notificacionDAO.countByEstado(NotificacionDAO.ESTADO_APLICADA));

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas", e);
        }

        return stats;
    }

    /**
     * Genera notificaciones para un niño basado en su esquema de vacunación
     */
    public List<Notificacion> generarNotificacionesParaNino(Integer ninoId) {
        List<Notificacion> notificacionesCreadas = new ArrayList<>();

        if (ninoId == null) {
            return notificacionesCreadas;
        }

        try {
            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return notificacionesCreadas;
            }

            Nino nino = ninoOpt.get();
            LocalDate fechaNacimiento = nino.getFechaNacimiento();

            // Obtener esquema de vacunación activo
            List<EsquemaVacunacion> esquemas = esquemaDAO.findActive();

            for (EsquemaVacunacion esquema : esquemas) {
                try {
                    // Calcular fecha programada para esta vacuna
                    LocalDate fechaProgramada = fechaNacimiento.plusDays(esquema.getEdadAplicacionDias());

                    // Solo crear notificaciones para fechas futuras o recientes
                    LocalDate hoy = LocalDate.now();
                    LocalDate fechaMinima = hoy.minusDays(30); // No más de 30 días atrás
                    LocalDate fechaMaxima = hoy.plusDays(365); // No más de 1 año adelante

                    if (fechaProgramada.isAfter(fechaMinima) && fechaProgramada.isBefore(fechaMaxima)) {

                        // Verificar que no existe ya esta notificación
                        if (!notificacionDAO.existsNotificacion(ninoId, esquema.getVacunaId(), esquema.getNumeroDosis())) {

                            Notificacion notificacion;

                            if (fechaProgramada.isBefore(hoy)) {
                                // Vacuna vencida
                                notificacion = notificacionDAO.crearAlertaVacunaVencida(
                                        ninoId, esquema.getVacunaId(), esquema.getNumeroDosis(), fechaProgramada);
                            } else {
                                // Recordatorio o próxima
                                notificacion = notificacionDAO.crearRecordatorioVacuna(
                                        ninoId, esquema.getVacunaId(), esquema.getNumeroDosis(), fechaProgramada);
                            }

                            if (notificacion != null) {
                                notificacionesCreadas.add(notificacion);
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error procesando esquema {} para niño {}", esquema.getId(), ninoId, e);
                }
            }

        } catch (Exception e) {
            logger.error("Error generando notificaciones para niño: " + ninoId, e);
        }

        logger.info("Generadas {} notificaciones para niño {}", notificacionesCreadas.size(), ninoId);
        return notificacionesCreadas;
    }

    /**
     * Busca notificaciones por fecha específica
     */
    public List<Notificacion> getNotificacionesPorFecha(LocalDate fecha) {
        if (fecha == null) {
            return new ArrayList<>();
        }

        try {
            return notificacionDAO.findByFechaProgramada(fecha);
        } catch (Exception e) {
            logger.error("Error al buscar notificaciones por fecha: " + fecha, e);
            return new ArrayList<>();
        }
    }

    /**
     * Busca notificaciones en un rango de fechas
     */
    public List<Notificacion> getNotificacionesPorRango(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return new ArrayList<>();
        }

        try {
            return notificacionDAO.findByFechaRange(fechaInicio, fechaFin);
        } catch (Exception e) {
            logger.error("Error al buscar notificaciones en rango: {} - {}", fechaInicio, fechaFin, e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene notificaciones más recientes
     */
    public List<Notificacion> getNotificacionesRecientes(int limite) {
        try {
            return notificacionDAO.findRecientes(limite > 0 ? limite : 10);
        } catch (Exception e) {
            logger.error("Error al obtener notificaciones recientes", e);
            return new ArrayList<>();
        }
    }

    /**
     * Elimina notificaciones obsoletas
     */
    public int limpiarNotificacionesObsoletas() {
        try {
            return notificacionDAO.cleanupNotificacionesAplicadas(90); // Eliminar aplicadas de más de 3 meses
        } catch (Exception e) {
            logger.error("Error al limpiar notificaciones obsoletas", e);
            return 0;
        }
    }

    // ==================== MÉTODOS PRIVADOS DE APOYO ====================

    /**
     * Procesa recordatorios de vacunas próximas
     */
    private int procesarRecordatoriosProximos() {
        int recordatorios = 0;

        try {
            List<Nino> ninos = ninoDAO.findActiveNinos();
            LocalDate hoy = LocalDate.now();
            LocalDate fechaLimite = hoy.plusDays(DIAS_RECORDATORIO_PREVIO);

            for (Nino nino : ninos) {
                try {
                    List<EsquemaVacunacion> esquemas = esquemaDAO.findActive();

                    for (EsquemaVacunacion esquema : esquemas) {
                        LocalDate fechaProgramada = nino.getFechaNacimiento().plusDays(esquema.getEdadAplicacionDias());

                        // Solo crear recordatorios para vacunas en los próximos días
                        if (fechaProgramada.isAfter(hoy) && !fechaProgramada.isAfter(fechaLimite)) {

                            if (!notificacionDAO.existsNotificacion(nino.getId(), esquema.getVacunaId(), esquema.getNumeroDosis())) {
                                Notificacion notificacion = notificacionDAO.crearRecordatorioVacuna(
                                        nino.getId(), esquema.getVacunaId(), esquema.getNumeroDosis(), fechaProgramada);

                                if (notificacion != null) {
                                    recordatorios++;
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error procesando recordatorios para niño: " + nino.getId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error en procesamiento de recordatorios", e);
        }

        return recordatorios;
    }

    /**
     * Procesa alertas de vacunas vencidas
     */
    private int procesarAlertasVencidas() {
        int alertas = 0;

        try {
            List<Nino> ninos = ninoDAO.findActiveNinos();
            LocalDate hoy = LocalDate.now();

            for (Nino nino : ninos) {
                try {
                    List<EsquemaVacunacion> esquemas = esquemaDAO.findActive();

                    for (EsquemaVacunacion esquema : esquemas) {
                        LocalDate fechaProgramada = nino.getFechaNacimiento().plusDays(esquema.getEdadAplicacionDias());

                        // Solo crear alertas para vacunas que ya debieron aplicarse
                        if (fechaProgramada.isBefore(hoy)) {

                            if (!notificacionDAO.existsNotificacion(nino.getId(), esquema.getVacunaId(), esquema.getNumeroDosis())) {
                                Notificacion notificacion = notificacionDAO.crearAlertaVacunaVencida(
                                        nino.getId(), esquema.getVacunaId(), esquema.getNumeroDosis(), fechaProgramada);

                                if (notificacion != null) {
                                    alertas++;
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error procesando alertas para niño: " + nino.getId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error en procesamiento de alertas", e);
        }

        return alertas;
    }

    /**
     * Calcula el tipo de notificación según los días hasta la fecha
     */
    private String calcularTipoNotificacion(long diasHasta) {
        if (diasHasta < 0) {
            return NotificacionDAO.TIPO_VENCIDA;
        } else if (diasHasta <= DIAS_RECORDATORIO_URGENTE) {
            return NotificacionDAO.TIPO_PROXIMA;
        } else {
            return NotificacionDAO.TIPO_RECORDATORIO;
        }
    }

    /**
     * Valida parámetros básicos para crear notificaciones
     */
    private boolean validarParametros(Integer ninoId, Integer vacunaId, LocalDate fecha) {
        return ninoId != null && vacunaId != null && fecha != null;
    }

    /**
     * Obtiene información de estadísticas completas para reporting
     */
    public String getReporteEstadisticas() {
        StringBuilder reporte = new StringBuilder();

        try {
            Map<String, Integer> stats = getEstadisticasBasicas();

            reporte.append("=== REPORTE DE NOTIFICACIONES ===\n");
            reporte.append("Fecha: ").append(LocalDate.now()).append("\n\n");

            reporte.append("ESTADO DE NOTIFICACIONES:\n");
            reporte.append("- Pendientes: ").append(stats.getOrDefault("pendientes", 0)).append("\n");
            reporte.append("- Enviadas: ").append(stats.getOrDefault("enviadas", 0)).append("\n");
            reporte.append("- Leídas: ").append(stats.getOrDefault("leidas", 0)).append("\n");
            reporte.append("- Aplicadas: ").append(stats.getOrDefault("aplicadas", 0)).append("\n");

            int total = stats.values().stream().mapToInt(Integer::intValue).sum();
            reporte.append("- TOTAL: ").append(total).append("\n\n");

            // Notificaciones para hoy
            List<Notificacion> hoy = getNotificacionesParaEnviarHoy();
            reporte.append("NOTIFICACIONES PARA HOY: ").append(hoy.size()).append("\n");

            // Notificaciones recientes
            List<Notificacion> recientes = getNotificacionesRecientes(5);
            reporte.append("ÚLTIMAS 5 NOTIFICACIONES:\n");
            for (Notificacion n : recientes) {
                reporte.append("- ").append(n.getFechaProgramada())
                        .append(" | ").append(n.getTipoNotificacion())
                        .append(" | ").append(n.getEstado()).append("\n");
            }

        } catch (Exception e) {
            logger.error("Error generando reporte de estadísticas", e);
            reporte.append("Error al generar reporte: ").append(e.getMessage());
        }

        return reporte.toString();
    }

    /**
     * Método de utilidad para debugging y testing
     */
    public void mostrarEstadisticas() {
        System.out.println(getReporteEstadisticas());
    }
}