package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.VacunaDAO;
import com.vacutrack.dao.NotificacionDAO;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.model.Nino;
import com.vacutrack.model.RegistroVacuna;
import com.vacutrack.model.Vacuna;
import com.vacutrack.service.VacunacionService;
import com.vacutrack.service.NotificacionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servlet para el registro de vacunas aplicadas
 * Maneja el formulario de vacunación y valida los datos
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/vacunacion")
public class VacunacionServlet extends HttpServlet {
    
    private NinoDAO ninoDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private VacunaDAO vacunaDAO;
    private NotificacionDAO notificacionDAO;
    private VacunacionService vacunacionService;
    private NotificacionService notificacionService;
    
    @Override
    public void init() throws ServletException {
        ninoDAO = NinoDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        vacunaDAO = VacunaDAO.getInstance();
        notificacionDAO = NotificacionDAO.getInstance();
        vacunacionService = VacunacionService.getInstance();
        notificacionService = NotificacionService.getInstance();
    }
    
    /**
     * Muestra el formulario de registro de vacuna
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado");
            return;
        }
        
        try {
            ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
            if (profesional == null) {
                response.sendRedirect(getServletContext().getContextPath() + "/login");
                return;
            }
            
            String ninoIdParam = request.getParameter("ninoId");
            String vacunaIdParam = request.getParameter("vacunaId");
            String numeroDosisParam = request.getParameter("numeroDosis");
            
            // Cargar catálogo de vacunas
            List<Vacuna> vacunasDisponibles = vacunaDAO.findActivas();
            request.setAttribute("vacunasDisponibles", vacunasDisponibles);
            
            // Si se especifica un niño, cargar su información
            if (ninoIdParam != null && !ninoIdParam.trim().isEmpty()) {
                try {
                    Integer ninoId = Integer.parseInt(ninoIdParam);
                    Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
                    
                    if (ninoOpt.isPresent()) {
                        Nino nino = ninoOpt.get();
                        request.setAttribute("ninoSeleccionado", nino);
                        
                        // Cargar próximas vacunas recomendadas para este niño
                        List<VacunacionService.ProximaVacuna> proximasVacunas = 
                            vacunacionService.obtenerProximasVacunas(ninoId, 10);
                        request.setAttribute("proximasVacunas", proximasVacunas);
                        
                        // Pre-seleccionar vacuna y dosis si se especificaron
                        if (vacunaIdParam != null && numeroDosisParam != null) {
                            try {
                                Integer vacunaId = Integer.parseInt(vacunaIdParam);
                                Integer numeroDosis = Integer.parseInt(numeroDosisParam);
                                
                                Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
                                if (vacunaOpt.isPresent()) {
                                    request.setAttribute("vacunaPreseleccionada", vacunaOpt.get());
                                    request.setAttribute("dosisPreseleccionada", numeroDosis);
                                }
                            } catch (NumberFormatException e) {
                                // Ignorar parámetros inválidos
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    request.setAttribute("error", "ID de niño no válido");
                }
            }
            
            // Información del profesional
            request.setAttribute("nombreProfesional", profesional.obtenerNombreCompleto());
            request.setAttribute("fechaActual", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            request.getRequestDispatcher("/WEB-INF/jsp/registro-vacuna.jsp").forward(request, response);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar formulario de vacunación", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del sistema");
        }
    }
    
    /**
     * Procesa el registro de una vacuna aplicada
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
        
        if (profesional == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        try {
            // Obtener parámetros del formulario
            String ninoIdParam = request.getParameter("ninoId");
            String vacunaIdParam = request.getParameter("vacunaId");
            String numeroDosisParam = request.getParameter("numeroDosis");
            String fechaAplicacionParam = request.getParameter("fechaAplicacion");
            String loteVacuna = request.getParameter("loteVacuna");
            String fechaVencimientoParam = request.getParameter("fechaVencimiento");
            String pesoAplicacionParam = request.getParameter("pesoAplicacion");
            String tallaAplicacionParam = request.getParameter("tallaAplicacion");
            String observaciones = request.getParameter("observaciones");
            String reaccionAdversaParam = request.getParameter("reaccionAdversa");
            String descripcionReaccion = request.getParameter("descripcionReaccion");
            
            // Validar parámetros básicos
            String error = validarParametrosVacunacion(ninoIdParam, vacunaIdParam, numeroDosisParam, fechaAplicacionParam);
            if (error != null) {
                request.setAttribute("error", error);
                preservarDatosFormulario(request, ninoIdParam, vacunaIdParam, numeroDosisParam, 
                    fechaAplicacionParam, loteVacuna, fechaVencimientoParam, pesoAplicacionParam, 
                    tallaAplicacionParam, observaciones, reaccionAdversaParam, descripcionReaccion);
                doGet(request, response);
                return;
            }
            
            // Convertir parámetros
            Integer ninoId = Integer.parseInt(ninoIdParam);
            Integer vacunaId = Integer.parseInt(vacunaIdParam);
            Integer numeroDosis = Integer.parseInt(numeroDosisParam);
            LocalDate fechaAplicacion = LocalDate.parse(fechaAplicacionParam);
            
            // Verificar que el niño existe
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (!ninoOpt.isPresent()) {
                request.setAttribute("error", "Niño no encontrado");
                doGet(request, response);
                return;
            }
            
            Nino nino = ninoOpt.get();
            
            // Verificar que la vacuna existe
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            if (!vacunaOpt.isPresent()) {
                request.setAttribute("error", "Vacuna no encontrada");
                preservarDatosFormulario(request, ninoIdParam, vacunaIdParam, numeroDosisParam, 
                    fechaAplicacionParam, loteVacuna, fechaVencimientoParam, pesoAplicacionParam, 
                    tallaAplicacionParam, observaciones, reaccionAdversaParam, descripcionReaccion);
                doGet(request, response);
                return;
            }
            
            Vacuna vacuna = vacunaOpt.get();
            
            // Validaciones específicas de vacunación
            error = validarVacunacion(nino, vacuna, numeroDosis, fechaAplicacion);
            if (error != null) {
                request.setAttribute("error", error);
                preservarDatosFormulario(request, ninoIdParam, vacunaIdParam, numeroDosisParam, 
                    fechaAplicacionParam, loteVacuna, fechaVencimientoParam, pesoAplicacionParam, 
                    tallaAplicacionParam, observaciones, reaccionAdversaParam, descripcionReaccion);
                doGet(request, response);
                return;
            }
            
            // Verificar que no esté ya aplicada esta dosis
            if (registroVacunaDAO.existeVacunaAplicada(ninoId, vacunaId, numeroDosis)) {
                request.setAttribute("error", "Esta dosis ya fue aplicada anteriormente");
                preservarDatosFormulario(request, ninoIdParam, vacunaIdParam, numeroDosisParam, 
                    fechaAplicacionParam, loteVacuna, fechaVencimientoParam, pesoAplicacionParam, 
                    tallaAplicacionParam, observaciones, reaccionAdversaParam, descripcionReaccion);
                doGet(request, response);
                return;
            }
            
            // Crear el registro de vacuna
            RegistroVacuna registro = new RegistroVacuna();
            registro.setNinoId(ninoId);
            registro.setVacunaId(vacunaId);
            registro.setProfesionalId(profesional.getId());
            registro.setCentroSaludId(profesional.getCentroSaludId());
            registro.setFechaAplicacion(fechaAplicacion);
            registro.setNumeroDosis(numeroDosis);
            registro.setLoteVacuna(loteVacuna != null && !loteVacuna.trim().isEmpty() ? loteVacuna.trim() : null);
            registro.setObservaciones(observaciones != null && !observaciones.trim().isEmpty() ? observaciones.trim() : null);
            
            // Fecha de vencimiento
            if (fechaVencimientoParam != null && !fechaVencimientoParam.trim().isEmpty()) {
                try {
                    registro.setFechaVencimiento(LocalDate.parse(fechaVencimientoParam));
                } catch (DateTimeParseException e) {
                    // Ignorar fecha inválida
                }
            }
            
            // Peso y talla
            if (pesoAplicacionParam != null && !pesoAplicacionParam.trim().isEmpty()) {
                try {
                    registro.setPesoAplicacion(new BigDecimal(pesoAplicacionParam));
                } catch (NumberFormatException e) {
                    // Ignorar valor inválido
                }
            }
            
            if (tallaAplicacionParam != null && !tallaAplicacionParam.trim().isEmpty()) {
                try {
                    registro.setTallaAplicacion(new BigDecimal(tallaAplicacionParam));
                } catch (NumberFormatException e) {
                    // Ignorar valor inválido
                }
            }
            
            // Reacción adversa
            boolean tieneReaccionAdversa = "true".equals(reaccionAdversaParam);
            registro.setReaccionAdversa(tieneReaccionAdversa);
            if (tieneReaccionAdversa && descripcionReaccion != null && !descripcionReaccion.trim().isEmpty()) {
                registro.setDescripcionReaccion(descripcionReaccion.trim());
            }
            
            // Guardar el registro
            registro = registroVacunaDAO.save(registro);
            
            if (registro.getId() == null) {
                request.setAttribute("error", "Error al registrar la vacuna. Intente nuevamente");
                preservarDatosFormulario(request, ninoIdParam, vacunaIdParam, numeroDosisParam, 
                    fechaAplicacionParam, loteVacuna, fechaVencimientoParam, pesoAplicacionParam, 
                    tallaAplicacionParam, observaciones, reaccionAdversaParam, descripcionReaccion);
                doGet(request, response);
                return;
            }
            
            // Marcar notificación relacionada como aplicada (si existe)
            try {
                notificacionService.marcarVacunaComoAplicada(ninoId, vacunaId, numeroDosis);
            } catch (Exception e) {
                getServletContext().log("Error al actualizar notificación", e);
                // No es crítico, continuar
            }
            
            // Generar nuevas notificaciones para próximas dosis
            try {
                notificacionService.generarNotificacionesNino(ninoId);
            } catch (Exception e) {
                getServletContext().log("Error al generar notificaciones", e);
                // No es crítico, continuar
            }
            
            // Redirigir con mensaje de éxito
            String mensaje = String.format("Vacuna %s (dosis %d) registrada exitosamente para %s", 
                vacuna.getNombre(), numeroDosis, nino.obtenerNombreCompleto());
            
            session.setAttribute("successMessage", mensaje);
            
            // Redirigir al dashboard profesional con el niño seleccionado
            response.sendRedirect(getServletContext().getContextPath() + 
                "/dashboard-profesional?action=ver-historial&ninoId=" + ninoId);
            
        } catch (Exception e) {
            getServletContext().log("Error al procesar registro de vacuna", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            doGet(request, response);
        }
    }
    
    /**
     * Valida los parámetros básicos del formulario
     */
    private String validarParametrosVacunacion(String ninoIdParam, String vacunaIdParam, 
            String numeroDosisParam, String fechaAplicacionParam) {
        
        // Validar campos requeridos
        if (ninoIdParam == null || ninoIdParam.trim().isEmpty()) {
            return "Debe seleccionar un niño";
        }
        
        if (vacunaIdParam == null || vacunaIdParam.trim().isEmpty()) {
            return "Debe seleccionar una vacuna";
        }
        
        if (numeroDosisParam == null || numeroDosisParam.trim().isEmpty()) {
            return "Debe especificar el número de dosis";
        }
        
        if (fechaAplicacionParam == null || fechaAplicacionParam.trim().isEmpty()) {
            return "La fecha de aplicación es requerida";
        }
        
        // Validar formatos
        try {
            Integer.parseInt(ninoIdParam);
        } catch (NumberFormatException e) {
            return "ID de niño no válido";
        }
        
        try {
            Integer.parseInt(vacunaIdParam);
        } catch (NumberFormatException e) {
            return "ID de vacuna no válido";
        }
        
        try {
            int numeroDosis = Integer.parseInt(numeroDosisParam);
            if (numeroDosis < 1 || numeroDosis > 10) {
                return "Número de dosis debe estar entre 1 y 10";
            }
        } catch (NumberFormatException e) {
            return "Número de dosis no válido";
        }
        
        try {
            LocalDate fechaAplicacion = LocalDate.parse(fechaAplicacionParam);
            LocalDate hoy = LocalDate.now();
            
            if (fechaAplicacion.isAfter(hoy)) {
                return "La fecha de aplicación no puede ser futura";
            }
            
            if (fechaAplicacion.isBefore(hoy.minusYears(20))) {
                return "La fecha de aplicación es muy antigua";
            }
            
        } catch (DateTimeParseException e) {
            return "Formato de fecha no válido";
        }
        
        return null; // Sin errores
    }
    
