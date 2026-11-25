package josue.com.bo.linde.modelo;

/** GET alertas?predioId={id}. */
public class AlertaVista {
    private int id;
    private int predioId;
    private String predio;
    private CapaValidacion capa;
    private PrioridadAlerta prioridad;
    private String mensaje;
    private String fecha;
    private boolean resuelta;

    public int getId() {
        return id;
    }

    public int getPredioId() {
        return predioId;
    }

    public String getPredio() {
        return predio;
    }

    public CapaValidacion getCapa() {
        return capa;
    }

    public PrioridadAlerta getPrioridad() {
        return prioridad;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getFecha() {
        return fecha;
    }

    public boolean isResuelta() {
        return resuelta;
    }
}
