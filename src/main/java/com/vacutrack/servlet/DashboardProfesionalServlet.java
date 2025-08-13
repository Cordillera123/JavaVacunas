package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.RegistroVacunaDAO;
import com.vacutrack.dao.VacunaDAO;
import com.vacutrack.dao.CentroSaludDAO;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.model.Nino;
import com.vacutrack.model.RegistroVacuna;
import com.vacutrack.model.Vacuna;
import com.vacutrack.model.CentroSalud;
import com.vacutrack.service.VacunacionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Servlet del dashboard principal para profesionales de enfermería
 * Muestra herramientas para búsqueda de pacientes y registro de vacunas
 * VERSIÓN CORREGIDA - Compatible con BD y DAOs existentes
 *
 * @author VACU-TRACK Team
 * @version 1.2 - Totalmente corregida
 */
@WebServlet("/dashboard-profesional")
public class DashboardProfesionalServlet extends HttpServlet {

    private NinoDAO ninoDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private VacunaDAO vacunaDAO;
    private CentroSaludDAO centroSaludDAO;
    private VacunacionService vacunacionService;

    @Override
    public void init() throws ServletException {
        ninoDAO = NinoDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        vacunaDAO = VacunaDAO.getInstance();
        centroSaludDAO = CentroSaludDAO.getInstance();
        vacunacionService = VacunacionService.getInstance();
    }