    /**
     * Validaciones específicas de la vacunación
     */
    private String validarVacunacion(Nino nino, Vacuna vacuna, Integer numeroDosis, LocalDate fechaAplicacion) {
        
        // Verificar que el número de dosis no exceda el total de la vacuna
        if (numeroDosis > vacuna.getDosisTotal()) {
            return String.format("La vacuna %s solo tiene %d dosis máximo", 
                vacuna.getNombre(), vacuna.getDosisTotal());
        }
        
        // Verificar que la fecha de aplicación sea posterior al nacimiento
        if (fechaAplicacion.isBefore(nino.getFechaNacimiento())) {
            return "La fecha de aplicación no puede ser anterior al nacimiento del niño";
        }
        
        // Si es dosis 2 o superior, verificar que la dosis anterior esté aplicada
        if (numeroDosis > 1) {
            boolean dosisAnteriorAplicada = registroVacunaDAO.existeVacunaAplicada(
                nino.getId(), vacuna.getId(), numeroDosis - 1);
            
            if (!dosisAnteriorAplicada) {
                return String.format("Debe aplicar primero la dosis %d de %s", 
                    numeroDosis - 1, vacuna.getNombre());
            }
        }
        
        return null; // Sin errores
    }
    
    /**
     * Preserva los datos del formulario en caso de error
     */
    private void preservarDatosFormulario(HttpServletRequest request, String ninoId, String vacunaId, 
            String numeroDosis, String fechaAplicacion, String loteVacuna, String fechaVencimiento,
            String pesoAplicacion, String tallaAplicacion, String observaciones, 
            String reaccionAdversa, String descripcionReaccion) {
        
        request.setAttribute("ninoIdForm", ninoId);
        request.setAttribute("vacunaIdForm", vacunaId);
        request.setAttribute("numeroDosisForm", numeroDosis);
        request.setAttribute("fechaAplicacionForm", fechaAplicacion);
        request.setAttribute("loteVacunaForm", loteVacuna);
        request.setAttribute("fechaVencimientoForm", fechaVencimiento);
        request.setAttribute("pesoAplicacionForm", pesoAplicacion);
        request.setAttribute("tallaAplicacionForm", tallaAplicacion);
        request.setAttribute("observacionesForm", observaciones);
        request.setAttribute("reaccionAdversaForm", reaccionAdversa);
        request.setAttribute("descripcionReaccionForm", descripcionReaccion);
    }
}