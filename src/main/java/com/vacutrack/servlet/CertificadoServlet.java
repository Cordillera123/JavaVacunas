package com.vacutrack.servlet;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;
import com.vacutrack.service.VacunacionService;
import com.vacutrack.service.CertificadoService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * Servlet para gestión de certificados de vacunación - VERSIÓN SIMPLIFICADA
 * Se adapta a la base de datos y DAOs existentes
 *
 * @author VACU-TRACK Team
 * @version 2.0 - Simplificada y adaptada
 */
@WebServlet("/certificados")
public class CertificadoServlet extends HttpServlet {

    // DAOs que existen en tu proyecto
    private NinoDAO ninoDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private CertificadoVacunacionDAO certificadoDAO;
    private PadreFamiliaDAO padreFamiliaDAO;
    private ProfesionalEnfermeriaDAO profesionalDAO;

    // Servicios
    private VacunacionService vacunacionService;
    private CertificadoService certificadoService;

    @Override
    public void init() throws ServletException {
        // Inicializar DAOs
        ninoDAO = NinoDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        certificadoDAO = CertificadoVacunacionDAO.getInstance();
        padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();

        // Inicializar servicios
        vacunacionService = VacunacionService.getInstance();
        certificadoService = CertificadoService.getInstance();
    }

