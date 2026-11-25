package josue.com.bo.linde.modelo;

public class ResultadoCapa {
    private CapaValidacion capa;
    private EstadoCapa estado;
    private String mensaje;
    private PrioridadAlerta prioridad;
    private EstadoUsoSuelo usoSuelo;

    public CapaValidacion getCapa() {
        return capa;
    }

    public EstadoCapa getEstado() {
        return estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public PrioridadAlerta getPrioridad() {
        return prioridad;
    }

    public EstadoUsoSuelo getUsoSuelo() {
        return usoSuelo;
    }
}
