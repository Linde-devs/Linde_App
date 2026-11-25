package josue.com.bo.linde.modelo;

/** Cuerpo de error de la API: {"error": "...", "campo": "..." | null, "tipo": "..." | null}. */
public class ErrorRespuesta {
    private String error;
    private String campo;
    private String tipo;

    public String getError() {
        return error;
    }

    public String getCampo() {
        return campo;
    }

    public String getTipo() {
        return tipo;
    }
}
