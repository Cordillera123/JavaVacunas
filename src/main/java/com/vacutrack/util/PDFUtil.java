package com.vacutrack.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import com.vacutrack.model.*;
import com.vacutrack.service.ReporteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Utilidad profesional para generar PDFs reales con iText
 * Genera certificados de vacunación con diseño profesional
 *
 * @author VACU-TRACK Team
 * @version 2.0 (PDF Real)
 */
public class PDFUtil {

    private static final Logger logger = LoggerFactory.getLogger(PDFUtil.class);

    // Formateadores de fecha
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Colores corporativos
    private static final DeviceRgb COLOR_PRIMARIO = new DeviceRgb(41, 128, 185); // Azul
    private static final DeviceRgb COLOR_SECUNDARIO = new DeviceRgb(52, 152, 219); // Azul claro
    private static final DeviceRgb COLOR_EXITO = new DeviceRgb(39, 174, 96); // Verde
    private static final DeviceRgb COLOR_ALERTA = new DeviceRgb(231, 76, 60); // Rojo
    private static final DeviceRgb COLOR_GRIS = new DeviceRgb(149, 165, 166); // Gris

    /**
     * Crea un certificado de vacunación profesional en PDF
     */
    public static void crearCertificadoVacunacion(ReporteService.CertificadoVacunacion certificado,
                                                  OutputStream outputStream) {
        try {
            if (!validarDatosCertificado(certificado)) {
                logger.warn("Datos del certificado inválidos");
                return;
            }

            // Crear documento PDF
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // Fuentes
            PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont fontRegular = PdfFontFactory.createFont("Helvetica");

            Nino nino = certificado.getNino();
            List<RegistroVacuna> registros = certificado.getRegistros();

            // ========== ENCABEZADO ==========
            crearEncabezado(document, fontBold);

            // ========== INFORMACIÓN DEL PACIENTE ==========
            crearSeccionPaciente(document, nino, fontBold, fontRegular);

            // ========== TABLA DE VACUNAS ==========
            crearTablaVacunas(document, registros, fontBold, fontRegular);

            // ========== ESTADÍSTICAS ==========
            crearSeccionEstadisticas(document, registros, fontBold, fontRegular);

            // ========== PIE DE PÁGINA ==========
            crearPiePagina(document, certificado, fontRegular);

            document.close();
            logger.info("PDF generado exitosamente para: {} {}", nino.getNombres(), nino.getApellidos());

        } catch (Exception e) {
            logger.error("Error al generar PDF", e);
            throw new RuntimeException("Error al generar certificado PDF", e);
        }
    }

    /**
     * Crea el encabezado del documento
     */
    private static void crearEncabezado(Document document, PdfFont fontBold) throws Exception {
        // Logo y título principal
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Celda izquierda (espacio para logo)
        Cell logoCell = new Cell().add(new Paragraph("🏥").setFontSize(40))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER);

        // Celda central (títulos)
        Paragraph titulo = new Paragraph("CERTIFICADO DE VACUNACIÓN")
                .setFont(fontBold)
                .setFontSize(18)
                .setFontColor(COLOR_PRIMARIO)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

        Paragraph subtitulo = new Paragraph("REPÚBLICA DEL ECUADOR\nMINISTERIO DE SALUD PÚBLICA")
                .setFont(fontBold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_GRIS);

