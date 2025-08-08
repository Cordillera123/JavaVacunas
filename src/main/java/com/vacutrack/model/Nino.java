package com.vacutrack.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clase modelo para Nino
 * Representa a los niños registrados en el sistema de vacunación
 * Contiene información personal, médica y de vacunación
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class Nino {

    // Atributos de la clase
    private Integer id;
    private Integer padreId;
    private PadreFamilia padre; // Relación con PadreFamilia
    private String nombres;
    private String apellidos;
    private String cedula; // Puede ser null si es muy pequeño
    private LocalDate fechaNacimiento;
    private String sexo; // 'M' o 'F'
    private String lugarNacimiento;
    private BigDecimal pesoNacimiento; // En kilogramos
    private BigDecimal tallaNacimiento; // En centímetros
    private Boolean activo;
    private LocalDateTime fechaRegistro;

    // Constantes para sexo
    public static final String SEXO_MASCULINO = "M";
    public static final String SEXO_FEMENINO = "F";

    // Patrones para validación
    private static final Pattern PATRON_CEDULA_ECUATORIANA = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");

    // Rangos normales para recién nacidos
    private static final BigDecimal PESO_MIN_NORMAL = new BigDecimal("2.5"); // 2.5 kg
    private static final BigDecimal PESO_MAX_NORMAL = new BigDecimal("4.5"); // 4.5 kg
    private static final BigDecimal TALLA_MIN_NORMAL = new BigDecimal("45"); // 45 cm
    private static final BigDecimal TALLA_MAX_NORMAL = new BigDecimal("55"); // 55 cm

    // Constructor vacío (requerido para JavaBeans)
    public Nino() {
        this.activo = true;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public Nino(Integer padreId, String nombres, String apellidos, LocalDate fechaNacimiento, String sexo) {
        this();
        this.padreId = padreId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
    }

    // Constructor con PadreFamilia
    public Nino(PadreFamilia padre, String nombres, String apellidos, LocalDate fechaNacimiento, String sexo) {
        this();
        this.padre = padre;
        this.padreId = padre != null ? padre.getId() : null;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
    }

    // Constructor completo
    public Nino(Integer id, Integer padreId, String nombres, String apellidos, String cedula,
                LocalDate fechaNacimiento, String sexo, String lugarNacimiento,
                BigDecimal pesoNacimiento, BigDecimal tallaNacimiento, Boolean activo, LocalDateTime fechaRegistro) {
        this.id = id;
        this.padreId = padreId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.cedula = cedula;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.lugarNacimiento = lugarNacimiento;
        this.pesoNacimiento = pesoNacimiento;
        this.tallaNacimiento = tallaNacimiento;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPadreId() {
        return padreId;
    }

    public void setPadreId(Integer padreId) {
        this.padreId = padreId;
    }

    public PadreFamilia getPadre() {
        return padre;
    }

    public void setPadre(PadreFamilia padre) {
        this.padre = padre;
        this.padreId = padre != null ? padre.getId() : null;
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

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        if (cedula != null && !cedula.trim().isEmpty()) {
            String cedulaLimpia = cedula.trim().replaceAll("[^0-9]", "");
            if (cedulaLimpia.length() == 10) {
                this.cedula = cedulaLimpia;
            } else {
                this.cedula = null; // Si no es válida, la dejamos nula
            }
        } else {
            this.cedula = null;
        }
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        if (SEXO_MASCULINO.equalsIgnoreCase(sexo) || SEXO_FEMENINO.equalsIgnoreCase(sexo)) {
            this.sexo = sexo.toUpperCase();
        } else {
            this.sexo = null;
        }
    }

    public String getLugarNacimiento() {
        return lugarNacimiento;
    }

    public void setLugarNacimiento(String lugarNacimiento) {
        this.lugarNacimiento = lugarNacimiento != null ? lugarNacimiento.trim() : null;
    }

    public BigDecimal getPesoNacimiento() {
        return pesoNacimiento;
    }

    public void setPesoNacimiento(BigDecimal pesoNacimiento) {
        this.pesoNacimiento = pesoNacimiento;
    }

    public BigDecimal getTallaNacimiento() {
        return tallaNacimiento;
    }

    public void setTallaNacimiento(BigDecimal tallaNacimiento) {
        this.tallaNacimiento = tallaNacimiento;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    // Métodos utilitarios

    /**
     * Obtiene el nombre completo del niño
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
     * Calcula la edad actual del niño
     * @return Period con años, meses y días
     */
    public Period calcularEdad() {
        if (this.fechaNacimiento == null) {
            return Period.ZERO;
        }
        return Period.between(this.fechaNacimiento, LocalDate.now());
    }

    /**
     * Calcula la edad en días desde el nacimiento
     * @return número de días de vida
     */
    public long calcularEdadEnDias() {
        if (this.fechaNacimiento == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.fechaNacimiento, LocalDate.now());
    }

    /**
     * Calcula la edad en días desde el nacimiento hasta una fecha específica
     * @param fechaReferencia fecha de referencia
     * @return número de días de vida hasta la fecha de referencia
     */
    public long calcularEdadEnDias(LocalDate fechaReferencia) {
        if (this.fechaNacimiento == null || fechaReferencia == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.fechaNacimiento, fechaReferencia);
    }

    /**
     * Obtiene la edad en formato legible
     * @return String con la edad formateada (ej: "2 años, 3 meses")
     */
    public String obtenerEdadFormateada() {
        Period edad = calcularEdad();
        StringBuilder edadStr = new StringBuilder();

        if (edad.getYears() > 0) {
            edadStr.append(edad.getYears()).append(edad.getYears() == 1 ? " año" : " años");
        }

        if (edad.getMonths() > 0) {
            if (edadStr.length() > 0) edadStr.append(", ");
            edadStr.append(edad.getMonths()).append(edad.getMonths() == 1 ? " mes" : " meses");
        }

        if (edad.getDays() > 0 && edad.getYears() == 0) {
            if (edadStr.length() > 0) edadStr.append(", ");
            edadStr.append(edad.getDays()).append(edad.getDays() == 1 ? " día" : " días");
        }

        return edadStr.length() > 0 ? edadStr.toString() : "Recién nacido";
    }

    /**
     * Verifica si el niño está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo;
    }

    /**
     * Verifica si es masculino
     * @return true si es masculino
     */
    public boolean esMasculino() {
        return SEXO_MASCULINO.equals(this.sexo);
    }

    /**
     * Verifica si es femenino
     * @return true si es femenino
     */
    public boolean esFemenino() {
        return SEXO_FEMENINO.equals(this.sexo);
    }

    /**
     * Obtiene el sexo en formato legible
     * @return "Masculino", "Femenino" o "No especificado"
     */
    public String obtenerSexoLegible() {
        if (SEXO_MASCULINO.equals(this.sexo)) {
            return "Masculino";
        } else if (SEXO_FEMENINO.equals(this.sexo)) {
            return "Femenino";
        }
        return "No especificado";
    }

    /**
     * Verifica si tiene cédula asignada
     * @return true si tiene cédula
     */
    public boolean tieneCedula() {
        return this.cedula != null && !this.cedula.isEmpty();
    }

    /**
     * Verifica si es un recién nacido (menos de 30 días)
     * @return true si tiene menos de 30 días
     */
    public boolean esRecienNacido() {
        return calcularEdadEnDias() < 30;
    }

    /**
     * Verifica si es un bebé (menos de 1 año)
     * @return true si tiene menos de 1 año
     */
    public boolean esBebe() {
        Period edad = calcularEdad();
        return edad.getYears() == 0;
    }

    /**
     * Verifica si el peso de nacimiento está en rango normal
     * @return true si está en rango normal
     */
    public boolean pesoNacimientoNormal() {
        if (this.pesoNacimiento == null) {
            return false;
        }
        return this.pesoNacimiento.compareTo(PESO_MIN_NORMAL) >= 0 &&
                this.pesoNacimiento.compareTo(PESO_MAX_NORMAL) <= 0;
    }

    /**
     * Verifica si la talla de nacimiento está en rango normal
     * @return true si está en rango normal
     */
    public boolean tallaNacimientoNormal() {
        if (this.tallaNacimiento == null) {
            return false;
        }
        return this.tallaNacimiento.compareTo(TALLA_MIN_NORMAL) >= 0 &&
                this.tallaNacimiento.compareTo(TALLA_MAX_NORMAL) <= 0;
    }

    /**
     * Obtiene información del padre/madre
     * @return String con información del padre/madre
     */
    public String obtenerInformacionPadre() {
        if (this.padre != null) {
            return this.padre.obtenerNombreCompleto();
        }
        return "Padre/Madre no especificado";
    }

    /**
     * Obtiene información completa del niño
     * @return String con toda la información disponible
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();

        info.append("Niño(a): ").append(obtenerNombreCompleto());
        info.append(" - ").append(obtenerSexoLegible());
        info.append(" - Edad: ").append(obtenerEdadFormateada());

        if (tieneCedula()) {
            info.append(" - CI: ").append(this.cedula);
        }

        info.append(" - Padre/Madre: ").append(obtenerInformacionPadre());

        if (this.lugarNacimiento != null) {
            info.append(" - Nació en: ").append(this.lugarNacimiento);
        }

        return info.toString();
    }

    /**
     * Obtiene información de nacimiento
     * @return String con datos de nacimiento
     */
    public String obtenerDatosNacimiento() {
        StringBuilder datos = new StringBuilder();

        datos.append("Nacimiento: ").append(this.fechaNacimiento);

        if (this.pesoNacimiento != null) {
            datos.append(" - Peso: ").append(this.pesoNacimiento).append(" kg");
        }

        if (this.tallaNacimiento != null) {
            datos.append(" - Talla: ").append(this.tallaNacimiento).append(" cm");
        }

        if (this.lugarNacimiento != null) {
            datos.append(" - Lugar: ").append(this.lugarNacimiento);
        }

        return datos.toString();
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
     * Valida si los datos del niño son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.padreId != null &&
                validarNombres() && validarApellidos() &&
                validarFechaNacimiento() && validarSexo() &&
                (this.cedula == null || validarCedula());
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
     * Valida la fecha de nacimiento
     * @return true si la fecha es válida
     */
    public boolean validarFechaNacimiento() {
        if (this.fechaNacimiento == null) {
            return false;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaMinima = hoy.minusYears(18); // Máximo 18 años

        return !this.fechaNacimiento.isAfter(hoy) && !this.fechaNacimiento.isBefore(fechaMinima);
    }

    /**
     * Valida el sexo
     * @return true si el sexo es válido
     */
    public boolean validarSexo() {
        return SEXO_MASCULINO.equals(this.sexo) || SEXO_FEMENINO.equals(this.sexo);
    }

    /**
     * Valida la cédula ecuatoriana
     * @return true si la cédula es válida o null
     */
    public boolean validarCedula() {
        if (this.cedula == null || this.cedula.isEmpty()) {
            return true; // Cédula es opcional para niños pequeños
        }

        if (!PATRON_CEDULA_ECUATORIANA.matcher(this.cedula).matches()) {
            return false;
        }

        // Validación del dígito verificador
        try {
            int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
            int suma = 0;
            int digitoVerificador = Integer.parseInt(this.cedula.substring(9, 10));

            for (int i = 0; i < 9; i++) {
                int digito = Integer.parseInt(this.cedula.substring(i, i + 1));
                int producto = digito * coeficientes[i];
                if (producto >= 10) {
                    producto -= 9;
                }
                suma += producto;
            }

            int residuo = suma % 10;
            int digitoEsperado = residuo == 0 ? 0 : 10 - residuo;

            return digitoEsperado == digitoVerificador;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.padreId == null) {
            mensajes.append("El padre/madre es requerido. ");
        }

        if (!validarNombres()) {
            mensajes.append("Los nombres son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarApellidos()) {
            mensajes.append("Los apellidos son requeridos, deben tener entre 2-100 caracteres y solo contener letras. ");
        }

        if (!validarFechaNacimiento()) {
            mensajes.append("La fecha de nacimiento debe ser válida y no puede ser futura ni anterior a 18 años. ");
        }

        if (!validarSexo()) {
            mensajes.append("El sexo debe ser 'M' (masculino) o 'F' (femenino). ");
        }

        if (!validarCedula()) {
            mensajes.append("La cédula no es válida. ");
        }

        if (this.pesoNacimiento != null && (this.pesoNacimiento.compareTo(BigDecimal.ZERO) <= 0 ||
                this.pesoNacimiento.compareTo(new BigDecimal("10")) > 0)) {
            mensajes.append("El peso de nacimiento debe estar entre 0.1 y 10 kg. ");
        }

        if (this.tallaNacimiento != null && (this.tallaNacimiento.compareTo(BigDecimal.ZERO) <= 0 ||
                this.tallaNacimiento.compareTo(new BigDecimal("100")) > 0)) {
            mensajes.append("La talla de nacimiento debe estar entre 1 y 100 cm. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Nino nino = (Nino) obj;
        return Objects.equals(id, nino.id) && Objects.equals(cedula, nino.cedula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cedula);
    }

    @Override
    public String toString() {
        return "Nino{" +
                "id=" + id +
                ", padreId=" + padreId +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", cedula='" + cedula + '\'' +
                ", fechaNacimiento=" + fechaNacimiento +
                ", sexo='" + sexo + '\'' +
                ", activo=" + activo +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append(obtenerNombreCompleto());
        display.append(" (").append(obtenerEdadFormateada()).append(")");

        if (tieneCedula()) {
            display.append(" - CI: ").append(this.cedula);
        }

        return display.toString();
    }

    /**
     * Representación para listas
     * @return String compacto para listas
     */
    public String toListDisplayString() {
        return obtenerNombreCompleto() + " - " + obtenerEdadFormateada() + " (" + obtenerSexoLegible() + ")";
    }

    /**
     * Representación para reportes médicos
     * @return String con información médica completa
     */
    public String toMedicalReportString() {
        StringBuilder report = new StringBuilder();
        report.append("INFORMACIÓN DEL PACIENTE\n");
        report.append("========================\n");
        report.append("Nombre: ").append(obtenerNombreCompleto()).append("\n");
        report.append("Sexo: ").append(obtenerSexoLegible()).append("\n");
        report.append("Edad: ").append(obtenerEdadFormateada()).append("\n");

        if (tieneCedula()) {
            report.append("Cédula: ").append(this.cedula).append("\n");
        }

        report.append("Fecha Nacimiento: ").append(this.fechaNacimiento).append("\n");

        if (this.lugarNacimiento != null) {
            report.append("Lugar Nacimiento: ").append(this.lugarNacimiento).append("\n");
        }

        if (this.pesoNacimiento != null) {
            report.append("Peso Nacimiento: ").append(this.pesoNacimiento).append(" kg");
            if (!pesoNacimientoNormal()) {
                report.append(" (FUERA DE RANGO NORMAL)");
            }
            report.append("\n");
        }

        if (this.tallaNacimiento != null) {
            report.append("Talla Nacimiento: ").append(this.tallaNacimiento).append(" cm");
            if (!tallaNacimientoNormal()) {
                report.append(" (FUERA DE RANGO NORMAL)");
            }
            report.append("\n");
        }

        report.append("Padre/Madre: ").append(obtenerInformacionPadre()).append("\n");
        report.append("Registro: ").append(this.fechaRegistro != null ?
                this.fechaRegistro.toLocalDate() : "No disponible").append("\n");

        return report.toString();
    }

    // Métodos estáticos para crear instancias predefinidas

    /**
     * Crea un niño con información básica
     * @param padre padre de familia asociado
     * @param nombres nombres del niño
     * @param apellidos apellidos del niño
     * @param fechaNacimiento fecha de nacimiento
     * @param sexo sexo del niño
     * @return Nino configurado
     */
    public static Nino crearBasico(PadreFamilia padre, String nombres, String apellidos,
                                   LocalDate fechaNacimiento, String sexo) {
        Nino nino = new Nino();
        nino.setPadre(padre);
        nino.setNombres(nombres);
        nino.setApellidos(apellidos);
        nino.setFechaNacimiento(fechaNacimiento);
        nino.setSexo(sexo);
        return nino;
    }

    /**
     * Crea un niño con información completa de nacimiento
     * @param padre padre de familia asociado
     * @param nombres nombres del niño
     * @param apellidos apellidos del niño
     * @param fechaNacimiento fecha de nacimiento
     * @param sexo sexo del niño
     * @param pesoNacimiento peso al nacer
     * @param tallaNacimiento talla al nacer
     * @param lugarNacimiento lugar de nacimiento
     * @return Nino configurado
     */
    public static Nino crearCompleto(PadreFamilia padre, String nombres, String apellidos,
                                     LocalDate fechaNacimiento, String sexo, BigDecimal pesoNacimiento,
                                     BigDecimal tallaNacimiento, String lugarNacimiento) {
        Nino nino = crearBasico(padre, nombres, apellidos, fechaNacimiento, sexo);
        nino.setPesoNacimiento(pesoNacimiento);
        nino.setTallaNacimiento(tallaNacimiento);
        nino.setLugarNacimiento(lugarNacimiento);
        return nino;
    }

    /**
     * Crea un niño de ejemplo para testing
     * @return Nino de ejemplo
     */
    public static Nino crearEjemplo() {
        Nino nino = new Nino();
        nino.setNombres("Juan Carlos");
        nino.setApellidos("González López");
        nino.setFechaNacimiento(LocalDate.now().minusMonths(6)); // 6 meses de edad
        nino.setSexo(SEXO_MASCULINO);
        nino.setPesoNacimiento(new BigDecimal("3.2"));
        nino.setTallaNacimiento(new BigDecimal("50"));
        nino.setLugarNacimiento("Hospital Metropolitano, Quito");
        return nino;
    }
}