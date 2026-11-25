package josue.com.bo.linde.modelo;

public class AlertaExpediente {
    private CapaValidacion capa;
    private PrioridadAlerta prioridad;
    private String mensaje;

    public CapaValidacion getCapa() {
        return capa;
    }

    public PrioridadAlerta getPrioridad() {
        return prioridad;
    }

    public String getMensaje() {
        return mensaje;
    }
}
