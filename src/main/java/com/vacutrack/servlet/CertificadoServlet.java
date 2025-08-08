package com.vacutrack.servlet;

import com.vacutrack.dao.NinoDAO;
import com.vacutrack.dao.CertificadoVacunacionDAO;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.Nino;
import com.vacutrack.model.RegistroVacuna;
import com.vacutrack.model.CertificadoVacunacion;
import com.vacutrack.service.VacunacionService;
import com.vacutrack.service.ReporteService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servlet para gestión de certificados de vacunación
 * Genera, visualiza y descarga certificados en formato PDF
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/certificados")
public class CertificadoServlet extends HttpServlet {
    
    private NinoDAO ninoDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private CertificadoVacunacionDAO certificadoDAO;
    private VacunacionService vacunacionService;
    private ReporteService reporteService;
    
    @Override
    public void init() throws ServletException {
        ninoDAO = NinoDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        certificadoDAO = CertificadoVacunacionDAO.getInstance();
        vacunacionService = VacunacionService.getInstance();
        reporteService = ReporteService.getInstance();
    }
    
    /**
     * Muestra la página de certificados o ejecuta acciones específicas
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
                case "descargar":
                    descargarCertificado(request, response);
                    break;
                case "generar":
                    generarCertificado(request, response);
                    break;
                case "visualizar":
                    visualizarProgreso(request, response);
                    break;
                default:
                    mostrarListaCertificados(request, response);
                    break;
            }
        } catch (Exception e) {
            getServletContext().log("Error en CertificadoServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del sistema");
        }
    }
    
    /**
     * Procesa solicitudes POST para generar certificados
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("generar".equals(action)) {
            generarCertificado(request, response);
        } else {
            doGet(request, response);
        }
    }
    
    /**
     * Muestra la lista de certificados disponibles
     */
    private void mostrarListaCertificados(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        if ("PADRE_FAMILIA".equals(tipoUsuario)) {
            mostrarCertificadosPadre(request, response);
        } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            mostrarCertificadosProfesional(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado");
        }
    }
    
