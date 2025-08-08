package com.vacutrack.servlet;

import com.vacutrack.dao.CentroSaludDAO;
import com.vacutrack.model.Usuario;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.model.CentroSalud;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

/**
 * Servlet para mostrar el mapa de centros de salud
 * Maneja ubicaciones estáticas del Centro Histórico de Quito
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/mapa")
public class MapaServlet extends HttpServlet {
    
    private CentroSaludDAO centroSaludDAO;
    
    @Override
    public void init() throws ServletException {
        centroSaludDAO = CentroSaludDAO.getInstance();
    }
    
    /**
     * Muestra el mapa de centros de salud o ejecuta acciones específicas
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
                case "obtener-centros-json":
                    obtenerCentrosJSON(request, response);
                    break;
                case "info-centro":
                    obtenerInfoCentro(request, response);
                    break;
                default:
                    mostrarMapa(request, response);
                    break;
            }
        } catch (Exception e) {
            getServletContext().log("Error en MapaServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del sistema");
        }
    }
    
    /**
     * Muestra la página principal del mapa
     */
    private void mostrarMapa(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String tipoUsuario = (String) session.getAttribute("tipoUsuario");
        
        // Obtener todos los centros de salud activos
        List<CentroSalud> centrosSalud = centroSaludDAO.findActivos();
        request.setAttribute("centrosSalud", centrosSalud);
        
        // Información del usuario
        if ("PADRE_FAMILIA".equals(tipoUsuario)) {
            PadreFamilia padre = (PadreFamilia) session.getAttribute("padre");
            if (padre != null) {
                request.setAttribute("nombreUsuario", padre.obtenerNombreCompleto());
                request.setAttribute("esPadre", true);
            }
        } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            ProfesionalEnfermeria profesional = (ProfesionalEnfermeria) session.getAttribute("profesional");
            if (profesional != null) {
                request.setAttribute("nombreUsuario", profesional.obtenerNombreCompleto());
                request.setAttribute("esProfesional", true);
                
                // Resaltar el centro de salud del profesional
                if (profesional.getCentroSaludId() != null) {
                    Optional<CentroSalud> centroPropio = centrosSalud.stream()
                        .filter(c -> c.getId().equals(profesional.getCentroSaludId()))
                        .findFirst();
                    centroPropio.ifPresent(centro -> request.setAttribute("centroPropio", centro));
                }
            }
        }
        
        // Información adicional para el mapa
        request.setAttribute("totalCentros", centrosSalud.size());
        request.setAttribute("sector", "Centro Histórico de Quito");
        
        // Coordenadas del centro del mapa (Plaza de la Independencia, Quito)
        request.setAttribute("latitudCentro", -0.2201049);
        request.setAttribute("longitudCentro", -78.5123450);
        request.setAttribute("zoomInicial", 15);
        
        // Mensaje informativo sobre datos estáticos
        request.setAttribute("mensajeInfo", 
            "Este mapa muestra los centros de salud del Centro Histórico de Quito con ubicaciones de referencia.");
        
        request.getRequestDispatcher("/WEB-INF/jsp/mapa-centros.jsp").forward(request, response);
    }
    
    /**
     * Retorna los centros de salud en formato JSON para el mapa
     */
    private void obtenerCentrosJSON(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        List<CentroSalud> centrosSalud = centroSaludDAO.findActivos();
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        StringBuilder json = new StringBuilder();
        json.append("{\"centros\": [");
        
        for (int i = 0; i < centrosSalud.size(); i++) {
            CentroSalud centro = centrosSalud.get(i);
            
            if (i > 0) json.append(",");
            
            json.append("{");
            json.append("\"id\": ").append(centro.getId()).append(",");
            json.append("\"nombre\": \"").append(escaparJSON(centro.getNombre())).append("\",");
            json.append("\"direccion\": \"").append(escaparJSON(centro.getDireccion())).append("\",");
            json.append("\"telefono\": \"").append(escaparJSON(centro.getTelefono())).append("\",");
            json.append("\"latitud\": ").append(centro.getCoordenadaX()).append(",");
            json.append("\"longitud\": ").append(centro.getCoordenadaY()).append(",");
            json.append("\"sector\": \"").append(escaparJSON(centro.getSector())).append("\"");
            json.append("}");
        }
        
        json.append("]}");
        
        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }
    
    /**
     * Obtiene información detallada de un centro específico
     */
    private void obtenerInfoCentro(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String centroIdParam = request.getParameter("centroId");
        
        if (centroIdParam == null || centroIdParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID de centro requerido");
            return;
        }
        
        try {
            Integer centroId = Integer.parseInt(centroIdParam);
            Optional<CentroSalud> centroOpt = centroSaludDAO.findById(centroId);
            
            if (!centroOpt.isPresent()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Centro no encontrado");
                return;
            }
            
            CentroSalud centro = centroOpt.get();
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"id\": ").append(centro.getId()).append(",");
            json.append("\"nombre\": \"").append(escaparJSON(centro.getNombre())).append("\",");
            json.append("\"direccion\": \"").append(escaparJSON(centro.getDireccion())).append("\",");
            json.append("\"telefono\": \"").append(escaparJSON(centro.getTelefono())).append("\",");
            json.append("\"sector\": \"").append(escaparJSON(centro.getSector())).append("\",");
            json.append("\"latitud\": ").append(centro.getCoordenadaX()).append(",");
            json.append("\"longitud\": ").append(centro.getCoordenadaY()).append(",");
            json.append("\"activo\": ").append(centro.estaActivo()).append(",");
            
            // Información adicional
            json.append("\"informacionCompleta\": \"").append(escaparJSON(centro.obtenerInformacionCompleta())).append("\",");
            
            // URL de Google Maps (aunque sea estática)
            String urlMaps = centro.obtenerUrlGoogleMaps();
            if (urlMaps != null) {
                json.append("\"urlGoogleMaps\": \"").append(escaparJSON(urlMaps)).append("\",");
            }
            
            // Horarios de atención (datos estáticos de ejemplo)
            json.append("\"horariosAtencion\": {");
            json.append("\"lunes_viernes\": \"07:00 - 16:00\",");
            json.append("\"sabados\": \"08:00 - 13:00\",");
            json.append("\"domingos\": \"Cerrado\"");
            json.append("},");
            
            // Servicios disponibles (datos estáticos de ejemplo)
            json.append("\"servicios\": [");
            json.append("\"Vacunación infantil\",");
            json.append("\"Control de crecimiento\",");
            json.append("\"Consulta pediátrica\",");
            json.append("\"Inmunizaciones\"");
            json.append("]");
            
            json.append("}");
            
            try (PrintWriter out = response.getWriter()) {
                out.print(json.toString());
            }
            
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID de centro no válido");
        }
    }
    
    /**
     * Escapa caracteres especiales para JSON
     */
    private String escaparJSON(String texto) {
        if (texto == null) return "";
        
        return texto.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}