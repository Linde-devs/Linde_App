package josue.com.bo.linde.modelo;

/** Inspección registrada. fecha es «aaaa-mm-dd»; fechaRegistro es ISO-8601; registradaPor es un correo. */
public class InspeccionCampo {
    private int id;
    private int predioId;
    private String fecha;
    private String tecnico;
    private ResultadoInspeccion resultado;
    private String comentario;
    private String fechaRegistro;
    private String registradaPor;

    public int getId() {
        return id;
    }

    public int getPredioId() {
        return predioId;
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

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getRegistradaPor() {
        return registradaPor;
    }
}
