package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
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

/**
 * Servlet del dashboard principal para profesionales de enfermería
 * Muestra herramientas para búsqueda de pacientes y registro de vacunas
 * 
 * @author VACU-TRACK Team
 * @version 1.0
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
        
        // 2. Estadísticas del centro de salud
        cargarEstadisticasCentro(request, profesional);
        
        // 3. Actividad reciente del profesional
        cargarActividadReciente(request, profesional);
        
        // 4. Catálogo de vacunas disponibles
        cargarCatalogoVacunas(request);
        
        // 5. Niños atendidos recientemente
        cargarNinosRecientes(request, profesional);
        
        // 6. Información adicional
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
            List<Nino> ninosEncontrados;
            
            if ("cedula".equals(tipoBusqueda)) {
                // Búsqueda por cédula
                Optional<Nino> ninoOpt = ninoDAO.findByCedula(termino.trim());
                ninosEncontrados = ninoOpt.map(List::of).orElse(List.of());
            } else {
                // Búsqueda por nombre
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
     * Carga estadísticas del centro de salud
     */
    private void cargarEstadisticasCentro(HttpServletRequest request, ProfesionalEnfermeria profesional) {
        
        if (profesional.getCentroSaludId() == null) {
            return;
        }
        
        try {
            LocalDate hoy = LocalDate.now();
            LocalDate inicioMes = hoy.withDayOfMonth(1);
            LocalDate inicioSemana = hoy.minusDays(7);
            
            // Estadísticas del centro
            int vacunasHoy = registroVacunaDAO.countByCentroAndFecha(profesional.getCentroSaludId(), hoy);
            int vacunasSemana = registroVacunaDAO.countByCentroAndFechaPeriodo(
                profesional.getCentroSaludId(), inicioSemana, hoy);
            int vacunasMes = registroVacunaDAO.countByCentroAndFechaPeriodo(
                profesional.getCentroSaludId(), inicioMes, hoy);
            
            // Estadísticas del profesional
            int vacunasProfesionalHoy = registroVacunaDAO.countByProfesionalAndFecha(profesional.getId(), hoy);
            int vacunasProfesionalSemana = registroVacunaDAO.countByProfesionalAndFechaPeriodo(
                profesional.getId(), inicioSemana, hoy);
            int vacunasProfesionalMes = registroVacunaDAO.countByProfesionalAndFechaPeriodo(
                profesional.getId(), inicioMes, hoy);
            
            // Niños únicos atendidos
            int ninosAtendidosMes = registroVacunaDAO.countNinosUnicosByProfesionalAndPeriodo(
                profesional.getId(), inicioMes, hoy);
            
            request.setAttribute("vacunasHoy", vacunasHoy);
            request.setAttribute("vacunasSemana", vacunasSemana);
            request.setAttribute("vacunasMes", vacunasMes);
            request.setAttribute("vacunasProfesionalHoy", vacunasProfesionalHoy);
            request.setAttribute("vacunasProfesionalSemana", vacunasProfesionalSemana);
            request.setAttribute("vacunasProfesionalMes", vacunasProfesionalMes);
            request.setAttribute("ninosAtendidosMes", ninosAtendidosMes);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar estadísticas del centro", e);
            // Establecer valores por defecto en caso de error
            request.setAttribute("vacunasHoy", 0);
            request.setAttribute("vacunasSemana", 0);
            request.setAttribute("vacunasMes", 0);
            request.setAttribute("vacunasProfesionalHoy", 0);
            request.setAttribute("vacunasProfesionalSemana", 0);
            request.setAttribute("vacunasProfesionalMes", 0);
            request.setAttribute("ninosAtendidosMes", 0);
        }
    }
    
    /**
     * Carga actividad reciente del profesional
     */
    private void cargarActividadReciente(HttpServletRequest request, ProfesionalEnfermeria profesional) {
        
        try {
            LocalDate hace7Dias = LocalDate.now().minusDays(7);
            List<RegistroVacuna> actividadReciente = registroVacunaDAO
                .findByProfesionalAndFechaPeriodo(profesional.getId(), hace7Dias, LocalDate.now())
                .stream()
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
            List<Vacuna> vacunasDisponibles = vacunaDAO.findActivas();
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
     * Carga niños atendidos recientemente
     */
    private void cargarNinosRecientes(HttpServletRequest request, ProfesionalEnfermeria profesional) {
        
        try {
            LocalDate hace30Dias = LocalDate.now().minusDays(30);
            
            // Obtener IDs de niños atendidos recientemente
            List<Integer> ninosIds = registroVacunaDAO
                .findByProfesionalAndFechaPeriodo(profesional.getId(), hace30Dias, LocalDate.now())
                .stream()
                .map(RegistroVacuna::getNinoId)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
            
            // Cargar información de los niños
            List<Nino> ninosRecientes = ninosIds.stream()
                .map(id -> ninoDAO.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
            
            request.setAttribute("ninosRecientes", ninosRecientes);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar niños recientes", e);
            request.setAttribute("ninosRecientes", List.of());
        }
    }
    
    /**
     * Carga información detallada de un niño específico
     */
    private void cargarInformacionDetalladaNino(HttpServletRequest request, Nino nino) {
        
        try {
            // Información básica del niño
            request.setAttribute("edadNino", nino.obtenerEdadFormateada());
            request.setAttribute("sexoNino", nino.obtenerSexoLegible());
            
            // Historial de vacunas aplicadas
            List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNinoId(nino.getId());
            request.setAttribute("historialVacunas", historialVacunas);
            
            // Estado del esquema de vacunación
            VacunacionService.EstadoEsquema estadoEsquema = vacunacionService.verificarEstadoEsquema(nino.getId());
            if (estadoEsquema != null) {
                request.setAttribute("porcentajeCompletitud", Math.round(estadoEsquema.getPorcentajeCompletitud()));
                request.setAttribute("vacunasAplicadas", estadoEsquema.getVacunasAplicadas());
                request.setAttribute("vacunasTotales", estadoEsquema.getVacunasTotales());
                request.setAttribute("estadoEsquema", estadoEsquema.getEstado());
            }
            
            // Próximas vacunas debido
            List<VacunacionService.ProximaVacuna> proximasVacunas = vacunacionService.obtenerProximasVacunas(nino.getId(), 5);
            request.setAttribute("proximasVacunas", proximasVacunas);
            
            // Vacunas vencidas
            List<VacunacionService.VacunaVencida> vacunasVencidas = vacunacionService.obtenerVacunasVencidas(nino.getId());
            request.setAttribute("vacunasVencidas", vacunasVencidas);
            request.setAttribute("tieneVacunasVencidas", !vacunasVencidas.isEmpty());
            
            // Información del padre
            if (nino.getPadre() != null) {
                request.setAttribute("informacionPadre", nino.getPadre().obtenerInformacionCompleta());
            }
            
            // Esquema completo para referencia del profesional
            List<VacunacionService.VacunaEsquema> esquemaCompleto = vacunacionService.obtenerEsquemaCompleto(nino.getId());
            request.setAttribute("esquemaCompleto", esquemaCompleto);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar información detallada del niño: " + nino.getId(), e);
            request.setAttribute("errorNino", "Error al cargar información del niño");
        }
    }
}