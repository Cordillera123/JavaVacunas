package com.vacutrack.service;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;
import com.vacutrack.util.PDFUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio simplificado de reportes para VACU-TRACK
 * Versión estudiantil con funciones básicas esenciales
 *
 * @author VACU-TRACK Team
 * @version 1.0 (Simplificado)
 */
public class ReporteService {

    private static final Logger logger = LoggerFactory.getLogger(ReporteService.class);

    // Instancia singleton
    private static ReporteService instance;

    // DAOs principales
    private final NinoDAO ninoDAO;
    private final RegistroVacunaDAO registroVacunaDAO;
    private final VacunaDAO vacunaDAO;
    private final CentroSaludDAO centroSaludDAO;
    private final PadreFamiliaDAO padreFamiliaDAO;
    private final ProfesionalEnfermeriaDAO profesionalDAO;

    // Meta de cobertura para Ecuador
    private static final double META_COBERTURA = 95.0;

    /**
     * Constructor privado para patrón singleton
     */
    private ReporteService() {
        this.ninoDAO = NinoDAO.getInstance();
        this.registroVacunaDAO = RegistroVacunaDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.centroSaludDAO = CentroSaludDAO.getInstance();
        this.padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        this.profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
    }

    /**
     * Obtiene la instancia singleton
     */
    public static synchronized ReporteService getInstance() {
        if (instance == null) {
            instance = new ReporteService();
        }
        return instance;
    }

    /**
     * Genera dashboard básico con indicadores principales
     */
    public DashboardBasico generarDashboardBasico() {
        logger.info("Generando dashboard básico");

        try {
            DashboardBasico dashboard = new DashboardBasico();
            dashboard.setFechaGeneracion(LocalDateTime.now());

            // Indicadores básicos
            dashboard.setTotalNinos(ninoDAO.countActivos());
            dashboard.setTotalVacunasAplicadas(registroVacunaDAO.countTotal());
            dashboard.setCentrosActivos(centroSaludDAO.countActivos());
            dashboard.setProfesionalesActivos(profesionalDAO.countActivos());
            dashboard.setPadresRegistrados(padreFamiliaDAO.countActivos());

            // Calcular cobertura general
            double coberturaGeneral = calcularCoberturaGeneral();
            dashboard.setCoberturaGeneral(coberturaGeneral);
            dashboard.setCumpleMeta(coberturaGeneral >= META_COBERTURA);

            logger.info("Dashboard generado exitosamente");
            return dashboard;

        } catch (Exception e) {
            logger.error("Error al generar dashboard básico", e);
            return new DashboardBasico();
        }
    }

    /**
     * Genera reporte básico de cobertura
     */
    public ReporteCoberturaBasico generarReporteCobertura() {
        logger.info("Generando reporte de cobertura");

        try {
            ReporteCoberturaBasico reporte = new ReporteCoberturaBasico();
            reporte.setFechaGeneracion(LocalDateTime.now());

            // Cobertura por vacuna
            List<CoberturaVacuna> coberturaVacunas = calcularCoberturaPorVacuna();
            reporte.setCoberturaVacunas(coberturaVacunas);

            // Cobertura por centro de salud
            List<CoberturaCentro> coberturaCentros = calcularCoberturaPorCentro();
            reporte.setCoberturaCentros(coberturaCentros);

            // Estadísticas por edad
            EstadisticasEdad estadisticasEdad = calcularEstadisticasPorEdad();
            reporte.setEstadisticasEdad(estadisticasEdad);

            logger.info("Reporte de cobertura generado exitosamente");
            return reporte;

        } catch (Exception e) {
            logger.error("Error al generar reporte de cobertura", e);
            return new ReporteCoberturaBasico();
        }
    }

    /**
     * Exporta certificado de vacunación a PDF
     */
    public byte[] exportarCertificadoPDF(Integer ninoId) {
        logger.info("Exportando certificado PDF para niño ID: {}", ninoId);

        try {
            // Obtener datos del niño
            Nino nino = ninoDAO.findById(ninoId);
            if (nino == null) {
                logger.warn("Niño no encontrado: {}", ninoId);
                return null;
            }

            // Obtener registros de vacunas
            List<RegistroVacuna> registros = registroVacunaDAO.findByNinoId(ninoId);

            // Crear certificado
            CertificadoVacunacion certificado = new CertificadoVacunacion();
            certificado.setNino(nino);
            certificado.setRegistros(registros);
            certificado.setFechaGeneracion(LocalDateTime.now());

            // Generar PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PDFUtil.crearCertificadoVacunacion(certificado, baos);

            logger.info("Certificado PDF generado exitosamente");
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Error al generar certificado PDF", e);
            return null;
        }
    }

