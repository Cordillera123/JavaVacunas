package com.vacutrack.servlet;

import com.vacutrack.dao.UsuarioDAO;
import com.vacutrack.dao.PadreFamiliaDAO;
import com.vacutrack.dao.ProfesionalEnfermeriaDAO;
import com.vacutrack.model.Usuario;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.ProfesionalEnfermeria;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

/**
 * Servlet para manejar el inicio de sesión de usuarios
 * Diferencia entre padres de familia y profesionales de enfermería
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    
    private UsuarioDAO usuarioDAO;
    private PadreFamiliaDAO padreFamiliaDAO;
    private ProfesionalEnfermeriaDAO profesionalDAO;
    
    @Override
    public void init() throws ServletException {
        usuarioDAO = UsuarioDAO.getInstance();
        padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
    }
    
    /**
     * Muestra la página de login
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Si ya está autenticado, redirigir al dashboard correspondiente
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuario") != null) {
            String tipoUsuario = (String) session.getAttribute("tipoUsuario");
            redirectToDashboard(response, tipoUsuario);
            return;
        }
        
        // Mostrar página de login
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }
    
    /**
     * Procesa el intento de login
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String cedula = request.getParameter("cedula");
        String password = request.getParameter("password");
        String tipoUsuario = request.getParameter("tipoUsuario"); // "PADRE" o "PROFESIONAL"
        
        // Validar parámetros básicos
        if (cedula == null || cedula.trim().isEmpty() || 
            password == null || password.trim().isEmpty() ||
            tipoUsuario == null || tipoUsuario.trim().isEmpty()) {
            
            request.setAttribute("error", "Por favor complete todos los campos");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }
        
        // Limpiar cédula
        cedula = cedula.trim();
        password = password.trim();
        
        // Validar formato básico de cédula
        if (!cedula.matches("\\d{10}")) {
            request.setAttribute("error", "La cédula debe tener 10 dígitos");
            request.setAttribute("cedula", cedula);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }
        
        try {
            // Buscar usuario por cédula
            Optional<Usuario> usuarioOpt = usuarioDAO.findByCedula(cedula);
            
            if (!usuarioOpt.isPresent()) {
                request.setAttribute("error", "Usuario no encontrado");
                request.setAttribute("cedula", cedula);
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Verificar que el usuario esté activo
            if (!usuario.estaActivo()) {
                request.setAttribute("error", "Usuario inactivo. Contacte al administrador");
                request.setAttribute("cedula", cedula);
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            
            // Verificar password (comparación simple sin hash)
            if (!password.equals(usuario.getPasswordHash())) {
                request.setAttribute("error", "Contraseña incorrecta");
                request.setAttribute("cedula", cedula);
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            
            // Verificar que el tipo de usuario coincida
            String tipoUsuarioReal = usuario.getTipoUsuario() != null ? 
                usuario.getTipoUsuario().getNombre() : null;
                
            boolean tipoValido = false;
            if ("PADRE".equals(tipoUsuario) && "PADRE_FAMILIA".equals(tipoUsuarioReal)) {
                tipoValido = true;
            } else if ("PROFESIONAL".equals(tipoUsuario) && "PROFESIONAL_ENFERMERIA".equals(tipoUsuarioReal)) {
                tipoValido = true;
            }
            
            if (!tipoValido) {
                request.setAttribute("error", "Tipo de usuario incorrecto para estas credenciales");
                request.setAttribute("cedula", cedula);
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            
            // Login exitoso - crear sesión
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", usuario);
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("cedula", usuario.getCedula());
            session.setAttribute("tipoUsuario", tipoUsuarioReal);
            
            // Cargar información específica según tipo de usuario
            if ("PADRE_FAMILIA".equals(tipoUsuarioReal)) {
                Optional<PadreFamilia> padreOpt = padreFamiliaDAO.findByUsuario(usuario.getId());
                if (padreOpt.isPresent()) {
                    session.setAttribute("padre", padreOpt.get());
                    session.setAttribute("nombreUsuario", padreOpt.get().obtenerNombreCompleto());
                }
            } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuarioReal)) {
                Optional<ProfesionalEnfermeria> profesionalOpt = profesionalDAO.findByUsuario(usuario.getId());
                if (profesionalOpt.isPresent()) {
                    session.setAttribute("profesional", profesionalOpt.get());
                    session.setAttribute("nombreUsuario", profesionalOpt.get().obtenerNombreCompleto());
                }
            }
            
            // Actualizar última sesión
            usuarioDAO.updateUltimaSesion(usuario.getId());
            
            // Redirigir al dashboard correspondiente
            redirectToDashboard(response, tipoUsuarioReal);
            
        } catch (Exception e) {
            // Log del error
            getServletContext().log("Error en login para usuario: " + cedula, e);
            
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            request.setAttribute("cedula", cedula);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        }
    }
    
    /**
     * Redirige al dashboard correspondiente según el tipo de usuario
     */
    private void redirectToDashboard(HttpServletResponse response, String tipoUsuario) throws IOException {
        if ("PADRE_FAMILIA".equals(tipoUsuario)) {
            response.sendRedirect(getServletContext().getContextPath() + "/dashboard-padre");
        } else if ("PROFESIONAL_ENFERMERIA".equals(tipoUsuario)) {
            response.sendRedirect(getServletContext().getContextPath() + "/dashboard-profesional");
        } else {
            // Fallback al login si tipo no reconocido
            response.sendRedirect(getServletContext().getContextPath() + "/login");
        }
    }
}