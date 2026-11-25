package josue.com.bo.linde.modelo;

/** fecha es DateOnly: "aaaa-mm-dd". */
public class InspeccionExpediente {
    private String fecha;
    private String tecnico;
    private ResultadoInspeccion resultado;
    private String comentario;

    public String getFecha() {
        return fecha;
    }

    public String getTecnico() {
        return tecnico;
    }

    public ResultadoInspeccion getResultado() {
        return resultado;
    }

    public String getComentario() {
        return comentario;
    }
}