    /**
     * Obtiene estadísticas básicas del sistema
     */
    public EstadisticasBasicas obtenerEstadisticasBasicas() {
        logger.info("Obteniendo estadísticas básicas");

        try {
            EstadisticasBasicas stats = new EstadisticasBasicas();

            // Conteos básicos
            stats.setTotalNinos(ninoDAO.countActivos());
            stats.setTotalVacunas(vacunaDAO.countActivas());
            stats.setTotalCentros(centroSaludDAO.countActivos());
            stats.setTotalRegistros(registroVacunaDAO.countTotal());

            // Porcentajes básicos
            double coberturaGeneral = calcularCoberturaGeneral();
            stats.setCoberturaGeneral(coberturaGeneral);

            // Distribución por género
            Map<String, Integer> porGenero = calcularDistribucionGenero();
            stats.setDistribucionGenero(porGenero);

            return stats;

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas básicas", e);
            return new EstadisticasBasicas();
        }
    }

    // ============= MÉTODOS PRIVADOS DE CÁLCULO =============

    /**
     * Calcula cobertura general del sistema
     */
    private double calcularCoberturaGeneral() {
        try {
            int totalNinos = ninoDAO.countActivos();
            if (totalNinos == 0) return 0.0;

            int ninosConAlgunaVacuna = registroVacunaDAO.countNinosUnicos();
            return (double) ninosConAlgunaVacuna / totalNinos * 100.0;

        } catch (Exception e) {
            logger.error("Error al calcular cobertura general", e);
            return 0.0;
        }
    }

    /**
     * Calcula cobertura por vacuna
     */
    private List<CoberturaVacuna> calcularCoberturaPorVacuna() {
        List<CoberturaVacuna> coberturas = new ArrayList<>();

        try {
            List<Vacuna> vacunas = vacunaDAO.findActivas();
            int totalNinos = ninoDAO.countActivos();

            for (Vacuna vacuna : vacunas) {
                CoberturaVacuna cobertura = new CoberturaVacuna();
                cobertura.setVacunaId(vacuna.getId());
                cobertura.setVacunaNombre(vacuna.getNombre());

                int ninosVacunados = registroVacunaDAO.countNinosVacunadosPorVacuna(vacuna.getId());
                cobertura.setNinosVacunados(ninosVacunados);
                cobertura.setTotalNinos(totalNinos);

                double porcentaje = totalNinos > 0 ? (ninosVacunados * 100.0) / totalNinos : 0.0;
                cobertura.setPorcentajeCobertura(porcentaje);
                cobertura.setCumpleMeta(porcentaje >= META_COBERTURA);

                coberturas.add(cobertura);
            }

        } catch (Exception e) {
            logger.error("Error al calcular cobertura por vacuna", e);
        }

        return coberturas;
    }

    /**
     * Calcula cobertura por centro de salud
     */
    private List<CoberturaCentro> calcularCoberturaPorCentro() {
        List<CoberturaCentro> coberturas = new ArrayList<>();

        try {
            List<CentroSalud> centros = centroSaludDAO.findActivos();

            for (CentroSalud centro : centros) {
                CoberturaCentro cobertura = new CoberturaCentro();
                cobertura.setCentroId(centro.getId());
                cobertura.setCentroNombre(centro.getNombre());

                int vacunasAplicadas = registroVacunaDAO.countByCentro(centro.getId());
                cobertura.setVacunasAplicadas(vacunasAplicadas);

                int ninosAtendidos = registroVacunaDAO.countNinosUnicosByCentro(centro.getId());
                cobertura.setNinosAtendidos(ninosAtendidos);

                coberturas.add(cobertura);
            }

            // Ordenar por vacunas aplicadas
            coberturas.sort((a, b) -> Integer.compare(b.getVacunasAplicadas(), a.getVacunasAplicadas()));

        } catch (Exception e) {
            logger.error("Error al calcular cobertura por centro", e);
        }

        return coberturas;
    }

