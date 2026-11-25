package josue.com.bo.linde.modelo;

/** Respuesta de auth/login. expira es ISO-8601 (UTC). */
public class RespuestaLogin {
    private String token;
    private String expira;
    private Usuario usuario;

    public String getToken() {
        return token;
    }

    public String getExpira() {
        return expira;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
