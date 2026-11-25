package josue.com.bo.linde.modelo;

/** Respuesta de auth/registro. codigo es «PRD-000001». */
public class ProductorVista {
    private int id;
    private String codigo;
    private String nombre;
    private String apellido;
    private String ci;
    private String runpa;
    private String telefono;
    private String municipio;
    private String correo;
    private String fechaRegistro;
    private int cantidadPredios;

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCi() {
        return ci;
    }

    public String getRunpa() {
        return runpa;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getCorreo() {
        return correo;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public int getCantidadPredios() {
        return cantidadPredios;
    }
}
