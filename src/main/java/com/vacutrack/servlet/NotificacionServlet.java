package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.NotificacionDAO;
import com.vacutrack.dao.VacunaDAO;
import com.vacutrack.model.Usuario;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.model.Nino;
import com.vacutrack.model.Notificacion;
import com.vacutrack.model.Vacuna;
import com.vacutrack.service.NotificacionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Servlet para gestión de notificaciones del sistema
 * Maneja alertas de vacunas, recordatorios y notificaciones urgentes
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/notificaciones")
public class NotificacionServlet extends HttpServlet {
    
    private NinoDAO ninoDAO;
    private NotificacionDAO notificacionDAO;
    private VacunaDAO vacunaDAO;
    private NotificacionService notificacionService;
    
    @Override
    public void init() throws ServletException {
        ninoDAO = NinoDAO.getInstance();
        notificacionDAO = NotificacionDAO.getInstance();
        vacunaDAO = VacunaDAO.getInstance();
        notificacionService = NotificacionService.getInstance();
    }
    
    /**
     * Muestra la página de notificaciones o ejecuta acciones específicas
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            switch (action != null ? action : "") {
                case "marcar-leida":
                    marcarNotificacionLeida(request, response);
                    break;
                case "marcar-todas-leidas":
                    marcarTodasLeidas(request, response);
                    break;
                case "obtener-contador":
                    obtenerContadorNotificaciones(request, response);
                    break;
                case "generar-notificaciones":
                    generarNotificacionesManual(request, response);
                    break;
                default:
                    mostrarNotificaciones(request, response);
                    break;
            }
        } catch (Exception e) {
            getServletContext().log("Error en NotificacionServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del sistema");
        }
    }
    
    /**
     * Procesa solicitudes POST para acciones de notificaciones
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        switch (action != null ? action : "") {
            case "marcar-leida":
                marcarNotificacionLeida(request, response);
                break;
            case "marcar-todas-leidas":
                marcarTodasLeidas(request, response);
                break;
            default:
                doGet(request, response);
                break;
        }
    }
    
    /**
     * Muestra la página principal de notificaciones
     */
    private void mostrarNotificaciones(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        if ("PADRE_FAMILIA".equals(tipoUsuario)) {
            mostrarNotificacionesPadre(request, response);
        } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            mostrarNotificacionesProfesional(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado");
        }
    }
    
