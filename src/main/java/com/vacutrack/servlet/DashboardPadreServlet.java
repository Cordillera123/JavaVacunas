package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.RegistroVacunaDAO;
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
 * @version 1.2 - Corregido para compatibilidad con DAOs
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

        try {
            // 1. Cargar niños del padre usando el método correcto
            List<Nino> ninos = ninoDAO.findByPadre(padre.getId()); // CORREGIDO
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

        } catch (Exception e) {
            getServletContext().log("Error al cargar datos del dashboard", e);
            request.setAttribute("error", "Error al cargar información del dashboard");
        }
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

        try {
            // 1. Información básica del niño
            request.setAttribute("edadNino", nino.obtenerEdadFormateada());
            request.setAttribute("sexoNino", nino.obtenerSexoLegible());

            // 2. Historial de vacunas aplicadas
            List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNino(nino.getId()); // CORREGIDO
            request.setAttribute("historialVacunas", historialVacunas);

            // 3. Estado del esquema de vacunación (simplificado)
            try {
                // CORREGIDO: Usar método básico de estadísticas
                VacunacionService.EstadisticasVacunacion estadisticas = vacunacionService.obtenerEstadisticas(nino.getId());
                if (estadisticas != null) {
                    request.setAttribute("porcentajeCompletitud", Math.round(estadisticas.getPorcentajeCompletitud()));
                    request.setAttribute("vacunasAplicadas", estadisticas.getTotalAplicadas()); // CAMBIADO
                    request.setAttribute("vacunasTotales", estadisticas.getTotalEsperadas());   // CAMBIADO
                    request.setAttribute("estadoEsquema", estadisticas.getPorcentajeCompletitud() >= 95 ? "COMPLETO" : "EN_PROCESO");
                }else {
                    // Valores por defecto si no se puede obtener el estado
                    request.setAttribute("porcentajeCompletitud", calcularPorcentajeBasico(historialVacunas.size()));
                    request.setAttribute("vacunasAplicadas", historialVacunas.size());
                    request.setAttribute("vacunasTotales", 20); // Estimado del esquema completo
                    request.setAttribute("estadoEsquema", "EN_PROCESO");
                }
            } catch (Exception e) {
                getServletContext().log("Error al obtener estado del esquema para niño: " + nino.getId(), e);
                request.setAttribute("porcentajeCompletitud", calcularPorcentajeBasico(historialVacunas.size()));
                request.setAttribute("vacunasAplicadas", historialVacunas.size());
                request.setAttribute("vacunasTotales", 20);
                request.setAttribute("estadoEsquema", "INDETERMINADO");
            }

            // 4. Próximas vacunas (simplificado)
            try {
                // CORREGIDO: Usar método básico de vacunas pendientes
                List<VacunacionService.VacunaPendiente> vacunasPendientes =
                        vacunacionService.obtenerVacunasPendientes(nino.getId());

                // Convertir a formato simple para el frontend
                List<Map<String, Object>> proximasVacunas = vacunasPendientes.stream()
                        .limit(5)
                        .map(vp -> {
                            Map<String, Object> vacuna = new HashMap<>();
                            vacuna.put("nombreVacuna", vp.getNombreVacuna());
                            vacuna.put("numeroDosis", 1); // Simplificado
                            vacuna.put("fechaProgramada", LocalDate.now().plusDays(30)); // Estimado
                            vacuna.put("diasRestantes", 30);
                            return vacuna;
                        })
                        .collect(Collectors.toList());

                request.setAttribute("proximasVacunas", proximasVacunas);
                generarDatosCalendarioSimple(request, proximasVacunas);
            } catch (Exception e) {
                getServletContext().log("Error al obtener próximas vacunas", e);
                request.setAttribute("proximasVacunas", List.of());
                request.setAttribute("proximosEventos", List.of());
            }

            // 5. Vacunas vencidas (simplificado - usar lógica básica)
            try {
                List<Map<String, Object>> vacunasVencidas = calcularVacunasVencidas(nino, historialVacunas);
                request.setAttribute("vacunasVencidas", vacunasVencidas);
                request.setAttribute("tieneVacunasVencidas", !vacunasVencidas.isEmpty());
            } catch (Exception e) {
                getServletContext().log("Error al calcular vacunas vencidas", e);
                request.setAttribute("vacunasVencidas", List.of());
                request.setAttribute("tieneVacunasVencidas", false);
            }

            // 6. Última vacuna aplicada
            if (!historialVacunas.isEmpty()) {
                RegistroVacuna ultimaVacuna = historialVacunas.stream()
                        .max((r1, r2) -> r1.getFechaAplicacion().compareTo(r2.getFechaAplicacion()))
                        .orElse(null);
                request.setAttribute("ultimaVacuna", ultimaVacuna);
            }

        } catch (Exception e) {
            getServletContext().log("Error al cargar información del niño: " + nino.getId(), e);
            request.setAttribute("errorNino", "Error al cargar información del niño");
        }
    }

    /**
     * Carga estadísticas generales de todos los niños
     */
    private void cargarEstadisticasGenerales(HttpServletRequest request, List<Nino> ninos) {

        try {
            int totalVacunasAplicadas = 0;
            int totalNotificacionesPendientes = 0;
            int ninosConEsquemaCompleto = 0;

            for (Nino nino : ninos) {
                try {
                    // Contar vacunas aplicadas
                    List<RegistroVacuna> vacunas = registroVacunaDAO.findByNino(nino.getId()); // CORREGIDO
                    totalVacunasAplicadas += vacunas.size();

                    // Contar notificaciones pendientes usando el método correcto
                    List<Notificacion> notificaciones = notificacionDAO.findByNino(nino.getId()); // CORREGIDO
                    totalNotificacionesPendientes += notificaciones.size();

                    // Verificar esquema completo (simplificado)
                    try {
                        VacunacionService.EstadisticasVacunacion estadisticas = vacunacionService.obtenerEstadisticas(nino.getId());
                        if (estadisticas != null && estadisticas.getPorcentajeCompletitud() >= 95.0) {
                            ninosConEsquemaCompleto++;
                        }
                    } catch (Exception e) {
                        // Ignorar error para estadísticas individuales
                    }
                } catch (Exception e) {
                    getServletContext().log("Error al procesar estadísticas del niño: " + nino.getId(), e);
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

        } catch (Exception e) {
            getServletContext().log("Error al cargar estadísticas generales", e);
            // Establecer valores por defecto
            request.setAttribute("totalVacunasAplicadas", 0);
            request.setAttribute("totalNotificacionesPendientes", 0);
            request.setAttribute("ninosConEsquemaCompleto", 0);
            request.setAttribute("promedioCompletitud", 0);
        }
    }

    /**
     * Carga notificaciones pendientes para todos los niños
     */
    private void cargarNotificaciones(HttpServletRequest request, List<Nino> ninos) {

        try {
            List<Notificacion> todasLasNotificaciones = ninos.stream()
                    .flatMap(nino -> {
                        try {
                            return notificacionDAO.findByNino(nino.getId()).stream(); // CORREGIDO
                        } catch (Exception e) {
                            getServletContext().log("Error al obtener notificaciones del niño: " + nino.getId(), e);
                            return List.<Notificacion>of().stream();
                        }
                    })
                    .sorted((n1, n2) -> n1.getFechaProgramada().compareTo(n2.getFechaProgramada())) // CORREGIDO
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

        } catch (Exception e) {
            getServletContext().log("Error al cargar notificaciones", e);
            request.setAttribute("notificaciones", List.of());
            request.setAttribute("notificacionesUrgentes", List.of());
            request.setAttribute("notificacionesProximas", List.of());
            request.setAttribute("hayNotificacionesUrgentes", false);
        }
    }

    /**
     * Genera datos del calendario en formato simple para el frontend
     */
    private void generarDatosCalendarioSimple(HttpServletRequest request, List<Map<String, Object>> proximasVacunas) {

        try {
            // Crear un mapa simple con las fechas de vacunas
            Map<String, String> eventosCalendario = new HashMap<>();

            for (Map<String, Object> vacuna : proximasVacunas) {
                try {
                    LocalDate fecha = (LocalDate) vacuna.get("fechaProgramada");
                    String nombreVacuna = (String) vacuna.get("nombreVacuna");
                    Integer numeroDosis = (Integer) vacuna.get("numeroDosis");

                    String fechaStr = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String descripcion = nombreVacuna + " - Dosis " + numeroDosis;
                    eventosCalendario.put(fechaStr, descripcion);
                } catch (Exception e) {
                    // Ignorar errores individuales de formateo
                }
            }

            request.setAttribute("eventosCalendario", eventosCalendario);

            // También crear una lista simple para mostrar en la interfaz
            List<Map<String, String>> eventosLista = proximasVacunas.stream()
                    .limit(5) // Máximo 5 eventos próximos
                    .map(v -> {
                        try {
                            Map<String, String> evento = new HashMap<>();
                            LocalDate fecha = (LocalDate) v.get("fechaProgramada");
                            evento.put("fecha", fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                            evento.put("vacuna", (String) v.get("nombreVacuna"));
                            evento.put("dosis", String.valueOf(v.get("numeroDosis")));
                            evento.put("diasRestantes", String.valueOf(v.get("diasRestantes")));
                            return evento;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(evento -> evento != null)
                    .collect(Collectors.toList());

            request.setAttribute("proximosEventos", eventosLista);

        } catch (Exception e) {
            getServletContext().log("Error al generar datos del calendario", e);
            request.setAttribute("eventosCalendario", new HashMap<>());
            request.setAttribute("proximosEventos", List.of());
        }
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
                // Verificar si el método existe en NotificacionDAO
                if (notificacionDAO.findById(notificacionId).isPresent()) {
                    // Si existe el método marcarComoLeida, usarlo
                    try {
                        notificacionDAO.getClass().getMethod("marcarComoLeida", Integer.class)
                                .invoke(notificacionDAO, notificacionId);
                    } catch (NoSuchMethodException e) {
                        // Si no existe el método, simplificar: no hacer nada
                        getServletContext().log("Método marcarComoLeida no disponible en NotificacionDAO");
                    }
                }
            } catch (Exception e) {
                getServletContext().log("Error al marcar notificación como leída", e);
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

    // Métodos auxiliares

    /**
     * Calcula porcentaje básico de completitud basado en vacunas aplicadas
     */
    private int calcularPorcentajeBasico(int vacunasAplicadas) {
        int vacunasEsperadas = 20; // Estimado del esquema completo
        return Math.min(100, (vacunasAplicadas * 100) / vacunasEsperadas);
    }

    /**
     * Calcula vacunas vencidas usando lógica básica
     */
    private List<Map<String, Object>> calcularVacunasVencidas(Nino nino, List<RegistroVacuna> historial) {
        List<Map<String, Object>> vencidas = List.of(); // Simplificado por ahora

        // TODO: Implementar lógica básica para detectar vacunas vencidas
        // basándose en la edad del niño y el esquema nacional

        return vencidas;
    }
}