    /**
     * Calcula estadísticas por edad
     */
    private EstadisticasEdad calcularEstadisticasPorEdad() {
        EstadisticasEdad stats = new EstadisticasEdad();
        Map<String, Integer> porGrupoEdad = new HashMap<>();

        try {
            List<Nino> ninos = ninoDAO.findActivos();
            LocalDate hoy = LocalDate.now();

            for (Nino nino : ninos) {
                int edadMeses = (int) ChronoUnit.MONTHS.between(nino.getFechaNacimiento(), hoy);
                String grupoEtario = determinarGrupoEtario(edadMeses);
                porGrupoEdad.merge(grupoEtario, 1, Integer::sum);
            }

            stats.setDistribucionPorEdad(porGrupoEdad);
            stats.setTotalNinos(ninos.size());

        } catch (Exception e) {
            logger.error("Error al calcular estadísticas por edad", e);
        }

        return stats;
    }

    /**
     * Calcula distribución por género
     */
    private Map<String, Integer> calcularDistribucionGenero() {
        Map<String, Integer> distribucion = new HashMap<>();

        try {
            List<Nino> ninos = ninoDAO.findActivos();
            for (Nino nino : ninos) {
                String genero = nino.getSexo();
                distribucion.merge(genero, 1, Integer::sum);
            }
        } catch (Exception e) {
            logger.error("Error al calcular distribución por género", e);
        }

        return distribucion;
    }

    /**
     * Determina grupo etario según edad en meses
     */
    private String determinarGrupoEtario(int edadMeses) {
        if (edadMeses < 1) return "Recién nacidos (0-1 mes)";
        if (edadMeses < 12) return "Bebés (1-11 meses)";
        if (edadMeses < 24) return "Niños pequeños (1-2 años)";
        if (edadMeses < 60) return "Preescolares (2-5 años)";
        return "Escolares (5+ años)";
    }

    // ============= CLASES INTERNAS SIMPLIFICADAS =============

    /**
     * Dashboard básico con indicadores principales
     */
    public static class DashboardBasico {
        private LocalDateTime fechaGeneracion;
        private int totalNinos;
        private int totalVacunasAplicadas;
        private int centrosActivos;
        private int profesionalesActivos;
        private int padresRegistrados;
        private double coberturaGeneral;
        private boolean cumpleMeta;

        // Getters y Setters
        public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
        public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
        public int getTotalNinos() { return totalNinos; }
        public void setTotalNinos(int totalNinos) { this.totalNinos = totalNinos; }
        public int getTotalVacunasAplicadas() { return totalVacunasAplicadas; }
        public void setTotalVacunasAplicadas(int totalVacunasAplicadas) { this.totalVacunasAplicadas = totalVacunasAplicadas; }
        public int getCentrosActivos() { return centrosActivos; }
        public void setCentrosActivos(int centrosActivos) { this.centrosActivos = centrosActivos; }
        public int getProfesionalesActivos() { return profesionalesActivos; }
        public void setProfesionalesActivos(int profesionalesActivos) { this.profesionalesActivos = profesionalesActivos; }
        public int getPadresRegistrados() { return padresRegistrados; }
        public void setPadresRegistrados(int padresRegistrados) { this.padresRegistrados = padresRegistrados; }
        public double getCoberturaGeneral() { return coberturaGeneral; }
        public void setCoberturaGeneral(double coberturaGeneral) { this.coberturaGeneral = coberturaGeneral; }
        public boolean isCumpleMeta() { return cumpleMeta; }
        public void setCumpleMeta(boolean cumpleMeta) { this.cumpleMeta = cumpleMeta; }
    }

    /**
     * Reporte básico de cobertura
     */
    public static class ReporteCoberturaBasico {
        private LocalDateTime fechaGeneracion;
        private List<CoberturaVacuna> coberturaVacunas;
        private List<CoberturaCentro> coberturaCentros;
        private EstadisticasEdad estadisticasEdad;

        // Getters y Setters
        public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
        public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
        public List<CoberturaVacuna> getCoberturaVacunas() { return coberturaVacunas; }
        public void setCoberturaVacunas(List<CoberturaVacuna> coberturaVacunas) { this.coberturaVacunas = coberturaVacunas; }
        public List<CoberturaCentro> getCoberturaCentros() { return coberturaCentros; }
        public void setCoberturaCentros(List<CoberturaCentro> coberturaCentros) { this.coberturaCentros = coberturaCentros; }
        public EstadisticasEdad getEstadisticasEdad() { return estadisticasEdad; }
        public void setEstadisticasEdad(EstadisticasEdad estadisticasEdad) { this.estadisticasEdad = estadisticasEdad; }
    }