    /**
     * Muestra el dashboard principal del profesional
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }

        // Verificar que sea un profesional de enfermería
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        if (!"PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }

        try {
            ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
            if (profesional == null) {
                response.sendRedirect(getServletContext().getContextPath() + "/login");
                return;
            }

            // Determinar la acción a realizar
            String action = request.getParameter("action");

            switch (action != null ? action : "") {
                case "buscar-nino":
                    buscarNino(request, response, profesional);
                    break;
                case "ver-historial":
                    verHistorialNino(request, response, profesional);
                    break;
                default:
                    cargarDashboardPrincipal(request, response, profesional);
                    break;
            }

        } catch (Exception e) {
            getServletContext().log("Error al cargar dashboard de profesional", e);
            request.setAttribute("error", "Error al cargar la información. Intente nuevamente");
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard-profesional.jsp").forward(request, response);
        }
    }

    /**
     * Maneja acciones específicas del dashboard
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        switch (action != null ? action : "") {
            case "buscar-nino":
                buscarNino(request, response, null);
                break;
            default:
                doGet(request, response);
                break;
        }
    }

    /**
     * Carga el dashboard principal con estadísticas y herramientas
     */
    private void cargarDashboardPrincipal(HttpServletRequest request, HttpServletResponse response,
                                          ProfesionalEnfermeria profesional) throws ServletException, IOException {

        // 1. Información del profesional y centro de salud
        cargarInformacionProfesional(request, profesional);

        // 2. Estadísticas básicas del profesional
        cargarEstadisticasBasicas(request, profesional);

        // 3. Actividad reciente del profesional (simplificado)
        cargarActividadReciente(request, profesional);

        // 4. Catálogo de vacunas disponibles
        cargarCatalogoVacunas(request);

        // 5. Información adicional
        request.setAttribute("fechaActual", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        request.setAttribute("vistaActual", "dashboard");

        // Mostrar dashboard
        request.getRequestDispatcher("/WEB-INF/jsp/dashboard-profesional.jsp").forward(request, response);
    }

    /**
     * Busca un niño por cédula o nombre
     */
    private void buscarNino(HttpServletRequest request, HttpServletResponse response,
                            ProfesionalEnfermeria profesional) throws ServletException, IOException {

        if (profesional == null) {
            HttpSession session = request.getSession(false);
            profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
        }

        String termino = request.getParameter("termino");
        String tipoBusqueda = request.getParameter("tipoBusqueda"); // "cedula" o "nombre"

        // Cargar información base del dashboard
        cargarInformacionProfesional(request, profesional);
        cargarCatalogoVacunas(request);

        if (termino == null || termino.trim().isEmpty()) {
            request.setAttribute("error", "Ingrese un término de búsqueda");
            request.setAttribute("vistaActual", "busqueda");
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard-profesional.jsp").forward(request, response);
            return;
        }

        try {
            List<Nino> ninosEncontrados = new ArrayList<>();

            if ("cedula".equals(tipoBusqueda)) {
                // CORREGIDO: Búsqueda por cédula - retorna Optional
                Optional<Nino> ninoOpt = ninoDAO.findByCedula(termino.trim());
                if (ninoOpt.isPresent()) {
                    ninosEncontrados.add(ninoOpt.get());
                }
            } else {
                // CORREGIDO: Usar searchByName en lugar de findByNombre
                ninosEncontrados = ninoDAO.searchByName(termino.trim());
            }

            request.setAttribute("ninosEncontrados", ninosEncontrados);
            request.setAttribute("terminoBusqueda", termino);
            request.setAttribute("tipoBusqueda", tipoBusqueda);
            request.setAttribute("vistaActual", "busqueda");

            // Si se encontró exactamente un niño, cargar su información detallada
            if (ninosEncontrados.size() == 1) {
                Nino nino = ninosEncontrados.get(0);
                cargarInformacionDetalladaNino(request, nino);
                request.setAttribute("ninoSeleccionado", nino);
            }

        } catch (Exception e) {
            getServletContext().log("Error en búsqueda de niño", e);
            request.setAttribute("error", "Error al buscar el niño. Intente nuevamente");
        }

        request.getRequestDispatcher("/WEB-INF/jsp/dashboard-profesional.jsp").forward(request, response);
    }

    /**
     * Ve el historial completo de un niño
     */
    private void verHistorialNino(HttpServletRequest request, HttpServletResponse response,
                                  ProfesionalEnfermeria profesional) throws ServletException, IOException {

        String ninoIdParam = request.getParameter("ninoId");

        if (ninoIdParam == null || ninoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "ID de niño no válido");
            cargarDashboardPrincipal(request, response, profesional);
            return;
        }

        try {
            Integer ninoId = Integer.parseInt(ninoIdParam);
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);

            if (!ninoOpt.isPresent()) {
                request.setAttribute("error", "Niño no encontrado");
                cargarDashboardPrincipal(request, response, profesional);
                return;
            }

            Nino nino = ninoOpt.get();

            // Cargar información base
            cargarInformacionProfesional(request, profesional);
            cargarCatalogoVacunas(request);

            // Cargar información detallada del niño
            cargarInformacionDetalladaNino(request, nino);
            request.setAttribute("ninoSeleccionado", nino);
            request.setAttribute("vistaActual", "historial");

        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de niño no válido");
            cargarDashboardPrincipal(request, response, profesional);
            return;
        }

