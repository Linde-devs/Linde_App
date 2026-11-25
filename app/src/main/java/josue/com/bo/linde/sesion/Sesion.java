package josue.com.bo.linde.sesion;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.Date;

import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.modelo.RespuestaLogin;
import josue.com.bo.linde.modelo.Usuario;
import josue.com.bo.linde.util.Formatos;

/** Sesión del productor: solo token, expira y usuario. La contraseña nunca se guarda. */
public final class Sesion {

    private static final String ARCHIVO = "linde_sesion";
    private static final String TOKEN = "token";
    private static final String EXPIRA = "expira";
    private static final String USUARIO = "usuario";
    private static final Gson GSON = new Gson();

    private Sesion() {
    }

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public static void guardar(Context contexto, RespuestaLogin respuesta) {
        prefs(contexto).edit()
                .putString(TOKEN, respuesta.getToken())
                .putString(EXPIRA, respuesta.getExpira())
                .putString(USUARIO, GSON.toJson(respuesta.getUsuario()))
                .apply();
        ClienteApi.sesionIniciada();
    }

    public static void cerrar(Context contexto) {
        prefs(contexto).edit().clear().apply();
    }

    @Nullable
    public static String token(Context contexto) {
        return prefs(contexto).getString(TOKEN, null);
    }

    @Nullable
    public static String expira(Context contexto) {
        return prefs(contexto).getString(EXPIRA, null);
    }

    @Nullable
    public static Usuario usuario(Context contexto) {
        String json = prefs(contexto).getString(USUARIO, null);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, Usuario.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Hay token y todavía no venció (expira es ISO-8601 en UTC). */
    public static boolean haySesionVigente(Context contexto) {
        if (token(contexto) == null) return false;
        Date vence = Formatos.leerIso(expira(contexto));
        return vence != null && vence.after(new Date());
    }
}
