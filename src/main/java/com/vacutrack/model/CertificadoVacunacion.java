package com.vacutrack.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Clase modelo para CertificadoVacunacion
 * Representa los certificados oficiales de vacunación que se pueden generar en PDF
 * Contiene información completa del niño y su historial de vacunación
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class CertificadoVacunacion {

    // Atributos de la clase
    private Integer id;
    private Integer ninoId;
    private Nino nino; // Relación con Nino
    private String codigoCertificado;
    private LocalDateTime fechaGeneracion;
    private BigDecimal porcentajeCompletitud;
    private String urlArchivo;
    private Boolean vigente;
    private List<RegistroVacuna> registrosVacunas; // Lista de vacunas aplicadas
    private List<Notificacion> notificacionesPendientes; // Lista de vacunas pendientes

    // Constantes para el certificado
    public static final String PREFIJO_CODIGO = "CERT-VACU";
    public static final String FORMATO_CODIGO = "CERT-VACU-%s-%04d";
    public static final BigDecimal PORCENTAJE_MINIMO_COMPLETO = new BigDecimal("80.00");
    public static final String TIPO_ARCHIVO = "application/pdf";
    public static final String EXTENSION_ARCHIVO = ".pdf";

    // Formatters para fechas
    private static final DateTimeFormatter FORMATO_FECHA_CERTIFICADO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_CORTA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_CODIGO_FECHA =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // Constructor vacío (requerido para JavaBeans)
    public CertificadoVacunacion() {
        this.fechaGeneracion = LocalDateTime.now();
        this.vigente = true;
        this.porcentajeCompletitud = BigDecimal.ZERO;
        this.registrosVacunas = new ArrayList<>();
        this.notificacionesPendientes = new ArrayList<>();
        this.codigoCertificado = generarCodigoCertificado();
    }

    // Constructor con niño
    public CertificadoVacunacion(Nino nino) {
        this();
        this.nino = nino;
        this.ninoId = nino != null ? nino.getId() : null;
        this.codigoCertificado = generarCodigoCertificado();
    }

    // Constructor con niño y registros
    public CertificadoVacunacion(Nino nino, List<RegistroVacuna> registrosVacunas) {
        this(nino);
        this.registrosVacunas = registrosVacunas != null ? new ArrayList<>(registrosVacunas) : new ArrayList<>();
        this.porcentajeCompletitud = calcularPorcentajeCompletitud();
    }

    // Constructor completo
    public CertificadoVacunacion(Integer id, Integer ninoId, String codigoCertificado,
                                 LocalDateTime fechaGeneracion, BigDecimal porcentajeCompletitud,
                                 String urlArchivo, Boolean vigente) {
        this.id = id;
        this.ninoId = ninoId;
        this.codigoCertificado = codigoCertificado;
        this.fechaGeneracion = fechaGeneracion;
        this.porcentajeCompletitud = porcentajeCompletitud;
        this.urlArchivo = urlArchivo;
        this.vigente = vigente;
        this.registrosVacunas = new ArrayList<>();
        this.notificacionesPendientes = new ArrayList<>();
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNinoId() {
        return ninoId;
    }

    public void setNinoId(Integer ninoId) {
        this.ninoId = ninoId;
    }

    public Nino getNino() {
        return nino;
    }

    public void setNino(Nino nino) {
        this.nino = nino;
        this.ninoId = nino != null ? nino.getId() : null;
    }

    public String getCodigoCertificado() {
        return codigoCertificado;
    }

    public void setCodigoCertificado(String codigoCertificado) {
        this.codigoCertificado = codigoCertificado != null ? codigoCertificado.trim().toUpperCase() : null;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public BigDecimal getPorcentajeCompletitud() {
        return porcentajeCompletitud;
    }

    public void setPorcentajeCompletitud(BigDecimal porcentajeCompletitud) {
        this.porcentajeCompletitud = porcentajeCompletitud != null ? porcentajeCompletitud : BigDecimal.ZERO;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo != null ? urlArchivo.trim() : null;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente != null ? vigente : true;
    }

    public List<RegistroVacuna> getRegistrosVacunas() {
        return registrosVacunas;
    }

    public void setRegistrosVacunas(List<RegistroVacuna> registrosVacunas) {
        this.registrosVacunas = registrosVacunas != null ? new ArrayList<>(registrosVacunas) : new ArrayList<>();
        this.porcentajeCompletitud = calcularPorcentajeCompletitud();
    }

    public List<Notificacion> getNotificacionesPendientes() {
        return notificacionesPendientes;
    }

    public void setNotificacionesPendientes(List<Notificacion> notificacionesPendientes) {
        this.notificacionesPendientes = notificacionesPendientes != null ?
                new ArrayList<>(notificacionesPendientes) : new ArrayList<>();
    }

    // Métodos utilitarios

    /**
     * Verifica si el certificado está vigente
     * @return true si está vigente
     */
    public boolean estaVigente() {
        return this.vigente != null && this.vigente;
    }

    /**
     * Verifica si el esquema está completo (80% o más)
     * @return true si está completo
     */
    public boolean estaCompleto() {
        return this.porcentajeCompletitud != null &&
                this.porcentajeCompletitud.compareTo(PORCENTAJE_MINIMO_COMPLETO) >= 0;
    }

    /**
     * Verifica si tiene archivo generado
     * @return true si tiene archivo PDF
     */
    public boolean tieneArchivo() {
        return this.urlArchivo != null && !this.urlArchivo.trim().isEmpty();
    }

    /**
     * Obtiene el nombre del niño
     * @return String con el nombre del niño
     */
    public String obtenerNombreNino() {
        return this.nino != null ? this.nino.obtenerNombreCompleto() : "Niño no especificado";
    }

    /**
     * Obtiene la cédula del niño
     * @return String con la cédula o "Sin cédula"
     */
    public String obtenerCedulaNino() {
        if (this.nino != null && this.nino.tieneCedula()) {
            return this.nino.getCedula();
        }
        return "Sin cédula";
    }

    /**
     * Obtiene la edad actual del niño
     * @return String con la edad formateada
     */
    public String obtenerEdadNino() {
        return this.nino != null ? this.nino.obtenerEdadFormateada() : "Edad no disponible";
    }

    /**
     * Obtiene información del padre/madre
     * @return String con información del representante
     */
    public String obtenerInformacionPadre() {
        if (this.nino != null && this.nino.getPadre() != null) {
            PadreFamilia padre = this.nino.getPadre();
            StringBuilder info = new StringBuilder();
            info.append(padre.obtenerNombreCompleto());

            String cedula = padre.obtenerCedula();
            if (cedula != null) {
                info.append(" - CI: ").append(cedula);
            }

            String email = padre.obtenerEmail();
            if (email != null) {
                info.append(" - ").append(email);
            }

            return info.toString();
        }
        return "Representante no especificado";
    }

    /**
     * Calcula el porcentaje de completitud basado en registros actuales
     * @return BigDecimal con el porcentaje (0-100)
     */
    public BigDecimal calcularPorcentajeCompletitud() {
        if (this.registrosVacunas == null || this.registrosVacunas.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Contar vacunas únicas aplicadas (por código de vacuna y número de dosis)
        long vacunasAplicadas = this.registrosVacunas.stream()
                .map(r -> r.obtenerCodigoVacuna() + "-" + r.getNumeroDosis())
                .distinct()
                .count();

        // Total esperado en el esquema ecuatoriano (aproximadamente 15-20 dosis)
        // Este número debería calcularse dinámicamente basado en el esquema completo
        int totalEsperado = obtenerTotalVacunasEsperadas();

        if (totalEsperado == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal porcentaje = BigDecimal.valueOf(vacunasAplicadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalEsperado), 2, BigDecimal.ROUND_HALF_UP);

        return porcentaje.compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.valueOf(100) : porcentaje;
    }

    /**
     * Obtiene el número total de vacunas esperadas según la edad
     * @return número total de dosis esperadas
     */
    private int obtenerTotalVacunasEsperadas() {
        if (this.nino == null || this.nino.getFechaNacimiento() == null) {
            return 20; // Valor por defecto
        }

        long edadEnDias = this.nino.calcularEdadEnDias();

        // Cálculo aproximado basado en el esquema ecuatoriano
        if (edadEnDias < 60) {
            return 2; // BCG, HB(0)
        } else if (edadEnDias < 120) {
            return 7; // + 5 vacunas de 2 meses
        } else if (edadEnDias < 180) {
            return 12; // + 5 vacunas de 4 meses
        } else if (edadEnDias < 365) {
            return 17; // + 5 vacunas de 6 meses + Influenza
        } else if (edadEnDias < 540) {
            return 19; // + SRP, FA
        } else {
            return 21; // + SRP(2), Varicela (esquema completo)
        }
    }

    /**
     * Obtiene el estado del certificado
     * @return String con el estado actual
     */
    public String obtenerEstado() {
        if (!estaVigente()) {
            return "NO VIGENTE";
        } else if (estaCompleto()) {
            return "COMPLETO";
        } else if (this.porcentajeCompletitud.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "EN PROGRESO";
        } else {
            return "INICIADO";
        }
    }

    /**
     * Obtiene el color CSS para el estado
     * @return String con código de color
     */
    public String obtenerColorEstado() {
        String estado = obtenerEstado();
        switch (estado) {
            case "COMPLETO":
                return "#198754"; // Verde
            case "EN PROGRESO":
                return "#0d6efd"; // Azul
            case "INICIADO":
                return "#fd7e14"; // Naranja
            case "NO VIGENTE":
                return "#dc3545"; // Rojo
            default:
                return "#6c757d"; // Gris
        }
    }

    /**
     * Genera un código único para el certificado
     * @return String con código único
     */
    private String generarCodigoCertificado() {
        String fechaCodigo = LocalDateTime.now().format(FORMATO_CODIGO_FECHA);
        String uuidCorto = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PREFIJO_CODIGO + "-" + fechaCodigo + "-" + uuidCorto;
    }

    /**
     * Regenera el código del certificado
     */
    public void regenerarCodigo() {
        this.codigoCertificado = generarCodigoCertificado();
    }

    /**
     * Actualiza la fecha de generación
     */
    public void actualizarFechaGeneracion() {
        this.fechaGeneracion = LocalDateTime.now();
    }

    /**
     * Invalida el certificado
     */
    public void invalidar() {
        this.vigente = false;
    }

    /**
     * Restaura la vigencia del certificado
     */
    public void restaurarVigencia() {
        this.vigente = true;
    }

    /**
     * Agrega un registro de vacuna al certificado
     * @param registro registro a agregar
     */
    public void agregarRegistroVacuna(RegistroVacuna registro) {
        if (registro != null) {
            this.registrosVacunas.add(registro);
            this.porcentajeCompletitud = calcularPorcentajeCompletitud();
        }
    }

    /**
     * Remueve un registro de vacuna del certificado
     * @param registro registro a remover
     */
    public void removerRegistroVacuna(RegistroVacuna registro) {
        if (registro != null) {
            this.registrosVacunas.remove(registro);
            this.porcentajeCompletitud = calcularPorcentajeCompletitud();
        }
    }

    /**
     * Obtiene las vacunas aplicadas agrupadas por tipo
     * @return String con resumen de vacunas
     */
    public String obtenerResumenVacunas() {
        if (this.registrosVacunas == null || this.registrosVacunas.isEmpty()) {
            return "No hay vacunas registradas";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("Vacunas aplicadas (").append(this.registrosVacunas.size()).append(" dosis):\n");

        this.registrosVacunas.stream()
                .sorted((r1, r2) -> r1.getFechaAplicacion().compareTo(r2.getFechaAplicacion()))
                .forEach(registro -> {
                    resumen.append("• ").append(registro.obtenerInformacionCertificado()).append("\n");
                });

        return resumen.toString();
    }

    /**
     * Obtiene las vacunas pendientes
     * @return String con vacunas pendientes
     */
    public String obtenerVacunasPendientes() {
        if (this.notificacionesPendientes == null || this.notificacionesPendientes.isEmpty()) {
            return "No hay vacunas pendientes registradas";
        }

        StringBuilder pendientes = new StringBuilder();
        pendientes.append("Vacunas pendientes (").append(this.notificacionesPendientes.size()).append("):\n");

        this.notificacionesPendientes.stream()
                .filter(n -> n.debeSerMostrada() && !n.fueAplicada())
                .sorted((n1, n2) -> n1.getFechaProgramada().compareTo(n2.getFechaProgramada()))
                .forEach(notificacion -> {
                    pendientes.append("• ").append(notificacion.obtenerNombreVacuna())
                            .append(" (Dosis ").append(notificacion.getNumeroDosis()).append(")")
                            .append(" - ").append(notificacion.getFechaProgramada()).append("\n");
                });

        return pendientes.toString();
    }

    /**
     * Genera el contenido completo del certificado para PDF
     * @return String con todo el contenido del certificado
     */
    public String generarContenidoCertificado() {
        StringBuilder certificado = new StringBuilder();

        // Encabezado
        certificado.append("═══════════════════════════════════════════════════════\n");
        certificado.append("               CERTIFICADO DE VACUNACIÓN\n");
        certificado.append("                      VACU-TRACK\n");
        certificado.append("═══════════════════════════════════════════════════════\n\n");

        // Código y fecha
        certificado.append("Código: ").append(this.codigoCertificado).append("\n");
        certificado.append("Fecha de emisión: ").append(this.fechaGeneracion.format(FORMATO_FECHA_CERTIFICADO)).append("\n");
        certificado.append("Estado: ").append(obtenerEstado()).append("\n\n");

        // Información del niño
        certificado.append("INFORMACIÓN DEL PACIENTE\n");
        certificado.append("═══════════════════════════\n");
        certificado.append("Nombre: ").append(obtenerNombreNino()).append("\n");
        certificado.append("Cédula: ").append(obtenerCedulaNino()).append("\n");
        certificado.append("Edad actual: ").append(obtenerEdadNino()).append("\n");

        if (this.nino != null) {
            certificado.append("Fecha nacimiento: ").append(this.nino.getFechaNacimiento()).append("\n");
            certificado.append("Sexo: ").append(this.nino.obtenerSexoLegible()).append("\n");
        }

        certificado.append("Representante: ").append(obtenerInformacionPadre()).append("\n\n");

        // Progreso de vacunación
        certificado.append("PROGRESO DE VACUNACIÓN\n");
        certificado.append("═══════════════════════\n");
        certificado.append("Completitud: ").append(this.porcentajeCompletitud).append("%\n");
        certificado.append("Vacunas aplicadas: ").append(this.registrosVacunas.size()).append(" dosis\n\n");

        // Detalle de vacunas aplicadas
        certificado.append("VACUNAS APLICADAS\n");
        certificado.append("═══════════════════\n");
        certificado.append(obtenerResumenVacunas()).append("\n");

        // Vacunas pendientes
        certificado.append("VACUNAS PENDIENTES\n");
        certificado.append("════════════════════\n");
        certificado.append(obtenerVacunasPendientes()).append("\n");

        // Pie del certificado
        certificado.append("═══════════════════════════════════════════════════════\n");
        certificado.append("Este certificado es válido y ha sido generado\n");
        certificado.append("automáticamente por el sistema VACU-TRACK\n");
        certificado.append("Quito, Ecuador - ").append(LocalDateTime.now().format(FORMATO_FECHA_CORTA)).append("\n");
        certificado.append("═══════════════════════════════════════════════════════\n");

        return certificado.toString();
    }

    /**
     * Genera nombre de archivo para el PDF
     * @return String con nombre de archivo
     */
    public String generarNombreArchivo() {
        StringBuilder nombre = new StringBuilder();
        nombre.append("Certificado_Vacunacion_");

        if (this.nino != null) {
            String nombreNino = this.nino.obtenerNombreCompleto()
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    .replaceAll("_{2,}", "_");
            nombre.append(nombreNino).append("_");
        }

        nombre.append(LocalDateTime.now().format(FORMATO_CODIGO_FECHA));
        nombre.append(EXTENSION_ARCHIVO);

        return nombre.toString();
    }

    /**
     * Obtiene información resumida del certificado
     * @return String con información resumida
     */
    public String obtenerInformacionResumida() {
        StringBuilder info = new StringBuilder();

        info.append("Certificado: ").append(this.codigoCertificado).append("\n");
        info.append("Paciente: ").append(obtenerNombreNino()).append("\n");
        info.append("Estado: ").append(obtenerEstado()).append(" (").append(this.porcentajeCompletitud).append("%)\n");
        info.append("Vacunas: ").append(this.registrosVacunas.size()).append(" aplicadas");

        long pendientes = this.notificacionesPendientes.stream()
                .filter(n -> n.debeSerMostrada() && !n.fueAplicada())
                .count();

        if (pendientes > 0) {
            info.append(", ").append(pendientes).append(" pendientes");
        }

        info.append("\nGenerado: ").append(this.fechaGeneracion.format(FORMATO_FECHA_CORTA));

        return info.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del certificado son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.ninoId != null &&
                this.codigoCertificado != null && !this.codigoCertificado.trim().isEmpty() &&
                this.fechaGeneracion != null &&
                this.porcentajeCompletitud != null &&
                this.porcentajeCompletitud.compareTo(BigDecimal.ZERO) >= 0 &&
                this.porcentajeCompletitud.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.ninoId == null) {
            mensajes.append("El niño es requerido. ");
        }

        if (this.codigoCertificado == null || this.codigoCertificado.trim().isEmpty()) {
            mensajes.append("El código del certificado es requerido. ");
        }

        if (this.fechaGeneracion == null) {
            mensajes.append("La fecha de generación es requerida. ");
        }

        if (this.porcentajeCompletitud == null) {
            mensajes.append("El porcentaje de completitud es requerido. ");
        } else if (this.porcentajeCompletitud.compareTo(BigDecimal.ZERO) < 0 ||
                this.porcentajeCompletitud.compareTo(BigDecimal.valueOf(100)) > 0) {
            mensajes.append("El porcentaje debe estar entre 0 y 100. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CertificadoVacunacion that = (CertificadoVacunacion) obj;
        return Objects.equals(id, that.id) && Objects.equals(codigoCertificado, that.codigoCertificado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigoCertificado);
    }

    @Override
    public String toString() {
        return "CertificadoVacunacion{" +
                "id=" + id +
                ", ninoId=" + ninoId +
                ", codigoCertificado='" + codigoCertificado + '\'' +
                ", fechaGeneracion=" + fechaGeneracion +
                ", porcentajeCompletitud=" + porcentajeCompletitud +
                ", vigente=" + vigente +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return this.codigoCertificado + " - " + obtenerNombreNino() +
                " (" + this.porcentajeCompletitud + "% completo)";
    }

    /**
     * Representación para listas
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        return obtenerNombreNino() + " - " + obtenerEstado() +
                " - " + this.fechaGeneracion.format(FORMATO_FECHA_CORTA);
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un certificado básico para un niño
     * @param nino niño para el que crear certificado
     * @return CertificadoVacunacion básico
     */
    public static CertificadoVacunacion crearBasico(Nino nino) {
        return new CertificadoVacunacion(nino);
    }

    /**
     * Crea un certificado completo con registros de vacunas
     * @param nino niño para el certificado
     * @param registros lista de vacunas aplicadas
     * @return CertificadoVacunacion completo
     */
    public static CertificadoVacunacion crearCompleto(Nino nino, List<RegistroVacuna> registros) {
        return new CertificadoVacunacion(nino, registros);
    }

    /**
     * Crea un certificado con registros y notificaciones
     * @param nino niño para el certificado
     * @param registros vacunas aplicadas
     * @param notificaciones notificaciones pendientes
     * @return CertificadoVacunacion con toda la información
     */
    public static CertificadoVacunacion crearConNotificaciones(Nino nino, List<RegistroVacuna> registros,
                                                               List<Notificacion> notificaciones) {
        CertificadoVacunacion certificado = new CertificadoVacunacion(nino, registros);
        certificado.setNotificacionesPendientes(notificaciones);
        return certificado;
    }

    /**
     * Crea un certificado de ejemplo para testing
     * @return CertificadoVacunacion de ejemplo
     */
    public static CertificadoVacunacion crearEjemplo() {
        CertificadoVacunacion certificado = new CertificadoVacunacion();
        certificado.setPorcentajeCompletitud(new BigDecimal("75.50"));
        certificado.setVigente(true);
        return certificado;
    }
}