package josue.com.bo.linde.modelo;

/**
 * POST predios / PUT predios/{id}. Sin productorId: la API usa el del token del productor.
 * Los opcionales vacíos van como null (Gson no los envía y la API los toma como null).
 */
public class PredioDto {
    private final String nombre;
    private final String departamento;
    private final String municipio;
    private final String cultivo;
    private final double superficieDeclaradaHa;
    private final Double desmonteDeclaradoHa;
    private final String numeroTituloInra;
    private final String titularCi;
    private final Double superficieTituladaHa;

    public PredioDto(String nombre, String departamento, String municipio, String cultivo, double superficieDeclaradaHa,
                     Double desmonteDeclaradoHa, String numeroTituloInra, String titularCi, Double superficieTituladaHa) {
        this.nombre = nombre;
        this.departamento = departamento;
        this.municipio = municipio;
        this.cultivo = cultivo;
        this.superficieDeclaradaHa = superficieDeclaradaHa;
        this.desmonteDeclaradoHa = desmonteDeclaradoHa;
        this.numeroTituloInra = numeroTituloInra;
        this.titularCi = titularCi;
        this.superficieTituladaHa = superficieTituladaHa;
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

    public Double getDesmonteDeclaradoHa() {
        return desmonteDeclaradoHa;
    }

    public String getNumeroTituloInra() {
        return numeroTituloInra;
    }

    public String getTitularCi() {
        return titularCi;
    }

    public Double getSuperficieTituladaHa() {
        return superficieTituladaHa;
    }
}
