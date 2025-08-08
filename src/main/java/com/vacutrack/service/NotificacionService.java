package com.vacutrack.service;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;
import com.vacutrack.util.DateUtil;
import com.vacutrack.util.ValidationUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de notificaciones inteligente
 * Maneja recordatorios automáticos, alertas de vacunas vencidas,
 * notificaciones de cumpleaños y sistema de prioridades
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class NotificacionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionService.class);
    
    // Instancia singleton
    private static NotificacionService instance;
    
    // DAOs
    private final NotificacionDAO notificacionDAO;
    private final NinoDAO ninoDAO;
    private final VacunaDAO vacunaDAO;
    private final RegistroVacunaDAO registroVacunaDAO;
    private final EsquemaVacunacionDAO esquemaDAO;
    private final PadreFamiliaDAO padreFamiliaDAO;
    
    // Configuración de notificaciones
    private static final int DIAS_RECORDATORIO_PREVIO = 7;
    private static final int DIAS_RECORDATORIO_URGENTE = 1;
    private static final int DIAS_TOLERANCIA_VENCIMIENTO = 30;
    private static final int DIAS_ADELANTE_CUMPLEANOS = 3;
    
    /**
     * Constructor privado para patrón singleton
     */
    private NotificacionService() {
        this.notificacionDAO = NotificacionDAO.getInstance();
        this.ninoDAO = NinoDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.registroVacunaDAO = RegistroVacunaDAO.getInstance();
        this.esquemaDAO = EsquemaVacunacionDAO.getInstance();
        this.padreFamiliaDAO = PadreFamiliaDAO.getInstance();
    }
    
    /**
     * Obtiene la instancia singleton del servicio
     * @return instancia de NotificacionService
     */
    public static synchronized NotificacionService getInstance() {
        if (instance == null) {
            instance = new NotificacionService();
        }
        return instance;
    }
    
    /**
     * Procesa todas las notificaciones automáticas del sistema
     * Este método debe ejecutarse diariamente
     * @return resumen del procesamiento
     */
    public ResumenProcesamiento procesarNotificacionesAutomaticas() {
        logger.info("Iniciando procesamiento automático de notificaciones");
        
        ResumenProcesamiento resumen = new ResumenProcesamiento();
        resumen.setFechaProcesamiento(LocalDateTime.now());
        
        try {
            // 1. Procesar recordatorios de vacunas
            int recordatorios = procesarRecordatoriosVacunas();
            resumen.setRecordatoriosCreados(recordatorios);
            
            // 2. Procesar alertas de vacunas vencidas
            int alertasVencidas = procesarAlertasVacunasVencidas();
            resumen.setAlertasVencidasCreadas(alertasVencidas);
            
            // 3. Procesar notificaciones de cumpleaños
            int cumpleanos = procesarNotificacionesCumpleanos();
            resumen.setCumpleanosCreados(cumpleanos);
            
            // 4. Actualizar estados de notificaciones vencidas
            int vencidas = notificacionDAO.procesarNotificacionesVencidas();
            resumen.setNotificacionesVencidas(vencidas);
            
            // 5. Procesar esquemas completados
            int esquemas = procesarEsquemasCompletados();
            resumen.setEsquemasCompletadosNotificados(esquemas);
            
            logger.info("Procesamiento completado: {} recordatorios, {} alertas, {} cumpleaños", 
                recordatorios, alertasVencidas, cumpleanos);
            
            resumen.setExitoso(true);
            
        } catch (Exception e) {
            logger.error("Error en procesamiento automático de notificaciones", e);
            resumen.setExitoso(false);
            resumen.setMensajeError(e.getMessage());
        }
        
        return resumen;
    }
    
    /**
     * Programa un recordatorio de vacuna
     * @param usuarioId ID del padre de familia
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param fechaProgramada fecha programada para la vacuna
     * @return notificación creada o null si falló
     */
    public Notificacion programarRecordatorioVacuna(Integer usuarioId, Integer ninoId, 
                                                   Integer vacunaId, LocalDateTime fechaProgramada) {
        logger.debug("Programando recordatorio de vacuna para niño {} - vacuna {}", ninoId, vacunaId);
        
        try {
            // Verificar que no existe ya un recordatorio similar
            List<Notificacion> existentes = notificacionDAO.findByNino(ninoId).stream()
                .filter(n -> n.getVacunaId() != null && n.getVacunaId().equals(vacunaId))
                .filter(n -> NotificacionDAO.TIPO_RECORDATORIO_VACUNA.equals(n.getTipoNotificacion()))
                .filter(n -> NotificacionDAO.ESTADO_PENDIENTE.equals(n.getEstado()))
                .toList();
            
            if (!existentes.isEmpty()) {
                logger.debug("Ya existe recordatorio para esta vacuna, niño {} - vacuna {}", ninoId, vacunaId);
                return existentes.get(0);
            }
            
            // Obtener información del niño y vacuna
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            
            if (ninoOpt.isEmpty() || vacunaOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            Vacuna vacuna = vacunaOpt.get();
            
            // Crear notificación
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuarioId);
            notificacion.setNinoId(ninoId);
            notificacion.setVacunaId(vacunaId);
            notificacion.setTipoNotificacion(NotificacionDAO.TIPO_RECORDATORIO_VACUNA);
            notificacion.setTitulo("Recordatorio de Vacunación");
            
            String mensaje = String.format("Es momento de aplicar la vacuna %s a %s %s. " +
                "Fecha recomendada: %s. No olvides agendar tu cita en el centro de salud más cercano.",
                vacuna.getNombre(), nino.getNombres(), nino.getApellidos(), 
                DateUtil.formatearFecha(fechaProgramada.toLocalDate()));
            notificacion.setMensaje(mensaje);
            
            // Calcular prioridad según proximidad
            long diasHasta = ChronoUnit.DAYS.between(LocalDateTime.now(), fechaProgramada);
            String prioridad = calcularPrioridadPorDias(diasHasta, true);
            notificacion.setPrioridad(prioridad);
            
            notificacion.setEstado(NotificacionDAO.ESTADO_PENDIENTE);
            notificacion.setFechaProgramada(fechaProgramada.minusDays(DIAS_RECORDATORIO_PREVIO));
            notificacion.setFechaVencimiento(fechaProgramada.plusDays(DIAS_TOLERANCIA_VENCIMIENTO));
            notificacion.setActivo(true);
            notificacion.setFechaCreacion(LocalDateTime.now());
            
            if (notificacionDAO.insert(notificacion)) {
                logger.info("Recordatorio programado: {} para {} {} en {}", 
                    vacuna.getNombre(), nino.getNombres(), nino.getApellidos(), fechaProgramada.toLocalDate());
                return notificacion;
            }
            
        } catch (Exception e) {
            logger.error("Error al programar recordatorio de vacuna", e);
        }
        
        return null;
    }
    
    /**
     * Crea una notificación de alerta por vacuna vencida
     * @param usuarioId ID del padre de familia
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna vencida
     * @param diasVencida días que ha estado vencida
     * @return notificación creada o null si falló
     */
    public Notificacion crearAlertaVacunaVencida(Integer usuarioId, Integer ninoId, 
                                               Integer vacunaId, int diasVencida) {
        logger.info("Creando alerta de vacuna vencida: niño {} - vacuna {} - {} días", 
            ninoId, vacunaId, diasVencida);
        
        try {
            // Verificar que no existe ya una alerta similar reciente
            List<Notificacion> alertasRecientes = notificacionDAO.findByNino(ninoId).stream()
                .filter(n -> n.getVacunaId() != null && n.getVacunaId().equals(vacunaId))
                .filter(n -> NotificacionDAO.TIPO_VACUNA_VENCIDA.equals(n.getTipoNotificacion()))
                .filter(n -> n.getFechaCreacion().isAfter(LocalDateTime.now().minusDays(7)))
                .toList();
            
            if (!alertasRecientes.isEmpty()) {
                logger.debug("Ya existe alerta reciente para vacuna vencida");
                return alertasRecientes.get(0);
            }
            
            // Obtener información del niño y vacuna
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            
            if (ninoOpt.isEmpty() || vacunaOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            Vacuna vacuna = vacunaOpt.get();
            
            // Crear notificación de alerta
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuarioId);
            notificacion.setNinoId(ninoId);
            notificacion.setVacunaId(vacunaId);
            notificacion.setTipoNotificacion(NotificacionDAO.TIPO_VACUNA_VENCIDA);
            notificacion.setTitulo("¡VACUNA VENCIDA - ACCIÓN REQUERIDA!");
            
            String gravedad = diasVencida > 60 ? "CRÍTICA" : diasVencida > 30 ? "ALTA" : "MODERADA";
            String mensaje = String.format("⚠️ ATENCIÓN: La vacuna %s para %s %s está vencida desde hace %d días. " +
                "Gravedad: %s. Es URGENTE que contacte a su centro de salud para reagendar la aplicación. " +
                "Las vacunas son fundamentales para la salud de su hijo/a.",
                vacuna.getNombre(), nino.getNombres(), nino.getApellidos(), diasVencida, gravedad);
            notificacion.setMensaje(mensaje);
            
            // Prioridad urgente para vacunas vencidas
            notificacion.setPrioridad(NotificacionDAO.PRIORIDAD_URGENTE);
            notificacion.setEstado(NotificacionDAO.ESTADO_PENDIENTE);
            notificacion.setFechaProgramada(LocalDateTime.now());
            notificacion.setActivo(true);
            notificacion.setFechaCreacion(LocalDateTime.now());
            
            if (notificacionDAO.insert(notificacion)) {
                logger.info("Alerta de vacuna vencida creada: {} para {} {} ({} días)", 
                    vacuna.getNombre(), nino.getNombres(), nino.getApellidos(), diasVencida);
                return notificacion;
            }
            
        } catch (Exception e) {
            logger.error("Error al crear alerta de vacuna vencida", e);
        }
        
        return null;
    }
    
    /**
     * Crea una notificación de cumpleaños
     * @param usuarioId ID del padre de familia
     * @param ninoId ID del niño
     * @param edad edad que cumple
     * @return notificación creada o null si falló
     */
    public Notificacion crearNotificacionCumpleanos(Integer usuarioId, Integer ninoId, int edad) {
        logger.debug("Creando notificación de cumpleaños para niño {} - {} años", ninoId, edad);
        
        try {
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            
            // Verificar que no existe ya una notificación de cumpleaños para este año
            List<Notificacion> cumpleanosExistentes = notificacionDAO.findByNino(ninoId).stream()
                .filter(n -> NotificacionDAO.TIPO_CUMPLEANOS.equals(n.getTipoNotificacion()))
                .filter(n -> n.getFechaCreacion().getYear() == LocalDateTime.now().getYear())
                .toList();
            
            if (!cumpleanosExistentes.isEmpty()) {
                return cumpleanosExistentes.get(0);
            }
            
            // Crear notificación
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuarioId);
            notificacion.setNinoId(ninoId);
            notificacion.setTipoNotificacion(NotificacionDAO.TIPO_CUMPLEANOS);
            notificacion.setTitulo("🎉 ¡Feliz Cumpleaños!");
            
            String mensaje = String.format("🎂 ¡%s %s cumple %d año%s! Es un buen momento para " +
                "revisar su esquema de vacunación y asegurarse de que esté al día. " +
                "¡Que tengan un día lleno de alegría! 🎈",
                nino.getNombres(), nino.getApellidos(), edad, edad == 1 ? "" : "s");
            notificacion.setMensaje(mensaje);
            
            notificacion.setPrioridad(NotificacionDAO.PRIORIDAD_NORMAL);
            notificacion.setEstado(NotificacionDAO.ESTADO_PENDIENTE);
            notificacion.setFechaProgramada(nino.getFechaNacimiento().atStartOfDay().plusYears(edad));
            notificacion.setActivo(true);
            notificacion.setFechaCreacion(LocalDateTime.now());
            
            if (notificacionDAO.insert(notificacion)) {
                logger.info("Notificación de cumpleaños creada: {} {} - {} años", 
                    nino.getNombres(), nino.getApellidos(), edad);
                return notificacion;
            }
            
        } catch (Exception e) {
            logger.error("Error al crear notificación de cumpleaños", e);
        }
        
        return null;
    }
    
    /**
     * Crea notificación por reacción adversa grave
     * @param registro registro de vacuna con reacción
     * @return notificación creada o null si falló
     */
    public Notificacion crearNotificacionReaccionGrave(RegistroVacuna registro) {
        logger.warn("Creando notificación por reacción adversa grave en registro {}", registro.getId());
        
        try {
            Optional<Nino> ninoOpt = ninoDAO.findById(registro.getNinoId());
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(registro.getVacunaId());
            
            if (ninoOpt.isEmpty() || vacunaOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            Vacuna vacuna = vacunaOpt.get();
            
            // Buscar al padre responsable
            Integer padreFamiliaId = nino.getPadreFamiliaId();
            if (padreFamiliaId == null) {
                return null;
            }
            
            Optional<PadreFamilia> padreOpt = padreFamiliaDAO.findById(padreFamiliaId);
            if (padreOpt.isEmpty()) {
                return null;
            }
            
            PadreFamilia padre = padreOpt.get();
            
            // Crear notificación urgente
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(padre.getUsuarioId());
            notificacion.setNinoId(registro.getNinoId());
            notificacion.setVacunaId(registro.getVacunaId());
            notificacion.setTipoNotificacion(NotificacionDAO.TIPO_ALERTA_SISTEMA);
            notificacion.setTitulo("🚨 ALERTA: Reacción Adversa Reportada");
            
            String mensaje = String.format("Se ha reportado una reacción adversa %s a la vacuna %s " +
                "aplicada a %s %s el %s. Por favor, consulte inmediatamente con el centro de salud " +
                "donde se aplicó la vacuna. Lote: %s",
                registro.getGravedadReaccion().toLowerCase(),
                vacuna.getNombre(),
                nino.getNombres(), nino.getApellidos(),
                DateUtil.formatearFecha(registro.getFechaAplicacion()),
                registro.getLote() != null ? registro.getLote() : "No especificado");
            notificacion.setMensaje(mensaje);
            
            notificacion.setPrioridad(NotificacionDAO.PRIORIDAD_URGENTE);
            notificacion.setEstado(NotificacionDAO.ESTADO_PENDIENTE);
            notificacion.setFechaProgramada(LocalDateTime.now());
            notificacion.setActivo(true);
            notificacion.setFechaCreacion(LocalDateTime.now());
            
            if (notificacionDAO.insert(notificacion)) {
                logger.info("Notificación de reacción adversa creada para registro {}", registro.getId());
                return notificacion;
            }
            
        } catch (Exception e) {
            logger.error("Error al crear notificación de reacción adversa", e);
        }
        
        return null;
    }
    
    /**
     * Obtiene notificaciones no leídas para un usuario con prioridad
     * @param usuarioId ID del usuario
     * @return lista de notificaciones ordenadas por prioridad
     */
    public List<Notificacion> getNotificacionesNoLeidas(Integer usuarioId) {
        logger.debug("Obteniendo notificaciones no leídas para usuario {}", usuarioId);
        
        try {
            List<Notificacion> notificaciones = notificacionDAO.getNotificacionesNoLeidas(usuarioId);
            
            // Ordenar por prioridad y fecha
            notificaciones.sort((a, b) -> {
                // Primero por prioridad (urgente primero)
                int prioridadComparison = compararPrioridades(a.getPrioridad(), b.getPrioridad());
                if (prioridadComparison != 0) {
                    return prioridadComparison;
                }
                
                // Luego por fecha programada
                return a.getFechaProgramada().compareTo(b.getFechaProgramada());
            });
            
            logger.debug("Encontradas {} notificaciones no leídas para usuario {}", 
                notificaciones.size(), usuarioId);
            
            return notificaciones;
            
        } catch (Exception e) {
            logger.error("Error al obtener notificaciones no leídas para usuario: " + usuarioId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Marca una notificación como leída
     * @param notificacionId ID de la notificación
     * @param usuarioId ID del usuario (para verificar permisos)
     * @return true si se marcó exitosamente
     */
    public boolean marcarComoLeida(Integer notificacionId, Integer usuarioId) {
        logger.debug("Marcando notificación {} como leída por usuario {}", notificacionId, usuarioId);
        
        try {
            Optional<Notificacion> notificacionOpt = notificacionDAO.findById(notificacionId);
            if (notificacionOpt.isEmpty()) {
                return false;
            }
            
            Notificacion notificacion = notificacionOpt.get();
            
            // Verificar que el usuario tenga permisos
            if (!notificacion.getUsuarioId().equals(usuarioId)) {
                logger.warn("Usuario {} intentó marcar notificación {} de otro usuario", 
                    usuarioId, notificacionId);
                return false;
            }
            
            return notificacionDAO.markAsRead(notificacionId);
            
        } catch (Exception e) {
            logger.error("Error al marcar notificación como leída", e);
            return false;
        }
    }
    
    /**
     * Obtiene estadísticas de notificaciones para un usuario
     * @param usuarioId ID del usuario
     * @return estadísticas de notificaciones
     */
    public EstadisticasNotificaciones getEstadisticasUsuario(Integer usuarioId) {
        logger.debug("Obteniendo estadísticas de notificaciones para usuario {}", usuarioId);
        
        try {
            EstadisticasNotificaciones stats = new EstadisticasNotificaciones();
            
            stats.setTotalPendientes((int)notificacionDAO.countByUsuarioAndEstado(usuarioId, NotificacionDAO.ESTADO_PENDIENTE));
            stats.setTotalEnviadas((int)notificacionDAO.countByUsuarioAndEstado(usuarioId, NotificacionDAO.ESTADO_ENVIADA));
            stats.setTotalLeidas((int)notificacionDAO.countByUsuarioAndEstado(usuarioId, NotificacionDAO.ESTADO_LEIDA));
            stats.setTotalVencidas((int)notificacionDAO.countByUsuarioAndEstado(usuarioId, NotificacionDAO.ESTADO_VENCIDA));
            
            // Contar urgentes
            List<Notificacion> todasNotificaciones = notificacionDAO.findByUsuario(usuarioId);
            long urgentes = todasNotificaciones.stream()
                .filter(n -> NotificacionDAO.PRIORIDAD_URGENTE.equals(n.getPrioridad()))
                .filter(n -> !NotificacionDAO.ESTADO_LEIDA.equals(n.getEstado()))
                .count();
            stats.setTotalUrgentes((int)urgentes);
            
            stats.setFechaGeneracion(LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de notificaciones", e);
            return new EstadisticasNotificaciones();
        }
    }
    
    // Métodos privados de procesamiento automático
    
    /**
     * Procesa recordatorios de vacunas próximas
     */
    private int procesarRecordatoriosVacunas() {
        logger.debug("Procesando recordatorios de vacunas");
        
        int recordatoriosCreados = 0;
        
        try {
            // Obtener todos los niños activos
            List<Nino> ninos = ninoDAO.findActivos();
            
            for (Nino nino : ninos) {
                try {
                    // Calcular próximas vacunas
                    VacunacionService vacunacionService = VacunacionService.getInstance();
                    List<VacunacionService.ProximaVacuna> proximasVacunas = 
                        vacunacionService.calcularProximasVacunas(nino);
                    
                    for (VacunacionService.ProximaVacuna proxima : proximasVacunas) {
                        // Solo crear recordatorios para vacunas en los próximos 14 días
                        long diasHasta = ChronoUnit.DAYS.between(LocalDate.now(), proxima.getFechaRecomendada());
                        
                        if (diasHasta >= 0 && diasHasta <= 14) {
                            Notificacion recordatorio = programarRecordatorioVacuna(
                                nino.getPadreFamiliaId(),
                                nino.getId(),
                                proxima.getVacunaId(),
                                proxima.getFechaRecomendada().atStartOfDay()
                            );
                            
                            if (recordatorio != null) {
                                recordatoriosCreados++;
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
        
        logger.debug("Recordatorios de vacunas procesados: {}", recordatoriosCreados);
        return recordatoriosCreados;
    }
    
    /**
     * Procesa alertas de vacunas vencidas
     */
    private int procesarAlertasVacunasVencidas() {
        logger.debug("Procesando alertas de vacunas vencidas");
        
        int alertasCreadas = 0;
        
        try {
            // Obtener todos los niños activos
            List<Nino> ninos = ninoDAO.findActivos();
            
            for (Nino nino : ninos) {
                try {
                    VacunacionService vacunacionService = VacunacionService.getInstance();
                    VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(nino.getId());
                    
                    if (estado != null && estado.getVacunasVencidas() > 0) {
                        // Obtener detalles de vacunas vencidas
                        List<VacunacionService.VacunaPendiente> pendientes = 
                            obtenerVacunasPendientesVencidas(nino);
                        
                        for (VacunacionService.VacunaPendiente pendiente : pendientes) {
                            if (pendiente.isVencida()) {
                                Notificacion alerta = crearAlertaVacunaVencida(
                                    nino.getPadreFamiliaId(),
                                    nino.getId(),
                                    pendiente.getVacunaId(),
                                    pendiente.getDiasVencida()
                                );
                                
                                if (alerta != null) {
                                    alertasCreadas++;
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
        
        logger.debug("Alertas de vacunas vencidas procesadas: {}", alertasCreadas);
        return alertasCreadas;
    }
    
    /**
     * Procesa notificaciones de cumpleaños
     */
    private int procesarNotificacionesCumpleanos() {
        logger.debug("Procesando notificaciones de cumpleaños");
        
        int cumpleanosCreados = 0;
        
        try {
            LocalDate hoy = LocalDate.now();
            LocalDate finRango = hoy.plusDays(DIAS_ADELANTE_CUMPLEANOS);
            
            // Obtener niños que cumplen años en los próximos días
            List<Nino> ninos = ninoDAO.findActivos();
            
            for (Nino nino : ninos) {
                try {
                    LocalDate cumpleanos = nino.getFechaNacimiento()
                        .withYear(hoy.getYear());
                    
                    // Si ya pasó este año, considerar el próximo año
                    if (cumpleanos.isBefore(hoy)) {
                        cumpleanos = cumpleanos.plusYears(1);
                    }
                    
                    // Verificar si está en el rango
                    if (!cumpleanos.isAfter(finRango)) {
                        int edad = cumpleanos.getYear() - nino.getFechaNacimiento().getYear();
                        
                        Notificacion cumpleanosNotif = crearNotificacionCumpleanos(
                            nino.getPadreFamiliaId(),
                            nino.getId(),
                            edad
                        );
                        
                        if (cumpleanosNotif != null) {
                            cumpleanosCreados++;
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error procesando cumpleaños para niño: " + nino.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error en procesamiento de cumpleaños", e);
        }
        
        logger.debug("Notificaciones de cumpleaños procesadas: {}", cumpleanosCreados);
        return cumpleanosCreados;
    }
    
    /**
     * Procesa notificaciones de esquemas completados
     */
    private int procesarEsquemasCompletados() {
        logger.debug("Procesando notificaciones de esquemas completados");
        
        int esquemas = 0;
        
        try {
            // Obtener todos los niños activos
            List<Nino> ninos = ninoDAO.findActivos();
            
            for (Nino nino : ninos) {
                try {
                    VacunacionService vacunacionService = VacunacionService.getInstance();
                    VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(nino.getId());
                    
                    if (estado != null && estado.getPorcentajeCompletitud() >= 95.0) {
                        // Verificar si ya se notificó la completitud
                        List<Notificacion> notificacionesEsquema = notificacionDAO.findByNino(nino.getId()).stream()
                            .filter(n -> NotificacionDAO.TIPO_ESQUEMA_COMPLETADO.equals(n.getTipoNotificacion()))
                            .filter(n -> n.getFechaCreacion().isAfter(LocalDateTime.now().minusDays(30)))
                            .toList();
                        
                        if (notificacionesEsquema.isEmpty()) {
                            Notificacion esquemaCompleto = crearNotificacionEsquemaCompletado(nino, estado);
                            if (esquemaCompleto != null) {
                                esquemas++;
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error procesando esquema completado para niño: " + nino.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error en procesamiento de esquemas completados", e);
        }
        
        return esquemas;
    }
    
    // Métodos auxiliares
    
    /**
     * Crea notificación de esquema completado
     */
    private Notificacion crearNotificacionEsquemaCompletado(Nino nino, VacunacionService.EstadoEsquema estado) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(nino.getPadreFamiliaId());
            notificacion.setNinoId(nino.getId());
            notificacion.setTipoNotificacion(NotificacionDAO.TIPO_ESQUEMA_COMPLETADO);
            notificacion.setTitulo("🎉 ¡Esquema de Vacunación Completado!");
            
            String mensaje = String.format("¡Felicitaciones! %s %s ha completado el %.1f%% de su esquema de vacunación. " +
                "Su hijo/a está protegido/a contra las principales enfermedades prevenibles. " +
                "Continúe con los controles médicos regulares.",
                nino.getNombres(), nino.getApellidos(), estado.getPorcentajeCompletitud());
            notificacion.setMensaje(mensaje);
            
            notificacion.setPrioridad(NotificacionDAO.PRIORIDAD_NORMAL);
            notificacion.setEstado(NotificacionDAO.ESTADO_PENDIENTE);
            notificacion.setFechaProgramada(LocalDateTime.now());
            notificacion.setActivo(true);
            notificacion.setFechaCreacion(LocalDateTime.now());
            
            if (notificacionDAO.insert(notificacion)) {
                return notificacion;
            }
            
        } catch (Exception e) {
            logger.error("Error al crear notificación de esquema completado", e);
        }
        
        return null;
    }
    
    /**
     * Obtiene vacunas pendientes vencidas para un niño
     */
    private List<VacunacionService.VacunaPendiente> obtenerVacunasPendientesVencidas(Nino nino) {
        // Esta sería una implementación simplificada
        // En realidad se obtendría del VacunacionService
        return new ArrayList<>();
    }
    
    /**
     * Calcula prioridad basada en días hasta la fecha
     */
    private String calcularPrioridadPorDias(long dias, boolean esObligatoria) {
        if (dias < 0) {
            return NotificacionDAO.PRIORIDAD_URGENTE;
        } else if (dias <= DIAS_RECORDATORIO_URGENTE) {
            return esObligatoria ? NotificacionDAO.PRIORIDAD_URGENTE : NotificacionDAO.PRIORIDAD_ALTA;
        } else if (dias <= DIAS_RECORDATORIO_PREVIO) {
            return NotificacionDAO.PRIORIDAD_ALTA;
        } else if (dias <= 14) {
            return NotificacionDAO.PRIORIDAD_NORMAL;
        } else {
            return NotificacionDAO.PRIORIDAD_BAJA;
        }
    }
    
    /**
     * Compara prioridades para ordenamiento
     */
    private int compararPrioridades(String prioridad1, String prioridad2) {
        Map<String, Integer> ordenPrioridades = Map.of(
            NotificacionDAO.PRIORIDAD_URGENTE, 0,
            NotificacionDAO.PRIORIDAD_ALTA, 1,
            NotificacionDAO.PRIORIDAD_NORMAL, 2,
            NotificacionDAO.PRIORIDAD_BAJA, 3
        );
        
        int orden1 = ordenPrioridades.getOrDefault(prioridad1, 4);
        int orden2 = ordenPrioridades.getOrDefault(prioridad2, 4);
        
        return Integer.compare(orden1, orden2);
    }
    
    // Clases para resultados y estadísticas
    
    /**
     * Clase para el resumen del procesamiento automático
     */
    public static class ResumenProcesamiento {
        private LocalDateTime fechaProcesamiento;
        private boolean exitoso;
        private String mensajeError;
        private int recordatoriosCreados;
        private int alertasVencidasCreadas;
        private int cumpleanosCreados;
        private int notificacionesVencidas;
        private int esquemasCompletadosNotificados;
        
        // Getters y Setters
        public LocalDateTime getFechaProcesamiento() { return fechaProcesamiento; }
        public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) { this.fechaProcesamiento = fechaProcesamiento; }
        public boolean isExitoso() { return exitoso; }
        public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
        public String getMensajeError() { return mensajeError; }
        public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
        public int getRecordatoriosCreados() { return recordatoriosCreados; }
        public void setRecordatoriosCreados(int recordatoriosCreados) { this.recordatoriosCreados = recordatoriosCreados; }
        public int getAlertasVencidasCreadas() { return alertasVencidasCreadas; }
        public void setAlertasVencidasCreadas(int alertasVencidasCreadas) { this.alertasVencidasCreadas = alertasVencidasCreadas; }
        public int getCumpleanosCreados() { return cumpleanosCreados; }
        public void setCumpleanosCreados(int cumpleanosCreados) { this.cumpleanosCreados = cumpleanosCreados; }
        public int getNotificacionesVencidas() { return notificacionesVencidas; }
        public void setNotificacionesVencidas(int notificacionesVencidas) { this.notificacionesVencidas = notificacionesVencidas; }
        public int getEsquemasCompletadosNotificados() { return esquemasCompletadosNotificados; }
        public void setEsquemasCompletadosNotificados(int esquemasCompletadosNotificados) { this.esquemasCompletadosNotificados = esquemasCompletadosNotificados; }
        
        public int getTotalNotificacionesCreadas() {
            return recordatoriosCreados + alertasVencidasCreadas + cumpleanosCreados + esquemasCompletadosNotificados;
        }
    }
    
    /**
     * Clase para estadísticas de notificaciones de usuario
     */
    public static class EstadisticasNotificaciones {
        private int totalPendientes;
        private int totalEnviadas;
        private int totalLeidas;
        private int totalVencidas;
        private int totalUrgentes;
        private LocalDateTime fechaGeneracion;
        
        // Getters y Setters
        public int getTotalPendientes() { return totalPendientes; }
        public void setTotalPendientes(int totalPendientes) { this.totalPendientes = totalPendientes; }
        public int getTotalEnviadas() { return totalEnviadas; }
        public void setTotalEnviadas(int totalEnviadas) { this.totalEnviadas = totalEnviadas; }
        public int getTotalLeidas() { return totalLeidas; }
        public void setTotalLeidas(int totalLeidas) { this.totalLeidas = totalLeidas; }
        public int getTotalVencidas() { return totalVencidas; }
        public void setTotalVencidas(int totalVencidas) { this.totalVencidas = totalVencidas; }
        public int getTotalUrgentes() { return totalUrgentes; }
        public void setTotalUrgentes(int totalUrgentes) { this.totalUrgentes = totalUrgentes; }
        public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
        public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
        
        public int getTotalNotificaciones() {
            return totalPendientes + totalEnviadas + totalLeidas + totalVencidas;
        }
    }
}