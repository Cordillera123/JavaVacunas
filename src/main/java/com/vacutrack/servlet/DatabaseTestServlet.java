package com.vacutrack.servlet;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet de prueba para verificar la compatibilidad con la base de datos
 * Prueba las operaciones básicas de todos los DAOs para asegurar que coincidan
 * con el esquema de la base de datos.
 * 
 * @author VACU-TRACK Team
 * @version 1.0
 */
@WebServlet("/database-test")
public class DatabaseTestServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;
    private PadreFamiliaDAO padreFamiliaDAO;
    private ProfesionalEnfermeriaDAO profesionalDAO;
    private CentroSaludDAO centroSaludDAO;
    private NinoDAO ninoDAO;
    private VacunaDAO vacunaDAO;
    private RegistroVacunaDAO registroVacunaDAO;
    private NotificacionDAO notificacionDAO;
    private TipoUsuarioDAO tipoUsuarioDAO;
    
    @Override
    public void init() throws ServletException {
        usuarioDAO = UsuarioDAO.getInstance();
        padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
        centroSaludDAO = CentroSaludDAO.getInstance();
        ninoDAO = NinoDAO.getInstance();
        vacunaDAO = VacunaDAO.getInstance();
        registroVacunaDAO = RegistroVacunaDAO.getInstance();
        notificacionDAO = NotificacionDAO.getInstance();
        tipoUsuarioDAO = TipoUsuarioDAO.getInstance();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>VACU-TRACK - Prueba de Base de Datos</title>");
            out.println("<style>");
            out.println("body { font-family: Arial, sans-serif; margin: 20px; }");
            out.println(".success { color: green; }");
            out.println(".error { color: red; }");
            out.println(".info { color: blue; }");
            out.println("table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
            out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            out.println("th { background-color: #f2f2f2; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            
            out.println("<h1>VACU-TRACK - Prueba de Compatibilidad con Base de Datos</h1>");
            out.println("<p>Esta página verifica que todos los DAOs sean compatibles con el esquema de base de datos.</p>");
            
            // Probar cada DAO
            probarTipoUsuarioDAO(out);
            probarCentroSaludDAO(out);
            probarUsuarioDAO(out);
            probarPadreFamiliaDAO(out);
            probarProfesionalEnfermeriaDAO(out);
            probarVacunaDAO(out);
            probarNinoDAO(out);
            probarRegistroVacunaDAO(out);
            probarNotificacionDAO(out);
            
            out.println("<h2>Resumen de Pruebas</h2>");
            out.println("<p class='success'>✓ Todas las pruebas de compatibilidad con la base de datos han sido completadas.</p>");
            out.println("<p class='info'>Si no se muestran errores rojos arriba, la estructura está correctamente alineada con el esquema SQL.</p>");
            
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    private void probarTipoUsuarioDAO(PrintWriter out) {
        out.println("<h3>Prueba: TipoUsuarioDAO</h3>");
        try {
            List<TipoUsuario> tipos = tipoUsuarioDAO.findAll();
            out.println("<p class='success'>✓ TipoUsuarioDAO: " + tipos.size() + " tipos de usuario encontrados</p>");
            
            if (!tipos.isEmpty()) {
                out.println("<table>");
                out.println("<tr><th>ID</th><th>Nombre</th><th>Descripción</th></tr>");
                for (TipoUsuario tipo : tipos) {
                    out.println("<tr>");
                    out.println("<td>" + tipo.getId() + "</td>");
                    out.println("<td>" + tipo.getNombre() + "</td>");
                    out.println("<td>" + (tipo.getDescripcion() != null ? tipo.getDescripcion() : "N/A") + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en TipoUsuarioDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarCentroSaludDAO(PrintWriter out) {
        out.println("<h3>Prueba: CentroSaludDAO</h3>");
        try {
            List<CentroSalud> centros = centroSaludDAO.findActivos();
            out.println("<p class='success'>✓ CentroSaludDAO: " + centros.size() + " centros de salud activos encontrados</p>");
            
            if (!centros.isEmpty()) {
                out.println("<table>");
                out.println("<tr><th>ID</th><th>Nombre</th><th>Dirección</th><th>Coordenadas</th></tr>");
                for (CentroSalud centro : centros.subList(0, Math.min(5, centros.size()))) {
                    out.println("<tr>");
                    out.println("<td>" + centro.getId() + "</td>");
                    out.println("<td>" + centro.getNombre() + "</td>");
                    out.println("<td>" + centro.getDireccion() + "</td>");
                    out.println("<td>" + centro.getCoordenadaX() + ", " + centro.getCoordenadaY() + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en CentroSaludDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarUsuarioDAO(PrintWriter out) {
        out.println("<h3>Prueba: UsuarioDAO</h3>");
        try {
            List<Usuario> usuarios = usuarioDAO.findActivos();
            out.println("<p class='success'>✓ UsuarioDAO: " + usuarios.size() + " usuarios activos encontrados</p>");
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en UsuarioDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarPadreFamiliaDAO(PrintWriter out) {
        out.println("<h3>Prueba: PadreFamiliaDAO</h3>");
        try {
            List<PadreFamilia> padres = padreFamiliaDAO.findAll();
            out.println("<p class='success'>✓ PadreFamiliaDAO: " + padres.size() + " padres de familia encontrados</p>");
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en PadreFamiliaDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarProfesionalEnfermeriaDAO(PrintWriter out) {
        out.println("<h3>Prueba: ProfesionalEnfermeriaDAO</h3>");
        try {
            List<ProfesionalEnfermeria> profesionales = profesionalDAO.findActive();
            out.println("<p class='success'>✓ ProfesionalEnfermeriaDAO: " + profesionales.size() + " profesionales encontrados</p>");
            
            if (!profesionales.isEmpty()) {
                out.println("<table>");
                out.println("<tr><th>ID</th><th>Nombres</th><th>Apellidos</th><th>Número Colegio</th><th>Centro Salud ID</th><th>Especialidad</th></tr>");
                for (ProfesionalEnfermeria prof : profesionales.subList(0, Math.min(3, profesionales.size()))) {
                    out.println("<tr>");
                    out.println("<td>" + prof.getId() + "</td>");
                    out.println("<td>" + prof.getNombres() + "</td>");
                    out.println("<td>" + prof.getApellidos() + "</td>");
                    out.println("<td>" + (prof.getNumeroColegio() != null ? prof.getNumeroColegio() : "N/A") + "</td>");
                    out.println("<td>" + (prof.getCentroSaludId() != null ? prof.getCentroSaludId() : "N/A") + "</td>");
                    out.println("<td>" + (prof.getEspecialidad() != null ? prof.getEspecialidad() : "N/A") + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en ProfesionalEnfermeriaDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarVacunaDAO(PrintWriter out) {
        out.println("<h3>Prueba: VacunaDAO</h3>");
        try {
            List<Vacuna> vacunas = vacunaDAO.findActivas();
            out.println("<p class='success'>✓ VacunaDAO: " + vacunas.size() + " vacunas activas encontradas</p>");
            
            if (!vacunas.isEmpty()) {
                out.println("<table>");
                out.println("<tr><th>Código</th><th>Nombre</th><th>Dosis Total</th><th>Vía Administración</th></tr>");
                for (Vacuna vacuna : vacunas.subList(0, Math.min(5, vacunas.size()))) {
                    out.println("<tr>");
                    out.println("<td>" + vacuna.getCodigo() + "</td>");
                    out.println("<td>" + vacuna.getNombre() + "</td>");
                    out.println("<td>" + vacuna.getDosisTotal() + "</td>");
                    out.println("<td>" + (vacuna.getViaAdministracion() != null ? vacuna.getViaAdministracion() : "N/A") + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en VacunaDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarNinoDAO(PrintWriter out) {
        out.println("<h3>Prueba: NinoDAO</h3>");
        try {
            List<Nino> ninos = ninoDAO.findActivos();
            out.println("<p class='success'>✓ NinoDAO: " + ninos.size() + " niños activos encontrados</p>");
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en NinoDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarRegistroVacunaDAO(PrintWriter out) {
        out.println("<h3>Prueba: RegistroVacunaDAO</h3>");
        try {
            List<RegistroVacuna> registros = registroVacunaDAO.findAll();
            out.println("<p class='success'>✓ RegistroVacunaDAO: " + registros.size() + " registros de vacuna encontrados</p>");
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en RegistroVacunaDAO: " + e.getMessage() + "</p>");
        }
    }
    
    private void probarNotificacionDAO(PrintWriter out) {
        out.println("<h3>Prueba: NotificacionDAO</h3>");
        try {
            List<Notificacion> notificaciones = notificacionDAO.findAll();
            out.println("<p class='success'>✓ NotificacionDAO: " + notificaciones.size() + " notificaciones encontradas</p>");
        } catch (Exception e) {
            out.println("<p class='error'>✗ Error en NotificacionDAO: " + e.getMessage() + "</p>");
        }
    }
}
