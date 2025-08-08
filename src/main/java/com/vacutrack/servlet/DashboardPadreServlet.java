package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.NotificacionDAO;
import com.vacutrack.dao.VacunaDAO;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.Nino;
import com.vacutrack.model.RegistroVacuna;
import com.vacutrack.model.Notificacion;
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

/**
 * Servlet del dashboard principal para padres de familia
 * Muestra información de los niños, vacunas y notificaciones
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/dashboard-padre")
public class DashboardPadreServlet extends HttpServlet {
    
    private NinoDAO ninoDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private NotificacionDAO notificacionDAO;
    private VacunaDAO vacunaDAO;
    private VacunacionService vacunacionService;
    
    @Override
    public void init() throws ServletException {
        ninoDAO = NinoDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        notificacionDAO = NotificacionDAO.getInstance();
        vacunaDAO = VacunaDAO.getInstance();
        vacunacionService = VacunacionService.getInstance();
    }
    
    /**
     * Muestra el dashboard principal del padre
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        // Verificar que sea un padre de familia
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        if (!"PADRE_FAMILIA".equals(tipoUsuario)) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        try {
            PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
            if (padre == null) {
                response.sendRedirect(getServletContext().getContextPath() + "/login");
                return;
            }
            
            // Cargar datos del dashboard
            cargarDatosDashboard(request, padre);
            
            // Mostrar dashboard
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard-padre.jsp").forward(request, response);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar dashboard de padre", e);
            request.setAttribute("error", "Error al cargar la información. Intente nuevamente");
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard-padre.jsp").forward(request, response);
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
            case "marcar-notificacion-leida":
                marcarNotificacionLeida(request, response);
                break;
            case "seleccionar-nino":
                seleccionarNino(request, response);
                break;
            default:
                doGet(request, response);
                break;
        }
    }
    
    /**
     * Carga todos los datos necesarios para el dashboard
     */
    private void cargarDatosDashboard(HttpServletRequest request, PadreFamilia padre) {
        
        // 1. Cargar niños del padre
        List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
        request.setAttribute("ninos", ninos);
        request.setAttribute("totalNinos", ninos.size());
        
        if (ninos.isEmpty()) {
            request.setAttribute("sinNinos", true);
            return;
        }
        
        // 2. Seleccionar niño activo (el primero por defecto o el seleccionado)
        Nino ninoSeleccionado = obtenerNinoSeleccionado(request, ninos);
        request.setAttribute("ninoSeleccionado", ninoSeleccionado);
        
        // 3. Cargar información del niño seleccionado
        if (ninoSeleccionado != null) {
            cargarInformacionNino(request, ninoSeleccionado);
        }
        
        // 4. Cargar estadísticas generales
        cargarEstadisticasGenerales(request, ninos);
        
        // 5. Cargar notificaciones pendientes
        cargarNotificaciones(request, ninos);
        
        // 6. Información adicional para el dashboard
        request.setAttribute("fechaActual", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        request.setAttribute("nombrePadre", padre.obtenerNombreCompleto());
    }
    
    /**
     * Obtiene el niño seleccionado (desde parámetro o por defecto el primero)
     */
    private Nino obtenerNinoSeleccionado(HttpServletRequest request, List<Nino> ninos) {
        String ninoIdParam = request.getParameter("ninoId");
        
        if (ninoIdParam != null && !ninoIdParam.isEmpty()) {
            try {
                Integer ninoId = Integer.parseInt(ninoIdParam);
                return ninos.stream()
                    .filter(n -> n.getId().equals(ninoId))
                    .findFirst()
                    .orElse(ninos.get(0));
            } catch (NumberFormatException e) {
                // Si el parámetro no es válido, devolver el primero
                return ninos.get(0);
            }
        }
        
        return ninos.get(0); // Por defecto, el primer niño
    }
    
    /**
     * Carga información específica del niño seleccionado
     */
    private void cargarInformacionNino(HttpServletRequest request, Nino nino) {
        
        // 1. Información básica del niño
        request.setAttribute("edadNino", nino.obtenerEdadFormateada());
        request.setAttribute("sexoNino", nino.obtenerSexoLegible());
        
        // 2. Historial de vacunas aplicadas
        List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNinoId(nino.getId());
        request.setAttribute("historialVacunas", historialVacunas);
        
        // 3. Estado del esquema de vacunación
        try {
            VacunacionService.EstadoEsquema estadoEsquema = vacunacionService.verificarEstadoEsquema(nino.getId());
            if (estadoEsquema != null) {
                request.setAttribute("porcentajeCompletitud", Math.round(estadoEsquema.getPorcentajeCompletitud()));
                request.setAttribute("vacunasAplicadas", estadoEsquema.getVacunasAplicadas());
                request.setAttribute("vacunasTotales", estadoEsquema.getVacunasTotales());
                request.setAttribute("estadoEsquema", estadoEsquema.getEstado());
            }
        } catch (Exception e) {
            getServletContext().log("Error al obtener estado del esquema para niño: " + nino.getId(), e);
            request.setAttribute("porcentajeCompletitud", 0);
            request.setAttribute("vacunasAplicadas", 0);
            request.setAttribute("vacunasTotales", 0);
        }
        
        // 4. Próximas vacunas
        List<VacunacionService.ProximaVacuna> proximasVacunas = vacunacionService.obtenerProximasVacunas(nino.getId(), 5);
        request.setAttribute("proximasVacunas", proximasVacunas);
        
        // 5. Vacunas vencidas
        List<VacunacionService.VacunaVencida> vacunasVencidas = vacunacionService.obtenerVacunasVencidas(nino.getId());
        request.setAttribute("vacunasVencidas", vacunasVencidas);
        request.setAttribute("tieneVacunasVencidas", !vacunasVencidas.isEmpty());
        
        // 6. Última vacuna aplicada
        if (!historialVacunas.isEmpty()) {
            RegistroVacuna ultimaVacuna = historialVacunas.stream()
                .max((r1, r2) -> r1.getFechaAplicacion().compareTo(r2.getFechaAplicacion()))
                .orElse(null);
            request.setAttribute("ultimaVacuna", ultimaVacuna);
        }
        
        // 7. Generar datos para el calendario (próximas vacunas en formato JSON simple)
        generarDatosCalendario(request, proximasVacunas);
    }
    
    /**
     * Carga estadísticas generales de todos los niños
     */
    private void cargarEstadisticasGenerales(HttpServletRequest request, List<Nino> ninos) {
        
        int totalVacunasAplicadas = 0;
        int totalNotificacionesPendientes = 0;
        int ninosConEsquemaCompleto = 0;
        
        for (Nino nino : ninos) {
            // Contar vacunas aplicadas
            List<RegistroVacuna> vacunas = registroVacunaDAO.findByNinoId(nino.getId());
            totalVacunasAplicadas += vacunas.size();
            
            // Contar notificaciones pendientes
            List<Notificacion> notificaciones = notificacionDAO.findPendientesByNino(nino.getId());
            totalNotificacionesPendientes += notificaciones.size();
            
            // Verificar esquema completo
            try {
                VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(nino.getId());
                if (estado != null && estado.getPorcentajeCompletitud() >= 95.0) {
                    ninosConEsquemaCompleto++;
                }
            } catch (Exception e) {
                // Ignorar error para estadísticas
            }
        }
        
        request.setAttribute("totalVacunasAplicadas", totalVacunasAplicadas);
        request.setAttribute("totalNotificacionesPendientes", totalNotificacionesPendientes);
        request.setAttribute("ninosConEsquemaCompleto", ninosConEsquemaCompleto);
        
        // Calcular porcentaje promedio de completitud
        if (!ninos.isEmpty()) {
            double promedioCompletitud = (double) ninosConEsquemaCompleto / ninos.size() * 100;
            request.setAttribute("promedioCompletitud", Math.round(promedioCompletitud));
        } else {
            request.setAttribute("promedioCompletitud", 0);
        }
    }
    
    /**
     * Carga notificaciones pendientes para todos los niños
     */
    private void cargarNotificaciones(HttpServletRequest request, List<Nino> ninos) {
        
        List<Notificacion> todasLasNotificaciones = ninos.stream()
            .flatMap(nino -> notificacionDAO.findPendientesByNino(nino.getId()).stream())
            .sorted((n1, n2) -> n1.getFechaProgramada().compareTo(n2.getFechaProgramada()))
            .limit(10) // Máximo 10 notificaciones recientes
            .collect(Collectors.toList());
        
        request.setAttribute("notificaciones", todasLasNotificaciones);
        
        // Separar por tipo
        List<Notificacion> notificacionesUrgentes = todasLasNotificaciones.stream()
            .filter(n -> "VENCIDA".equals(n.getTipoNotificacion()))
            .collect(Collectors.toList());
        
        List<Notificacion> notificacionesProximas = todasLasNotificaciones.stream()
            .filter(n -> "PROXIMA".equals(n.getTipoNotificacion()))
            .collect(Collectors.toList());
        
        request.setAttribute("notificacionesUrgentes", notificacionesUrgentes);
        request.setAttribute("notificacionesProximas", notificacionesProximas);
        request.setAttribute("hayNotificacionesUrgentes", !notificacionesUrgentes.isEmpty());
    }
    
    /**
     * Genera datos del calendario en formato simple para el frontend
     */
    private void generarDatosCalendario(HttpServletRequest request, List<VacunacionService.ProximaVacuna> proximasVacunas) {
        
        // Crear un mapa simple con las fechas de vacunas
        Map<String, String> eventosCalendario = new HashMap<>();
        
        for (VacunacionService.ProximaVacuna vacuna : proximasVacunas) {
            String fecha = vacuna.getFechaProgramada().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String descripcion = vacuna.getVacunaNombre() + " - Dosis " + vacuna.getNumeroDosis();
            eventosCalendario.put(fecha, descripcion);
        }
        
        request.setAttribute("eventosCalendario", eventosCalendario);
        
        // También crear una lista simple para mostrar en la interfaz
        List<Map<String, String>> eventosLista = proximasVacunas.stream()
            .limit(5) // Máximo 5 eventos próximos
            .map(v -> {
                Map<String, String> evento = new HashMap<>();
                evento.put("fecha", v.getFechaProgramada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                evento.put("vacuna", v.getVacunaNombre());
                evento.put("dosis", String.valueOf(v.getNumeroDosis()));
                evento.put("diasRestantes", String.valueOf(v.getDiasRestantes()));
                return evento;
            })
            .collect(Collectors.toList());
        
        request.setAttribute("proximosEventos", eventosLista);
    }
    
    /**
     * Marca una notificación como leída
     */
    private void marcarNotificacionLeida(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String notificacionIdParam = request.getParameter("notificacionId");
        
        if (notificacionIdParam != null && !notificacionIdParam.isEmpty()) {
            try {
                Integer notificacionId = Integer.parseInt(notificacionIdParam);
                notificacionDAO.marcarComoLeida(notificacionId);
            } catch (NumberFormatException e) {
                // Ignorar error de formato
            }
        }
        
        // Redirigir de vuelta al dashboard
        doGet(request, response);
    }
    
    /**
     * Maneja la selección de un niño específico
     */
    private void seleccionarNino(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // El parámetro ninoId se maneja en doGet, solo redirigir
        doGet(request, response);
    }
}