    /**
     * Muestra certificados para padres de familia
     */
    private void mostrarCertificadosPadre(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
        
        if (padre == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        // Obtener todos los niños del padre
        List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
        request.setAttribute("ninos", ninos);
        
        // Obtener información de progreso para cada niño
        for (Nino nino : ninos) {
            try {
                VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(nino.getId());
                nino.setAttribute("estadoEsquema", estado);
                
                // Obtener certificados existentes
                List<CertificadoVacunacion> certificados = certificadoDAO.findByNinoId(nino.getId());
                nino.setAttribute("certificados", certificados);
                
            } catch (Exception e) {
                getServletContext().log("Error al obtener estado del esquema para niño: " + nino.getId(), e);
            }
        }
        
        request.setAttribute("esPadre", true);
        request.setAttribute("nombreUsuario", padre.obtenerNombreCompleto());
        request.getRequestDispatcher("/WEB-INF/jsp/certificados.jsp").forward(request, response);
    }
    
    /**
     * Muestra certificados para profesionales de enfermería
     */
    private void mostrarCertificadosProfesional(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
        
        if (profesional == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/login");
            return;
        }
        
        String ninoIdParam = request.getParameter("ninoId");
        
        if (ninoIdParam != null && !ninoIdParam.trim().isEmpty()) {
            try {
                Integer ninoId = Integer.parseInt(ninoIdParam);
                Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
                
                if (ninoOpt.isPresent()) {
                    Nino nino = ninoOpt.get();
                    
                    // Obtener estado del esquema
                    VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(ninoId);
                    
                    // Obtener certificados existentes
                    List<CertificadoVacunacion> certificados = certificadoDAO.findByNinoId(ninoId);
                    
                    request.setAttribute("ninoSeleccionado", nino);
                    request.setAttribute("estadoEsquema", estado);
                    request.setAttribute("certificados", certificados);
                }
            } catch (NumberFormatException e) {
                request.setAttribute("error", "ID de niño no válido");
            }
        }
        
        request.setAttribute("esProfesional", true);
        request.setAttribute("nombreUsuario", profesional.obtenerNombreCompleto());
        request.getRequestDispatcher("/WEB-INF/jsp/certificados.jsp").forward(request, response);
    }
    
    /**
     * Genera un nuevo certificado de vacunación
     */
    private void generarCertificado(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String ninoIdParam = request.getParameter("ninoId");
        
        if (ninoIdParam == null || ninoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "Debe especificar el niño");
            mostrarListaCertificados(request, response);
            return;
        }
        
        try {
            Integer ninoId = Integer.parseInt(ninoIdParam);
            
            // Verificar acceso al niño
            if (!tieneAccesoAlNino(request, ninoId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tiene acceso a este niño");
                return;
            }
            
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (!ninoOpt.isPresent()) {
                request.setAttribute("error", "Niño no encontrado");
                mostrarListaCertificados(request, response);
                return;
            }
            
            Nino nino = ninoOpt.get();
            
            // Calcular porcentaje de completitud
            VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(ninoId);
            double porcentajeCompletitud = estado != null ? estado.getPorcentajeCompletitud() : 0.0;
            
            // Crear registro del certificado
            CertificadoVacunacion certificado = new CertificadoVacunacion();
            certificado.setNinoId(ninoId);
            certificado.setCodigoCertificado(generarCodigoCertificado());
            certificado.setPorcentajeCompletitud(new java.math.BigDecimal(porcentajeCompletitud));
            certificado.setVigente(true);
            
            // Guardar certificado
            certificado = certificadoDAO.save(certificado);
            
            if (certificado.getId() == null) {
                request.setAttribute("error", "Error al generar el certificado. Intente nuevamente");
                mostrarListaCertificados(request, response);
                return;
            }
            
            // Generar PDF
            byte[] pdfBytes = reporteService.exportarCertificadoPDF(ninoId);
            
            if (pdfBytes == null) {
                request.setAttribute("error", "Error al generar el archivo PDF");
                mostrarListaCertificados(request, response);
                return;
            }
            
            // Configurar respuesta para descarga
            String nombreArchivo = String.format("certificado_%s_%s.pdf", 
                nino.obtenerNombreCompleto().replaceAll("\\s+", "_").toLowerCase(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
            response.setContentLength(pdfBytes.length);
            
            // Escribir PDF a la respuesta
            try (OutputStream out = response.getOutputStream()) {
                out.write(pdfBytes);
                out.flush();
            }
            
            // Actualizar mensaje de éxito en sesión
            HttpSession session = request.getSession();
            session.setAttribute("successMessage", 
                "Certificado generado exitosamente para " + nino.obtenerNombreCompleto());
            
        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de niño no válido");
            mostrarListaCertificados(request, response);
        } catch (Exception e) {
            getServletContext().log("Error al generar certificado", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            mostrarListaCertificados(request, response);
        }
    }
    
    /**
     * Descarga un certificado existente
     */
    private void descargarCertificado(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String certificadoIdParam = request.getParameter("certificadoId");
        
        if (certificadoIdParam == null || certificadoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "Certificado no especificado");
            mostrarListaCertificados(request, response);
            return;
        }
        
        try {
            Integer certificadoId = Integer.parseInt(certificadoIdParam);
            
            Optional<CertificadoVacunacion> certificadoOpt = certificadoDAO.findById(certificadoId);
            if (!certificadoOpt.isPresent()) {
                request.setAttribute("error", "Certificado no encontrado");
                mostrarListaCertificados(request, response);
                return;
            }
            
            CertificadoVacunacion certificado = certificadoOpt.get();
            
            // Verificar acceso al niño del certificado
            if (!tieneAccesoAlNino(request, certificado.getNinoId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tiene acceso a este certificado");
                return;
            }
            
            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(certificado.getNinoId());
            if (!ninoOpt.isPresent()) {
                request.setAttribute("error", "Niño no encontrado para el certificado");
                mostrarListaCertificados(request, response);
                return;
            }
            
            Nino nino = ninoOpt.get();
            
            // Generar PDF actualizado
            byte[] pdfBytes = reporteService.exportarCertificadoPDF(certificado.getNinoId());
            
            if (pdfBytes == null) {
                request.setAttribute("error", "Error al generar el archivo PDF");
                mostrarListaCertificados(request, response);
                return;
            }
            
            // Configurar respuesta para descarga
            String nombreArchivo = String.format("certificado_%s_%s.pdf", 
                nino.obtenerNombreCompleto().replaceAll("\\s+", "_").toLowerCase(),
                certificado.getFechaGeneracion().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
            response.setContentLength(pdfBytes.length);
            
            // Escribir PDF a la respuesta
            try (OutputStream out = response.getOutputStream()) {
                out.write(pdfBytes);
                out.flush();
            }
            
        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de certificado no válido");
            mostrarListaCertificados(request, response);
        } catch (Exception e) {
            getServletContext().log("Error al descargar certificado", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            mostrarListaCertificados(request, response);
        }
    }
    
    /**
     * Visualiza el progreso detallado del esquema de vacunación
     */
    private void visualizarProgreso(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String ninoIdParam = request.getParameter("ninoId");
        
        if (ninoIdParam == null || ninoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "Debe especificar el niño");
            mostrarListaCertificados(request, response);
            return;
        }
        
        try {
            Integer ninoId = Integer.parseInt(ninoIdParam);
            
            // Verificar acceso al niño
            if (!tieneAccesoAlNino(request, ninoId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tiene acceso a este niño");
                return;
            }
            
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (!ninoOpt.isPresent()) {
                request.setAttribute("error", "Niño no encontrado");
                mostrarListaCertificados(request, response);
                return;
            }
            
            Nino nino = ninoOpt.get();
            
            // Obtener información completa del esquema
            VacunacionService.EstadoEsquema estadoEsquema = vacunacionService.verificarEstadoEsquema(ninoId);
            List<VacunacionService.VacunaEsquema> esquemaCompleto = vacunacionService.obtenerEsquemaCompleto(ninoId);
            List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNinoId(ninoId);
            List<VacunacionService.ProximaVacuna> proximasVacunas = vacunacionService.obtenerProximasVacunas(ninoId, 10);
            List<VacunacionService.VacunaVencida> vacunasVencidas = vacunacionService.obtenerVacunasVencidas(ninoId);
            
            // Preparar datos para la vista
            request.setAttribute("nino", nino);
            request.setAttribute("estadoEsquema", estadoEsquema);
            request.setAttribute("esquemaCompleto", esquemaCompleto);
            request.setAttribute("historialVacunas", historialVacunas);
            request.setAttribute("proximasVacunas", proximasVacunas);
            request.setAttribute("vacunasVencidas", vacunasVencidas);
            request.setAttribute("vistaProgreso", true);
            
            // Información adicional
            request.setAttribute("fechaGeneracion", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
            request.getRequestDispatcher("/WEB-INF/jsp/esquema-vacunacion.jsp").forward(request, response);
            
        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de niño no válido");
            mostrarListaCertificados(request, response);
        } catch (Exception e) {
            getServletContext().log("Error al visualizar progreso", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            mostrarListaCertificados(request, response);
        }
    }
    
    /**
     * Verifica si el usuario tiene acceso al niño especificado
     */
    private boolean tieneAccesoAlNino(HttpServletRequest request, Integer ninoId) {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        if ("PADRE_FAMILIA".equals(tipoUsuario)) {
            PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
            if (padre == null) return false;
            
            // Verificar que el niño pertenece al padre
            List<Nino> ninos = ninoDAO.findByPadre(padre.getId());
            return ninos.stream().anyMatch(n -> n.getId().equals(ninoId));
            
        } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            // Los profesionales pueden acceder a cualquier niño
            return true;
        }
        
        return false;
    }
    
    /**
     * Genera un código único para el certificado
     */
    private String generarCodigoCertificado() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CERT-" + timestamp + "-" + uuid;
    }
}