package josue.com.bo.linde.api;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback común: al crearlo muestra la carga y bloquea los botones; al terminar los libera.
 * No toca la vista si la Activity ya se cerró, ignora las llamadas canceladas y en onFailure informa
 * «Sin conexión con el servidor». Uso:
 * <pre>
 * api.listarPredios().enqueue(new LlamadaApi&lt;List&lt;PredioVista&gt;&gt;(this, progreso, boton) {
 *     protected void exito(List&lt;PredioVista&gt; lista) { ... }
 * });
 * </pre>
 */
public abstract class LlamadaApi<T> implements Callback<T> {

    private final WeakReference<Activity> actividad;
    private final View carga;
    private final View[] bloquear;

    protected LlamadaApi(Activity actividad, @Nullable View carga, View... bloquear) {
        this.actividad = new WeakReference<>(actividad);
        this.carga = carga;
        this.bloquear = bloquear;
        ponerEsperando(true);
    }

    /** Respuesta 2xx. En un 204 (DELETE) el cuerpo es null. */
    protected abstract void exito(@Nullable T respuesta);

    /** Respuesta con error de la API o sin conexión (codigo 0). Por defecto muestra el mensaje tal cual. */
    protected void error(@NonNull ErrorApi error) {
        Activity a = actividad.get();
        if (a != null) Toast.makeText(a, error.getError(), Toast.LENGTH_LONG).show();
    }

    @Override
    public final void onResponse(@NonNull Call<T> llamada, @NonNull Response<T> respuesta) {
        if (!viva()) return;
        ponerEsperando(false);
        if (respuesta.isSuccessful()) {
            exito(respuesta.body());
        } else {
            error(ErrorApi.de(respuesta));
        }
    }

    @Override
    public final void onFailure(@NonNull Call<T> llamada, @NonNull Throwable t) {
        if (llamada.isCanceled() || !viva()) return;
        ponerEsperando(false);
        error(ErrorApi.sinConexion());
    }

    @Nullable
    protected Activity actividad() {
        return actividad.get();
    }

    private boolean viva() {
        Activity a = actividad.get();
        return a != null && !a.isFinishing() && !a.isDestroyed();
    }

    private void ponerEsperando(boolean esperando) {
        if (carga != null) carga.setVisibility(esperando ? View.VISIBLE : View.GONE);
        if (bloquear != null) {
            for (View v : bloquear) if (v != null) v.setEnabled(!esperando);
        }
    }
}