    /**
     * Muestra notificaciones para padres de familia
     */
    private void mostrarNotificacionesPadre(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
        
        if (padre == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        // Obtener filtros
        String tipoFiltro = request.getParameter("tipo"); // URGENTE, PROXIMA, RECORDATORIO
        String estadoFiltro = request.getParameter("estado"); // PENDIENTE, LEIDA
        String ninoIdParam = request.getParameter("ninoId");
        
        // Obtener todos los niños del padre
        List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
        request.setAttribute("ninos", ninos);
        
        if (ninos.isEmpty()) {
            request.setAttribute("sinNinos", true);
            request.setAttribute("nombrePadre", padre.obtenerNombreCompleto());
            request.getRequestDispatcher("/WEB-INF/jsp/notificaciones.jsp").forward(request, response);
            return;
        }
        
        // Obtener IDs de los niños
        List<Integer> ninosIds = ninos.stream().map(Nino::getId).collect(Collectors.toList());
        
        // Aplicar filtros y obtener notificaciones
        List<Notificacion> notificaciones;
        
        if (ninoIdParam != null && !ninoIdParam.trim().isEmpty()) {
            try {
                Integer ninoId = Integer.parseInt(ninoIdParam);
                if (ninosIds.contains(ninoId)) {
                    notificaciones = obtenerNotificacionesFiltradas(List.of(ninoId), tipoFiltro, estadoFiltro);
                } else {
                    notificaciones = List.of(); // Niño no pertenece al padre
                }
            } catch (NumberFormatException e) {
                notificaciones = obtenerNotificacionesFiltradas(ninosIds, tipoFiltro, estadoFiltro);
            }
        } else {
            notificaciones = obtenerNotificacionesFiltradas(ninosIds, tipoFiltro, estadoFiltro);
        }
        
        // Enriquecer notificaciones con información adicional
        enriquecerNotificaciones(notificaciones);
        
        // Organizar notificaciones por categorías
        organizarNotificacionesPorCategoria(request, notificaciones);
        
        // Estadísticas de notificaciones
        calcularEstadisticasNotificaciones(request, ninosIds);
        
        // Información del usuario
        request.setAttribute("esPadre", true);
        request.setAttribute("nombrePadre", padre.obtenerNombreCompleto());
        request.setAttribute("filtroTipo", tipoFiltro);
        request.setAttribute("filtroEstado", estadoFiltro);
        request.setAttribute("filtroNinoId", ninoIdParam);
        
        request.getRequestDispatcher("/WEB-INF/jsp/notificaciones.jsp").forward(request, response);
    }
    
    /**
     * Muestra notificaciones para profesionales de enfermería
     */
    private void mostrarNotificacionesProfesional(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
        
        if (profesional == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        // Los profesionales ven notificaciones del sistema en general
        String tipoFiltro = request.getParameter("tipo");
        String estadoFiltro = request.getParameter("estado");
        String fechaDesdeParam = request.getParameter("fechaDesde");
        String fechaHastaParam = request.getParameter("fechaHasta");
        
        LocalDate fechaDesde = null;
        LocalDate fechaHasta = null;
        
        // Parsear fechas si se proporcionan
        try {
            if (fechaDesdeParam != null && !fechaDesdeParam.trim().isEmpty()) {
                fechaDesde = LocalDate.parse(fechaDesdeParam);
            }
            if (fechaHastaParam != null && !fechaHastaParam.trim().isEmpty()) {
                fechaHasta = LocalDate.parse(fechaHastaParam);
            }
        } catch (Exception e) {
            // Ignorar fechas inválidas
        }
        
        // Por defecto, mostrar notificaciones de la última semana
        if (fechaDesde == null && fechaHasta == null) {
            fechaDesde = LocalDate.now().minusDays(7);
            fechaHasta = LocalDate.now();
        }
        
        // Obtener notificaciones según filtros
        List<Notificacion> notificaciones = obtenerNotificacionesProfesional(
            tipoFiltro, estadoFiltro, fechaDesde, fechaHasta);
        
        // Enriquecer notificaciones
        enriquecerNotificaciones(notificaciones);
        
        // Organizar por categorías
        organizarNotificacionesPorCategoria(request, notificaciones);
        
        // Estadísticas generales para profesionales
        calcularEstadisticasGenerales(request);
        
        // Información del usuario
        request.setAttribute("esProfesional", true);
        request.setAttribute("nombreProfesional", profesional.obtenerNombreCompleto());
        request.setAttribute("filtroTipo", tipoFiltro);
        request.setAttribute("filtroEstado", estadoFiltro);
        request.setAttribute("filtroFechaDesde", fechaDesdeParam);
        request.setAttribute("filtroFechaHasta", fechaHastaParam);
        
        request.getRequestDispatcher("/WEB-INF/jsp/notificaciones.jsp").forward(request, response);
    }
    
    /**
     * Marca una notificación específica como leída
     */
    private void marcarNotificacionLeida(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String notificacionIdParam = request.getParameter("notificacionId");
        String returnJson = request.getParameter("json"); // Para respuestas AJAX
        
        boolean exito = false;
        String mensaje = "Error al marcar notificación";
        
        if (notificacionIdParam != null && !notificacionIdParam.trim().isEmpty()) {
            try {
                Integer notificacionId = Integer.parseInt(notificacionIdParam);
                
                // Verificar que el usuario tenga acceso a esta notificación
                if (tieneAccesoANotificacion(request, notificacionId)) {
                    exito = notificacionDAO.marcarComoLeida(notificacionId);
                    mensaje = exito ? "Notificación marcada como leída" : "Error al actualizar notificación";
                } else {
                    mensaje = "No tiene acceso a esta notificación";
                }
            } catch (NumberFormatException e) {
                mensaje = "ID de notificación no válido";
            }
        }
        
        // Responder según el tipo de solicitud
        if ("true".equals(returnJson)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            try (PrintWriter out = response.getWriter()) {
                out.print(String.format("{\"success\": %s, \"message\": \"%s\"}", exito, mensaje));
            }
        } else {
            if (exito) {
                request.getSession().setAttribute("successMessage", mensaje);
            } else {
                request.getSession().setAttribute("errorMessage", mensaje);
            }
            response.sendRedirect(getServletContext().getContextPath() + "/notificaciones");
        }
    }
    
    /**
     * Marca todas las notificaciones pendientes como leídas
     */
    private void marcarTodasLeidas(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        int notificacionesActualizadas = 0;
        
        try {
            if ("PADRE_FAMILIA".equals(tipoUsuario)) {
                PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
                if (padre != null) {
                    List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
                    List<Integer> ninosIds = ninos.stream().map(Nino::getId).collect(Collectors.toList());
                    notificacionesActualizadas = notificacionDAO.marcarTodasLeidasPorNinos(ninosIds);
                }
            } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
                // Los profesionales pueden marcar todas las notificaciones del sistema
                notificacionesActualizadas = notificacionDAO.marcarTodasLeidas();
            }
            
            String mensaje = String.format("Se marcaron %d notificaciones como leídas", notificacionesActualizadas);
            session.setAttribute("successMessage", mensaje);
            
        } catch (Exception e) {
            getServletContext().log("Error al marcar todas las notificaciones como leídas", e);
            session.setAttribute("errorMessage", "Error al actualizar las notificaciones");
        }
        
        response.sendRedirect(getServletContext().getContextPath() + "/notificaciones");
    }
    
    /**
     * Obtiene el contador de notificaciones pendientes (para AJAX)
     */
    private void obtenerContadorNotificaciones(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        int notificacionesPendientes = 0;
        int notificacionesUrgentes = 0;
        
        try {
            if ("PADRE_FAMILIA".equals(tipoUsuario)) {
                PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
                if (padre != null) {
                    List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
                    List<Integer> ninosIds = ninos.stream().map(Nino::getId).collect(Collectors.toList());
                    
                    notificacionesPendientes = notificacionDAO.countPendientesByNinos(ninosIds);
                    notificacionesUrgentes = notificacionDAO.countUrgentesByNinos(ninosIds);
                }
            } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
                notificacionesPendientes = notificacionDAO.countPendientes();
                notificacionesUrgentes = notificacionDAO.countUrgentes();
            }
        } catch (Exception e) {
            getServletContext().log("Error al obtener contador de notificaciones", e);
        }
        
        // Responder en formato JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            out.printf("{\"pendientes\": %d, \"urgentes\": %d}", notificacionesPendientes, notificacionesUrgentes);
        }
    }
    
    /**
     * Genera notificaciones manualmente (solo para profesionales)
     */
    private void generarNotificacionesManual(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        if (!"PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo los profesionales pueden generar notificaciones");
            return;
        }
        
        try {
            int notificacionesGeneradas = notificacionService.generarNotificacionesAutomaticas();
            
            String mensaje = String.format("Se generaron %d nuevas notificaciones", notificacionesGeneradas);
            session.setAttribute("successMessage", mensaje);
            
        } catch (Exception e) {
            getServletContext().log("Error al generar notificaciones manuales", e);
            session.setAttribute("errorMessage", "Error al generar las notificaciones");
        }
        
        response.sendRedirect(getServletContext().getContextPath() + "/notificaciones");
    }
    
    /**
     * Obtiene notificaciones filtradas para una lista de niños
     */
    private List<Notificacion> obtenerNotificacionesFiltradas(List<Integer> ninosIds, 
            String tipoFiltro, String estadoFiltro) {
        
        if (ninosIds.isEmpty()) {
            return List.of();
        }
        
        // Aplicar filtros
        if (tipoFiltro != null && !tipoFiltro.trim().isEmpty() && !"TODAS".equals(tipoFiltro)) {
            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !"TODOS".equals(estadoFiltro)) {
                return notificacionDAO.findByNinosAndTipoAndEstado(ninosIds, tipoFiltro, estadoFiltro);
            } else {
                return notificacionDAO.findByNinosAndTipo(ninosIds, tipoFiltro);
            }
        } else if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !"TODOS".equals(estadoFiltro)) {
            return notificacionDAO.findByNinosAndEstado(ninosIds, estadoFiltro);
        } else {
            return notificacionDAO.findByNinos(ninosIds);
        }
    }
    
    /**
     * Obtiene notificaciones para profesionales con filtros
     */
    private List<Notificacion> obtenerNotificacionesProfesional(String tipoFiltro, String estadoFiltro, 
            LocalDate fechaDesde, LocalDate fechaHasta) {
        
        return notificacionDAO.findWithFilters(tipoFiltro, estadoFiltro, fechaDesde, fechaHasta);
    }
    
    /**
     * Enriquece las notificaciones con información adicional
     */
    private void enriquecerNotificaciones(List<Notificacion> notificaciones) {
        
        for (Notificacion notificacion : notificaciones) {
            try {
                // Obtener información del niño
                if (notificacion.getNinoId() != null) {
                    Optional<Nino> ninoOpt = ninoDAO.findById(notificacion.getNinoId());
                    ninoOpt.ifPresent(notificacion::setNino);
                }
                
                // Obtener información de la vacuna
                if (notificacion.getVacunaId() != null) {
                    Optional<Vacuna> vacunaOpt = vacunaDAO.findById(notificacion.getVacunaId());
                    vacunaOpt.ifPresent(notificacion::setVacuna);
                }
                
                // Calcular días hasta/desde la fecha programada
                LocalDate hoy = LocalDate.now();
                LocalDate fechaProgramada = notificacion.getFechaProgramada();
                
                if (fechaProgramada != null) {
                    long diasDiferencia = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaProgramada);
                    notificacion.setAttribute("diasDiferencia", diasDiferencia);
                    
                    if (diasDiferencia < 0) {
                        notificacion.setAttribute("diasVencida", Math.abs(diasDiferencia));
                    } else {
                        notificacion.setAttribute("diasRestantes", diasDiferencia);
                    }
                }
                
            } catch (Exception e) {
                getServletContext().log("Error al enriquecer notificación: " + notificacion.getId(), e);
            }
        }
    }
    
    /**
     * Organiza las notificaciones por categorías
     */
    private void organizarNotificacionesPorCategoria(HttpServletRequest request, List<Notificacion> notificaciones) {
        
        Map<String, List<Notificacion>> categorias = new HashMap<>();
        
        categorias.put("urgentes", notificaciones.stream()
            .filter(n -> "VENCIDA".equals(n.getTipoNotificacion()))
            .collect(Collectors.toList()));
        
        categorias.put("proximas", notificaciones.stream()
            .filter(n -> "PROXIMA".equals(n.getTipoNotificacion()))
            .collect(Collectors.toList()));
        
        categorias.put("recordatorios", notificaciones.stream()
            .filter(n -> "RECORDATORIO".equals(n.getTipoNotificacion()))
            .collect(Collectors.toList()));
        
        categorias.put("pendientes", notificaciones.stream()
            .filter(n -> "PENDIENTE".equals(n.getEstado()))
            .collect(Collectors.toList()));
        
        categorias.put("leidas", notificaciones.stream()
            .filter(n -> "LEIDA".equals(n.getEstado()))
            .collect(Collectors.toList()));
        
        request.setAttribute("categoriasNotificaciones", categorias);
        request.setAttribute("todasNotificaciones", notificaciones);
        
        // Contadores por categoría
        request.setAttribute("totalUrgentes", categorias.get("urgentes").size());
        request.setAttribute("totalProximas", categorias.get("proximas").size());
        request.setAttribute("totalRecordatorios", categorias.get("recordatorios").size());
        request.setAttribute("totalPendientes", categorias.get("pendientes").size());
        request.setAttribute("totalLeidas", categorias.get("leidas").size());
        request.setAttribute("totalNotificaciones", notificaciones.size());
    }
    
    /**
     * Calcula estadísticas de notificaciones para padres
     */
    private void calcularEstadisticasNotificaciones(HttpServletRequest request, List<Integer> ninosIds) {
        
        try {
            Map<String, Integer> estadisticas = new HashMap<>();
            
            estadisticas.put("totalPendientes", notificacionDAO.countPendientesByNinos(ninosIds));
            estadisticas.put("totalUrgentes", notificacionDAO.countUrgentesByNinos(ninosIds));
            estadisticas.put("totalProximas", notificacionDAO.countProximasByNinos(ninosIds));
            estadisticas.put("totalRecordatorios", notificacionDAO.countRecordatoriosByNinos(ninosIds));
            
            request.setAttribute("estadisticasNotificaciones", estadisticas);
            
        } catch (Exception e) {
            getServletContext().log("Error al calcular estadísticas de notificaciones", e);
        }
    }
    
    /**
     * Calcula estadísticas generales para profesionales
     */
    private void calcularEstadisticasGenerales(HttpServletRequest request) {
        
        try {
            Map<String, Integer> estadisticas = new HashMap<>();
            
            estadisticas.put("totalPendientes", notificacionDAO.countPendientes());
            estadisticas.put("totalUrgentes", notificacionDAO.countUrgentes());
            estadisticas.put("totalProximas", notificacionDAO.countProximas());
            estadisticas.put("totalRecordatorios", notificacionDAO.countRecordatorios());
            estadisticas.put("totalHoy", notificacionDAO.countByFecha(LocalDate.now()));
            estadisticas.put("totalSemana", notificacionDAO.countByFechaPeriodo(
                LocalDate.now().minusDays(7), LocalDate.now()));
            
            request.setAttribute("estadisticasGenerales", estadisticas);
            
        } catch (Exception e) {
            getServletContext().log("Error al calcular estadísticas generales", e);
        }
    }
    
    /**
     * Verifica si el usuario tiene acceso a una notificación específica
     */
    private boolean tieneAccesoANotificacion(HttpServletRequest request, Integer notificacionId) {
        
        try {
            Optional<Notificacion> notificacionOpt = notificacionDAO.findById(notificacionId);
            if (!notificacionOpt.isPresent()) {
                return false;
            }
            
            Notificacion notificacion = notificacionOpt.get();
            HttpSession session = request.getSession();
            String tipoUsuario = (String) session.getAttribute("tipoUsuario");
            
            if ("PADRE_FAMILIA".equals(tipoUsuario)) {
                PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
                if (padre == null) return false;
                
                // Verificar que el niño de la notificación pertenece al padre
                List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
                return ninos.stream().anyMatch(n -> n.getId().equals(notificacion.getNinoId()));
                
            } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
                // Los profesionales tienen acceso a todas las notificaciones
                return true;
            }
            
        } catch (Exception e) {
            getServletContext().log("Error al verificar acceso a notificación", e);
        }
        
        return false;
    }
}