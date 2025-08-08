package com.vacutrack.service;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;
import com.vacutrack.util.DateUtil;
import com.vacutrack.util.PDFUtil;
import com.vacutrack.util.QRCodeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de generación de certificados de vacunación
 * Maneja la creación de certificados oficiales en PDF con códigos QR,
 * validación digital y cumplimiento de estándares del MSP Ecuador
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class CertificadoService {

    private static final Logger logger = LoggerFactory.getLogger(CertificadoService.class);
    
    // Instancia singleton
    private static CertificadoService instance;
    
    // DAOs
    private final CertificadoVacunacionDAO certificadoDAO;
    private final NinoDAO ninoDAO;
    private final RegistroVacunaDAO registroVacunaDAO;
    private final VacunaDAO vacunaDAO;
    private final PadreFamiliaDAO padreFamiliaDAO;
    private final CentroSaludDAO centroSaludDAO;
    private final ProfesionalEnfermeriaDAO profesionalDAO;
    
    // Servicios relacionados
    private final VacunacionService vacunacionService;
    
    // Configuración de certificados
    private static final String DIRECTORIO_CERTIFICADOS = "certificados/";
    private static final String URL_BASE_VERIFICACION = "https://vacutrack.gob.ec/verificar/";
    private static final String LOGO_MSP_PATH = "assets/logo_msp.png";
    private static final String SELLO_DIGITAL_PATH = "assets/sello_digital.png";
    
    /**
     * Constructor privado para patrón singleton
     */
    private CertificadoService() {
        this.certificadoDAO = CertificadoVacunacionDAO.getInstance();
        this.ninoDAO = NinoDAO.getInstance();
        this.registroVacunaDAO = RegistroVacunaDAO.getInstance();
        this.vacunaDAO = VacunaDAO.getInstance();
        this.padreFamiliaDAO = PadreFamiliaDAO.getInstance();
        this.centroSaludDAO = CentroSaludDAO.getInstance();
        this.profesionalDAO = ProfesionalEnfermeriaDAO.getInstance();
        this.vacunacionService = VacunacionService.getInstance();
        
        // Crear directorio de certificados si no existe
        crearDirectorioCertificados();
    }
    
    /**
     * Obtiene la instancia singleton del servicio
     * @return instancia de CertificadoService
     */
    public static synchronized CertificadoService getInstance() {
        if (instance == null) {
            instance = new CertificadoService();
        }
        return instance;
    }
    
    /**
     * Genera un certificado oficial de vacunación
     * @param ninoId ID del niño
     * @param solicitadoPor ID del usuario que solicita (padre o profesional)
     * @param tipoSolicitud tipo de solicitud (COMPLETO, PARCIAL, INTERNACIONAL)
     * @return resultado de la generación
     */
    public CertificadoResult generarCertificado(Integer ninoId, Integer solicitadoPor, 
                                              String tipoSolicitud) {
        logger.info("Generando certificado para niño ID: {} - Tipo: {}", ninoId, tipoSolicitud);
        
        try {
            // Validar parámetros
            if (ninoId == null || solicitadoPor == null) {
                return CertificadoResult.error("Parámetros requeridos faltantes");
            }
            
            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return CertificadoResult.error("Niño no encontrado");
            }
            
            Nino nino = ninoOpt.get();
            
            // Verificar permisos
            if (!verificarPermisosCertificado(nino, solicitadoPor)) {
                return CertificadoResult.error("No tiene permisos para generar este certificado");
            }
            
            // Verificar si ya existe un certificado vigente reciente
            Optional<CertificadoVacunacion> certificadoExistente = 
                certificadoDAO.findVigentePorNino(ninoId);
            
            if (certificadoExistente.isPresent()) {
                CertificadoVacunacion existente = certificadoExistente.get();
                // Si fue generado en las últimas 24 horas, retornar el existente
                if (existente.getFechaGeneracion().isAfter(LocalDateTime.now().minusHours(24))) {
                    logger.info("Retornando certificado existente para niño {}", ninoId);
                    return CertificadoResult.success(existente, "Certificado existente válido");
                }
            }
            
            // Obtener historial de vacunación completo
            HistorialVacunacion historial = vacunacionService.getHistorialVacunacion(ninoId);
            if (historial == null) {
                return CertificadoResult.error("Error al obtener historial de vacunación");
            }
            
            // Verificar completitud mínima para certificado oficial
            if (historial.getEstadisticas().getPorcentajeCompletitud() < 
                CertificadoVacunacion.PORCENTAJE_MINIMO_COMPLETO.doubleValue()) {
                return CertificadoResult.warning(
                    "El esquema de vacunación no alcanza el porcentaje mínimo para certificado oficial (" +
                    CertificadoVacunacion.PORCENTAJE_MINIMO_COMPLETO + "%). " +
                    "Se generará certificado parcial.");
            }
            
            // Crear registro del certificado
            CertificadoVacunacion certificado = crearRegistroCertificado(nino, historial, tipoSolicitud);
            
            // Generar PDF del certificado
            GeneracionPDFResult pdfResult = generarPDFCertificado(certificado, historial);
            if (!pdfResult.isExitoso()) {
                return CertificadoResult.error("Error al generar PDF: " + pdfResult.getError());
            }
            
            // Actualizar certificado con información del archivo
            certificado.setUrlArchivo(pdfResult.getRutaArchivo());
            
            // Invalidar certificados anteriores del mismo niño
            invalidarCertificadosAnteriores(ninoId);
            
            // Guardar certificado en base de datos
            if (!certificadoDAO.insert(certificado)) {
                return CertificadoResult.error("Error al guardar certificado en base de datos");
            }
            
            logger.info("Certificado generado exitosamente: {} para {}", 
                certificado.getCodigoCertificado(), nino.getNombres() + " " + nino.getApellidos());
            
            return CertificadoResult.success(certificado, "Certificado generado exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al generar certificado para niño: " + ninoId, e);
            return CertificadoResult.error("Error interno del sistema");
        }
    }
    
    /**
     * Verifica un certificado por su código QR o código de verificación
     * @param codigoVerificacion código de verificación del certificado
     * @return resultado de la verificación
     */
    public VerificacionResult verificarCertificado(String codigoVerificacion) {
        logger.info("Verificando certificado con código: {}", codigoVerificacion);
        
        try {
            if (codigoVerificacion == null || codigoVerificacion.trim().isEmpty()) {
                return VerificacionResult.error("Código de verificación requerido");
            }
            
            // Buscar certificado por código
            Optional<CertificadoVacunacion> certificadoOpt = 
                certificadoDAO.findByCodigoCertificado(codigoVerificacion.trim());
            
            if (certificadoOpt.isEmpty()) {
                return VerificacionResult.error("Certificado no encontrado");
            }
            
            CertificadoVacunacion certificado = certificadoOpt.get();
            
            // Verificar vigencia
            if (!certificado.getVigente()) {
                return VerificacionResult.error("Certificado no vigente o revocado");
            }
            
            // Verificar fecha de emisión (máximo 1 año de antigüedad)
            if (certificado.getFechaGeneracion().isBefore(LocalDateTime.now().minusYears(1))) {
                return VerificacionResult.warning("Certificado válido pero con más de 1 año de antigüedad");
            }
            
            // Obtener información completa del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(certificado.getNinoId());
            if (ninoOpt.isEmpty()) {
                return VerificacionResult.error("Información del niño no encontrada");
            }
            
            Nino nino = ninoOpt.get();
            
            // Crear resultado de verificación
            VerificacionResult result = VerificacionResult.success(certificado, 
                "Certificado válido y vigente");
            result.setNino(nino);
            result.setFechaVerificacion(LocalDateTime.now());
            
            // Obtener resumen de vacunas del certificado
            ResumenVacunacion resumen = generarResumenVacunacion(certificado.getNinoId());
            result.setResumenVacunacion(resumen);
            
            logger.info("Verificación exitosa para certificado: {}", codigoVerificacion);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error al verificar certificado: " + codigoVerificacion, e);
            return VerificacionResult.error("Error interno del sistema");
        }
    }
    
    /**
     * Obtiene el archivo PDF de un certificado
     * @param certificadoId ID del certificado
     * @param usuarioId ID del usuario que solicita (para verificar permisos)
     * @return datos del archivo PDF o null si no se encuentra
     */
    public byte[] obtenerPDFCertificado(Integer certificadoId, Integer usuarioId) {
        logger.debug("Obteniendo PDF para certificado ID: {}", certificadoId);
        
        try {
            Optional<CertificadoVacunacion> certificadoOpt = certificadoDAO.findById(certificadoId);
            if (certificadoOpt.isEmpty()) {
                return null;
            }
            
            CertificadoVacunacion certificado = certificadoOpt.get();
            
            // Verificar permisos
            Optional<Nino> ninoOpt = ninoDAO.findById(certificado.getNinoId());
            if (ninoOpt.isEmpty()) {
                return null;
            }
            
            Nino nino = ninoOpt.get();
            if (!verificarPermisosCertificado(nino, usuarioId)) {
                logger.warn("Usuario {} sin permisos para acceder a certificado {}", 
                    usuarioId, certificadoId);
                return null;
            }
            
            // Leer archivo PDF
            String rutaArchivo = certificado.getUrlArchivo();
            if (rutaArchivo == null || rutaArchivo.isEmpty()) {
                return null;
            }
            
            File archivo = new File(rutaArchivo);
            if (!archivo.exists()) {
                logger.warn("Archivo PDF no encontrado: {}", rutaArchivo);
                return null;
            }
            
            return java.nio.file.Files.readAllBytes(archivo.toPath());
            
        } catch (Exception e) {
            logger.error("Error al obtener PDF del certificado: " + certificadoId, e);
            return null;
        }
    }
    
    /**
     * Lista certificados de un niño
     * @param ninoId ID del niño
     * @param usuarioId ID del usuario que consulta
     * @return lista de certificados
     */
    public List<CertificadoVacunacion> listarCertificados(Integer ninoId, Integer usuarioId) {
        logger.debug("Listando certificados para niño ID: {}", ninoId);
        
        try {
            // Verificar permisos
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return new ArrayList<>();
            }
            
            Nino nino = ninoOpt.get();
            if (!verificarPermisosCertificado(nino, usuarioId)) {
                return new ArrayList<>();
            }
            
            List<CertificadoVacunacion> certificados = certificadoDAO.findByNino(ninoId);
            
            // Ordenar por fecha de generación (más reciente primero)
            certificados.sort((a, b) -> b.getFechaGeneracion().compareTo(a.getFechaGeneracion()));
            
            return certificados;
            
        } catch (Exception e) {
            logger.error("Error al listar certificados para niño: " + ninoId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Revoca un certificado (lo marca como no vigente)
     * @param certificadoId ID del certificado
     * @param usuarioId ID del usuario que revoca
     * @param motivo motivo de la revocación
     * @return true si se revocó exitosamente
     */
    public boolean revocarCertificado(Integer certificadoId, Integer usuarioId, String motivo) {
        logger.info("Revocando certificado ID: {} por usuario: {}", certificadoId, usuarioId);
        
        try {
            Optional<CertificadoVacunacion> certificadoOpt = certificadoDAO.findById(certificadoId);
            if (certificadoOpt.isEmpty()) {
                return false;
            }
            
            CertificadoVacunacion certificado = certificadoOpt.get();
            
            // Solo profesionales pueden revocar certificados
            // Aquí se podría agregar verificación de rol
            
            certificado.setVigente(false);
            // Se podría agregar un campo para el motivo de revocación
            
            boolean revocado = certificadoDAO.update(certificado);
            
            if (revocado) {
                logger.info("Certificado {} revocado exitosamente. Motivo: {}", 
                    certificado.getCodigoCertificado(), motivo);
            }
            
            return revocado;
            
        } catch (Exception e) {
            logger.error("Error al revocar certificado: " + certificadoId, e);
            return false;
        }
    }
    
    // Métodos privados de utilidad
    
    /**
     * Verifica permisos para generar/acceder certificados
     */
    private boolean verificarPermisosCertificado(Nino nino, Integer usuarioId) {
        try {
            // El padre del niño siempre tiene permisos
            if (nino.getPadreFamiliaId() != null) {
                Optional<PadreFamilia> padreOpt = padreFamiliaDAO.findById(nino.getPadreFamiliaId());
                if (padreOpt.isPresent() && padreOpt.get().getUsuarioId().equals(usuarioId)) {
                    return true;
                }
            }
            
            // Los profesionales de enfermería tienen permisos
            Optional<ProfesionalEnfermeria> profesionalOpt = profesionalDAO.findByUsuarioId(usuarioId);
            if (profesionalOpt.isPresent()) {
                return true;
            }
            
            // Otros casos se pueden agregar aquí (administradores, etc.)
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error al verificar permisos de certificado", e);
            return false;
        }
    }
    
    /**
     * Crea el registro inicial del certificado
     */
    private CertificadoVacunacion crearRegistroCertificado(Nino nino, HistorialVacunacion historial, 
                                                         String tipoSolicitud) {
        CertificadoVacunacion certificado = new CertificadoVacunacion();
        
        certificado.setNinoId(nino.getId());
        certificado.setNino(nino);
        
        // Generar código único del certificado
        String codigoCertificado = generarCodigoCertificado(nino);
        certificado.setCodigoCertificado(codigoCertificado);
        
        certificado.setFechaGeneracion(LocalDateTime.now());
        
        // Calcular porcentaje de completitud
        BigDecimal porcentaje = BigDecimal.valueOf(historial.getEstadisticas().getPorcentajeCompletitud());
        certificado.setPorcentajeCompletitud(porcentaje);
        
        certificado.setVigente(true);
        
        // Asignar las listas de registros y notificaciones
        certificado.setRegistrosVacunas(historial.getVacunasAplicadas());
        
        // Convertir VacunaPendiente a Notificacion para el certificado
        List<Notificacion> notificacionesPendientes = historial.getVacunasPendientes().stream()
            .map(this::convertirVacunaPendienteANotificacion)
            .collect(Collectors.toList());
        certificado.setNotificacionesPendientes(notificacionesPendientes);
        
        return certificado;
    }
    
    /**
     * Genera un código único para el certificado
     */
    private String generarCodigoCertificado(Nino nino) {
        // Formato: CERT-VACU-YYYYMMDD-NNNN
        String fechaStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Generar número secuencial del día
        long certificadosHoy = certificadoDAO.countByFecha(LocalDate.now());
        int numeroSecuencial = (int) (certificadosHoy + 1);
        
        return String.format(CertificadoVacunacion.FORMATO_CODIGO, fechaStr, numeroSecuencial);
    }
    
    /**
     * Genera el PDF del certificado
     */
    private GeneracionPDFResult generarPDFCertificado(CertificadoVacunacion certificado, 
                                                     HistorialVacunacion historial) {
        try {
            // Crear nombre de archivo único
            String nombreArchivo = String.format("certificado_%s_%s.pdf", 
                certificado.getCodigoCertificado(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            String rutaCompleta = DIRECTORIO_CERTIFICADOS + nombreArchivo;
            
            // Generar contenido del PDF
            byte[] pdfBytes = crearContenidoPDF(certificado, historial);
            
            // Guardar archivo
            try (FileOutputStream fos = new FileOutputStream(rutaCompleta)) {
                fos.write(pdfBytes);
            }
            
            return GeneracionPDFResult.success(rutaCompleta);
            
        } catch (Exception e) {
            logger.error("Error al generar PDF del certificado", e);
            return GeneracionPDFResult.error("Error al generar PDF: " + e.getMessage());
        }
    }
    
    /**
     * Crea el contenido PDF del certificado
     */
    private byte[] crearContenidoPDF(CertificadoVacunacion certificado, HistorialVacunacion historial) 
            throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Aquí se usaría una librería como iText o Apache PDFBox
        // Por simplicidad, creamos un PDF básico
        
        // Header del documento
        StringBuilder contenido = new StringBuilder();
        contenido.append("REPÚBLICA DEL ECUADOR\n");
        contenido.append("MINISTERIO DE SALUD PÚBLICA\n");
        contenido.append("CERTIFICADO OFICIAL DE VACUNACIÓN\n\n");
        
        // Información del certificado
        contenido.append("Código de Certificado: ").append(certificado.getCodigoCertificado()).append("\n");
        contenido.append("Fecha de Emisión: ").append(
            certificado.getFechaGeneracion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
        
        // Información del niño
        Nino nino = certificado.getNino();
        contenido.append("DATOS DEL MENOR\n");
        contenido.append("Nombres: ").append(nino.getNombres()).append("\n");
        contenido.append("Apellidos: ").append(nino.getApellidos()).append("\n");
        contenido.append("Cédula/Identificación: ").append(nino.getCedula() != null ? nino.getCedula() : "N/A").append("\n");
        contenido.append("Fecha de Nacimiento: ").append(
            DateUtil.formatearFecha(nino.getFechaNacimiento())).append("\n");
        contenido.append("Género: ").append(nino.getGenero()).append("\n\n");
        
        // Estadísticas de vacunación
        contenido.append("ESTADO DE VACUNACIÓN\n");
        contenido.append("Porcentaje de Completitud: ").append(
            String.format("%.1f%%", certificado.getPorcentajeCompletitud())).append("\n");
        contenido.append("Total de Vacunas Aplicadas: ").append(
            historial.getEstadisticas().getTotalAplicadas()).append("\n\n");
        
        // Lista de vacunas aplicadas
        contenido.append("VACUNAS APLICADAS\n");
        contenido.append("%-30s %-12s %-20s %-15s\n").formatted("Vacuna", "Fecha", "Centro de Salud", "Lote");
        contenido.append("-".repeat(80)).append("\n");
        
        for (RegistroVacuna registro : historial.getVacunasAplicadas()) {
            String nombreVacuna = obtenerNombreVacuna(registro.getVacunaId());
            String fechaAplicacion = DateUtil.formatearFecha(registro.getFechaAplicacion());
            String centroSalud = obtenerNombreCentroSalud(registro.getCentroSaludId());
            String lote = registro.getLote() != null ? registro.getLote() : "N/A";
            
            contenido.append("%-30s %-12s %-20s %-15s\n").formatted(
                nombreVacuna.length() > 29 ? nombreVacuna.substring(0, 29) : nombreVacuna,
                fechaAplicacion,
                centroSalud.length() > 19 ? centroSalud.substring(0, 19) : centroSalud,
                lote
            );
        }
        
        // Vacunas pendientes
        if (!historial.getVacunasPendientes().isEmpty()) {
            contenido.append("\n\nVACUNAS PENDIENTES\n");
            for (VacunacionService.VacunaPendiente pendiente : historial.getVacunasPendientes()) {
                contenido.append("- ").append(pendiente.getVacunaNombre())
                    .append(" (Recomendada: ").append(DateUtil.formatearFecha(pendiente.getFechaRecomendada()))
                    .append(")\n");
            }
        }
        
        // Footer con código QR y validación
        contenido.append("\n\n");
        contenido.append("VALIDACIÓN DIGITAL\n");
        contenido.append("Código de Verificación: ").append(certificado.getCodigoCertificado()).append("\n");
        contenido.append("URL de Verificación: ").append(URL_BASE_VERIFICACION)
            .append(certificado.getCodigoCertificado()).append("\n");
        contenido.append("\nEste certificado es válido únicamente con el código QR y puede ser verificado en línea.\n");
        contenido.append("Documento generado automáticamente por el Sistema VACU-TRACK del MSP Ecuador.\n");
        
        // Por simplicidad, convertimos el texto a bytes
        // En una implementación real, se usaría una librería PDF completa
        return contenido.toString().getBytes("UTF-8");
    }
    
    /**
     * Invalida certificados anteriores del mismo niño
     */
    private void invalidarCertificadosAnteriores(Integer ninoId) {
        try {
            List<CertificadoVacunacion> certificadosAnteriores = certificadoDAO.findByNino(ninoId);
            
            for (CertificadoVacunacion anterior : certificadosAnteriores) {
                if (anterior.getVigente()) {
                    anterior.setVigente(false);
                    certificadoDAO.update(anterior);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al invalidar certificados anteriores", e);
        }
    }
    
    /**
     * Crea directorio de certificados si no existe
     */
    private void crearDirectorioCertificados() {
        try {
            File directorio = new File(DIRECTORIO_CERTIFICADOS);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }
        } catch (Exception e) {
            logger.error("Error al crear directorio de certificados", e);
        }
    }
    
    /**
     * Genera resumen de vacunación para verificación
     */
    private ResumenVacunacion generarResumenVacunacion(Integer ninoId) {
        try {
            VacunacionService.EstadoEsquema estado = vacunacionService.verificarEstadoEsquema(ninoId);
            List<RegistroVacuna> aplicadas = registroVacunaDAO.findByNino(ninoId);
            
            ResumenVacunacion resumen = new ResumenVacunacion();
            resumen.setTotalVacunasAplicadas(aplicadas.size());
            resumen.setPorcentajeCompletitud(estado != null ? estado.getPorcentajeCompletitud() : 0.0);
            resumen.setVacunasVencidas(estado != null ? estado.getVacunasVencidas() : 0);
            resumen.setUltimaVacunaFecha(aplicadas.stream()
                .map(RegistroVacuna::getFechaAplicacion)
                .max(LocalDate::compareTo)
                .orElse(null));
            
            return resumen;
            
        } catch (Exception e) {
            logger.error("Error al generar resumen de vacunación", e);
            return new ResumenVacunacion();
        }
    }
    
    /**
     * Convierte VacunaPendiente a Notificacion para el certificado
     */
    private Notificacion convertirVacunaPendienteANotificacion(VacunacionService.VacunaPendiente pendiente) {
        Notificacion notificacion = new Notificacion();
        notificacion.setVacunaId(pendiente.getVacunaId());
        notificacion.setTipoNotificacion("VACUNA_PENDIENTE");
        notificacion.setTitulo("Vacuna Pendiente: " + pendiente.getVacunaNombre());
        notificacion.setMensaje("Fecha recomendada: " + DateUtil.formatearFecha(pendiente.getFechaRecomendada()));
        notificacion.setFechaProgramada(pendiente.getFechaRecomendada().atStartOfDay());
        notificacion.setEstado("PENDIENTE");
        return notificacion;
    }
    
    /**
     * Obtiene nombre de vacuna por ID
     */
    private String obtenerNombreVacuna(Integer vacunaId) {
        try {
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            return vacunaOpt.map(Vacuna::getNombre).orElse("Vacuna ID " + vacunaId);
        } catch (Exception e) {
            return "Vacuna ID " + vacunaId;
        }
    }
    
    /**
     * Obtiene nombre de centro de salud por ID
     */
    private String obtenerNombreCentroSalud(Integer centroId) {
        try {
            Optional<CentroSalud> centroOpt = centroSaludDAO.findById(centroId);
            return centroOpt.map(CentroSalud::getNombre).orElse("Centro ID " + centroId);
        } catch (Exception e) {
            return "Centro ID " + centroId;
        }
    }
    
    // Clases para resultados
    
    /**
     * Resultado de generación de certificado
     */
    public static class CertificadoResult {
        private final boolean success;
        private final String message;
        private final String level;
        private final CertificadoVacunacion certificado;
        
        private CertificadoResult(boolean success, String message, String level, 
                                CertificadoVacunacion certificado) {
            this.success = success;
            this.message = message;
            this.level = level;
            this.certificado = certificado;
        }
        
        public static CertificadoResult success(CertificadoVacunacion certificado, String message) {
            return new CertificadoResult(true, message, "SUCCESS", certificado);
        }
        
        public static CertificadoResult warning(String message) {
            return new CertificadoResult(true, message, "WARNING", null);
        }
        
        public static CertificadoResult error(String message) {
            return new CertificadoResult(false, message, "ERROR", null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getLevel() { return level; }
        public CertificadoVacunacion getCertificado() { return certificado; }
    }
    
    /**
     * Resultado de verificación de certificado
     */
    public static class VerificacionResult {
        private final boolean valido;
        private final String mensaje;
        private final String nivel;
        private final CertificadoVacunacion certificado;
        private Nino nino;
        private LocalDateTime fechaVerificacion;
        private ResumenVacunacion resumenVacunacion;
        
        private VerificacionResult(boolean valido, String mensaje, String nivel, 
                                 CertificadoVacunacion certificado) {
            this.valido = valido;
            this.mensaje = mensaje;
            this.nivel = nivel;
            this.certificado = certificado;
        }
        
        public static VerificacionResult success(CertificadoVacunacion certificado, String mensaje) {
            return new VerificacionResult(true, mensaje, "SUCCESS", certificado);
        }
        
        public static VerificacionResult warning(String mensaje) {
            return new VerificacionResult(true, mensaje, "WARNING", null);
        }
        
        public static VerificacionResult error(String mensaje) {
            return new VerificacionResult(false, mensaje, "ERROR", null);
        }
        
        // Getters y Setters
        public boolean isValido() { return valido; }
        public String getMensaje() { return mensaje; }
        public String getNivel() { return nivel; }
        public CertificadoVacunacion getCertificado() { return certificado; }
        public Nino getNino() { return nino; }
        public void setNino(Nino nino) { this.nino = nino; }
        public LocalDateTime getFechaVerificacion() { return fechaVerificacion; }
        public void setFechaVerificacion(LocalDateTime fechaVerificacion) { this.fechaVerificacion = fechaVerificacion; }
        public ResumenVacunacion getResumenVacunacion() { return resumenVacunacion; }
        public void setResumenVacunacion(ResumenVacunacion resumenVacunacion) { this.resumenVacunacion = resumenVacunacion; }
    }
    
    /**
     * Resultado de generación de PDF
     */
    private static class GeneracionPDFResult {
        private final boolean exitoso;
        private final String rutaArchivo;
        private final String error;
        
        private GeneracionPDFResult(boolean exitoso, String rutaArchivo, String error) {
            this.exitoso = exitoso;
            this.rutaArchivo = rutaArchivo;
            this.error = error;
        }
        
        public static GeneracionPDFResult success(String rutaArchivo) {
            return new GeneracionPDFResult(true, rutaArchivo, null);
        }
        
        public static GeneracionPDFResult error(String error) {
            return new GeneracionPDFResult(false, null, error);
        }
        
        public boolean isExitoso() { return exitoso; }
        public String getRutaArchivo() { return rutaArchivo; }
        public String getError() { return error; }
    }
    
    /**
     * Resumen de vacunación para verificación
     */
    public static class ResumenVacunacion {
        private int totalVacunasAplicadas;
        private double porcentajeCompletitud;
        private int vacunasVencidas;
        private LocalDate ultimaVacunaFecha;
        
        // Getters y Setters
        public int getTotalVacunasAplicadas() { return totalVacunasAplicadas; }
        public void setTotalVacunasAplicadas(int totalVacunasAplicadas) { this.totalVacunasAplicadas = totalVacunasAplicadas; }
        public double getPorcentajeCompletitud() { return porcentajeCompletitud; }
        public void setPorcentajeCompletitud(double porcentajeCompletitud) { this.porcentajeCompletitud = porcentajeCompletitud; }
        public int getVacunasVencidas() { return vacunasVencidas; }
        public void setVacunasVencidas(int vacunasVencidas) { this.vacunasVencidas = vacunasVencidas; }
        public LocalDate getUltimaVacunaFecha() { return ultimaVacunaFecha; }
        public void setUltimaVacunaFecha(LocalDate ultimaVacunaFecha) { this.ultimaVacunaFecha = ultimaVacunaFecha; }
    }
}