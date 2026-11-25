package josue.com.bo.linde.modelo;

/** POST predios/{predioId}/inspecciones. fecha es «aaaa-mm-dd»; comentario null si está vacío. */
public class InspeccionDto {
    private final String fecha;
    private final String tecnico;
    private final ResultadoInspeccion resultado;
    private final String comentario;

    public InspeccionDto(String fecha, String tecnico, ResultadoInspeccion resultado, String comentario) {
        this.fecha = fecha;
        this.tecnico = tecnico;
        this.resultado = resultado;
        this.comentario = comentario;
    }

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
