package josue.com.bo.linde.modelo;

/** Un requisito para enviar a revisión. ruta es la ruta de la web que lo resuelve (informativa en la app). */
public class Requisito {
    private String clave;
    private String nombre;
    private boolean cumplido;
    private String detalle;
    private String ruta;

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isCumplido() {
        return cumplido;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getRuta() {
        return ruta;
    }
}
