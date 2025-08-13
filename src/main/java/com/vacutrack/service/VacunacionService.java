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
 * Servicio de vacunación simplificado para proyecto estudiantil
 * Maneja la lógica de negocio relacionada con vacunas
 */
public class VacunacionService {

    private static final Logger logger = LoggerFactory.getLogger(VacunacionService.class);
    private static VacunacionService instance;

    // DAOs
    private final RegistroVacunaDAO registroVacunaDAO;
    private final VacunaDAO vacunaDAO;
    private final EsquemaVacunacionDAO esquemaDAO;
    private final NinoDAO ninoDAO;
    private final CentroSaludDAO centroSaludDAO;

    // Constantes
    private static final int DIAS_TOLERANCIA = 30;
    private static final int DIAS_ANTICIPACION = 14;

    private VacunacionService() {
        this.registroVacunaDAO = RegistroVacunaDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.esquemaDAO = EsquemaVacunacionDAO.getInstance();
        this.ninoDAO = NinoDAO.getInstance();
        this.centroSaludDAO = CentroSaludDAO.getInstance();
    }

    public static synchronized VacunacionService getInstance() {
        if (instance == null) {
            instance = new VacunacionService();
        }
        return instance;
    }

    /**
     * Aplica una vacuna a un niño
     */
    public ResultadoVacunacion aplicarVacuna(DatosAplicacion datos) {
        logger.info("Aplicando vacuna ID {} a niño ID {}", datos.getVacunaId(), datos.getNinoId());

        try {
            // Validaciones básicas
            String error = validarDatos(datos);
            if (error != null) {
                return ResultadoVacunacion.error(error);
            }

            // Obtener entidades
            Optional<Nino> ninoOpt = ninoDAO.findById(datos.getNinoId());
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(datos.getVacunaId());

            if (ninoOpt.isEmpty()) {
                return ResultadoVacunacion.error("Niño no encontrado");
            }
            if (vacunaOpt.isEmpty()) {
                return ResultadoVacunacion.error("Vacuna no encontrada");
            }

            Nino nino = ninoOpt.get();
            Vacuna vacuna = vacunaOpt.get();

            // Verificar si la vacuna ya fue aplicada
            if (yaFueAplicada(nino.getId(), vacuna.getId())) {
                return ResultadoVacunacion.error("Esta vacuna ya fue aplicada a este niño");
            }

            // Crear registro
            RegistroVacuna registro = crearRegistro(datos, nino, vacuna);

            if (registroVacunaDAO.insert(registro)) {
                logger.info("Vacuna aplicada exitosamente");
                return ResultadoVacunacion.exito(registro, "Vacuna aplicada correctamente");
            } else {
                return ResultadoVacunacion.error("Error al guardar el registro");
            }

        } catch (Exception e) {
            logger.error("Error al aplicar vacuna", e);
            return ResultadoVacunacion.error("Error interno del sistema");
        }
    }

