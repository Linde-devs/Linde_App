package josue.com.bo.linde.modelo;

import java.util.List;

/** documentos son textos como "TituloPropiedad SPP-NAL-070167". fechaGeneracion es ISO-8601. */
public class Expediente {
    private int id;
    private String codigo;
    private String fechaGeneracion;
    private String productor;
    private String ciProductor;
    private int predioId;
    private String predio;
    private String departamento;
    private String municipio;
    private double superficieDeclaradaHa;
    private double areaPoligonoHa;
    private int versionPoligono;
    private List<Coordenada> vertices;
    private List<ResultadoCapa> validaciones;
    private List<String> documentos;
    private EstadoInforme estado;
    private List<AlertaExpediente> alertas;
    private List<InspeccionExpediente> inspecciones;

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public String getProductor() {
        return productor;
    }

    public String getCiProductor() {
        return ciProductor;
    }

    public int getPredioId() {
        return predioId;
    }

    public String getPredio() {
        return predio;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getMunicipio() {
        return municipio;
    }

    public double getSuperficieDeclaradaHa() {
        return superficieDeclaradaHa;
    }

    public double getAreaPoligonoHa() {
        return areaPoligonoHa;
    }

    public int getVersionPoligono() {
        return versionPoligono;
    }

    public List<Coordenada> getVertices() {
        return vertices;
    }

    public List<ResultadoCapa> getValidaciones() {
        return validaciones;
    }

    public List<String> getDocumentos() {
        return documentos;
    }

    public EstadoInforme getEstado() {
        return estado;
    }

    public List<AlertaExpediente> getAlertas() {
        return alertas;
    }

    public List<InspeccionExpediente> getInspecciones() {
        return inspecciones;
    }
}