    /**
     * Cobertura por vacuna
     */
    public static class CoberturaVacuna {
        private Integer vacunaId;
        private String vacunaNombre;
        private int ninosVacunados;
        private int totalNinos;
        private double porcentajeCobertura;
        private boolean cumpleMeta;

        // Getters y Setters
        public Integer getVacunaId() { return vacunaId; }
        public void setVacunaId(Integer vacunaId) { this.vacunaId = vacunaId; }
        public String getVacunaNombre() { return vacunaNombre; }
        public void setVacunaNombre(String vacunaNombre) { this.vacunaNombre = vacunaNombre; }
        public int getNinosVacunados() { return ninosVacunados; }
        public void setNinosVacunados(int ninosVacunados) { this.ninosVacunados = ninosVacunados; }
        public int getTotalNinos() { return totalNinos; }
        public void setTotalNinos(int totalNinos) { this.totalNinos = totalNinos; }
        public double getPorcentajeCobertura() { return porcentajeCobertura; }
        public void setPorcentajeCobertura(double porcentajeCobertura) { this.porcentajeCobertura = porcentajeCobertura; }
        public boolean isCumpleMeta() { return cumpleMeta; }
        public void setCumpleMeta(boolean cumpleMeta) { this.cumpleMeta = cumpleMeta; }
    }

    /**
     * Cobertura por centro de salud
     */
    public static class CoberturaCentro {
        private Integer centroId;
        private String centroNombre;
        private int vacunasAplicadas;
        private int ninosAtendidos;

        // Getters y Setters
        public Integer getCentroId() { return centroId; }
        public void setCentroId(Integer centroId) { this.centroId = centroId; }
        public String getCentroNombre() { return centroNombre; }
        public void setCentroNombre(String centroNombre) { this.centroNombre = centroNombre; }
        public int getVacunasAplicadas() { return vacunasAplicadas; }
        public void setVacunasAplicadas(int vacunasAplicadas) { this.vacunasAplicadas = vacunasAplicadas; }
        public int getNinosAtendidos() { return ninosAtendidos; }
        public void setNinosAtendidos(int ninosAtendidos) { this.ninosAtendidos = ninosAtendidos; }
    }

    /**
     * Estadísticas por edad
     */
    public static class EstadisticasEdad {
        private Map<String, Integer> distribucionPorEdad;
        private int totalNinos;

        // Getters y Setters
        public Map<String, Integer> getDistribucionPorEdad() { return distribucionPorEdad; }
        public void setDistribucionPorEdad(Map<String, Integer> distribucionPorEdad) { this.distribucionPorEdad = distribucionPorEdad; }
        public int getTotalNinos() { return totalNinos; }
        public void setTotalNinos(int totalNinos) { this.totalNinos = totalNinos; }
    }

    /**
     * Estadísticas básicas del sistema
     */
    public static class EstadisticasBasicas {
        private int totalNinos;
        private int totalVacunas;
        private int totalCentros;
        private int totalRegistros;
        private double coberturaGeneral;
        private Map<String, Integer> distribucionGenero;

        // Getters y Setters
        public int getTotalNinos() { return totalNinos; }
        public void setTotalNinos(int totalNinos) { this.totalNinos = totalNinos; }
        public int getTotalVacunas() { return totalVacunas; }
        public void setTotalVacunas(int totalVacunas) { this.totalVacunas = totalVacunas; }
        public int getTotalCentros() { return totalCentros; }
        public void setTotalCentros(int totalCentros) { this.totalCentros = totalCentros; }
        public int getTotalRegistros() { return totalRegistros; }
        public void setTotalRegistros(int totalRegistros) { this.totalRegistros = totalRegistros; }
        public double getCoberturaGeneral() { return coberturaGeneral; }
        public void setCoberturaGeneral(double coberturaGeneral) { this.coberturaGeneral = coberturaGeneral; }
        public Map<String, Integer> getDistribucionGenero() { return distribucionGenero; }
        public void setDistribucionGenero(Map<String, Integer> distribucionGenero) { this.distribucionGenero = distribucionGenero; }
    }

    /**
     * Certificado de vacunación para PDF
     */
    public static class CertificadoVacunacion {
        private Nino nino;
        private List<RegistroVacuna> registros;
        private LocalDateTime fechaGeneracion;

        // Getters y Setters
        public Nino getNino() { return nino; }
        public void setNino(Nino nino) { this.nino = nino; }
        public List<RegistroVacuna> getRegistros() { return registros; }
        public void setRegistros(List<RegistroVacuna> registros) { this.registros = registros; }
        public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
        public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    }
}