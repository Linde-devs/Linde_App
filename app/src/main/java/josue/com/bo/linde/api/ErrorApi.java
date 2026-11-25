package josue.com.bo.linde.api;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.Map;

import josue.com.bo.linde.modelo.ErrorRespuesta;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Error de la API listo para mostrar. ErrorApi.de(respuesta) nunca devuelve null y getError() siempre tiene texto:
 * el mensaje de la API tal cual o, si no se pudo leer, uno genérico según el código.
 */
public final class ErrorApi {

    public static final String SIN_CONEXION = "Sin conexión con el servidor. Revisa tu Internet e intenta de nuevo.";
    private static final Gson GSON = new Gson();

    private final String error;
    private final String campo;
    private final String tipo;
    private final int codigo;

    private ErrorApi(String error, String campo, String tipo, int codigo) {
        this.error = error;
        this.campo = campo;
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public static ErrorApi de(Response<?> respuesta) {
        ErrorRespuesta cuerpo = null;
        try (ResponseBody errorBody = respuesta.errorBody()) {
            if (errorBody != null) cuerpo = GSON.fromJson(errorBody.charStream(), ErrorRespuesta.class);
        } catch (Exception ignorada) {
            cuerpo = null;
        }
        String mensaje = cuerpo != null ? cuerpo.getError() : null;
        if (mensaje == null || mensaje.trim().isEmpty()) mensaje = generico(respuesta.code());
        return new ErrorApi(mensaje,
                cuerpo != null ? cuerpo.getCampo() : null,
                cuerpo != null ? cuerpo.getTipo() : null,
                respuesta.code());
    }

    /** Para onFailure (sin red, servidor apagado, tiempo agotado). */
    public static ErrorApi sinConexion() {
        return new ErrorApi(SIN_CONEXION, null, null, 0);
    }

    private static String generico(int codigo) {
        switch (codigo) {
            case 400:
                return "Hay datos no válidos. Revisa el formulario.";
            case 401:
                return "Tu sesión venció. Vuelve a iniciar sesión.";
            case 403:
                return "Acceso denegado";
            case 404:
                return "No se encontró lo que buscabas.";
            case 409:
                return "El estado actual no permite esta operación.";
            default:
                return "Ocurrió un error inesperado.";
        }
    }

    public String getError() {
        return error;
    }

    /** Nombre del campo que falló (camelCase, igual que en los DTO) o null. */
    public String getCampo() {
        return campo;
    }

    /** Tipo del error geométrico (p. ej. «Autointerseccion») o null. */
    public String getTipo() {
        return tipo;
    }

    /** Código HTTP (0 si no hubo respuesta). */
    public int getCodigo() {
        return codigo;
    }

    /**
     * Si el error trae «campo» y está en el mapa, muestra el mensaje en ese TextInputLayout y devuelve true.
     * Las claves del mapa son los nombres de los campos del DTO (por ejemplo "numeroTituloInra").
     */
    public boolean marcarCampo(Map<String, TextInputLayout> campos) {
        if (campo == null || campos == null) return false;
        TextInputLayout destino = campos.get(campo);
        if (destino == null) return false;
        destino.setError(error);
        destino.requestFocus();
        return true;
    }
}
