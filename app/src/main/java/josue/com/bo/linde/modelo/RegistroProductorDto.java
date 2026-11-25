package josue.com.bo.linde.modelo;

/** POST auth/registro. runpa es opcional (null si no se escribió). */
public class RegistroProductorDto {
    private final String nombre;
    private final String apellido;
    private final String ci;
    private final String runpa;
    private final String telefono;
    private final String municipio;
    private final String correo;
    private final String contrasena;

    public RegistroProductorDto(String nombre, String apellido, String ci, String runpa, String telefono,
                                String municipio, String correo, String contrasena) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.ci = ci;
        this.runpa = runpa;
        this.telefono = telefono;
        this.municipio = municipio;
        this.correo = correo;
        this.contrasena = contrasena;
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

    public String getContrasena() {
        return contrasena;
    }
}