        Cell titleCell = new Cell().add(titulo).add(subtitulo)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER);

        // Celda derecha (fecha)
        Cell dateCell = new Cell().add(new Paragraph("📅\n" +
                        java.time.LocalDate.now().format(DATE_FORMATTER))
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER);

        headerTable.addCell(logoCell);
        headerTable.addCell(titleCell);
        headerTable.addCell(dateCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Crea la sección de información del paciente
     */
    private static void crearSeccionPaciente(Document document, Nino nino, PdfFont fontBold, PdfFont fontRegular) throws Exception {
        // Título de sección
        Paragraph seccionTitulo = new Paragraph("DATOS DEL PACIENTE")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(COLOR_PRIMARIO)
                .setMarginBottom(10);
        document.add(seccionTitulo);

        // Tabla de datos del paciente
        Table pacienteTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Datos del paciente
        agregarCampoPaciente(pacienteTable, "Nombres:", nino.getNombres(), fontBold, fontRegular);
        agregarCampoPaciente(pacienteTable, "Apellidos:", nino.getApellidos(), fontBold, fontRegular);

        if (nino.getCedula() != null) {
            agregarCampoPaciente(pacienteTable, "Cédula:", nino.getCedula(), fontBold, fontRegular);
        }

        agregarCampoPaciente(pacienteTable, "Fecha de Nacimiento:",
                nino.getFechaNacimiento().format(DATE_FORMATTER), fontBold, fontRegular);

        String sexo = nino.getSexo() != null ?
                ("M".equals(nino.getSexo()) ? "Masculino" : "Femenino") : "No especificado";
        agregarCampoPaciente(pacienteTable, "Sexo:", sexo, fontBold, fontRegular);

        // Calcular edad
        int edadAnios = java.time.Period.between(nino.getFechaNacimiento(),
                java.time.LocalDate.now()).getYears();
        agregarCampoPaciente(pacienteTable, "Edad Actual:", edadAnios + " años", fontBold, fontRegular);

        if (nino.getLugarNacimiento() != null) {
            agregarCampoPaciente(pacienteTable, "Lugar de Nacimiento:", nino.getLugarNacimiento(), fontBold, fontRegular);
        }

        document.add(pacienteTable);
    }

    /**
     * Agrega un campo a la tabla del paciente
     */
    private static void agregarCampoPaciente(Table table, String etiqueta, String valor,
                                             PdfFont fontBold, PdfFont fontRegular) {
        Cell labelCell = new Cell().add(new Paragraph(etiqueta)
                        .setFont(fontBold)
                        .setFontColor(COLOR_PRIMARIO))
                .setPadding(8)
                .setBackgroundColor(new DeviceRgb(248, 249, 250));

        Cell valueCell = new Cell().add(new Paragraph(valor != null ? valor : "No especificado")
                        .setFont(fontRegular))
                .setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Crea la tabla de vacunas aplicadas
     */
    private static void crearTablaVacunas(Document document, List<RegistroVacuna> registros,
                                          PdfFont fontBold, PdfFont fontRegular) throws Exception {
        // Título de sección
        Paragraph seccionTitulo = new Paragraph("REGISTRO DE VACUNAS APLICADAS")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(COLOR_PRIMARIO)
                .setMarginBottom(10);
        document.add(seccionTitulo);

        if (registros == null || registros.isEmpty()) {
            Paragraph noVacunas = new Paragraph("No se registran vacunas aplicadas.")
                    .setFont(fontRegular)
                    .setFontColor(COLOR_ALERTA)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(20);
            document.add(noVacunas);
            return;
        }

        // Crear tabla de vacunas
        Table vacunasTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2f, 1f, 1f, 1.5f, 1f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Encabezados de la tabla
        String[] headers = {"#", "Vacuna", "Dosis", "Fecha", "Lote", "Estado"};
        for (String header : headers) {
            Cell headerCell = new Cell().add(new Paragraph(header)
                            .setFont(fontBold)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_PRIMARIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8);
            vacunasTable.addHeaderCell(headerCell);
        }

        // Datos de las vacunas
        int contador = 1;
        for (RegistroVacuna registro : registros) {
            // Número
            vacunasTable.addCell(new Cell().add(new Paragraph(String.valueOf(contador))
                            .setFont(fontRegular)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            // Nombre de vacuna
            String nombreVacuna = registro.getVacunaNombre() != null ?
                    registro.getVacunaNombre() : "Vacuna ID: " + registro.getVacunaId();
            vacunasTable.addCell(new Cell().add(new Paragraph(nombreVacuna)
                            .setFont(fontRegular))
                    .setPadding(6));

            // Dosis
            String dosis = registro.getNumeroDosis() != null ?
                    registro.getNumeroDosis().toString() : "1";
            vacunasTable.addCell(new Cell().add(new Paragraph(dosis)
                            .setFont(fontRegular)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            // Fecha
            String fecha = registro.getFechaAplicacion() != null ?
                    registro.getFechaAplicacion().format(DATE_FORMATTER) : "No especificada";
            vacunasTable.addCell(new Cell().add(new Paragraph(fecha)
                            .setFont(fontRegular)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            // Lote
            String lote = registro.getLoteVacuna() != null ?
                    registro.getLoteVacuna() : "N/A";
            vacunasTable.addCell(new Cell().add(new Paragraph(lote)
                            .setFont(fontRegular)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            // Estado (con reacción adversa)
            String estado = "✓ Aplicada";
            DeviceRgb colorEstado = COLOR_EXITO;

            if (registro.getReaccionAdversa() != null && registro.getReaccionAdversa()) {
                estado = "⚠ Reacción";
                colorEstado = COLOR_ALERTA;
            }

            vacunasTable.addCell(new Cell().add(new Paragraph(estado)
                            .setFont(fontRegular)
                            .setFontColor(colorEstado)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            contador++;
        }

        document.add(vacunasTable);
    }

    /**
     * Crea la sección de estadísticas
     */
    private static void crearSeccionEstadisticas(Document document, List<RegistroVacuna> registros,
                                                 PdfFont fontBold, PdfFont fontRegular) throws Exception {
        // Título de sección
        Paragraph seccionTitulo = new Paragraph("ESTADÍSTICAS DE VACUNACIÓN")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(COLOR_PRIMARIO)
                .setMarginBottom(10);
        document.add(seccionTitulo);

        if (registros == null || registros.isEmpty()) {
            return;
        }

        // Calcular estadísticas
        Set<Integer> vacunasUnicas = new HashSet<>();
        int reaccionesAdversas = 0;

        for (RegistroVacuna registro : registros) {
            vacunasUnicas.add(registro.getVacunaId());
            if (registro.getReaccionAdversa() != null && registro.getReaccionAdversa()) {
                reaccionesAdversas++;
            }
        }

        // Tabla de estadísticas
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Estadística 1: Total dosis
        Cell stat1 = crearCeldaEstadistica("📊", "Total Dosis", String.valueOf(registros.size()),
                COLOR_PRIMARIO, fontBold, fontRegular);
        statsTable.addCell(stat1);

        // Estadística 2: Tipos de vacunas
        Cell stat2 = crearCeldaEstadistica("💉", "Tipos de Vacunas", String.valueOf(vacunasUnicas.size()),
                COLOR_SECUNDARIO, fontBold, fontRegular);
        statsTable.addCell(stat2);

        // Estadística 3: Reacciones adversas
        Cell stat3 = crearCeldaEstadistica("⚠️", "Reacciones Adversas", String.valueOf(reaccionesAdversas),
                reaccionesAdversas > 0 ? COLOR_ALERTA : COLOR_EXITO, fontBold, fontRegular);
        statsTable.addCell(stat3);

        document.add(statsTable);

        // Verificación de vacunas básicas
        crearVerificacionVacunasBasicas(document, registros, fontBold, fontRegular);
    }

    /**
     * Crea una celda de estadística
     */
    private static Cell crearCeldaEstadistica(String icono, String titulo, String valor,
                                              DeviceRgb color, PdfFont fontBold, PdfFont fontRegular) {
        Paragraph iconoPara = new Paragraph(icono)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

        Paragraph tituloPara = new Paragraph(titulo)
                .setFont(fontRegular)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_GRIS);

        Paragraph valorPara = new Paragraph(valor)
                .setFont(fontBold)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(color);

        return new Cell()
                .add(iconoPara)
                .add(tituloPara)
                .add(valorPara)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(15)
                .setBackgroundColor(new DeviceRgb(248, 249, 250));
    }

    /**
     * Crea la verificación de vacunas básicas
     */
    private static void crearVerificacionVacunasBasicas(Document document, List<RegistroVacuna> registros,
                                                        PdfFont fontBold, PdfFont fontRegular) throws Exception {
        Paragraph titulo = new Paragraph("Verificación del Esquema Nacional Básico:")
                .setFont(fontBold)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(titulo);

        String[] vacunasBasicas = {"BCG", "Hepatitis B", "Pentavalente", "Polio", "Neumococo", "Rotavirus", "SRP"};

        Table verificacionTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        for (String vacuna : vacunasBasicas) {
            boolean tiene = registros.stream()
                    .anyMatch(r -> r.getVacunaNombre() != null &&
                            r.getVacunaNombre().toLowerCase().contains(vacuna.toLowerCase()));

            Cell vacunaCell = new Cell().add(new Paragraph(vacuna)
                            .setFont(fontRegular))
                    .setPadding(5);

            String estado = tiene ? "✓ SÍ" : "✗ NO";
            DeviceRgb color = tiene ? COLOR_EXITO : COLOR_ALERTA;

            Cell estadoCell = new Cell().add(new Paragraph(estado)
                            .setFont(fontBold)
                            .setFontColor(color)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(5);

            verificacionTable.addCell(vacunaCell);
            verificacionTable.addCell(estadoCell);
        }

        document.add(verificacionTable);
    }

    /**
     * Crea el pie de página
     */
    private static void crearPiePagina(Document document, ReporteService.CertificadoVacunacion certificado,
                                       PdfFont fontRegular) throws Exception {
        document.add(new Paragraph("\n"));

        // Información del certificado
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell fechaCell = new Cell().add(new Paragraph("Fecha de generación: " +
                        certificado.getFechaGeneracion().format(DATETIME_FORMATTER))
                        .setFont(fontRegular)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER);

        Cell sistemaCell = new Cell().add(new Paragraph("Sistema: VACU-TRACK v2.0")
                        .setFont(fontRegular)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER);

        infoTable.addCell(fechaCell);
        infoTable.addCell(sistemaCell);
        document.add(infoTable);

        // Nota importante
        Paragraph nota = new Paragraph("\nNOTA IMPORTANTE: Este certificado es un documento oficial que certifica las vacunas aplicadas según el registro del sistema. Para consultas médicas, acuda a su centro de salud.")
                .setFont(fontRegular)
                .setFontSize(8)
                .setFontColor(COLOR_GRIS)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(20)
                .setPadding(10)
                .setBackgroundColor(new DeviceRgb(248, 249, 250));
        document.add(nota);

        // Líneas de firma
        document.add(new Paragraph("\n\n"));
        Table firmasTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell firma1 = new Cell().add(new Paragraph("_________________________\nPROFESIONAL DE SALUD")
                        .setFont(fontRegular)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER);

        Cell firma2 = new Cell().add(new Paragraph("_________________________\nPADRE/MADRE/TUTOR")
                        .setFont(fontRegular)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER);

        firmasTable.addCell(firma1);
        firmasTable.addCell(firma2);
        document.add(firmasTable);
    }

    /**
     * Valida que los datos para generar PDF sean válidos
     */
    public static boolean validarDatosCertificado(ReporteService.CertificadoVacunacion certificado) {
        if (certificado == null) {
            logger.warn("Certificado es null");
            return false;
        }

        if (certificado.getNino() == null) {
            logger.warn("Niño es null en el certificado");
            return false;
        }

        Nino nino = certificado.getNino();
        if (nino.getNombres() == null || nino.getNombres().trim().isEmpty()) {
            logger.warn("Nombre del niño es requerido");
            return false;
        }

        if (nino.getApellidos() == null || nino.getApellidos().trim().isEmpty()) {
            logger.warn("Apellidos del niño son requeridos");
            return false;
        }

        if (nino.getFechaNacimiento() == null) {
            logger.warn("Fecha de nacimiento es requerida");
            return false;
        }

        return true;
    }

    /**
     * Genera un ejemplo de certificado para testing
     */
    public static ReporteService.CertificadoVacunacion generarCertificadoEjemplo() {
        // Crear niño de ejemplo
        Nino nino = new Nino();
        nino.setNombres("Juan Carlos");
        nino.setApellidos("Pérez García");
        nino.setCedula("1234567890");
        nino.setFechaNacimiento(java.time.LocalDate.of(2023, 6, 15));
        nino.setSexo("M");
        nino.setLugarNacimiento("Quito, Ecuador");

        // Crear registros de ejemplo
        List<RegistroVacuna> registros = new java.util.ArrayList<>();

        RegistroVacuna registro1 = new RegistroVacuna();
        registro1.setVacunaNombre("BCG");
        registro1.setNumeroDosis(1);
        registro1.setFechaAplicacion(java.time.LocalDate.of(2023, 6, 16));
        registro1.setLoteVacuna("BCG2023001");
        registros.add(registro1);

        RegistroVacuna registro2 = new RegistroVacuna();
        registro2.setVacunaNombre("Hepatitis B");
        registro2.setNumeroDosis(1);
        registro2.setFechaAplicacion(java.time.LocalDate.of(2023, 6, 16));
        registro2.setLoteVacuna("HB2023002");
        registros.add(registro2);

        // Crear certificado
        ReporteService.CertificadoVacunacion certificado = new ReporteService.CertificadoVacunacion();
        certificado.setNino(nino);
        certificado.setRegistros(registros);
        certificado.setFechaGeneracion(java.time.LocalDateTime.now());

        return certificado;
    }
}