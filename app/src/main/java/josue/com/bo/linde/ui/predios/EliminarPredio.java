package josue.com.bo.linde.ui.predios;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.EstadoExpediente;
import josue.com.bo.linde.modelo.PredioVista;

/** Confirmación y DELETE predios/{id} (204 sin cuerpo). Lo usan la lista y el detalle. */
final class EliminarPredio {

    private EliminarPredio() {
    }

    static void confirmar(Activity actividad, PredioVista predio, Runnable alEliminar) {
        String mensaje = actividad.getString(R.string.predios_eliminar_mensaje, predio.getNombre());
        if (predio.getEstado() == EstadoExpediente.Aprobado || predio.getEstado() == EstadoExpediente.ConAlertas) {
            mensaje += " " + actividad.getString(R.string.predios_eliminar_con_expediente);
        }
        new MaterialAlertDialogBuilder(actividad)
                .setTitle(R.string.predios_eliminar_titulo)
                .setMessage(mensaje)
                .setPositiveButton(R.string.comun_eliminar, (d, w) -> eliminar(actividad, predio, alEliminar))
                .setNegativeButton(R.string.comun_cancelar, null)
                .show();
    }

    private static void eliminar(Activity actividad, PredioVista predio, Runnable alEliminar) {
        ClienteApi.get(actividad).eliminarPredio(predio.getId()).enqueue(new LlamadaApi<Void>(actividad, null) {
            @Override
            protected void exito(@Nullable Void nada) {
                Toast.makeText(actividad, R.string.predios_eliminado, Toast.LENGTH_SHORT).show();
                alEliminar.run();
            }
        });
    }
}
