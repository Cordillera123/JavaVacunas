package com.vacutrack.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase modelo para CentroSalud
 * Representa los centros de salud donde se aplican las vacunas
 * Incluye coordenadas estáticas para el Centro Histórico de Quito
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class CentroSalud {

    // Atributos de la clase
    private Integer id;
    private String nombre;
    private String direccion;
    private String telefono;
    private BigDecimal coordenadaX; // Latitud
    private BigDecimal coordenadaY; // Longitud
    private String sector;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    // Constantes para el sector por defecto
    public static final String SECTOR_CENTRO_HISTORICO = "Centro Histórico";
    public static final String CIUDAD_QUITO = "Quito";

    // Constructor vacío (requerido para JavaBeans)
    public CentroSalud() {
        this.activo = true;
        this.sector = SECTOR_CENTRO_HISTORICO;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public CentroSalud(String nombre, String direccion) {
        this();
        this.nombre = nombre;
        this.direccion = direccion;
    }

    // Constructor con parámetros principales
    public CentroSalud(String nombre, String direccion, String telefono, BigDecimal coordenadaX, BigDecimal coordenadaY) {
        this();
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
    }

    // Constructor completo
    public CentroSalud(Integer id, String nombre, String direccion, String telefono,
                       BigDecimal coordenadaX, BigDecimal coordenadaY, String sector,
                       Boolean activo, LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
        this.sector = sector;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim() : null;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion != null ? direccion.trim() : null;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        // Limpiar y formatear teléfono básico
        if (telefono != null) {
            this.telefono = telefono.trim().replaceAll("[^0-9-]", "");
        } else {
            this.telefono = null;
        }
    }

    public BigDecimal getCoordenadaX() {
        return coordenadaX;
    }

    public void setCoordenadaX(BigDecimal coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    public BigDecimal getCoordenadaY() {
        return coordenadaY;
    }

    public void setCoordenadaY(BigDecimal coordenadaY) {
        this.coordenadaY = coordenadaY;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector != null ? sector.trim() : SECTOR_CENTRO_HISTORICO;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos utilitarios

    /**
     * Verifica si el centro de salud está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo;
    }

    /**
     * Verifica si el centro tiene coordenadas configuradas
     * @return true si tiene coordenadas válidas
     */
    public boolean tieneCoordenadas() {
        return this.coordenadaX != null && this.coordenadaY != null;
    }

    /**
     * Obtiene las coordenadas como string para mapas
     * @return String en formato "latitud,longitud" o null si no tiene coordenadas
     */
    public String obtenerCoordenadasString() {
        if (tieneCoordenadas()) {
            return coordenadaX + "," + coordenadaY;
        }
        return null;
    }

    /**
     * Genera URL para Google Maps
     * @return URL de Google Maps o null si no tiene coordenadas
     */
    public String obtenerUrlGoogleMaps() {
        if (tieneCoordenadas()) {
            return "https://www.google.com/maps?q=" + coordenadaX + "," + coordenadaY;
        }
        return null;
    }

    /**
     * Obtiene información completa del centro
     * @return String con nombre y dirección
     */
    public String obtenerInformacionCompleta() {
        StringBuilder info = new StringBuilder();
        info.append(this.nombre);
        if (this.direccion != null && !this.direccion.isEmpty()) {
            info.append(" - ").append(this.direccion);
        }
        if (this.telefono != null && !this.telefono.isEmpty()) {
            info.append(" - Tel: ").append(this.telefono);
        }
        return info.toString();
    }

    // Métodos para validación

    /**
     * Valida si los datos del centro de salud son válidos
     * @return true si los datos son válidos
     */
    public boolean esValido() {
        return this.nombre != null && !this.nombre.trim().isEmpty() &&
                this.direccion != null && !this.direccion.trim().isEmpty();
    }

    /**
     * Obtiene los mensajes de validación
     * @return String con mensajes de error o null si es válido
     */
    public String obtenerMensajesValidacion() {
        StringBuilder mensajes = new StringBuilder();

        if (this.nombre == null || this.nombre.trim().isEmpty()) {
            mensajes.append("El nombre es requerido. ");
        }

        if (this.direccion == null || this.direccion.trim().isEmpty()) {
            mensajes.append("La dirección es requerida. ");
        }

        if (this.telefono != null && !this.telefono.isEmpty() && !validarTelefono(this.telefono)) {
            mensajes.append("El formato del teléfono no es válido. ");
        }

        return mensajes.length() > 0 ? mensajes.toString().trim() : null;
    }

    /**
     * Valida formato básico de teléfono ecuatoriano
     * @param telefono teléfono a validar
     * @return true si el formato es válido
     */
    private boolean validarTelefono(String telefono) {
        // Formato básico para teléfonos de Ecuador (02-XXXXXXX o 09XXXXXXXX)
        return telefono.matches("^(02-\\d{7}|09\\d{8}|\\d{7,10})$");
    }

    // Métodos Override

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CentroSalud that = (CentroSalud) obj;
        return Objects.equals(id, that.id) && Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre);
    }

    @Override
    public String toString() {
        return "CentroSalud{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", direccion='" + direccion + '\'' +
                ", telefono='" + telefono + '\'' +
                ", coordenadaX=" + coordenadaX +
                ", coordenadaY=" + coordenadaY +
                ", sector='" + sector + '\'' +
                ", activo=" + activo +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }

    /**
     * Representación simple para mostrar en UI
     * @return String para mostrar en interfaces de usuario
     */
    public String toDisplayString() {
        return this.nombre + " - " + this.direccion;
    }

    // Métodos estáticos para crear centros predefinidos del Centro Histórico de Quito

    /**
     * Crea el Centro de Salud No 1 Centro Histórico
     * @return CentroSalud configurado
     */
    public static CentroSalud crearCentroNo1() {
        CentroSalud centro = new CentroSalud();
        centro.setNombre("Centro de Salud No 1 Centro Histórico");
        centro.setDireccion("García Moreno N4-36 y Espejo");
        centro.setTelefono("02-295-1234");
        centro.setCoordenadaX(new BigDecimal("-0.2201049"));
        centro.setCoordenadaY(new BigDecimal("-78.5123450"));
        return centro;
    }

    /**
     * Crea el Subcentro de Salud La Merced
     * @return CentroSalud configurado
     */
    public static CentroSalud crearSubcentroLaMerced() {
        CentroSalud centro = new CentroSalud();
        centro.setNombre("Subcentro de Salud La Merced");
        centro.setDireccion("Cuenca 740 y Chile");
        centro.setTelefono("02-295-5678");
        centro.setCoordenadaX(new BigDecimal("-0.2185076"));
        centro.setCoordenadaY(new BigDecimal("-78.5145623"));
        return centro;
    }

    /**
     * Crea el Centro de Salud San Marcos
     * @return CentroSalud configurado
     */
    public static CentroSalud crearCentroSanMarcos() {
        CentroSalud centro = new CentroSalud();
        centro.setNombre("Centro de Salud San Marcos");
        centro.setDireccion("Junín E1-54 y Montúfar");
        centro.setTelefono("02-295-9012");
        centro.setCoordenadaX(new BigDecimal("-0.2156789"));
        centro.setCoordenadaY(new BigDecimal("-78.5098234"));
        return centro;
    }

    /**
     * Crea el Dispensario Central
     * @return CentroSalud configurado
     */
    public static CentroSalud crearDispensarioCentral() {
        CentroSalud centro = new CentroSalud();
        centro.setNombre("Dispensario Central");
        centro.setDireccion("Venezuela N3-67 y Olmedo");
        centro.setTelefono("02-295-3456");
        centro.setCoordenadaX(new BigDecimal("-0.2234567"));
        centro.setCoordenadaY(new BigDecimal("-78.5167890"));
        return centro;
    }

    /**
     * Crea el Centro de Salud San Sebastián
     * @return CentroSalud configurado
     */
    public static CentroSalud crearCentroSanSebastian() {
        CentroSalud centro = new CentroSalud();
        centro.setNombre("Centro de Salud Tipo A San Sebastián");
        centro.setDireccion("San Sebastián y Pte. Luis Cordero");
        centro.setTelefono("02-295-7890");
        centro.setCoordenadaX(new BigDecimal("-0.2178901"));
        centro.setCoordenadaY(new BigDecimal("-78.5234567"));
        return centro;
    }
}