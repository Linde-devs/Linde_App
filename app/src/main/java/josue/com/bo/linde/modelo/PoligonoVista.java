package josue.com.bo.linde.modelo;

import java.util.List;

/** precisionesM puede venir vacía (archivo o dibujo): no siempre es paralela a vertices. */
public class PoligonoVista {
    private int id;
    private int predioId;
    private int version;
    private boolean vigente;
    private boolean esPunto;
    private Double areaHa;
    private String fecha;
    private String autor;
    private String motivo;
    private List<Coordenada> vertices;
    private List<Double> precisionesM;

    public int getId() {
        return id;
    }

    public int getPredioId() {
        return predioId;
    }

    public int getVersion() {
        return version;
    }

    public boolean isVigente() {
        return vigente;
    }

    public boolean isEsPunto() {
        return esPunto;
    }

    public Double getAreaHa() {
        return areaHa;
    }

    public String getFecha() {
        return fecha;
    }

    public String getAutor() {
        return autor;
    }

    public String getMotivo() {
        return motivo;
    }

    public List<Coordenada> getVertices() {
        return vertices;
    }

    public List<Double> getPrecisionesM() {
        return precisionesM;
    }
}