    /**
     * Obtiene el historial de vacunación de un niño
     */
    public List<RegistroVacuna> obtenerHistorial(Integer ninoId) {
        try {
            return registroVacunaDAO.findByNino(ninoId);
        } catch (Exception e) {
            logger.error("Error al obtener historial", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene las vacunas pendientes según la edad del niño
     */
    public List<VacunaPendiente> obtenerVacunasPendientes(Integer ninoId) {
        List<VacunaPendiente> pendientes = new ArrayList<>();

        try {
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return pendientes;
            }

            Nino nino = ninoOpt.get();
            int edadMeses = calcularEdadEnMeses(nino.getFechaNacimiento());

            // Obtener vacunas aplicadas
            List<RegistroVacuna> aplicadas = registroVacunaDAO.findByNino(ninoId);
            Set<Integer> vacunasAplicadas = aplicadas.stream()
                    .map(RegistroVacuna::getVacunaId)
                    .collect(Collectors.toSet());

            // Obtener esquema de vacunación
            List<EsquemaVacunacion> esquemas = esquemaDAO.findAll();

            for (EsquemaVacunacion esquema : esquemas) {
                // Solo incluir vacunas para la edad actual o menores
                if (esquema.getEdadMeses() <= edadMeses &&
                        !vacunasAplicadas.contains(esquema.getVacunaId())) {

                    VacunaPendiente pendiente = new VacunaPendiente();
                    pendiente.setVacunaId(esquema.getVacunaId());
                    pendiente.setNombreVacuna(obtenerNombreVacuna(esquema.getVacunaId()));
                    pendiente.setEdadRecomendada(esquema.getEdadMeses());
                    pendiente.setObligatoria(esquema.getObligatoria());

                    // Calcular si está atrasada
                    int mesesAtraso = edadMeses - esquema.getEdadMeses();
                    pendiente.setAtrasada(mesesAtraso > 1);
                    pendiente.setMesesAtraso(Math.max(0, mesesAtraso));

                    pendientes.add(pendiente);
                }
            }

            // Ordenar por prioridad (atrasadas primero, luego por edad)
            pendientes.sort((a, b) -> {
                if (a.isAtrasada() != b.isAtrasada()) {
                    return Boolean.compare(b.isAtrasada(), a.isAtrasada());
                }
                return Integer.compare(a.getEdadRecomendada(), b.getEdadRecomendada());
            });

        } catch (Exception e) {
            logger.error("Error al obtener vacunas pendientes", e);
        }

        return pendientes;
    }

    /**
     * Busca centros de salud (simulado para proyecto estudiantil)
     */
    public List<CentroSalud> buscarCentrosCercanos(String departamento, String municipio) {
        try {
            // Para proyecto estudiantil, retornar centros del mismo municipio
            return centroSaludDAO.findByMunicipio(municipio);
        } catch (Exception e) {
            logger.error("Error al buscar centros", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene estadísticas básicas de vacunación
     */
    public EstadisticasVacunacion obtenerEstadisticas(Integer ninoId) {
        try {
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return null;
            }

            Nino nino = ninoOpt.get();
            int edadMeses = calcularEdadEnMeses(nino.getFechaNacimiento());

            List<RegistroVacuna> aplicadas = registroVacunaDAO.findByNino(ninoId);
            List<EsquemaVacunacion> esquemas = esquemaDAO.findAll();

            // Contar vacunas esperadas hasta la edad actual
            long esperadas = esquemas.stream()
                    .filter(e -> e.getEdadMeses() <= edadMeses && e.getObligatoria())
                    .count();

            // Contar vacunas obligatorias aplicadas
            Set<Integer> vacunasAplicadas = aplicadas.stream()
                    .map(RegistroVacuna::getVacunaId)
                    .collect(Collectors.toSet());

            long obligatoriasAplicadas = esquemas.stream()
                    .filter(e -> e.getEdadMeses() <= edadMeses && e.getObligatoria())
                    .filter(e -> vacunasAplicadas.contains(e.getVacunaId()))
                    .count();

            EstadisticasVacunacion stats = new EstadisticasVacunacion();
            stats.setTotalEsperadas((int)esperadas);
            stats.setTotalAplicadas(aplicadas.size());
            stats.setObligatoriasAplicadas((int)obligatoriasAplicadas);

            if (esperadas > 0) {
                stats.setPorcentajeCompletitud((obligatoriasAplicadas * 100.0) / esperadas);
            } else {
                stats.setPorcentajeCompletitud(100.0);
            }

            return stats;

        } catch (Exception e) {
            logger.error("Error al calcular estadísticas", e);
            return null;
        }
    }

    // Métodos auxiliares privados

    private String validarDatos(DatosAplicacion datos) {
        if (datos == null) return "Datos requeridos";
        if (datos.getNinoId() == null) return "ID del niño requerido";
        if (datos.getVacunaId() == null) return "ID de vacuna requerido";
        if (datos.getFechaAplicacion() == null) return "Fecha de aplicación requerida";
        if (datos.getFechaAplicacion().isAfter(LocalDate.now())) return "La fecha no puede ser futura";
        if (datos.getCentroSaludId() == null) return "Centro de salud requerido";

        return null; // Válido
    }

    private boolean yaFueAplicada(Integer ninoId, Integer vacunaId) {
        List<RegistroVacuna> registros = registroVacunaDAO.findByNinoAndVacuna(ninoId, vacunaId);
        return !registros.isEmpty();
    }

    private RegistroVacuna crearRegistro(DatosAplicacion datos, Nino nino, Vacuna vacuna) {
        RegistroVacuna registro = new RegistroVacuna();
        registro.setNinoId(datos.getNinoId());
        registro.setVacunaId(datos.getVacunaId());
        registro.setFechaAplicacion(datos.getFechaAplicacion());
        registro.setCentroSaludId(datos.getCentroSaludId());
        registro.setLote(datos.getLote());
        registro.setObservaciones(datos.getObservaciones());
        registro.setActivo(true);
        registro.setFechaRegistro(LocalDateTime.now());

        return registro;
    }

    private int calcularEdadEnMeses(LocalDate fechaNacimiento) {
        return (int) ChronoUnit.MONTHS.between(fechaNacimiento, LocalDate.now());
    }

    private String obtenerNombreVacuna(Integer vacunaId) {
        Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
        return vacunaOpt.map(Vacuna::getNombre).orElse("Vacuna desconocida");
    }

    // Clases auxiliares simplificadas

    public static class DatosAplicacion {
        private Integer ninoId;
        private Integer vacunaId;
        private LocalDate fechaAplicacion;
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
        public Integer getCentroSaludId() { return centroSaludId; }
        public void setCentroSaludId(Integer centroSaludId) { this.centroSaludId = centroSaludId; }
        public String getLote() { return lote; }
        public void setLote(String lote) { this.lote = lote; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }

    public static class ResultadoVacunacion {
        private final boolean exito;
        private final String mensaje;
        private final RegistroVacuna registro;

        private ResultadoVacunacion(boolean exito, String mensaje, RegistroVacuna registro) {
            this.exito = exito;
            this.mensaje = mensaje;
            this.registro = registro;
        }

        public static ResultadoVacunacion exito(RegistroVacuna registro, String mensaje) {
            return new ResultadoVacunacion(true, mensaje, registro);
        }

        public static ResultadoVacunacion error(String mensaje) {
            return new ResultadoVacunacion(false, mensaje, null);
        }

        public boolean isExito() { return exito; }
        public String getMensaje() { return mensaje; }
        public RegistroVacuna getRegistro() { return registro; }
    }

    public static class VacunaPendiente {
        private Integer vacunaId;
        private String nombreVacuna;
        private Integer edadRecomendada;
        private Boolean obligatoria;
        private boolean atrasada;
        private int mesesAtraso;

        // Getters y Setters
        public Integer getVacunaId() { return vacunaId; }
        public void setVacunaId(Integer vacunaId) { this.vacunaId = vacunaId; }
        public String getNombreVacuna() { return nombreVacuna; }
        public void setNombreVacuna(String nombreVacuna) { this.nombreVacuna = nombreVacuna; }
        public Integer getEdadRecomendada() { return edadRecomendada; }
        public void setEdadRecomendada(Integer edadRecomendada) { this.edadRecomendada = edadRecomendada; }
        public Boolean getObligatoria() { return obligatoria; }
        public void setObligatoria(Boolean obligatoria) { this.obligatoria = obligatoria; }
        public boolean isAtrasada() { return atrasada; }
        public void setAtrasada(boolean atrasada) { this.atrasada = atrasada; }
        public int getMesesAtraso() { return mesesAtraso; }
        public void setMesesAtraso(int mesesAtraso) { this.mesesAtraso = mesesAtraso; }
    }

    public static class EstadisticasVacunacion {
        private int totalEsperadas;
        private int totalAplicadas;
        private int obligatoriasAplicadas;
        private double porcentajeCompletitud;

        // Getters y Setters
        public int getTotalEsperadas() { return totalEsperadas; }
        public void setTotalEsperadas(int totalEsperadas) { this.totalEsperadas = totalEsperadas; }
        public int getTotalAplicadas() { return totalAplicadas; }
        public void setTotalAplicadas(int totalAplicadas) { this.totalAplicadas = totalAplicadas; }
        public int getObligatoriasAplicadas() { return obligatoriasAplicadas; }
        public void setObligatoriasAplicadas(int obligatoriasAplicadas) { this.obligatoriasAplicadas = obligatoriasAplicadas; }
        public double getPorcentajeCompletitud() { return porcentajeCompletitud; }
        public void setPorcentajeCompletitud(double porcentajeCompletitud) { this.porcentajeCompletitud = porcentajeCompletitud; }
    }
}