package josue.com.bo.linde.modelo;

import java.util.List;

/** Resultado de un envío a revisión (POST predios/{id}/enviar y GET predios/{id}/validaciones). */
public class ResultadoValidacion {
    private int id;
    private int predioId;
    private int versionPoligono;
    private String fecha;
    private List<ResultadoCapa> capas;
    private boolean cortado;
    private EstadoExpediente estadoResultante;
    private String mensaje;

    public int getId() {
        return id;
    }

    public int getPredioId() {
        return predioId;
    }

    public int getVersionPoligono() {
        return versionPoligono;
    }

    public String getFecha() {
        return fecha;
    }

    public List<ResultadoCapa> getCapas() {
        return capas;
    }

    public boolean isCortado() {
        return cortado;
    }

    public EstadoExpediente getEstadoResultante() {
        return estadoResultante;
    }

    public String getMensaje() {
        return mensaje;
    }
}
