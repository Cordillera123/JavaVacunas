package com.vacutrack.service;

import com.vacutrack.dao.*;
import com.vacutrack.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio SIMPLIFICADO de generación de certificados de vacunación
 * Versión para estudiantes - sin complejidades innecesarias
 * Genera carnets digitales simples en formato texto
 *
 * @author VACU-TRACK Team
 * @version 2.0 - Simplificado para estudiantes
 */
public class CertificadoService {

    private static final Logger logger = LoggerFactory.getLogger(CertificadoService.class);

    // Instancia singleton
    private static CertificadoService instance;

    // DAOs - SOLO los que realmente existen
    private final CertificadoVacunacionDAO certificadoDAO;
    private final NinoDAO ninoDAO;
    private final RegistroVacunaDAO registroVacunaDAO;
    private final VacunaDAO vacunaDAO;
    private final PadreFamiliaDAO padreFamiliaDAO;
    private final CentroSaludDAO centroSaludDAO;
    private final ProfesionalEnfermeriaDAO profesionalDAO;
    private final NotificacionDAO notificacionDAO;

    // Configuración SIMPLIFICADA
    private static final String DIRECTORIO_CERTIFICADOS = "certificados/";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        this.notificacionDAO = NotificacionDAO.getInstance();

        // Crear directorio si no existe
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
     * SIMPLIFICADO: Genera un certificado básico de vacunación
     * @param ninoId ID del niño
     * @param usuarioSolicitante ID del usuario que solicita
     * @return resultado de la generación
     */
    public CertificadoResult generarCertificado(Integer ninoId, Integer usuarioSolicitante) {
        logger.info("Generando certificado para niño ID: {}", ninoId);

        try {
            // Validar parámetros básicos
            if (ninoId == null || usuarioSolicitante == null) {
                return CertificadoResult.error("Parámetros requeridos faltantes");
            }

            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return CertificadoResult.error("Niño no encontrado");
            }

            Nino nino = ninoOpt.get();

            // Verificar permisos básicos
            if (!verificarPermisosCertificado(nino, usuarioSolicitante)) {
                return CertificadoResult.error("No tiene permisos para generar este certificado");
            }

            // Obtener registros de vacunación del niño
            List<RegistroVacuna> registrosVacunas = registroVacunaDAO.findByNino(ninoId);

            // Obtener notificaciones pendientes
            List<Notificacion> notificacionesPendientes = notificacionDAO.getNotificacionesActivas(ninoId);

            // Calcular porcentaje de completitud (simplificado)
            BigDecimal porcentajeCompletitud = calcularPorcentajeCompletitud(registrosVacunas, nino);

            // Verificar si ya existe un certificado vigente reciente (últimas 24 horas)
            Optional<CertificadoVacunacion> certificadoExistente =
                    certificadoDAO.getUltimoCertificado(ninoId);

            if (certificadoExistente.isPresent()) {
                CertificadoVacunacion existente = certificadoExistente.get();
                if (existente.getVigente() &&
                        existente.getFechaGeneracion().isAfter(LocalDateTime.now().minusHours(24))) {
                    logger.info("Retornando certificado existente para niño {}", ninoId);
                    return CertificadoResult.success(existente, "Certificado existente válido");
                }
            }

            // Crear nuevo certificado
            CertificadoVacunacion certificado = certificadoDAO.crearCertificado(ninoId, porcentajeCompletitud);
            if (certificado == null) {
                return CertificadoResult.error("Error al crear registro del certificado");
            }

            // Cargar información completa
            certificado.setNino(nino);
            certificado.setRegistrosVacunas(registrosVacunas);
            certificado.setNotificacionesPendientes(notificacionesPendientes);

            // Generar archivo de texto simple
            String rutaArchivo = generarArchivoTexto(certificado);
            if (rutaArchivo != null) {
                certificadoDAO.updateUrlArchivo(certificado.getId(), rutaArchivo);
                certificado.setUrlArchivo(rutaArchivo);
            }

            // Invalidar certificados anteriores
            invalidarCertificadosAnteriores(ninoId, certificado.getId());

            logger.info("Certificado generado exitosamente: {} para {}",
                    certificado.getCodigoCertificado(), nino.obtenerNombreCompleto());

            return CertificadoResult.success(certificado, "Certificado generado exitosamente");

        } catch (Exception e) {
            logger.error("Error al generar certificado para niño: " + ninoId, e);
            return CertificadoResult.error("Error interno del sistema");
        }
    }

    /**
     * SIMPLIFICADO: Verifica un certificado por código
     * @param codigoVerificacion código del certificado
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
                    certificadoDAO.findByCodigo(codigoVerificacion.trim());

            if (certificadoOpt.isEmpty()) {
                return VerificacionResult.error("Certificado no encontrado");
            }

            CertificadoVacunacion certificado = certificadoOpt.get();

            // Verificar vigencia
            if (!certificado.getVigente()) {
                return VerificacionResult.error("Certificado no vigente");
            }

            // Verificar antigüedad (máximo 1 año)
            if (certificado.getFechaGeneracion().isBefore(LocalDateTime.now().minusYears(1))) {
                return VerificacionResult.warning("Certificado válido pero antiguo (más de 1 año)");
            }

            // Obtener información del niño
            Optional<Nino> ninoOpt = ninoDAO.findById(certificado.getNinoId());
            if (ninoOpt.isEmpty()) {
                return VerificacionResult.error("Información del niño no encontrada");
            }

            Nino nino = ninoOpt.get();
            certificado.setNino(nino);

            // Crear resultado exitoso
            VerificacionResult result = VerificacionResult.success(certificado,
                    "Certificado válido y vigente");
            result.setFechaVerificacion(LocalDateTime.now());

            logger.info("Verificación exitosa para certificado: {}", codigoVerificacion);
            return result;

        } catch (Exception e) {
            logger.error("Error al verificar certificado: " + codigoVerificacion, e);
            return VerificacionResult.error("Error interno del sistema");
        }
    }

    /**
     * SIMPLIFICADO: Lista certificados de un niño
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

            // Cargar información básica del niño en cada certificado
            for (CertificadoVacunacion cert : certificados) {
                cert.setNino(nino);
            }

            return certificados;

        } catch (Exception e) {
            logger.error("Error al listar certificados para niño: " + ninoId, e);
            return new ArrayList<>();
        }
    }

    /**
     * SIMPLIFICADO: Obtiene el contenido del certificado como texto
     * @param certificadoId ID del certificado
     * @param usuarioId ID del usuario que solicita
     * @return contenido del certificado o null si no tiene acceso
     */
    public String obtenerContenidoCertificado(Integer certificadoId, Integer usuarioId) {
        logger.debug("Obteniendo contenido para certificado ID: {}", certificadoId);

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

            // Cargar información completa
            certificado.setNino(nino);
            List<RegistroVacuna> registros = registroVacunaDAO.findByNino(nino.getId());
            certificado.setRegistrosVacunas(registros);

            List<Notificacion> notificaciones = notificacionDAO.getNotificacionesActivas(nino.getId());
            certificado.setNotificacionesPendientes(notificaciones);

            // Generar contenido completo
            return generarContenidoCompleto(certificado);

        } catch (Exception e) {
            logger.error("Error al obtener contenido del certificado: " + certificadoId, e);
            return null;
        }
    }

    /**
     * SIMPLIFICADO: Revoca un certificado
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

            // Solo profesionales o administradores pueden revocar
            // (Se podría agregar verificación de rol aquí)

            boolean revocado = certificadoDAO.invalidarCertificado(certificadoId);

            if (revocado) {
                logger.info("Certificado {} revocado exitosamente. Motivo: {}",
                        certificadoOpt.get().getCodigoCertificado(), motivo);
            }

            return revocado;

        } catch (Exception e) {
            logger.error("Error al revocar certificado: " + certificadoId, e);
            return false;
        }
    }

    // Métodos privados de utilidad - SIMPLIFICADOS

    /**
     * Verifica permisos básicos para certificados
     */
    private boolean verificarPermisosCertificado(Nino nino, Integer usuarioId) {
        try {
            // El padre del niño siempre tiene permisos
            if (nino.getPadreId() != null) {
                Optional<PadreFamilia> padreOpt = padreFamiliaDAO.findById(nino.getPadreId());
                if (padreOpt.isPresent() &&
                        Objects.equals(padreOpt.get().getUsuarioId(), usuarioId)) {
                    return true;
                }
            }

            // Los profesionales de enfermería tienen permisos
            Optional<ProfesionalEnfermeria> profesionalOpt = profesionalDAO.findByUsuarioId(usuarioId);
            if (profesionalOpt.isPresent()) {
                return true;
            }

            // Aquí se podrían agregar más verificaciones (administradores, etc.)

            return false;

        } catch (Exception e) {
            logger.error("Error al verificar permisos de certificado", e);
            return false;
        }
    }

    /**
     * SIMPLIFICADO: Calcula porcentaje de completitud básico
     */
    private BigDecimal calcularPorcentajeCompletitud(List<RegistroVacuna> registros, Nino nino) {
        if (registros == null || registros.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Lógica simplificada: contar dosis únicas aplicadas
        Set<String> vacunasUnicas = new HashSet<>();
        for (RegistroVacuna registro : registros) {
            if (registro.getVacunaId() != null) {
                String key = registro.getVacunaId() + "-" +
                        (registro.getNumeroDosis() != null ? registro.getNumeroDosis() : 1);
                vacunasUnicas.add(key);
            }
        }

        // Esquema básico ecuatoriano: aproximadamente 20 dosis hasta los 18 meses
        int totalEsperado = calcularVacunasEsperadasPorEdad(nino);

        if (totalEsperado == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal porcentaje = BigDecimal.valueOf(vacunasUnicas.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalEsperado), 2, BigDecimal.ROUND_HALF_UP);

        // No puede ser mayor a 100%
        return porcentaje.compareTo(BigDecimal.valueOf(100)) > 0 ?
                BigDecimal.valueOf(100) : porcentaje;
    }

    /**
     * Calcula vacunas esperadas según la edad (simplificado)
     */
    private int calcularVacunasEsperadasPorEdad(Nino nino) {
        if (nino.getFechaNacimiento() == null) {
            return 20; // Valor por defecto
        }

        long edadEnDias = nino.calcularEdadEnDias();

        // Cálculo simplificado basado en el esquema ecuatoriano
        if (edadEnDias < 60) {
            return 2; // BCG, HB(0)
        } else if (edadEnDias < 120) {
            return 7; // + vacunas de 2 meses
        } else if (edadEnDias < 180) {
            return 12; // + vacunas de 4 meses
        } else if (edadEnDias < 365) {
            return 17; // + vacunas de 6 meses
        } else if (edadEnDias < 540) {
            return 19; // + vacunas de 12 meses
        } else {
            return 21; // Esquema completo hasta 18 meses
        }
    }

    /**
     * SIMPLIFICADO: Genera archivo de texto plano del certificado
     */
    private String generarArchivoTexto(CertificadoVacunacion certificado) {
        try {
            String nombreArchivo = String.format("certificado_%s_%s.txt",
                    certificado.getCodigoCertificado(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            String rutaCompleta = DIRECTORIO_CERTIFICADOS + nombreArchivo;

            String contenido = generarContenidoCompleto(certificado);

            try (FileWriter writer = new FileWriter(rutaCompleta)) {
                writer.write(contenido);
            }

            return rutaCompleta;

        } catch (IOException e) {
            logger.error("Error al generar archivo de certificado", e);
            return null;
        }
    }

    /**
     * SIMPLIFICADO: Genera el contenido completo del certificado
     */
    private String generarContenidoCompleto(CertificadoVacunacion certificado) {
        StringBuilder contenido = new StringBuilder();

        // Encabezado
        contenido.append("╔══════════════════════════════════════════════════════════╗\n");
        contenido.append("║              CERTIFICADO DE VACUNACIÓN                   ║\n");
        contenido.append("║                    VACU-TRACK                           ║\n");
        contenido.append("║              Sistema de Seguimiento                     ║\n");
        contenido.append("╚══════════════════════════════════════════════════════════╝\n\n");

        // Información del certificado
        contenido.append("INFORMACIÓN DEL CERTIFICADO\n");
        contenido.append("═══════════════════════════\n");
        contenido.append("Código: ").append(certificado.getCodigoCertificado()).append("\n");
        contenido.append("Fecha de emisión: ").append(
                certificado.getFechaGeneracion().format(FORMATO_FECHA_HORA)).append("\n");
        contenido.append("Estado: ").append(certificado.getVigente() ? "VIGENTE" : "NO VIGENTE").append("\n");
        contenido.append("Completitud: ").append(certificado.getPorcentajeCompletitud()).append("%\n\n");

        // Información del niño
        if (certificado.getNino() != null) {
            Nino nino = certificado.getNino();
            contenido.append("INFORMACIÓN DEL PACIENTE\n");
            contenido.append("════════════════════════\n");
            contenido.append("Nombre completo: ").append(nino.obtenerNombreCompleto()).append("\n");

            if (nino.getCedula() != null) {
                contenido.append("Cédula: ").append(nino.getCedula()).append("\n");
            }

            contenido.append("Fecha nacimiento: ").append(
                    nino.getFechaNacimiento().format(FORMATO_FECHA)).append("\n");
            contenido.append("Edad actual: ").append(nino.obtenerEdadFormateada()).append("\n");
            contenido.append("Sexo: ").append(nino.obtenerSexoLegible()).append("\n");

            if (nino.getLugarNacimiento() != null) {
                contenido.append("Lugar nacimiento: ").append(nino.getLugarNacimiento()).append("\n");
            }

            // Información del padre/representante
            if (nino.getPadre() != null) {
                PadreFamilia padre = nino.getPadre();
                contenido.append("Representante: ").append(padre.obtenerNombreCompleto());
                if (padre.obtenerCedula() != null) {
                    contenido.append(" (CI: ").append(padre.obtenerCedula()).append(")");
                }
                contenido.append("\n");

                if (padre.getTelefono() != null) {
                    contenido.append("Teléfono: ").append(padre.getTelefono()).append("\n");
                }
            }

            contenido.append("\n");
        }

        // Vacunas aplicadas
        contenido.append("VACUNAS APLICADAS\n");
        contenido.append("═════════════════\n");

        if (certificado.getRegistrosVacunas() != null && !certificado.getRegistrosVacunas().isEmpty()) {
            contenido.append(String.format("%-25s %-8s %-12s %-20s %-10s%n",
                    "VACUNA", "DOSIS", "FECHA", "CENTRO", "LOTE"));
            contenido.append("─".repeat(80)).append("\n");

            for (RegistroVacuna registro : certificado.getRegistrosVacunas()) {
                String nombreVacuna = obtenerNombreVacuna(registro.getVacunaId());
                String fecha = registro.getFechaAplicacion() != null ?
                        registro.getFechaAplicacion().format(FORMATO_FECHA) : "N/A";
                String centro = obtenerNombreCentroSalud(registro.getCentroSaludId());
                String lote = registro.getLoteVacuna() != null ? registro.getLoteVacuna() : "N/A";

                contenido.append(String.format("%-25s %-8s %-12s %-20s %-10s%n",
                        truncar(nombreVacuna, 24),
                        registro.getNumeroDosis() != null ? registro.getNumeroDosis() : "1",
                        fecha,
                        truncar(centro, 19),
                        truncar(lote, 9)
                ));

                // Agregar nota de reacción adversa si la hubo
                if (registro.tuvoReaccionAdversa() && registro.getDescripcionReaccion() != null) {
                    contenido.append("    ⚠️ Reacción: ").append(registro.getDescripcionReaccion()).append("\n");
                }
            }
        } else {
            contenido.append("No hay vacunas registradas.\n");
        }

        contenido.append("\n");

        // Vacunas pendientes
        contenido.append("VACUNAS PENDIENTES\n");
        contenido.append("══════════════════\n");

        if (certificado.getNotificacionesPendientes() != null &&
                !certificado.getNotificacionesPendientes().isEmpty()) {

            boolean hayPendientes = false;
            for (Notificacion notificacion : certificado.getNotificacionesPendientes()) {
                if (notificacion.debeSerMostrada() && !notificacion.fueAplicada()) {
                    if (!hayPendientes) {
                        contenido.append(String.format("%-25s %-8s %-12s %-15s%n",
                                "VACUNA", "DOSIS", "FECHA PROG.", "ESTADO"));
                        contenido.append("─".repeat(65)).append("\n");
                        hayPendientes = true;
                    }

                    String nombreVacuna = obtenerNombreVacuna(notificacion.getVacunaId());
                    String fechaProg = notificacion.getFechaProgramada() != null ?
                            notificacion.getFechaProgramada().format(FORMATO_FECHA) : "N/A";
                    String estado = notificacion.getTipoNotificacion();

                    contenido.append(String.format("%-25s %-8s %-12s %-15s%n",
                            truncar(nombreVacuna, 24),
                            notificacion.getNumeroDosis() != null ? notificacion.getNumeroDosis() : "1",
                            fechaProg,
                            truncar(estado, 14)
                    ));
                }
            }

            if (!hayPendientes) {
                contenido.append("¡Felicitaciones! No hay vacunas pendientes.\n");
            }
        } else {
            contenido.append("No hay información de vacunas pendientes.\n");
        }

        contenido.append("\n");

        // Pie del certificado
        contenido.append("VALIDACIÓN\n");
        contenido.append("══════════\n");
        contenido.append("Código de verificación: ").append(certificado.getCodigoCertificado()).append("\n");
        contenido.append("Este certificado es válido y ha sido generado automáticamente\n");
        contenido.append("por el sistema VACU-TRACK.\n");
        contenido.append("\n");
        contenido.append("Fecha de generación: ").append(LocalDateTime.now().format(FORMATO_FECHA_HORA)).append("\n");
        contenido.append("Quito, Ecuador\n");
        contenido.append("\n");
        contenido.append("╔══════════════════════════════════════════════════════════╗\n");
        contenido.append("║  Este documento es válido únicamente con el código       ║\n");
        contenido.append("║  de verificación y puede ser validado en línea.         ║\n");
        contenido.append("╚══════════════════════════════════════════════════════════╝\n");

        return contenido.toString();
    }

    /**
     * Obtiene nombre de vacuna por ID
     */
    private String obtenerNombreVacuna(Integer vacunaId) {
        if (vacunaId == null) return "Vacuna desconocida";

        try {
            Optional<Vacuna> vacunaOpt = vacunaDAO.findById(vacunaId);
            return vacunaOpt.map(Vacuna::obtenerNombreDisplay).orElse("Vacuna ID " + vacunaId);
        } catch (Exception e) {
            return "Vacuna ID " + vacunaId;
        }
    }

    /**
     * Obtiene nombre de centro de salud por ID
     */
    private String obtenerNombreCentroSalud(Integer centroId) {
        if (centroId == null) return "Centro desconocido";

        try {
            Optional<CentroSalud> centroOpt = centroSaludDAO.findById(centroId);
            return centroOpt.map(CentroSalud::getNombre).orElse("Centro ID " + centroId);
        } catch (Exception e) {
            return "Centro ID " + centroId;
        }
    }

    /**
     * Trunca texto para que quepa en columnas
     */
    private String truncar(String texto, int maxLength) {
        if (texto == null) return "";
        return texto.length() > maxLength ? texto.substring(0, maxLength) : texto;
    }

    /**
     * Invalida certificados anteriores del mismo niño
     */
    private void invalidarCertificadosAnteriores(Integer ninoId, Integer certificadoActualId) {
        try {
            List<CertificadoVacunacion> certificadosAnteriores = certificadoDAO.findByNino(ninoId);

            for (CertificadoVacunacion anterior : certificadosAnteriores) {
                if (!Objects.equals(anterior.getId(), certificadoActualId) && anterior.getVigente()) {
                    certificadoDAO.invalidarCertificado(anterior.getId());
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
                logger.info("Directorio de certificados creado: {}", DIRECTORIO_CERTIFICADOS);
            }
        } catch (Exception e) {
            logger.error("Error al crear directorio de certificados", e);
        }
    }

    // Clases para resultados - SIMPLIFICADAS

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

        public boolean isWarning() { return "WARNING".equals(level); }
        public boolean isError() { return "ERROR".equals(level); }
    }

    /**
     * Resultado de verificación de certificado
     */
    public static class VerificacionResult {
        private final boolean valido;
        private final String mensaje;
        private final String nivel;
        private final CertificadoVacunacion certificado;
        private LocalDateTime fechaVerificacion;

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
        public LocalDateTime getFechaVerificacion() { return fechaVerificacion; }
        public void setFechaVerificacion(LocalDateTime fechaVerificacion) {
            this.fechaVerificacion = fechaVerificacion;
        }

        public boolean isWarning() { return "WARNING".equals(nivel); }
        public boolean isError() { return "ERROR".equals(nivel); }
    }

    // Métodos de utilidad adicionales

    /**
     * Obtiene estadísticas simples de certificados
     * @return String con estadísticas básicas
     */
    public String obtenerEstadisticasCertificados() {
        try {
            StringBuilder stats = new StringBuilder();

            long totalCertificados = certificadoDAO.count();
            long certificadosVigentes = certificadoDAO.countVigentes();
            long certificadosNoVigentes = certificadoDAO.countNoVigentes();

            stats.append("ESTADÍSTICAS DE CERTIFICADOS\n");
            stats.append("============================\n");
            stats.append("Total certificados emitidos: ").append(totalCertificados).append("\n");
            stats.append("Certificados vigentes: ").append(certificadosVigentes).append("\n");
            stats.append("Certificados revocados: ").append(certificadosNoVigentes).append("\n");

            // Porcentaje de vigencia
            if (totalCertificados > 0) {
                double porcentajeVigencia = (certificadosVigentes * 100.0) / totalCertificados;
                stats.append("Porcentaje de vigencia: ").append(String.format("%.1f%%", porcentajeVigencia)).append("\n");
            }

            // Certificados completos (100% de vacunas)
            List<CertificadoVacunacion> certificadosCompletos = certificadoDAO.findCertificadosCompletos();
            stats.append("Certificados completos (100%): ").append(certificadosCompletos.size()).append("\n");

            return stats.toString();

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de certificados", e);
            return "Error al obtener estadísticas";
        }
    }

    /**
     * Obtiene certificados recientes (últimos 30 días)
     * @param limite número máximo de certificados a retornar
     * @return lista de certificados recientes
     */
    public List<CertificadoVacunacion> obtenerCertificadosRecientes(int limite) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
            List<CertificadoVacunacion> certificados = certificadoDAO.findByFechaRange(
                    fechaLimite, LocalDateTime.now());

            // Limitar resultados
            if (limite > 0 && certificados.size() > limite) {
                certificados = certificados.subList(0, limite);
            }

            // Cargar información básica del niño
            for (CertificadoVacunacion cert : certificados) {
                Optional<Nino> ninoOpt = ninoDAO.findById(cert.getNinoId());
                ninoOpt.ifPresent(cert::setNino);
            }

            return certificados;

        } catch (Exception e) {
            logger.error("Error al obtener certificados recientes", e);
            return new ArrayList<>();
        }
    }

    /**
     * Busca certificados por nombre del niño
     * @param nombreNino nombre del niño (búsqueda parcial)
     * @param usuarioId ID del usuario que busca (para verificar permisos)
     * @return lista de certificados encontrados
     */
    public List<CertificadoVacunacion> buscarCertificadosPorNombre(String nombreNino, Integer usuarioId) {
        try {
            if (nombreNino == null || nombreNino.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // Buscar niños por nombre
            List<Nino> ninos = ninoDAO.searchByName(nombreNino);
            List<CertificadoVacunacion> certificadosPermitidos = new ArrayList<>();

            for (Nino nino : ninos) {
                // Verificar permisos para cada niño
                if (verificarPermisosCertificado(nino, usuarioId)) {
                    List<CertificadoVacunacion> certificadosNino = certificadoDAO.findByNino(nino.getId());
                    for (CertificadoVacunacion cert : certificadosNino) {
                        cert.setNino(nino);
                        certificadosPermitidos.add(cert);
                    }
                }
            }

            // Ordenar por fecha de generación (más recientes primero)
            certificadosPermitidos.sort((a, b) ->
                    b.getFechaGeneracion().compareTo(a.getFechaGeneracion()));

            return certificadosPermitidos;

        } catch (Exception e) {
            logger.error("Error al buscar certificados por nombre: " + nombreNino, e);
            return new ArrayList<>();
        }
    }

    /**
     * Valida si un certificado puede ser generado para un niño
     * @param ninoId ID del niño
     * @return resultado de la validación
     */
    public CertificadoResult validarGeneracionCertificado(Integer ninoId) {
        try {
            if (ninoId == null) {
                return CertificadoResult.error("ID del niño es requerido");
            }

            // Verificar que el niño existe
            Optional<Nino> ninoOpt = ninoDAO.findById(ninoId);
            if (ninoOpt.isEmpty()) {
                return CertificadoResult.error("Niño no encontrado");
            }

            Nino nino = ninoOpt.get();

            // Verificar que el niño está activo
            if (!nino.getActivo()) {
                return CertificadoResult.error("El niño no está activo en el sistema");
            }

            // Verificar que tiene al menos una vacuna registrada
            List<RegistroVacuna> registros = registroVacunaDAO.findByNino(ninoId);
            if (registros.isEmpty()) {
                return CertificadoResult.warning("El niño no tiene vacunas registradas. " +
                        "Se generará un certificado en blanco.");
            }

            // Verificar edad razonable para vacunación (menor a 18 años)
            long edadEnDias = nino.calcularEdadEnDias();
            if (edadEnDias > 365 * 18) {
                return CertificadoResult.warning("El niño tiene más de 18 años. " +
                        "Verifique si aún requiere el esquema de vacunación infantil.");
            }

            return CertificadoResult.success(null, "El certificado puede ser generado");

        } catch (Exception e) {
            logger.error("Error al validar generación de certificado para niño: " + ninoId, e);
            return CertificadoResult.error("Error interno del sistema");
        }
    }

    /**
     * Regenera un certificado existente (actualiza porcentajes y datos)
     * @param certificadoId ID del certificado a regenerar
     * @param usuarioId ID del usuario que solicita
     * @return resultado de la regeneración
     */
    public CertificadoResult regenerarCertificado(Integer certificadoId, Integer usuarioId) {
        try {
            // Obtener certificado existente
            Optional<CertificadoVacunacion> certificadoOpt = certificadoDAO.findById(certificadoId);
            if (certificadoOpt.isEmpty()) {
                return CertificadoResult.error("Certificado no encontrado");
            }

            CertificadoVacunacion certificadoExistente = certificadoOpt.get();

            // Verificar permisos
            Optional<Nino> ninoOpt = ninoDAO.findById(certificadoExistente.getNinoId());
            if (ninoOpt.isEmpty()) {
                return CertificadoResult.error("Niño no encontrado");
            }

            Nino nino = ninoOpt.get();
            if (!verificarPermisosCertificado(nino, usuarioId)) {
                return CertificadoResult.error("No tiene permisos para regenerar este certificado");
            }

            // Actualizar datos del certificado
            List<RegistroVacuna> registrosActuales = registroVacunaDAO.findByNino(nino.getId());
            BigDecimal nuevoPorcentaje = calcularPorcentajeCompletitud(registrosActuales, nino);

            // Actualizar porcentaje en la base de datos
            certificadoDAO.updatePorcentajeCompletitud(certificadoId, nuevoPorcentaje);
            certificadoExistente.setPorcentajeCompletitud(nuevoPorcentaje);

            // Cargar información completa
            certificadoExistente.setNino(nino);
            certificadoExistente.setRegistrosVacunas(registrosActuales);

            List<Notificacion> notificaciones = notificacionDAO.getNotificacionesActivas(nino.getId());
            certificadoExistente.setNotificacionesPendientes(notificaciones);

            // Regenerar archivo
            String nuevaRuta = generarArchivoTexto(certificadoExistente);
            if (nuevaRuta != null) {
                certificadoDAO.updateUrlArchivo(certificadoId, nuevaRuta);
                certificadoExistente.setUrlArchivo(nuevaRuta);
            }

            logger.info("Certificado regenerado: {} para {}",
                    certificadoExistente.getCodigoCertificado(), nino.obtenerNombreCompleto());

            return CertificadoResult.success(certificadoExistente, "Certificado regenerado exitosamente");

        } catch (Exception e) {
            logger.error("Error al regenerar certificado: " + certificadoId, e);
            return CertificadoResult.error("Error interno del sistema");
        }
    }
}