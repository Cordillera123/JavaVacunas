package com.vacutrack.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase modelo para Vacuna
 * Representa el catálogo de vacunas disponibles en el esquema ecuatoriano
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class Vacuna {

    // Atributos de la clase
    private Integer id;
    private String codigo;
    private String nombre;
    private String nombreComercial;
    private String descripcion;
    private String fabricante;
    private Integer dosisTotal;
    private String viaAdministracion;
    private String sitioAplicacion;
    private String contraindicaciones;
    private String efectosAdversos;
    private Boolean activa;
    private LocalDateTime fechaCreacion;

    // Constantes para códigos de vacunas del esquema ecuatoriano
    public static final String CODIGO_BCG = "BCG";
    public static final String CODIGO_HEPATITIS_B = "HB0";
    public static final String CODIGO_ROTAVIRUS = "ROTA";
    public static final String CODIGO_POLIO_IPV = "IPV";
    public static final String CODIGO_NEUMOCOCO = "NEUMO";
    public static final String CODIGO_PENTAVALENTE = "PENTA";
    public static final String CODIGO_POLIO_BOPV = "bOPV";
    public static final String CODIGO_INFLUENZA = "INFLUENZA";
    public static final String CODIGO_SRP = "SRP";
    public static final String CODIGO_FIEBRE_AMARILLA = "FA";
    public static final String CODIGO_VARICELA = "VARICELA";

    // Constantes para vías de administración
    public static final String VIA_INTRADERMICA = "Intradérmica";
    public static final String VIA_INTRAMUSCULAR = "Intramuscular";
    public static final String VIA_ORAL = "Oral";
    public static final String VIA_SUBCUTANEA = "Subcutánea";

    // Constructor vacío (requerido para JavaBeans)
    public Vacuna() {
        this.activa = true;
        this.dosisTotal = 1;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public Vacuna(String codigo, String nombre) {
        this();
        this.codigo = codigo;
        this.nombre = nombre;
    }

    // Constructor con parámetros principales
    public Vacuna(String codigo, String nombre, String nombreComercial, String descripcion,
                  Integer dosisTotal, String viaAdministracion, String sitioAplicacion) {
        this();
        this.codigo = codigo;
        this.nombre = nombre;
        this.nombreComercial = nombreComercial;
        this.descripcion = descripcion;
        this.dosisTotal = dosisTotal;
        this.viaAdministracion = viaAdministracion;
        this.sitioAplicacion = sitioAplicacion;
    }

    // Constructor completo
    public Vacuna(Integer id, String codigo, String nombre, String nombreComercial, String descripcion,
                  String fabricante, Integer dosisTotal, String viaAdministracion, String sitioAplicacion,
                  String contraindicaciones, String efectosAdversos, Boolean activa, LocalDateTime fechaCreacion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.nombreComercial = nombreComercial;
        this.descripcion = descripcion;
        this.fabricante = fabricante;
        this.dosisTotal = dosisTotal;
        this.viaAdministracion = viaAdministracion;
        this.sitioAplicacion = sitioAplicacion;
        this.contraindicaciones = contraindicaciones;
        this.efectosAdversos = efectosAdversos;
        this.activa = activa;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo != null ? codigo.trim().toUpperCase() : null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim() : null;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial != null ? nombreComercial.trim() : null;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion != null ? descripcion.trim() : null;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante != null ? fabricante.trim() : null;
    }

    public Integer getDosisTotal() {
        return dosisTotal;
    }

    public void setDosisTotal(Integer dosisTotal) {
        this.dosisTotal = dosisTotal != null && dosisTotal > 0 ? dosisTotal : 1;
    }

    public String getViaAdministracion() {
        return viaAdministracion;
    }

    public void setViaAdministracion(String viaAdministracion) {
        this.viaAdministracion = viaAdministracion != null ? viaAdministracion.trim() : null;
    }

    public String getSitioAplicacion() {
        return sitioAplicacion;
    }

    public void setSitioAplicacion(String sitioAplicacion) {
        this.sitioAplicacion = sitioAplicacion != null ? sitioAplicacion.trim() : null;
    }

    public String getContraindicaciones() {
        return contraindicaciones;
    }

    public void setContraindicaciones(String contraindicaciones) {
        this.contraindicaciones = contraindicaciones != null ? contraindicaciones.trim() : null;
    }

    public String getEfectosAdversos() {
        return efectosAdversos;
    }

    public void setEfectosAdversos(String efectosAdversos) {
        this.efectosAdversos = efectosAdversos != null ? efectosAdversos.trim() : null;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa != null ? activa : true;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Verifica si la vacuna está activa
     * @return true si está activa
     */
    public boolean estaActiva() {
        return this.activa != null && this.activa;
    }

    /**
     * Verifica si la vacuna requiere múltiples dosis
     * @return true si requiere más de una dosis
     */
    public boolean requiereMultiplesDosis() {
        return this.dosisTotal != null && this.dosisTotal > 1;
    }

    /**
     * Verifica si es una vacuna oral
     * @return true si la vía de administración es oral
     */
    public boolean esVacunaOral() {
        return VIA_ORAL.equalsIgnoreCase(this.viaAdministracion);
    }

    /**
     * Verifica si es una vacuna inyectable
     * @return true si la vía es intramuscular, intradérmica o subcutánea
     */
    public boolean esVacunaInyectable() {
        return VIA_INTRAMUSCULAR.equalsIgnoreCase(this.viaAdministracion) ||
                VIA_INTRADERMICA.equalsIgnoreCase(this.viaAdministracion) ||
                VIA_SUBCUTANEA.equalsIgnoreCase(this.viaAdministracion);
    }

    /**
     * Obtiene el nombre para mostrar (comercial si existe, sino el nombre normal)
     * @return String con el nombre a mostrar
     */
    public String obtenerNombreDisplay() {
        if (this.nombreComercial != null && !this.nombreComercial.trim().isEmpty()) {
            return this.nombreComercial;
        }
        return this.nombre;
    }

    /**
     * Obtiene información completa de la vacuna
     * @return String con código, nombre y descripción
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();
        if (this.codigo != null) {
            info.append("[").append(this.codigo).append("] ");
        }
        info.append(obtenerNombreDisplay());
        if (this.descripcion != null && !this.descripcion.isEmpty()) {
            info.append(" - ").append(this.descripcion);
        }
        return info.toString();
    }

    /**
     * Obtiene información de administración
     * @return String con vía y sitio de aplicación
     */
    public String obtenerInformacionAdministracion() {
        StringBuilder info = new StringBuilder();
        if (this.viaAdministracion != null) {
            info.append("Vía: ").append(this.viaAdministracion);
        }
        if (this.sitioAplicacion != null) {
            if (info.length() > 0) info.append(" - ");
            info.append("Sitio: ").append(this.sitioAplicacion);
        }
        return info.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos de la vacuna son válidos
     * @return true si los datos son válidos
     */
    public boolean esValida() {
        return this.codigo != null && !this.codigo.trim().isEmpty() &&
                this.nombre != null && !this.nombre.trim().isEmpty() &&
                this.dosisTotal != null && this.dosisTotal > 0;
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válida
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.codigo == null || this.codigo.trim().isEmpty()) {
            mensajes.append("El código es requerido. ");
        }

        if (this.nombre == null || this.nombre.trim().isEmpty()) {
            mensajes.append("El nombre es requerido. ");
        }

        if (this.dosisTotal == null || this.dosisTotal <= 0) {
            mensajes.append("El número de dosis total debe ser mayor a 0. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Vacuna vacuna = (Vacuna) obj;
        return Objects.equals(id, vacuna.id) && Objects.equals(codigo, vacuna.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigo);
    }

    @Override
    public String toString() {
        return "Vacuna{" +
                "id=" + id +
                ", codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", nombreComercial='" + nombreComercial + '\'' +
                ", dosisTotal=" + dosisTotal +
                ", viaAdministracion='" + viaAdministracion + '\'' +
                ", activa=" + activa +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return this.codigo + " - " + obtenerNombreDisplay();
    }

    // Métodos estáticos para crear vacunas del esquema ecuatoriano

    /**
     * Crea la vacuna BCG
     * @return Vacuna BCG configurada
     */
    public static Vacuna crearBCG() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_BCG);
        vacuna.setNombre("BCG");
        vacuna.setNombreComercial("Bacilo de Calmette-Guérin");
        vacuna.setDescripcion("Vacuna contra tuberculosis");
        vacuna.setDosisTotal(1);
        vacuna.setViaAdministracion(VIA_INTRADERMICA);
        vacuna.setSitioAplicacion("Deltoides derecho");
        return vacuna;
    }

    /**
     * Crea la vacuna Hepatitis B para recién nacidos
     * @return Vacuna Hepatitis B configurada
     */
    public static Vacuna crearHepatitisB() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_HEPATITIS_B);
        vacuna.setNombre("Hepatitis B recién nacido");
        vacuna.setNombreComercial("Hepatitis B");
        vacuna.setDescripcion("Primera dosis de Hepatitis B al nacer");
        vacuna.setDosisTotal(1);
        vacuna.setViaAdministracion(VIA_INTRAMUSCULAR);
        vacuna.setSitioAplicacion("Vasto lateral del muslo");
        return vacuna;
    }

    /**
     * Crea la vacuna Rotavirus
     * @return Vacuna Rotavirus configurada
     */
    public static Vacuna crearRotavirus() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_ROTAVIRUS);
        vacuna.setNombre("Rotavirus");
        vacuna.setNombreComercial("RotaTeq/Rotarix");
        vacuna.setDescripcion("Vacuna contra rotavirus");
        vacuna.setDosisTotal(3);
        vacuna.setViaAdministracion(VIA_ORAL);
        vacuna.setSitioAplicacion("Vía oral");
        return vacuna;
    }

    /**
     * Crea la vacuna Polio inactivada
     * @return Vacuna IPV configurada
     */
    public static Vacuna crearPolioIPV() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_POLIO_IPV);
        vacuna.setNombre("Polio inactivada");
        vacuna.setNombreComercial("fIPV");
        vacuna.setDescripcion("Vacuna antipoliomielítica inactivada fraccionada");
        vacuna.setDosisTotal(3);
        vacuna.setViaAdministracion(VIA_INTRAMUSCULAR);
        vacuna.setSitioAplicacion("Vasto lateral del muslo");
        return vacuna;
    }

    /**
     * Crea la vacuna Neumococo
     * @return Vacuna Neumococo configurada
     */
    public static Vacuna crearNeumococo() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_NEUMOCOCO);
        vacuna.setNombre("Neumococo");
        vacuna.setNombreComercial("Prevenar/Synflorix");
        vacuna.setDescripcion("Vacuna antineumocócica conjugada");
        vacuna.setDosisTotal(3);
        vacuna.setViaAdministracion(VIA_INTRAMUSCULAR);
        vacuna.setSitioAplicacion("Vasto lateral del muslo");
        return vacuna;
    }

    /**
     * Crea la vacuna Pentavalente
     * @return Vacuna Pentavalente configurada
     */
    public static Vacuna crearPentavalente() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_PENTAVALENTE);
        vacuna.setNombre("Pentavalente");
        vacuna.setNombreComercial("DPT-HB-Hib");
        vacuna.setDescripcion("Difteria, Pertussis, Tétanos, Hepatitis B, Haemophilus influenzae b");
        vacuna.setDosisTotal(3);
        vacuna.setViaAdministracion(VIA_INTRAMUSCULAR);
        vacuna.setSitioAplicacion("Vasto lateral del muslo");
        return vacuna;
    }

    /**
     * Crea la vacuna SRP (Sarampión, Rubéola, Paperas)
     * @return Vacuna SRP configurada
     */
    public static Vacuna crearSRP() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_SRP);
        vacuna.setNombre("Sarampión, Rubéola, Paperas");
        vacuna.setNombreComercial("MMR");
        vacuna.setDescripcion("Vacuna triple viral");
        vacuna.setDosisTotal(2);
        vacuna.setViaAdministracion(VIA_SUBCUTANEA);
        vacuna.setSitioAplicacion("Deltoides");
        return vacuna;
    }

    /**
     * Crea la vacuna Varicela
     * @return Vacuna Varicela configurada
     */
    public static Vacuna crearVaricela() {
        Vacuna vacuna = new Vacuna();
        vacuna.setCodigo(CODIGO_VARICELA);
        vacuna.setNombre("Varicela");
        vacuna.setNombreComercial("Varicela");
        vacuna.setDescripcion("Vacuna contra varicela");
        vacuna.setDosisTotal(1);
        vacuna.setViaAdministracion(VIA_SUBCUTANEA);
        vacuna.setSitioAplicacion("Deltoides");
        return vacuna;
    }
}