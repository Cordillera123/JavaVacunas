package com.vacutrack.servlet;

import com.vacutrack.dao.UsuarioDAO;
import com.vacutrack.dao.PadreFamiliaDAO;
import com.vacutrack.dao.ProfesionalEnfermeriaDAO;
import com.vacutrack.dao.TipoUsuarioDAO;
import com.vacutrack.dao.CentroSaludDAO;
import com.vacutrack.model.Usuario;
import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.ProfesionalEnfermeria;
import com.vacutrack.model.TipoUsuario;
import com.vacutrack.model.CentroSalud;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Servlet para manejar el registro de nuevos usuarios
 * Soporta registro de padres de familia y profesionales de enfermería
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {
    
    private UsuarioDAO usuarioDAO;
    private PadreFamiliaDAO padreFamiliaDAO;
    private ProfesionalEnfermeriaDAO profesionalDAO;
    private TipoUsuarioDAO tipoUsuarioDAO;
    private CentroSaludDAO centroSaludDAO;
    
    @Override
    public void init() throws ServletException {
        usuarioDAO = UsuarioDAO.getInstance();
        padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
        tipoUsuarioDAO = TipoUsuarioDAO.getInstance();
        centroSaludDAO = CentroSaludDAO.getInstance();
    }
    
    /**
     * Muestra la página de registro
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            // Cargar centros de salud para profesionales
            List<CentroSalud> centrosSalud = centroSaludDAO.findActivos();
            request.setAttribute("centrosSalud", centrosSalud);
            
            // Mostrar página de registro
            request.getRequestDispatcher("/WEB-INF/jsp/registro.jsp").forward(request, response);
            
        } catch (Exception e) {
            getServletContext().log("Error al cargar página de registro", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error al cargar la página de registro");
        }
    }
    
    /**
     * Procesa el registro de un nuevo usuario
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String tipoUsuario = request.getParameter("tipoUsuario"); // "PADRE" o "PROFESIONAL"
        
        if ("PADRE".equals(tipoUsuario)) {
            procesarRegistroPadre(request, response);
        } else if ("PROFESIONAL".equals(tipoUsuario)) {
            procesarRegistroProfesional(request, response);
        } else {
            request.setAttribute("error", "Tipo de usuario no válido");
            doGet(request, response);
        }
    }
    
    /**
     * Procesa el registro de un padre de familia
     */
    private void procesarRegistroPadre(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            // Obtener parámetros
            String cedula = request.getParameter("cedula");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");
            String nombres = request.getParameter("nombres");
            String apellidos = request.getParameter("apellidos");
            String telefono = request.getParameter("telefono");
            String direccion = request.getParameter("direccion");
            
            // Validar parámetros básicos
            String error = validarParametrosPadre(cedula, email, password, confirmPassword, nombres, apellidos);
            if (error != null) {
                request.setAttribute("error", error);
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Verificar que la cédula no esté registrada
            if (usuarioDAO.findByCedula(cedula).isPresent()) {
                request.setAttribute("error", "La cédula ya está registrada en el sistema");
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Verificar que el email no esté registrado
            if (email != null && !email.trim().isEmpty() && usuarioDAO.findByEmail(email).isPresent()) {
                request.setAttribute("error", "El email ya está registrado en el sistema");
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Obtener tipo de usuario PADRE_FAMILIA
            Optional<TipoUsuario> tipoUsuarioOpt = tipoUsuarioDAO.findByNombre("PADRE_FAMILIA");
            if (!tipoUsuarioOpt.isPresent()) {
                request.setAttribute("error", "Error de configuración del sistema. Contacte al administrador");
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setCedula(cedula);
            usuario.setEmail(email.trim().isEmpty() ? null : email);
            usuario.setPasswordHash(password); // Sin hash para proyecto básico
            usuario.setTipoUsuarioId(tipoUsuarioOpt.get().getId());
            
            // Guardar usuario
            usuario = usuarioDAO.save(usuario);
            
            if (usuario.getId() == null) {
                request.setAttribute("error", "Error al crear el usuario. Intente nuevamente");
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Crear padre de familia
            PadreFamilia padre = new PadreFamilia();
            padre.setUsuarioId(usuario.getId());
            padre.setNombres(nombres);
            padre.setApellidos(apellidos);
            padre.setTelefono(telefono.trim().isEmpty() ? null : telefono);
            padre.setDireccion(direccion.trim().isEmpty() ? null : direccion);
            
            // Guardar padre
            padre = padreFamiliaDAO.save(padre);
            
            if (padre.getId() == null) {
                // Si falla, eliminar el usuario creado
                usuarioDAO.delete(usuario.getId());
                request.setAttribute("error", "Error al crear el perfil de padre. Intente nuevamente");
                preservarDatosPadre(request, cedula, email, nombres, apellidos, telefono, direccion);
                doGet(request, response);
                return;
            }
            
            // Registro exitoso
            request.setAttribute("success", "Registro exitoso. Ya puede iniciar sesión");
            request.setAttribute("cedula", cedula);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            
        } catch (Exception e) {
            getServletContext().log("Error en registro de padre", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            doGet(request, response);
        }
    }
    
    /**
     * Procesa el registro de un profesional de enfermería
     */
    private void procesarRegistroProfesional(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            // Obtener parámetros
            String cedula = request.getParameter("cedula");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");
            String nombres = request.getParameter("nombres");
            String apellidos = request.getParameter("apellidos");
            String numeroColegio = request.getParameter("numeroColegio");
            String centroSaludIdStr = request.getParameter("centroSaludId");
            String especialidad = request.getParameter("especialidad");
            
            // Validar parámetros básicos
            String error = validarParametrosProfesional(cedula, email, password, confirmPassword, 
                nombres, apellidos, numeroColegio, centroSaludIdStr);
            if (error != null) {
                request.setAttribute("error", error);
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            Integer centroSaludId = Integer.parseInt(centroSaludIdStr);
            
            // Verificar que la cédula no esté registrada
            if (usuarioDAO.findByCedula(cedula).isPresent()) {
                request.setAttribute("error", "La cédula ya está registrada en el sistema");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Verificar que el email no esté registrado
            if (email != null && !email.trim().isEmpty() && usuarioDAO.findByEmail(email).isPresent()) {
                request.setAttribute("error", "El email ya está registrado en el sistema");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Verificar que el centro de salud existe
            Optional<CentroSalud> centroOpt = centroSaludDAO.findById(centroSaludId);
            if (!centroOpt.isPresent()) {
                request.setAttribute("error", "Centro de salud no válido");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Obtener tipo de usuario PROFESIONAL_ENFERMERIA
            Optional<TipoUsuario> tipoUsuarioOpt = tipoUsuarioDAO.findByNombre("PROFESIONAL_ENFERMERIA");
            if (!tipoUsuarioOpt.isPresent()) {
                request.setAttribute("error", "Error de configuración del sistema. Contacte al administrador");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setCedula(cedula);
            usuario.setEmail(email.trim().isEmpty() ? null : email);
            usuario.setPasswordHash(password); // Sin hash para proyecto básico
            usuario.setTipoUsuarioId(tipoUsuarioOpt.get().getId());
            
            // Guardar usuario
            usuario = usuarioDAO.save(usuario);
            
            if (usuario.getId() == null) {
                request.setAttribute("error", "Error al crear el usuario. Intente nuevamente");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Crear profesional de enfermería
            ProfesionalEnfermeria profesional = new ProfesionalEnfermeria();
            profesional.setUsuarioId(usuario.getId());
            profesional.setNombres(nombres);
            profesional.setApellidos(apellidos);
            profesional.setNumeroColegio(numeroColegio);
            profesional.setCentroSaludId(centroSaludId);
            profesional.setEspecialidad(especialidad.trim().isEmpty() ? null : especialidad);
            
            // Guardar profesional
            profesional = profesionalDAO.save(profesional);
            
            if (profesional.getId() == null) {
                // Si falla, eliminar el usuario creado
                usuarioDAO.delete(usuario.getId());
                request.setAttribute("error", "Error al crear el perfil profesional. Intente nuevamente");
                preservarDatosProfesional(request, cedula, email, nombres, apellidos, 
                    numeroColegio, centroSaludIdStr, especialidad);
                doGet(request, response);
                return;
            }
            
            // Registro exitoso
            request.setAttribute("success", "Registro exitoso. Ya puede iniciar sesión");
            request.setAttribute("cedula", cedula);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            
        } catch (Exception e) {
            getServletContext().log("Error en registro de profesional", e);
            request.setAttribute("error", "Error interno del sistema. Intente nuevamente");
            doGet(request, response);
        }
    }
    
    /**
     * Valida parámetros para registro de padre
     */
    private String validarParametrosPadre(String cedula, String email, String password, 
            String confirmPassword, String nombres, String apellidos) {
        
        // Validar campos requeridos
        if (cedula == null || cedula.trim().isEmpty()) {
            return "La cédula es requerida";
        }
        if (password == null || password.trim().isEmpty()) {
            return "La contraseña es requerida";
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return "La confirmación de contraseña es requerida";
        }
        if (nombres == null || nombres.trim().isEmpty()) {
            return "Los nombres son requeridos";
        }
        if (apellidos == null || apellidos.trim().isEmpty()) {
            return "Los apellidos son requeridos";
        }
        
        // Validar formato de cédula
        if (!cedula.trim().matches("\\d{10}")) {
            return "La cédula debe tener 10 dígitos";
        }
        
        // Validar contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            return "Las contraseñas no coinciden";
        }
        
        // Validar longitud mínima de contraseña
        if (password.length() < 4) {
            return "La contraseña debe tener al menos 4 caracteres";
        }
        
        // Validar email si se proporciona
        if (email != null && !email.trim().isEmpty() && !email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            return "El formato del email no es válido";
        }
        
        // Validar nombres y apellidos
        if (nombres.trim().length() < 2 || nombres.trim().length() > 100) {
            return "Los nombres deben tener entre 2 y 100 caracteres";
        }
        if (apellidos.trim().length() < 2 || apellidos.trim().length() > 100) {
            return "Los apellidos deben tener entre 2 y 100 caracteres";
        }
        
        return null; // Sin errores
    }
    
    /**
     * Valida parámetros para registro de profesional
     */
    private String validarParametrosProfesional(String cedula, String email, String password, 
            String confirmPassword, String nombres, String apellidos, String numeroColegio, 
            String centroSaludId) {
        
        // Validaciones comunes con padre
        String errorComun = validarParametrosPadre(cedula, email, password, confirmPassword, nombres, apellidos);
        if (errorComun != null) {
            return errorComun;
        }
        
        // Validaciones específicas de profesional
        if (numeroColegio == null || numeroColegio.trim().isEmpty()) {
            return "El número de colegio profesional es requerido";
        }
        if (centroSaludId == null || centroSaludId.trim().isEmpty()) {
            return "Debe seleccionar un centro de salud";
        }
        
        // Validar que centroSaludId sea un número
        try {
            Integer.parseInt(centroSaludId);
        } catch (NumberFormatException e) {
            return "Centro de salud no válido";
        }
        
        return null; // Sin errores
    }
    
    /**
     * Preserva los datos del padre en caso de error
     */
    private void preservarDatosPadre(HttpServletRequest request, String cedula, String email, 
            String nombres, String apellidos, String telefono, String direccion) {
        request.setAttribute("cedula", cedula);
        request.setAttribute("email", email);
        request.setAttribute("nombres", nombres);
        request.setAttribute("apellidos", apellidos);
        request.setAttribute("telefono", telefono);
        request.setAttribute("direccion", direccion);
        request.setAttribute("tipoUsuario", "PADRE");
    }
    
    /**
     * Preserva los datos del profesional en caso de error
     */
    private void preservarDatosProfesional(HttpServletRequest request, String cedula, String email, 
            String nombres, String apellidos, String numeroColegio, String centroSaludId, String especialidad) {
        request.setAttribute("cedula", cedula);
        request.setAttribute("email", email);
        request.setAttribute("nombres", nombres);
        request.setAttribute("apellidos", apellidos);
        request.setAttribute("numeroColegio", numeroColegio);
        request.setAttribute("centroSaludIdSelected", centroSaludId);
        request.setAttribute("especialidad", especialidad);
        request.setAttribute("tipoUsuario", "PROFESIONAL");
    }
}