        request.getRequestDispatcher("/WEB-INF/jsp/dashboard-profesional.jsp").forward(request, response);
    }

    /**
     * Carga información del profesional y su centro de salud
     */
    private void cargarInformacionProfesional(HttpServletRequest request, ProfesionalEnfermeria profesional) {

        request.setAttribute("nombreProfesional", profesional.obtenerNombreCompleto());
        request.setAttribute("numeroColegio", profesional.getNumeroColegio());
        request.setAttribute("especialidad", profesional.getEspecialidad());

        // Información del centro de salud
        if (profesional.getCentroSaludId() != null) {
            try {
                Optional<CentroSalud> centroOpt = centroSaludDAO.findById(profesional.getCentroSaludId());
                if (centroOpt.isPresent()) {
                    CentroSalud centro = centroOpt.get();
                    request.setAttribute("centroSalud", centro);
                    request.setAttribute("nombreCentro", centro.getNombre());
                    request.setAttribute("direccionCentro", centro.getDireccion());
                }
            } catch (Exception e) {
                getServletContext().log("Error al cargar información del centro de salud", e);
            }
        }
    }

    /**
     * Carga estadísticas básicas del profesional (simplificado)
     */
    private void cargarEstadisticasBasicas(HttpServletRequest request, ProfesionalEnfermeria profesional) {

        try {
            LocalDate hoy = LocalDate.now();
            LocalDate inicioSemana = hoy.minusDays(7);
            LocalDate inicioMes = hoy.withDayOfMonth(1);

            // CORREGIDO: Usar findByFechaRange en lugar de findByFecha
            List<RegistroVacuna> vacunasHoy = registroVacunaDAO.findByFechaRange(hoy, hoy).stream()
                    .filter(rv -> profesional.getId().equals(rv.getProfesionalId()))
                    .collect(Collectors.toList());

            List<RegistroVacuna> vacunasSemana = registroVacunaDAO.findByFechaRange(inicioSemana, hoy).stream()
                    .filter(rv -> profesional.getId().equals(rv.getProfesionalId()))
                    .collect(Collectors.toList());

            List<RegistroVacuna> vacunasMes = registroVacunaDAO.findByFechaRange(inicioMes, hoy).stream()
                    .filter(rv -> profesional.getId().equals(rv.getProfesionalId()))
                    .collect(Collectors.toList());

            // Contar niños únicos atendidos este mes
            long ninosAtendidosMes = vacunasMes.stream()
                    .map(RegistroVacuna::getNinoId)
                    .distinct()
                    .count();

            request.setAttribute("vacunasProfesionalHoy", vacunasHoy.size());
            request.setAttribute("vacunasProfesionalSemana", vacunasSemana.size());
            request.setAttribute("vacunasProfesionalMes", vacunasMes.size());
            request.setAttribute("ninosAtendidosMes", (int) ninosAtendidosMes);

            // Estadísticas del centro (si aplica)
            if (profesional.getCentroSaludId() != null) {
                List<RegistroVacuna> vacunasCentroHoy = registroVacunaDAO.findByFechaRange(hoy, hoy).stream()
                        .filter(rv -> profesional.getCentroSaludId().equals(rv.getCentroSaludId()))
                        .collect(Collectors.toList());

                request.setAttribute("vacunasCentroHoy", vacunasCentroHoy.size());
            }

        } catch (Exception e) {
            getServletContext().log("Error al cargar estadísticas básicas", e);
            // Establecer valores por defecto
            request.setAttribute("vacunasProfesionalHoy", 0);
            request.setAttribute("vacunasProfesionalSemana", 0);
            request.setAttribute("vacunasProfesionalMes", 0);
            request.setAttribute("ninosAtendidosMes", 0);
            request.setAttribute("vacunasCentroHoy", 0);
        }
    }

    /**
     * Carga actividad reciente del profesional (simplificado)
     */
    private void cargarActividadReciente(HttpServletRequest request, ProfesionalEnfermeria profesional) {

        try {
            LocalDate hace7Dias = LocalDate.now().minusDays(7);

            // CORREGIDO: Usar findByFechaRange en lugar de findByFechaPeriodo
            List<RegistroVacuna> actividadReciente = registroVacunaDAO.findByFechaRange(hace7Dias, LocalDate.now())
                    .stream()
                    .filter(rv -> profesional.getId().equals(rv.getProfesionalId()))
                    .sorted((r1, r2) -> r2.getFechaAplicacion().compareTo(r1.getFechaAplicacion()))
                    .limit(10)
                    .collect(Collectors.toList());

            request.setAttribute("actividadReciente", actividadReciente);

        } catch (Exception e) {
            getServletContext().log("Error al cargar actividad reciente", e);
            request.setAttribute("actividadReciente", List.of());
        }
    }

    /**
     * Carga el catálogo de vacunas disponibles
     */
    private void cargarCatalogoVacunas(HttpServletRequest request) {

        try {
            // CORREGIDO: Usar findActiveVaccines en lugar de findActive
            List<Vacuna> vacunasDisponibles = vacunaDAO.findActiveVaccines();
            request.setAttribute("vacunasDisponibles", vacunasDisponibles);

            // Crear mapa para acceso rápido por ID
            Map<Integer, Vacuna> mapaVacunas = vacunasDisponibles.stream()
                    .collect(Collectors.toMap(Vacuna::getId, v -> v));
            request.setAttribute("mapaVacunas", mapaVacunas);

        } catch (Exception e) {
            getServletContext().log("Error al cargar catálogo de vacunas", e);
            request.setAttribute("vacunasDisponibles", List.of());
            request.setAttribute("mapaVacunas", new HashMap<>());
        }
    }

    /**
     * Carga información detallada de un niño específico (simplificado)
     */
    private void cargarInformacionDetalladaNino(HttpServletRequest request, Nino nino) {

        try {
            // Información básica del niño
            request.setAttribute("edadNino", nino.obtenerEdadFormateada());
            request.setAttribute("sexoNino", nino.obtenerSexoLegible());

            // Historial de vacunas aplicadas usando método existente
            List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNino(nino.getId());
            request.setAttribute("historialVacunas", historialVacunas);

            // Estado básico del esquema de vacunación
            try {
                VacunacionService.EstadisticasVacunacion estadisticas = vacunacionService.obtenerEstadisticas(nino.getId());
                if (estadisticas != null) {
                    request.setAttribute("porcentajeCompletitud", Math.round(estadisticas.getPorcentajeCompletitud()));
                    request.setAttribute("vacunasAplicadas", estadisticas.getTotalAplicadas());
                    request.setAttribute("vacunasTotales", estadisticas.getTotalEsperadas());
                    request.setAttribute("estadoEsquema",
                            estadisticas.getPorcentajeCompletitud() >= 95 ? "COMPLETO" : "EN_PROCESO");
                } else {
                    // Valores por defecto
                    request.setAttribute("porcentajeCompletitud", calcularPorcentajeBasico(historialVacunas.size()));
                    request.setAttribute("vacunasAplicadas", historialVacunas.size());
                    request.setAttribute("vacunasTotales", 20);
                    request.setAttribute("estadoEsquema", "EN_PROCESO");
                }
            } catch (Exception e) {
                getServletContext().log("Error al obtener estadísticas de vacunación", e);
                request.setAttribute("porcentajeCompletitud", calcularPorcentajeBasico(historialVacunas.size()));
                request.setAttribute("vacunasAplicadas", historialVacunas.size());
                request.setAttribute("vacunasTotales", 20);
                request.setAttribute("estadoEsquema", "INDETERMINADO");
            }

            // Próximas vacunas (simplificado)
            try {
                List<VacunacionService.VacunaPendiente> vacunasPendientes =
                        vacunacionService.obtenerVacunasPendientes(nino.getId());
                request.setAttribute("proximasVacunas", vacunasPendientes);
            } catch (Exception e) {
                getServletContext().log("Error al obtener próximas vacunas", e);
                request.setAttribute("proximasVacunas", List.of());
            }

            // Información del padre (si está disponible)
            if (nino.getPadre() != null) {
                request.setAttribute("informacionPadre", nino.getPadre().obtenerInformacionCompleta());
            }

        } catch (Exception e) {
            getServletContext().log("Error al cargar información detallada del niño: " + nino.getId(), e);
            request.setAttribute("errorNino", "Error al cargar información del niño");
        }
    }

    /**
     * Calcula porcentaje básico de completitud
     */
    private int calcularPorcentajeBasico(int vacunasAplicadas) {
        int vacunasEsperadas = 20; // Estimado del esquema completo
        return Math.min(100, (vacunasAplicadas * 100) / vacunasEsperadas);
    }
}