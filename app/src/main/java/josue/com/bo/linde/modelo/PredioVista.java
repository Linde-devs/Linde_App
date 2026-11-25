package josue.com.bo.linde.modelo;

/** Lo que la API devuelve de un predio. areaPoligonoHa es null si no tiene polígono o si es un punto. */
public class PredioVista {
    private int id;
    private int productorId;
    private String productor;
    private String nombre;
    private String departamento;
    private String municipio;
    private String cultivo;
    private double superficieDeclaradaHa;
    private String numeroTituloInra;
    private Double desmonteDeclaradoHa;
    private EstadoExpediente estado;
    private String fechaRegistro;
    private boolean tienePoligono;
    private boolean esPunto;
    private Double areaPoligonoHa;
    private String titularCi;
    private Double superficieTituladaHa;

    public int getId() {
        return id;
    }

    public int getProductorId() {
        return productorId;
    }

    public String getProductor() {
        return productor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getCultivo() {
        return cultivo;
    }

    public double getSuperficieDeclaradaHa() {
        return superficieDeclaradaHa;
    }

    public String getNumeroTituloInra() {
        return numeroTituloInra;
    }

    public Double getDesmonteDeclaradoHa() {
        return desmonteDeclaradoHa;
    }

    public EstadoExpediente getEstado() {
        return estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public boolean isTienePoligono() {
        return tienePoligono;
    }

    public boolean isEsPunto() {
        return esPunto;
    }

    public Double getAreaPoligonoHa() {
        return areaPoligonoHa;
    }

    public String getTitularCi() {
        return titularCi;
    }

    public Double getSuperficieTituladaHa() {
        return superficieTituladaHa;
    }
}
