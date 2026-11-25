package josue.com.bo.linde.modelo;

/** UsuarioDto de la API (nunca trae la contraseña). productorId es null para administradores. */
public class Usuario {
    private int id;
    private String nombre;
    private String apellido;
    private String ci;
    private String correo;
    private RolUsuario rol;
    private Integer productorId;

    public int getId() {
        return id;
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

    public String getCorreo() {
        return correo;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public Integer getProductorId() {
        return productorId;
    }

    public String nombreCompleto() {
        String n = nombre == null ? "" : nombre;
        String a = apellido == null ? "" : apellido;
        return (n + " " + a).trim();
    }
}