    /**
     * Maneja solicitudes GET
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar sesión
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        try {
            switch (action != null ? action : "") {
                case "generar":
                    generarCertificado(request, response);
                    break;
                case "ver":
                    verCertificado(request, response);
                    break;
                case "verificar":
                    verificarCertificado(request, response);
                    break;
                case "progreso":
                    mostrarProgreso(request, response);
                    break;
                default:
                    mostrarListaCertificados(request, response);
                    break;
            }
        } catch (Exception e) {
            log("Error en CertificadoServlet", e);
            request.setAttribute("error", "Error interno del sistema");
            mostrarListaCertificados(request, response);
        }
    }

    /**
     * Maneja solicitudes POST
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Muestra la lista principal de certificados
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
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            // Obtener niños del padre
            List<Nino> ninos = ninoDAO.findByPadre(padre.getId());

            // Crear un mapa para almacenar porcentajes de completitud
            Map<Integer, BigDecimal> porcentajesCompletitud = new HashMap<>();

            // Para cada niño, obtener su estado de vacunación y certificados
            for (Nino nino : ninos) {
                // Obtener certificados existentes
                List<CertificadoVacunacion> certificados = certificadoDAO.findByNino(nino.getId());

                // Calcular porcentaje de completitud
                BigDecimal porcentaje = calcularPorcentajeCompletitud(nino.getId());
                porcentajesCompletitud.put(nino.getId(), porcentaje);
            }

            request.setAttribute("ninos", ninos);
            request.setAttribute("porcentajesCompletitud", porcentajesCompletitud);
            request.setAttribute("esPadre", true);
            request.setAttribute("nombreUsuario", padre.obtenerNombreCompleto());

        } catch (Exception e) {
            log("Error al obtener certificados del padre", e);
            request.setAttribute("error", "Error al cargar la información");
        }

        request.getRequestDispatcher("/WEB-INF/jsp/certificados.jsp").forward(request, response);
    }

    /**
     * Muestra certificados para profesionales
     */
    private void mostrarCertificadosProfesional(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");

        if (profesional == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String ninoIdParam = request.getParameter("ninoId");

        if (ninoIdParam != null && !ninoIdParam.trim().isEmpty()) {
            try {
                Integer ninoId = Integer.parseInt(ninoIdParam);
                Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);

                if (ninoOpt.isPresent()) {
                    Nino nino = ninoOpt.get();

                    // Obtener certificados del niño
                    List<CertificadoVacunacion> certificados = certificadoDAO.findByNino(ninoId);

                    // Calcular estado actual
                    BigDecimal porcentaje = calcularPorcentajeCompletitud(ninoId);

                    request.setAttribute("ninoSeleccionado", nino);
                    request.setAttribute("certificados", certificados);
                    request.setAttribute("porcentajeCompletitud", porcentaje);
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
     * Genera un nuevo certificado
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
            Integer usuarioId = (Integer) request.getSession().getAttribute("usuarioId");

            // Verificar acceso
            if (!tieneAccesoAlNino(request, ninoId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tiene acceso a este niño");
                return;
            }

            // Generar certificado usando el servicio
            CertificadoService.CertificadoResult resultado =
                    certificadoService.generarCertificado(ninoId, usuarioId);

            if (resultado.isSuccess()) {
                request.setAttribute("success", resultado.getMessage());
                request.setAttribute("certificadoGenerado", resultado.getCertificado());
            } else {
                request.setAttribute("error", resultado.getMessage());
            }

        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de niño no válido");
        } catch (Exception e) {
            log("Error al generar certificado", e);
            request.setAttribute("error", "Error al generar el certificado");
        }

        mostrarListaCertificados(request, response);
    }

    /**
     * Ver el contenido de un certificado
     */
    private void verCertificado(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String certificadoIdParam = request.getParameter("certificadoId");

        if (certificadoIdParam == null || certificadoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "Certificado no especificado");
            mostrarListaCertificados(request, response);
            return;
        }

        try {
            Integer certificadoId = Integer.parseInt(certificadoIdParam);
            Integer usuarioId = (Integer) request.getSession().getAttribute("usuarioId");

            // Obtener contenido del certificado
            String contenido = certificadoService.obtenerContenidoCertificado(certificadoId, usuarioId);

            if (contenido != null) {
                request.setAttribute("contenidoCertificado", contenido);

                // Obtener información del certificado
                Optional<CertificadoVacunacion> certOpt = certificadoDAO.findById(certificadoId);
                if (certOpt.isPresent()) {
                    request.setAttribute("certificado", certOpt.get());
                }

                request.getRequestDispatcher("/WEB-INF/jsp/ver-certificado.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "No tiene acceso a este certificado");
                mostrarListaCertificados(request, response);
            }

        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de certificado no válido");
            mostrarListaCertificados(request, response);
        }
    }

    /**
     * Verificar un certificado por código
     */
    private void verificarCertificado(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String codigo = request.getParameter("codigo");

        if (codigo == null || codigo.trim().isEmpty()) {
            request.setAttribute("error", "Debe ingresar un código de verificación");
        } else {
            CertificadoService.VerificacionResult resultado =
                    certificadoService.verificarCertificado(codigo.trim());

            if (resultado.isValido()) {
                request.setAttribute("verificacionExitosa", true);
                request.setAttribute("certificadoVerificado", resultado.getCertificado());
                request.setAttribute("mensajeVerificacion", resultado.getMensaje());
            } else {
                request.setAttribute("error", resultado.getMensaje());
            }
        }

        request.getRequestDispatcher("/WEB-INF/jsp/verificar-certificado.jsp").forward(request, response);
    }

    /**
     * Mostrar progreso detallado de vacunación
     */
    private void mostrarProgreso(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ninoIdParam = request.getParameter("ninoId");

        if (ninoIdParam == null || ninoIdParam.trim().isEmpty()) {
            request.setAttribute("error", "Debe especificar el niño");
            mostrarListaCertificados(request, response);
            return;
        }

        try {
            Integer ninoId = Integer.parseInt(ninoIdParam);

            // Verificar acceso
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

            // Obtener información de vacunación
            List<RegistroVacuna> historialVacunas = registroVacunaDAO.findByNino(ninoId);
            BigDecimal porcentajeCompletitud = calcularPorcentajeCompletitud(ninoId);

            // Preparar datos para la vista
            request.setAttribute("nino", nino);
            request.setAttribute("historialVacunas", historialVacunas);
            request.setAttribute("porcentajeCompletitud", porcentajeCompletitud);
            request.setAttribute("fechaGeneracion", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            request.getRequestDispatcher("/WEB-INF/jsp/progreso-vacunacion.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID de niño no válido");
            mostrarListaCertificados(request, response);
        }
    }

    /**
     * Verifica si el usuario tiene acceso al niño especificado
     */
    private boolean tieneAccesoAlNino(HttpServletRequest request, Integer ninoId) {
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");

        try {
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
        } catch (Exception e) {
            log("Error al verificar acceso al niño: " + ninoId, e);
        }

        return false;
    }

    /**
     * Calcula el porcentaje de completitud del esquema de vacunación
     */
    private BigDecimal calcularPorcentajeCompletitud(Integer ninoId) {
        try {
            // Obtener total de vacunas en el esquema
            EsquemaVacunacionDAO esquemaDAO = EsquemaVacunacionDAO.getInstance();
            List<EsquemaVacunacion> esquemaTotal = esquemaDAO.findActive();

            if (esquemaTotal.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Obtener vacunas aplicadas
            List<RegistroVacuna> vacunasAplicadas = registroVacunaDAO.findByNino(ninoId);

            // Calcular porcentaje
            double porcentaje = (double) vacunasAplicadas.size() / esquemaTotal.size() * 100;
            return BigDecimal.valueOf(Math.min(porcentaje, 100.0)).setScale(2, BigDecimal.ROUND_HALF_UP);

        } catch (Exception e) {
            log("Error al calcular porcentaje de completitud", e);
            return BigDecimal.ZERO;
        }
    }
}