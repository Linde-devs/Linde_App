package josue.com.bo.linde.api;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import josue.com.bo.linde.BuildConfig;
import josue.com.bo.linde.sesion.Sesion;
import josue.com.bo.linde.ui.login.LoginActivity;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Un único Retrofit para toda la app. Agrega «Authorization: Bearer &lt;token&gt;» si hay sesión y, si llega un 401
 * fuera de auth/login y auth/registro (token vencido o borrado), cierra la sesión y vuelve al Login.
 * En auth/login un 401 es «Correo o contraseña incorrectos.»: ahí solo se muestra el mensaje.
 */
public final class ClienteApi {

    private static volatile LindeApi api;
    private static final AtomicBoolean volviendoAlLogin = new AtomicBoolean(false);

    private ClienteApi() {
    }

    public static LindeApi get(Context contexto) {
        if (api == null) {
            synchronized (ClienteApi.class) {
                if (api == null) api = crear(contexto.getApplicationContext());
            }
        }
        return api;
    }

    /** Lo llama Sesion.guardar: con una sesión nueva, un próximo 401 vuelve a llevar al Login. */
    public static void sesionIniciada() {
        volviendoAlLogin.set(false);
    }

    private static LindeApi crear(Context app) {
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(token(app))
                .addInterceptor(sesionVencida(app));

        if (BuildConfig.DEBUG) {
            // Solo en debug y sin cuerpos: así no quedan contraseñas ni archivos en el Logcat.
            HttpLoggingInterceptor registro = new HttpLoggingInterceptor();
            registro.setLevel(HttpLoggingInterceptor.Level.BASIC);
            registro.redactHeader("Authorization");
            http.addInterceptor(registro);
        }

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(http.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LindeApi.class);
    }

    private static Interceptor token(Context app) {
        return cadena -> {
            Request pedido = cadena.request();
            String token = Sesion.token(app);
            if (token == null || esRutaDeAcceso(pedido)) return cadena.proceed(pedido);
            return cadena.proceed(pedido.newBuilder().header("Authorization", "Bearer " + token).build());
        };
    }

    private static Interceptor sesionVencida(Context app) {
        return cadena -> {
            Request pedido = cadena.request();
            Response respuesta = cadena.proceed(pedido);
            if (respuesta.code() == 401 && !esRutaDeAcceso(pedido) && volviendoAlLogin.compareAndSet(false, true)) {
                Sesion.cerrar(app);
                Intent login = new Intent(app, LoginActivity.class)
                        .putExtra(LoginActivity.EXTRA_SESION_VENCIDA, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                app.startActivity(login);
            }
            return respuesta;
        };
    }

    private static boolean esRutaDeAcceso(Request pedido) {
        String ruta = pedido.url().encodedPath();
        return ruta.endsWith("/auth/login") || ruta.endsWith("/auth/registro");
    }
}
