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
 * Servicio principal de vacunación
 * Maneja toda la lógica de negocio relacionada con aplicación de vacunas,
 * control de esquemas, cálculo de próximas dosis y validaciones médicas
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class VacunacionService {

    private static final Logger logger = LoggerFactory.getLogger(VacunacionService.class);
    
    // Instancia singleton
    private static VacunacionService instance;
    
    // DAOs
    private final RegistroVacunaDAO registroVacunaDAO;
    private final VacunaDAO vacunaDAO;
    private final EsquemaVacunacionDAO esquemaDAO;
    private final NinoDAO ninoDAO;
    private final CentroSaludDAO centroSaludDAO;
    private final NotificacionDAO notificacionDAO;
    
    // Servicios relacionados
    private final NotificacionService notificacionService;
    
    // Constantes para validaciones
    private static final int DIAS_TOLERANCIA_ESQUEMA = 30;
    private static final int DIAS_ANTICIPACION_MAXIMA = 14;
    private static final int MESES_VALIDEZ_CERTIFICADO = 12;
    
    /**
     * Constructor privado para patrón singleton
     */
    private VacunacionService() {
        this.registroVacunaDAO = RegistroVacunaDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.esquemaDAO = EsquemaVacunacionDAO.getInstance();
        this.ninoDAO = NinoDAO.getInstance();
        this.centroSaludDAO = CentroSaludDAO.getInstance();
        this.notificacionDAO = NotificacionDAO.getInstance();
        this.notificacionService = NotificacionService.getInstance();
    }
    
    /**
     * Obtiene la instancia singleton del servicio
     * @return instancia de VacunacionService
     */
    public static synchronized VacunacionService getInstance() {
        if (instance == null) {
            instance = new VacunacionService();
        }
        return instance;
    }
    
    /**
     * Aplica una vacuna a un niño
     * @param aplicacionVacuna datos de la aplicación
     * @return resultado de la aplicación
     */
    public VacunacionResult aplicarVacuna(AplicacionVacuna aplicacionVacuna) {
        logger.info("Aplicando vacuna ID {} a niño ID {}", 
            aplicacionVacuna.getVacunaId(), aplicacionVacuna.getNinoId());
        
        try {
            // Validar datos de aplicación
            String validationError = validarAplicacionVacuna(aplicacionVacuna);
            if (validationError != null) {
                return VacunacionResult.error(validationError);
            }
            
            // Obtener información del niño y vacuna
            Optional<Nino> ninoOpt = ninoDAO.findById(aplicacionVacuna.getNinoId());
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(aplicacionVacuna.getVacunaId());
            
            if (ninoOpt.isEmpty()) {
                return VacunacionResult.error("Niño no encontrado");
            }
            
            if (vacunaOpt.isEmpty()) {
                return VacunacionResult.error("Vacuna no encontrada");
            }
            
            Nino nino = ninoOpt.get();
            Vacuna vacuna = vacunaOpt.get();
            
            // Verificar si la vacuna está activa
            if (!vacuna.getActiva()) {
                return VacunacionResult.error("La vacuna no está activa");
            }
            
            // Verificar edad del niño para la vacuna
            VacunacionResult ageValidation = validarEdadParaVacuna(nino, vacuna, aplicacionVacuna.getFechaAplicacion());
            if (!ageValidation.isSuccess()) {
                return ageValidation;
            }
            
            // Verificar si ya se aplicó esta vacuna
            VacunacionResult duplicateValidation = validarVacunaDuplicada(nino.getId(), vacuna.getId());
            if (!duplicateValidation.isSuccess()) {
                return duplicateValidation;
            }
            
            // Verificar intervalos entre dosis si es una serie
            VacunacionResult intervalValidation = validarIntervaloEntreDosis(nino.getId(), vacuna.getId(), 
                aplicacionVacuna.getFechaAplicacion());
            if (!intervalValidation.isSuccess()) {
                return intervalValidation;
            }
            
            // Crear registro de vacuna
            RegistroVacuna registro = crearRegistroVacuna(aplicacionVacuna, nino, vacuna);
            
            if (!registroVacunaDAO.insert(registro)) {
                return VacunacionResult.error("Error al registrar la aplicación de la vacuna");
            }
            
            logger.info("Vacuna {} aplicada exitosamente a {} {}", vacuna.getNombre(), 
                nino.getNombres(), nino.getApellidos());
            
            // Programar próximas notificaciones
            programarProximasNotificaciones(nino);
            
            // Crear resultado exitoso
            VacunacionResult result = VacunacionResult.success(registro, 
                "Vacuna aplicada exitosamente");
            
            // Calcular próximas vacunas
            result.setProximasVacunas(calcularProximasVacunas(nino));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error al aplicar vacuna", e);
            return VacunacionResult.error("Error interno del sistema");
        }
    }
    
    /**
     * Obtiene el historial completo de vacunación de un niño
     * @param ninoId ID del niño
     * @return historial de vacunación
     */
    public HistorialVacunacion getHistorialVacunacion(Integer ninoId) {
        logger.debug("Obteniendo historial de vacunación para niño ID: {}", ninoId);
        
        try {
            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            
            // Obtener registros de vacunas aplicadas
            List<RegistroVacuna> registrosAplicadas = registroVacunaDAO.findByNino(ninoId);
            
            // Obtener esquema completo
            List<EsquemaVacunacion> esquemaCompleto = esquemaDAO.getEsquemaCompleto();
            
            // Calcular vacunas pendientes
            List<VacunaPendiente> vacunasPendientes = calcularVacunasPendientes(nino, registrosAplicadas, esquemaCompleto);
            
            // Calcular próximas vacunas (próximos 90 días)
            List<ProximaVacuna> proximasVacunas = calcularProximasVacunas(nino);
            
            // Calcular estadísticas
            EstadisticasVacunacion estadisticas = calcularEstadisticas(nino, registrosAplicadas, esquemaCompleto);
            
            // Crear historial
            HistorialVacunacion historial = new HistorialVacunacion();
            historial.setNino(nino);
            historial.setVacunasAplicadas(registrosAplicadas);
            historial.setVacunasPendientes(vacunasPendientes);
            historial.setProximasVacunas(proximasVacunas);
            historial.setEstadisticas(estadisticas);
            historial.setFechaGeneracion(LocalDateTime.now());
            
            logger.debug("Historial generado: {} aplicadas, {} pendientes", 
                registrosAplicadas.size(), vacunasPendientes.size());
            
            return historial;
            
        } catch (Exception e) {
            logger.error("Error al obtener historial de vacunación para niño: " + ninoId, e);
            return null;
        }
    }
    
    /**
     * Calcula las próximas vacunas que debe recibir un niño
     * @param nino información del niño
     * @return lista de próximas vacunas
     */
    public List<ProximaVacuna> calcularProximasVacunas(Nino nino) {
        List<ProximaVacuna> proximasVacunas = new ArrayList<>();
        
        try {
            int edadDias = calcularEdadEnDias(nino.getFechaNacimiento());
            
            // Obtener vacunas ya aplicadas
            List<RegistroVacuna> aplicadas = registroVacunaDAO.findByNino(nino.getId());
            Set<Integer> vacunasAplicadas = aplicadas.stream()
                .map(RegistroVacuna::getVacunaId)
                .collect(Collectors.toSet());
            
            // Obtener próximas vacunas del esquema (próximos 180 días)
            List<EsquemaVacunacion> proximosEsquemas = esquemaDAO.getProximasVacunas(edadDias, 180);
            
            for (EsquemaVacunacion esquema : proximosEsquemas) {
                // Solo incluir vacunas no aplicadas
                if (!vacunasAplicadas.contains(esquema.getVacunaId())) {
                    ProximaVacuna proxima = new ProximaVacuna();
                    proxima.setVacunaId(esquema.getVacunaId());
                    proxima.setVacunaNombre(esquema.getVacunaNombre());
                    proxima.setNumeroDosis(esquema.getNumeroDosis());
                    proxima.setEsObligatoria(esquema.getEsObligatoria());
                    proxima.setEsRefuerzo(esquema.getEsRefuerzo());
                    
                    // Calcular fecha recomendada
                    LocalDate fechaRecomendada = nino.getFechaNacimiento().plusDays(esquema.getEdadAplicacionDias());
                    proxima.setFechaRecomendada(fechaRecomendada);
                    
                    // Calcular urgencia
                    long diasHastaVacuna = ChronoUnit.DAYS.between(LocalDate.now(), fechaRecomendada);
                    proxima.setUrgencia(calcularUrgencia(diasHastaVacuna, esquema.getEsObligatoria()));
                    
                    // Calcular ventana de aplicación
                    if (esquema.getIntervaloMinimoDias() != null && esquema.getIntervaloMaximoDias() != null) {
                        proxima.setVentanaInicio(fechaRecomendada.minusDays(esquema.getIntervaloMinimoDias()));
                        proxima.setVentanaFin(fechaRecomendada.plusDays(esquema.getIntervaloMaximoDias()));
                    } else {
                        proxima.setVentanaInicio(fechaRecomendada.minusDays(DIAS_ANTICIPACION_MAXIMA));
                        proxima.setVentanaFin(fechaRecomendada.plusDays(DIAS_TOLERANCIA_ESQUEMA));
                    }
                    
                    proximasVacunas.add(proxima);
                }
            }
            
            // Ordenar por urgencia y fecha
            proximasVacunas.sort((a, b) -> {
                if (!a.getUrgencia().equals(b.getUrgencia())) {
                    return a.getUrgencia().compareTo(b.getUrgencia());
                }
                return a.getFechaRecomendada().compareTo(b.getFechaRecomendada());
            });
            
        } catch (Exception e) {
            logger.error("Error al calcular próximas vacunas para niño: " + nino.getId(), e);
        }
        
        return proximasVacunas;
    }
    
    /**
     * Verifica el estado del esquema de vacunación de un niño
     * @param ninoId ID del niño
     * @return estado del esquema
     */
    public EstadoEsquema verificarEstadoEsquema(Integer ninoId) {
        logger.debug("Verificando estado de esquema para niño ID: {}", ninoId);
        
        try {
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            int edadDias = calcularEdadEnDias(nino.getFechaNacimiento());
            
            // Obtener vacunas aplicadas
            List<RegistroVacuna> aplicadas = registroVacunaDAO.findByNino(ninoId);
            Set<Integer> vacunasAplicadas = aplicadas.stream()
                .map(RegistroVacuna::getVacunaId)
                .collect(Collectors.toSet());
            
            // Obtener esquema hasta la edad actual
            List<EsquemaVacunacion> esquemaHastaAhora = esquemaDAO.findByEdad(edadDias);
            
            // Calcular estadísticas
            long totalVacunasEsperadas = esquemaHastaAhora.stream()
                .filter(e -> e.getEsObligatoria())
                .count();
            
            long vacunasObligatoriasAplicadas = esquemaHastaAhora.stream()
                .filter(e -> e.getEsObligatoria() && vacunasAplicadas.contains(e.getVacunaId()))
                .count();
            
            long vacunasVencidas = esquemaHastaAhora.stream()
                .filter(e -> {
                    if (!e.getEsObligatoria() || vacunasAplicadas.contains(e.getVacunaId())) {
                        return false;
                    }
                    
                    LocalDate fechaLimite = nino.getFechaNacimiento()
                        .plusDays(e.getEdadAplicacionDias() + DIAS_TOLERANCIA_ESQUEMA);
                    return LocalDate.now().isAfter(fechaLimite);
                })
                .count();
            
            // Crear estado
            EstadoEsquema estado = new EstadoEsquema();
            estado.setNinoId(ninoId);
            estado.setEdadDias(edadDias);
            estado.setTotalVacunasEsperadas((int)totalVacunasEsperadas);
            estado.setVacunasAplicadas((int)vacunasObligatoriasAplicadas);
            estado.setVacunasVencidas((int)vacunasVencidas);
            estado.setPorcentajeCompletitud(calcularPorcentajeCompletitud(vacunasObligatoriasAplicadas, totalVacunasEsperadas));
            estado.setEstadoGeneral(determinarEstadoGeneral(estado));
            estado.setFechaVerificacion(LocalDateTime.now());
            
            return estado;
            
        } catch (Exception e) {
            logger.error("Error al verificar estado de esquema para niño: " + ninoId, e);
            return null;
        }
    }
    
    /**
     * Busca centros de salud cercanos que tengan una vacuna específica
     * @param latitud latitud de referencia
     * @param longitud longitud de referencia
     * @param vacunaId ID de la vacuna
     * @param radioKm radio de búsqueda en kilómetros
     * @return lista de centros cercanos
     */
    public List<CentroConVacuna> buscarCentrosCercanos(double latitud, double longitud, 
                                                      Integer vacunaId, double radioKm) {
        logger.debug("Buscando centros cercanos para vacuna ID: {} en radio {} km", vacunaId, radioKm);
        
        List<CentroConVacuna> centrosConVacuna = new ArrayList<>();
        
        try {
            // Buscar centros cercanos
            List<CentroSalud> centrosCercanos = centroSaludDAO.findCercanos(latitud, longitud, radioKm);
            
            // Obtener información de la vacuna
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            if (vacunaOpt.isEmpty()) {
                return centrosConVacuna;
            }
            
            Vacuna vacuna = vacunaOpt.get();
            
            for (CentroSalud centro : centrosCercanos) {
                CentroConVacuna centroConVacuna = new CentroConVacuna();
                centroConVacuna.setCentro(centro);
                centroConVacuna.setVacuna(vacuna);
                centroConVacuna.setDisponible(true); // Asumir disponibilidad por defecto
                centroConVacuna.setDistanciaKm(calcularDistancia(latitud, longitud, 
                    centro.getLatitud().doubleValue(), centro.getLongitud().doubleValue()));
                
                centrosConVacuna.add(centroConVacuna);
            }
            
            // Ordenar por distancia
            centrosConVacuna.sort(Comparator.comparing(CentroConVacuna::getDistanciaKm));
            
        } catch (Exception e) {
            logger.error("Error al buscar centros cercanos", e);
        }
        
        return centrosConVacuna;
    }
    
    /**
     * Registra una reacción adversa a una vacuna
     * @param registroId ID del registro de vacuna
     * @param reaccion descripción de la reacción
     * @param gravedad gravedad de la reacción
     * @return true si se registró exitosamente
     */
    public boolean registrarReaccionAdversa(Integer registroId, String reaccion, String gravedad) {
        logger.info("Registrando reacción adversa para registro ID: {}", registroId);
        
        try {
            Optional<RegistroVacuna> registroOpt = registroVacunaDAO.findById(registroId);
            if (registroOpt.isEmpty()) {
                return false;
            }
            
            RegistroVacuna registro = registroOpt.get();
            registro.setReaccionAdversa(reaccion);
            registro.setGravedadReaccion(gravedad);
            
            if (registroVacunaDAO.update(registro)) {
                logger.info("Reacción adversa registrada para registro: {}", registroId);
                
                // Si es grave, crear notificación urgente
                if ("GRAVE".equals(gravedad) || "MUY_GRAVE".equals(gravedad)) {
                    notificacionService.crearNotificacionReaccionGrave(registro);
                }
                
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Error al registrar reacción adversa", e);
        }
        
        return false;
    }
    
    // Métodos privados de utilidad
    
    /**
     * Valida los datos de una aplicación de vacuna
     */
    private String validarAplicacionVacuna(AplicacionVacuna aplicacion) {
        if (aplicacion == null) {
            return "Datos de aplicación requeridos";
        }
        
        if (aplicacion.getNinoId() == null) {
            return "ID del niño es requerido";
        }
        
        if (aplicacion.getVacunaId() == null) {
            return "ID de la vacuna es requerido";
        }
        
        if (aplicacion.getFechaAplicacion() == null) {
            return "Fecha de aplicación es requerida";
        }
        
        if (aplicacion.getFechaAplicacion().isAfter(LocalDate.now())) {
            return "La fecha de aplicación no puede ser futura";
        }
        
        if (aplicacion.getProfesionalId() == null) {
            return "ID del profesional es requerido";
        }
        
        if (aplicacion.getCentroSaludId() == null) {
            return "ID del centro de salud es requerido";
        }
        
        return null; // Válido
    }
    
    /**
     * Valida la edad del niño para recibir una vacuna
     */
    private VacunacionResult validarEdadParaVacuna(Nino nino, Vacuna vacuna, LocalDate fechaAplicacion) {
        int edadDiasAplicacion = calcularEdadEnDias(nino.getFechaNacimiento(), fechaAplicacion);
        
        // Buscar el esquema correspondiente para esta vacuna
        List<EsquemaVacunacion> esquemas = esquemaDAO.findByVacuna(vacuna.getId());
        
        if (esquemas.isEmpty()) {
            return VacunacionResult.warning("No hay esquema definido para esta vacuna, proceder con precaución");
        }
        
        // Verificar si existe un esquema apropiado para la edad
        boolean edadApropiada = esquemas.stream()
            .anyMatch(e -> {
                int edadMinima = e.getEdadAplicacionDias() - DIAS_ANTICIPACION_MAXIMA;
                int edadMaxima = e.getEdadAplicacionDias() + DIAS_TOLERANCIA_ESQUEMA;
                return edadDiasAplicacion >= edadMinima && edadDiasAplicacion <= edadMaxima;
            });
        
        if (!edadApropiada) {
            return VacunacionResult.error("La edad del niño no es apropiada para esta vacuna según el esquema nacional");
        }
        
        return VacunacionResult.success(null, "Edad apropiada para la vacuna");
    }
    
    /**
     * Valida que no se aplique una vacuna duplicada
     */
    private VacunacionResult validarVacunaDuplicada(Integer ninoId, Integer vacunaId) {
        List<RegistroVacuna> registros = registroVacunaDAO.findByNinoAndVacuna(ninoId, vacunaId);
        
        if (!registros.isEmpty()) {
            // Verificar si es una vacuna que permite múltiples dosis
            List<EsquemaVacunacion> esquemas = esquemaDAO.findByVacuna(vacunaId);
            long dosisProgramadas = esquemas.size();
            
            if (registros.size() >= dosisProgramadas) {
                return VacunacionResult.error("El niño ya ha recibido todas las dosis programadas de esta vacuna");
            }
        }
        
        return VacunacionResult.success(null, "No hay duplicación de vacuna");
    }
    
    /**
     * Valida los intervalos entre dosis de una vacuna
     */
    private VacunacionResult validarIntervaloEntreDosis(Integer ninoId, Integer vacunaId, LocalDate fechaAplicacion) {
        List<RegistroVacuna> registrosAnteriores = registroVacunaDAO.findByNinoAndVacuna(ninoId, vacunaId);
        
        if (!registrosAnteriores.isEmpty()) {
            // Obtener la última aplicación
            RegistroVacuna ultimaAplicacion = registrosAnteriores.get(registrosAnteriores.size() - 1);
            
            // Buscar el esquema para la próxima dosis
            List<EsquemaVacunacion> esquemas = esquemaDAO.findByVacuna(vacunaId);
            Optional<EsquemaVacunacion> proximoDosis = esquemas.stream()
                .filter(e -> e.getNumeroDosis() == registrosAnteriores.size() + 1)
                .findFirst();
            
            if (proximoDosis.isPresent() && proximoDosis.get().getIntervaloMinimoDias() != null) {
                long diasTranscurridos = ChronoUnit.DAYS.between(ultimaAplicacion.getFechaAplicacion(), fechaAplicacion);
                
                if (diasTranscurridos < proximoDosis.get().getIntervaloMinimoDias()) {
                    return VacunacionResult.error(
                        String.format("Debe esperar al menos %d días desde la última dosis", 
                            proximoDosis.get().getIntervaloMinimoDias()));
                }
            }
        }
        
        return VacunacionResult.success(null, "Intervalo entre dosis correcto");
    }
    
    /**
     * Crea un registro de vacuna aplicada
     */
    private RegistroVacuna crearRegistroVacuna(AplicacionVacuna aplicacion, Nino nino, Vacuna vacuna) {
        RegistroVacuna registro = new RegistroVacuna();
        
        registro.setNinoId(aplicacion.getNinoId());
        registro.setVacunaId(aplicacion.getVacunaId());
        registro.setFechaAplicacion(aplicacion.getFechaAplicacion());
        registro.setProfesionalId(aplicacion.getProfesionalId());
        registro.setCentroSaludId(aplicacion.getCentroSaludId());
        registro.setLote(aplicacion.getLote());
        registro.setObservaciones(aplicacion.getObservaciones());
        registro.setActivo(true);
        registro.setFechaRegistro(LocalDateTime.now());
        
        // Calcular número de dosis
        List<RegistroVacuna> registrosAnteriores = registroVacunaDAO.findByNinoAndVacuna(nino.getId(), vacuna.getId());
        registro.setNumeroDosis(registrosAnteriores.size() + 1);
        
        return registro;
    }
    
    /**
     * Calcula las vacunas pendientes para un niño
     */
    private List<VacunaPendiente> calcularVacunasPendientes(Nino nino, List<RegistroVacuna> aplicadas, 
                                                          List<EsquemaVacunacion> esquemaCompleto) {
        List<VacunaPendiente> pendientes = new ArrayList<>();
        
        int edadDias = calcularEdadEnDias(nino.getFechaNacimiento());
        Set<Integer> vacunasAplicadas = aplicadas.stream()
            .map(RegistroVacuna::getVacunaId)
            .collect(Collectors.toSet());
        
        // Filtrar esquemas hasta la edad actual que no han sido aplicados
        esquemaCompleto.stream()
            .filter(e -> e.getEdadAplicacionDias() <= edadDias)
            .filter(e -> !vacunasAplicadas.contains(e.getVacunaId()))
            .forEach(e -> {
                VacunaPendiente pendiente = new VacunaPendiente();
                pendiente.setVacunaId(e.getVacunaId());
                pendiente.setVacunaNombre(e.getVacunaNombre());
                pendiente.setNumeroDosis(e.getNumeroDosis());
                pendiente.setEsObligatoria(e.getEsObligatoria());
                
                LocalDate fechaRecomendada = nino.getFechaNacimiento().plusDays(e.getEdadAplicacionDias());
                pendiente.setFechaRecomendada(fechaRecomendada);
                
                long diasVencida = ChronoUnit.DAYS.between(fechaRecomendada, LocalDate.now());
                pendiente.setDiasVencida(Math.max(0, (int)diasVencida));
                pendiente.setVencida(diasVencida > DIAS_TOLERANCIA_ESQUEMA);
                
                pendientes.add(pendiente);
            });
        
        return pendientes;
    }
    
    /**
     * Calcula estadísticas de vacunación
     */
    private EstadisticasVacunacion calcularEstadisticas(Nino nino, List<RegistroVacuna> aplicadas, 
                                                       List<EsquemaVacunacion> esquemaCompleto) {
        int edadDias = calcularEdadEnDias(nino.getFechaNacimiento());
        
        long totalEsperadas = esquemaCompleto.stream()
            .filter(e -> e.getEdadAplicacionDias() <= edadDias)
            .filter(e -> e.getEsObligatoria())
            .count();
        
        Set<Integer> vacunasAplicadas = aplicadas.stream()
            .map(RegistroVacuna::getVacunaId)
            .collect(Collectors.toSet());
        
        long aplicadasObligatorias = esquemaCompleto.stream()
            .filter(e -> e.getEdadAplicacionDias() <= edadDias)
            .filter(e -> e.getEsObligatoria())
            .filter(e -> vacunasAplicadas.contains(e.getVacunaId()))
            .count();
        
        EstadisticasVacunacion stats = new EstadisticasVacunacion();
        stats.setTotalEsperadas((int)totalEsperadas);
        stats.setTotalAplicadas(aplicadas.size());
        stats.setObligatoriasAplicadas((int)aplicadasObligatorias);
        stats.setPorcentajeCompletitud(calcularPorcentajeCompletitud(aplicadasObligatorias, totalEsperadas));
        
        return stats;
    }
    
    /**
     * Programa notificaciones para próximas vacunas
     */
    private void programarProximasNotificaciones(Nino nino) {
        try {
            List<ProximaVacuna> proximas = calcularProximasVacunas(nino);
            
            for (ProximaVacuna proxima : proximas) {
                // Solo programar para vacunas en los próximos 30 días
                long diasHasta = ChronoUnit.DAYS.between(LocalDate.now(), proxima.getFechaRecomendada());
                
                if (diasHasta <= 30 && diasHasta >= 0) {
                    notificacionService.programarRecordatorioVacuna(
                        nino.getPadreFamiliaId(), 
                        nino.getId(), 
                        proxima.getVacunaId(),
                        proxima.getFechaRecomendada().atStartOfDay()
                    );
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al programar notificaciones para niño: " + nino.getId(), e);
        }
    }
    
    /**
     * Calcula la edad en días
     */
    private int calcularEdadEnDias(LocalDate fechaNacimiento) {
        return calcularEdadEnDias(fechaNacimiento, LocalDate.now());
    }
    
    private int calcularEdadEnDias(LocalDate fechaNacimiento, LocalDate fechaReferencia) {
        return (int) ChronoUnit.DAYS.between(fechaNacimiento, fechaReferencia);
    }
    
    /**
     * Calcula el porcentaje de completitud
     */
    private double calcularPorcentajeCompletitud(long aplicadas, long esperadas) {
        if (esperadas == 0) return 100.0;
        return Math.min(100.0, (aplicadas * 100.0) / esperadas);
    }
    
    /**
     * Calcula la urgencia de una vacuna
     */
    private UrgenciaVacuna calcularUrgencia(long diasHasta, Boolean esObligatoria) {
        if (diasHasta < 0) {
            return UrgenciaVacuna.VENCIDA;
        } else if (diasHasta <= 7) {
            return esObligatoria ? UrgenciaVacuna.URGENTE : UrgenciaVacuna.ALTA;
        } else if (diasHasta <= 30) {
            return UrgenciaVacuna.ALTA;
        } else if (diasHasta <= 60) {
            return UrgenciaVacuna.NORMAL;
        } else {
            return UrgenciaVacuna.BAJA;
        }
    }
    
    /**
     * Determina el estado general del esquema
     */
    private EstadoGeneralEsquema determinarEstadoGeneral(EstadoEsquema estado) {
        if (estado.getVacunasVencidas() > 0) {
            return EstadoGeneralEsquema.ATRASADO;
        } else if (estado.getPorcentajeCompletitud() >= 95.0) {
            return EstadoGeneralEsquema.COMPLETO;
        } else if (estado.getPorcentajeCompletitud() >= 80.0) {
            return EstadoGeneralEsquema.EN_PROGRESO;
        } else {
            return EstadoGeneralEsquema.INCOMPLETO;
        }
    }
    
    /**
     * Calcula la distancia entre dos puntos geográficos
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        
        return Math.round(distance * 100.0) / 100.0;
    }
    
    // Clases internas para resultados y datos
    
    /**
     * Clase para representar el resultado de una aplicación de vacuna
     */
    public static class VacunacionResult {
        private final boolean success;
        private final String message;
        private final String level; // SUCCESS, WARNING, ERROR
        private final RegistroVacuna registro;
        private List<ProximaVacuna> proximasVacunas;
        
        private VacunacionResult(boolean success, String message, String level, RegistroVacuna registro) {
            this.success = success;
            this.message = message;
            this.level = level;
            this.registro = registro;
        }
        
        public static VacunacionResult success(RegistroVacuna registro, String message) {
            return new VacunacionResult(true, message, "SUCCESS", registro);
        }
        
        public static VacunacionResult warning(String message) {
            return new VacunacionResult(true, message, "WARNING", null);
        }
        
        public static VacunacionResult error(String message) {
            return new VacunacionResult(false, message, "ERROR", null);
        }
        
        // Getters y Setters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getLevel() { return level; }
        public RegistroVacuna getRegistro() { return registro; }
        public List<ProximaVacuna> getProximasVacunas() { return proximasVacunas; }
        public void setProximasVacunas(List<ProximaVacuna> proximasVacunas) { this.proximasVacunas = proximasVacunas; }
    }
    
    /**
     * Clase para datos de aplicación de vacuna
     */
    public static class AplicacionVacuna {
        private Integer ninoId;
        private Integer vacunaId;
        private LocalDate fechaAplicacion;
        private Integer profesionalId;
        private Integer centroSaludId;
        private String lote;
        private String observaciones;
        
        // Getters y Setters
        public Integer getNinoId() { return ninoId; }
        public void setNinoId(Integer ninoId) { this.ninoId = ninoId; }
        public Integer getVacunaId() { return vacunaId; }
        public void setVacunaId(Integer vacunaId) { this.vacunaId = vacunaId; }
        public LocalDate getFechaAplicacion() { return fechaAplicacion; }
        public void setFechaAplicacion(LocalDate fechaAplicacion) { this.fechaAplicacion = fechaAplicacion; }
        public Integer getProfesionalId() { return profesionalId; }
        public void setProfesionalId(Integer profesionalId) { this.profesionalId = profesionalId; }
        public Integer getCentroSaludId() { return centroSaludId; }
        public void setCentroSaludId(Integer centroSaludId) { this.centroSaludId = centroSaludId; }
        public String getLote() { return lote; }
        public void setLote(String lote) { this.lote = lote; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }
    
    // Enums para estados y urgencias
    
    public enum UrgenciaVacuna {
        VENCIDA, URGENTE, ALTA, NORMAL, BAJA
    }
    
    public enum EstadoGeneralEsquema {
        COMPLETO, EN_PROGRESO, INCOMPLETO, ATRASADO
    }
    
    // Clases adicionales se definirían aquí o en archivos separados según necesidad
}