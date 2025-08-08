package com.vacutrack.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clase modelo para ProfesionalEnfermeria
 * Representa la información específica de profesionales de enfermería en el sistema
 * Extiende la información básica del Usuario con datos médicos y profesionales
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class ProfesionalEnfermeria {

    // Atributos de la clase
    private Integer id;
    private Integer usuarioId;
    private Usuario usuario; // Relación con Usuario
    private String nombres;
    private String apellidos;
    private String numeroColegio; // Número de registro en el colegio de enfermeras
    private Integer centroSaludId;
    private CentroSalud centroSalud; // Relación con CentroSalud
    private String especialidad;
    private LocalDateTime fechaCreacion;

    // Constantes para especialidades comunes
    public static final String ESPECIALIDAD_ENFERMERIA_GENERAL = "Enfermería General";
    public static final String ESPECIALIDAD_ENFERMERIA_PEDIATRICA = "Enfermería Pediátrica";
    public static final String ESPECIALIDAD_VACUNACION = "Especialista en Vacunación";
    public static final String ESPECIALIDAD_SALUD_PUBLICA = "Salud Pública";
    public static final String ESPECIALIDAD_ENFERMERIA_FAMILIAR = "Enfermería Familiar y Comunitaria";
    public static final String ESPECIALIDAD_ENFERMERIA_NEONATAL = "Enfermería Neonatal";

    // Patrones para validación
    private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile(
            "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    private static final Pattern PATRON_NUMERO_COLEGIO = Pattern.compile(
            "^[A-Z0-9-]{4,20}$"); // Formato típico: ENF-12345 o similar

    // Constructor vacío (requerido para JavaBeans)
    public ProfesionalEnfermeria() {
        this.fechaCreacion = LocalDateTime.now();
        this.especialidad = ESPECIALIDAD_ENFERMERIA_GENERAL;
    }

    // Constructor con parámetros básicos
    public ProfesionalEnfermeria(Integer usuarioId, String nombres, String apellidos) {
        this();
        this.usuarioId = usuarioId;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    // Constructor con Usuario
    public ProfesionalEnfermeria(Usuario usuario, String nombres, String apellidos) {
        this();
        this.usuario = usuario;
        this.usuarioId = usuario != null ? usuario.getId() : null;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    // Constructor completo
    public ProfesionalEnfermeria(Integer id, Integer usuarioId, String nombres, String apellidos,
                                 String numeroColegio, Integer centroSaludId, String especialidad,
                                 LocalDateTime fechaCreacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.numeroColegio = numeroColegio;
        this.centroSaludId = centroSaludId;
        this.especialidad = especialidad;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        this.usuarioId = usuario != null ? usuario.getId() : null;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres != null ? normalizarTexto(nombres) : null;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos != null ? normalizarTexto(apellidos) : null;
    }

    public String getNumeroColegio() {
        return numeroColegio;
    }

    public void setNumeroColegio(String numeroColegio) {
        if (numeroColegio != null && !numeroColegio.trim().isEmpty()) {
            this.numeroColegio = numeroColegio.trim().toUpperCase();
        } else {
            this.numeroColegio = null;
        }
    }

    public Integer getCentroSaludId() {
        return centroSaludId;
    }

    public void setCentroSaludId(Integer centroSaludId) {
        this.centroSaludId = centroSaludId;
    }

    public CentroSalud getCentroSalud() {
        return centroSalud;
    }

    public void setCentroSalud(CentroSalud centroSalud) {
        this.centroSalud = centroSalud;
        this.centroSaludId = centroSalud != null ? centroSalud.getId() : null;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad != null ? especialidad.trim() : ESPECIALIDAD_ENFERMERIA_GENERAL;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Obtiene el nombre completo del profesional
     * @return String con nombres y apellidos
     */
    public String obtenerNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder();

        if (this.nombres != null && !this.nombres.trim().isEmpty()) {
            nombreCompleto.append(this.nombres);
        }

        if (this.apellidos != null && !this.apellidos.trim().isEmpty()) {
            if (nombreCompleto.length() > 0) {
                nombreCompleto.append(" ");
            }
            nombreCompleto.append(this.apellidos);
        }

        return nombreCompleto.length() > 0 ? nombreCompleto.toString() : "Sin nombre";
    }

    /**
     * Obtiene el nombre profesional con credenciales
     * @return String con nombre completo y título profesional
     */
    public String obtenerNombreProfesional() {
        StringBuilder nombreProfesional = new StringBuilder();
        nombreProfesional.append("Enfermera ");
        nombreProfesional.append(obtenerNombreCompleto());

        if (this.numeroColegio != null && !this.numeroColegio.isEmpty()) {
            nombreProfesional.append(" - Reg: ").append(this.numeroColegio);
        }

        return nombreProfesional.toString();
    }

    /**
     * Obtiene las iniciales del profesional
     * @return String con las iniciales (ej: "M.E.G.L.")
     */
    public String obtenerIniciales() {
        StringBuilder iniciales = new StringBuilder();

        if (this.nombres != null && !this.nombres.isEmpty()) {
            String[] partesNombres = this.nombres.trim().split("\\s+");
            for (String parte : partesNombres) {
                if (!parte.isEmpty()) {
                    iniciales.append(parte.charAt(0)).append(".");
                }
            }
        }

        if (this.apellidos != null && !this.apellidos.isEmpty()) {
            String[] partesApellidos = this.apellidos.trim().split("\\s+");
            for (String parte : partesApellidos) {
                if (!parte.isEmpty()) {
                    iniciales.append(parte.charAt(0)).append(".");
                }
            }
        }

        return iniciales.toString();
    }

    /**
     * Obtiene la cédula del usuario asociado
     * @return String con la cédula o null si no hay usuario
     */
    public String obtenerCedula() {
        return this.usuario != null ? this.usuario.getCedula() : null;
    }

    /**
     * Obtiene el email del usuario asociado
     * @return String con el email o null si no hay usuario
     */
    public String obtenerEmail() {
        return this.usuario != null ? this.usuario.getEmail() : null;
    }

    /**
     * Verifica si el profesional está activo (basado en el usuario)
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.usuario != null && this.usuario.estaActivo();
    }

    /**
     * Obtiene el nombre del centro de salud
     * @return String con el nombre del centro de salud
     */
    public String obtenerNombreCentroSalud() {
        if (this.centroSalud != null) {
            return this.centroSalud.getNombre();
        }
        return "Centro no asignado";
    }

    /**
     * Obtiene la dirección del centro de salud
     * @return String con la dirección del centro de salud
     */
    public String obtenerDireccionCentroSalud() {
        if (this.centroSalud != null) {
            return this.centroSalud.getDireccion();
        }
        return "Dirección no disponible";
    }

    /**
     * Verifica si tiene centro de salud asignado
     * @return true si tiene centro asignado
     */
    public boolean tieneCentroAsignado() {
        return this.centroSaludId != null && this.centroSalud != null;
    }

    /**
     * Verifica si tiene número de colegio
     * @return true si tiene número de colegio
     */
    public boolean tieneNumeroColegio() {
        return this.numeroColegio != null && !this.numeroColegio.isEmpty();
    }

    /**
     * Verifica si es especialista en pediatría
     * @return true si es especialista pediátrico
     */
    public boolean esEspecialistaPediatrico() {
        return this.especialidad != null &&
                (this.especialidad.toLowerCase().contains("pediátrica") ||
                        this.especialidad.toLowerCase().contains("pediatrica") ||
                        this.especialidad.toLowerCase().contains("neonatal"));
    }

    /**
     * Verifica si es especialista en vacunación
     * @return true si es especialista en vacunación
     */
    public boolean esEspecialistaVacunacion() {
        return this.especialidad != null &&
                this.especialidad.toLowerCase().contains("vacunación");
    }

    /**
     * Obtiene información profesional completa
     * @return String con información profesional completa
     */
    public String obtenerInformacionProfesional() {
        StringBuilder info = new StringBuilder();

        info.append(obtenerNombreProfesional());

        if (this.especialidad != null && !this.especialidad.equals(ESPECIALIDAD_ENFERMERIA_GENERAL)) {
            info.append(" - Esp: ").append(this.especialidad);
        }

        info.append(" - Centro: ").append(obtenerNombreCentroSalud());

        String cedula = obtenerCedula();
        if (cedula != null) {
            info.append(" - CI: ").append(cedula);
        }

        return info.toString();
    }

    /**
     * Obtiene información de contacto del centro
     * @return String con información de contacto
     */
    public String obtenerContactoCentro() {
        if (this.centroSalud != null) {
            return this.centroSalud.obtenerInformacionCompleta();
        }
        return "No hay información de contacto disponible";
    }

    /**
     * Calcula los años de experiencia (basado en fecha de creación)
     * @return número de años desde el registro
     */
    public long calcularAnosExperiencia() {
        if (this.fechaCreacion == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.YEARS.between(this.fechaCreacion.toLocalDate(),
                LocalDateTime.now().toLocalDate());
    }

    /**
     * Obtiene información para firma médica
     * @return String con información para firmar documentos médicos
     */
    public String obtenerFirmaMedica() {
        StringBuilder firma = new StringBuilder();

        firma.append(obtenerNombreCompleto());
        firma.append("\nEnfermera Profesional");

        if (this.numeroColegio != null) {
            firma.append("\nRegistro: ").append(this.numeroColegio);
        }

        if (this.especialidad != null && !this.especialidad.equals(ESPECIALIDAD_ENFERMERIA_GENERAL)) {
            firma.append("\n").append(this.especialidad);
        }

        firma.append("\n").append(obtenerNombreCentroSalud());

        return firma.toString();
    }

    /**
     * Normaliza texto (capitaliza primera letra de cada palabra)
     * @param texto texto a normalizar
     * @return texto normalizado
     */
    private String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        String textoLimpio = texto.trim().toLowerCase();
        StringBuilder resultado = new StringBuilder();
        boolean capitalizarSiguiente = true;

        for (char c : textoLimpio.toCharArray()) {
            if (Character.isLetter(c)) {
                if (capitalizarSiguiente) {
                    resultado.append(Character.toUpperCase(c));
                    capitalizarSiguiente = false;
                } else {
                    resultado.append(c);
                }
            } else if (Character.isWhitespace(c)) {
                resultado.append(c);
                capitalizarSiguiente = true;
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del profesional son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.usuarioId != null &&
                validarNombres() && validarApellidos() &&
                (this.numeroColegio == null || validarNumeroColegio());
    }

    /**
     * Valida los nombres
     * @return true si los nombres son válidos
     */
    public boolean validarNombres() {
        return this.nombres != null &&
                !this.nombres.trim().isEmpty() &&
                this.nombres.length() >= 2 &&
                this.nombres.length() <= 100 &&
                PATRON_SOLO_LETRAS.matcher(this.nombres).matches();
    }

    /**
     * Valida los apellidos
     * @return true si los apellidos son válidos
     */
    public boolean validarApellidos() {
        return this.apellidos != null &&
                !this.apellidos.trim().isEmpty() &&
                this.apellidos.length() >= 2 &&
                this.apellidos.length() <= 100 &&
                PATRON_SOLO_LETRAS.matcher(this.apellidos).matches();
    }

    /**
     * Valida el número de colegio
     * @return true si el número de colegio es válido
     */
    public boolean validarNumeroColegio() {
        if (this.numeroColegio == null || this.numeroColegio.isEmpty()) {
            return true; // Es opcional
        }
        return PATRON_NUMERO_COLEGIO.matcher(this.numeroColegio).matches() &&
                this.numeroColegio.length() >= 4 && this.numeroColegio.length() <= 20;
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.usuarioId == null) {
            mensajes.append("El usuario es requerido. ");
        }

        if (!validarNombres()) {
            mensajes.append("Los nombres son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarApellidos()) {
            mensajes.append("Los apellidos son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarNumeroColegio()) {
            mensajes.append("El número de colegio debe tener entre 4-20 caracteres alfanuméricos. ");
        }

        if (this.especialidad != null && this.especialidad.length() > 100) {
            mensajes.append("La especialidad no puede exceder 100 caracteres. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ProfesionalEnfermeria that = (ProfesionalEnfermeria) obj;
        return Objects.equals(id, that.id) && Objects.equals(usuarioId, that.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioId);
    }

    @Override
    public String toString() {
        return "ProfesionalEnfermeria{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", numeroColegio='" + numeroColegio + '\'' +
                ", centroSaludId=" + centroSaludId +
                ", especialidad='" + especialidad + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return obtenerNombreProfesional() + " - " + obtenerNombreCentroSalud();
    }

    /**
     * Representación para listas de selección
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append("Enf. ").append(obtenerNombreCompleto());

        if (this.especialidad != null && !this.especialidad.equals(ESPECIALIDAD_ENFERMERIA_GENERAL)) {
            display.append(" (").append(this.especialidad).append(")");
        }

        return display.toString();
    }

    /**
     * Representación para reportes médicos
     * @return String con información completa para reportes
     */
    public String toMedicalReportString() {
        StringBuilder report = new StringBuilder();
        report.append("PROFESIONAL RESPONSABLE\n");
        report.append("=======================\n");
        report.append("Nombre: ").append(obtenerNombreCompleto()).append("\n");

        String cedula = obtenerCedula();
        if (cedula != null) {
            report.append("Cédula: ").append(cedula).append("\n");
        }

        if (this.numeroColegio != null) {
            report.append("Registro Coleg.: ").append(this.numeroColegio).append("\n");
        }

        report.append("Especialidad: ").append(this.especialidad).append("\n");
        report.append("Centro de Salud: ").append(obtenerNombreCentroSalud()).append("\n");
        report.append("Dirección: ").append(obtenerDireccionCentroSalud()).append("\n");

        String email = obtenerEmail();
        if (email != null) {
            report.append("Email: ").append(email).append("\n");
        }

        long anosExperiencia = calcularAnosExperiencia();
        if (anosExperiencia > 0) {
            report.append("Años en sistema: ").append(anosExperiencia).append("\n");
        }

        return report.toString();
    }

    /**
     * Representación para credencial profesional
     * @return String formateado como credencial
     */
    public String toCredentialString() {
        StringBuilder credential = new StringBuilder();
        credential.append("═══════════════════════════════\n");
        credential.append("    CREDENCIAL PROFESIONAL\n");
        credential.append("═══════════════════════════════\n");
        credential.append("Nombre: ").append(obtenerNombreCompleto()).append("\n");
        credential.append("Profesión: Enfermería\n");

        if (this.numeroColegio != null) {
            credential.append("Registro: ").append(this.numeroColegio).append("\n");
        }

        credential.append("Especialidad: ").append(this.especialidad).append("\n");
        credential.append("Centro: ").append(obtenerNombreCentroSalud()).append("\n");

        String cedula = obtenerCedula();
        if (cedula != null) {
            credential.append("CI: ").append(cedula).append("\n");
        }

        credential.append("═══════════════════════════════");
        return credential.toString();
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un profesional de enfermería básico
     * @param usuario usuario asociado
     * @param nombres nombres del profesional
     * @param apellidos apellidos del profesional
     * @return ProfesionalEnfermeria configurado
     */
    public static ProfesionalEnfermeria crearBasico(Usuario usuario, String nombres, String apellidos) {
        ProfesionalEnfermeria profesional = new ProfesionalEnfermeria();
        profesional.setUsuario(usuario);
        profesional.setNombres(nombres);
        profesional.setApellidos(apellidos);
        return profesional;
    }

    /**
     * Crea un profesional con especialidad
     * @param usuario usuario asociado
     * @param nombres nombres del profesional
     * @param apellidos apellidos del profesional
     * @param especialidad especialidad del profesional
     * @param centroSalud centro de salud asignado
     * @return ProfesionalEnfermeria configurado
     */
    public static ProfesionalEnfermeria crearConEspecialidad(Usuario usuario, String nombres, String apellidos,
                                                             String especialidad, CentroSalud centroSalud) {
        ProfesionalEnfermeria profesional = crearBasico(usuario, nombres, apellidos);
        profesional.setEspecialidad(especialidad);
        profesional.setCentroSalud(centroSalud);
        return profesional;
    }

    /**
     * Crea un profesional completo
     * @param usuario usuario asociado
     * @param nombres nombres del profesional
     * @param apellidos apellidos del profesional
     * @param numeroColegio número de colegio
     * @param especialidad especialidad
     * @param centroSalud centro de salud
     * @return ProfesionalEnfermeria configurado
     */
    public static ProfesionalEnfermeria crearCompleto(Usuario usuario, String nombres, String apellidos,
                                                      String numeroColegio, String especialidad, CentroSalud centroSalud) {
        ProfesionalEnfermeria profesional = crearConEspecialidad(usuario, nombres, apellidos, especialidad, centroSalud);
        profesional.setNumeroColegio(numeroColegio);
        return profesional;
    }

    /**
     * Crea un profesional especialista en vacunación
     * @param usuario usuario asociado
     * @param nombres nombres del profesional
     * @param apellidos apellidos del profesional
     * @param centroSalud centro de salud
     * @return ProfesionalEnfermeria especializado en vacunación
     */
    public static ProfesionalEnfermeria crearEspecialistaVacunacion(Usuario usuario, String nombres, String apellidos,
                                                                    CentroSalud centroSalud) {
        return crearConEspecialidad(usuario, nombres, apellidos, ESPECIALIDAD_VACUNACION, centroSalud);
    }

    /**
     * Crea un profesional de ejemplo para testing
     * @return ProfesionalEnfermeria de ejemplo
     */
    public static ProfesionalEnfermeria crearEjemplo() {
        ProfesionalEnfermeria profesional = new ProfesionalEnfermeria();
        profesional.setNombres("Ana María");
        profesional.setApellidos("Rodríguez Pérez");
        profesional.setNumeroColegio("ENF-12345");
        profesional.setEspecialidad(ESPECIALIDAD_ENFERMERIA_PEDIATRICA);
        return profesional;
    }
}