package josue.com.bo.linde.ui.alertas;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import josue.com.bo.linde.R;
import josue.com.bo.linde.api.*;
import josue.com.bo.linde.modelo.*;
import josue.com.bo.linde.util.*;
import retrofit2.Call;

public class AlertasActivity extends AppCompatActivity {
    private int predioId;
    private LinearLayout lista;
    private TextView estado;
    private View progreso;
    private SwipeRefreshLayout refrescar;
    private Call<List<AlertaVista>> llamada;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) { finish(); return; }
        setContentView(R.layout.activity_alertas);
        Pantalla.ajustarBordes(findViewById(R.id.alertas_raiz));
        Pantalla.barra(this, (MaterialToolbar) findViewById(R.id.alertas_barra), R.string.alertas_titulo, true);
        lista = findViewById(R.id.alertas_lista);
        estado = findViewById(R.id.alertas_estado);
        progreso = findViewById(R.id.alertas_progreso);
        refrescar = findViewById(R.id.alertas_refrescar);
        refrescar.setOnRefreshListener(this::cargar);
        findViewById(R.id.alertas_reintentar).setOnClickListener(v -> cargar());
        cargar();
    }

    @Override protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        super.onDestroy();
    }

    private void cargar() {
        if (llamada != null) llamada.cancel();
        estado.setVisibility(View.GONE);
        findViewById(R.id.alertas_reintentar).setVisibility(View.GONE);
        llamada = ClienteApi.get(this).listarAlertas(predioId);
        llamada.enqueue(new LlamadaApi<List<AlertaVista>>(this, progreso) {
            @Override protected void exito(@Nullable List<AlertaVista> datos) {
                refrescar.setRefreshing(false);
                lista.removeAllViews();
                if (datos == null || datos.isEmpty()) {
                    estado.setText(R.string.alertas_vacio);
                    estado.setVisibility(View.VISIBLE);
                    return;
                }
                for (AlertaVista alerta : datos) {
                    View item = getLayoutInflater().inflate(R.layout.item_alertas, lista, false);
                    TextView prioridad = item.findViewById(R.id.alertas_item_prioridad);
                    prioridad.setText("Prioridad " + Formatos.prioridad(alerta.getPrioridad()));
                    int color = alerta.getPrioridad() == PrioridadAlerta.Alta ? R.color.linde_error
                            : alerta.getPrioridad() == PrioridadAlerta.Media ? R.color.linde_aviso : R.color.linde_gris;
                    prioridad.setTextColor(getColor(color));
                    ((TextView) item.findViewById(R.id.alertas_item_capa)).setText(Formatos.capa(alerta.getCapa()));
                    ((TextView) item.findViewById(R.id.alertas_item_mensaje)).setText(alerta.getMensaje());
                    ((TextView) item.findViewById(R.id.alertas_item_fecha)).setText(Formatos.fechaHora(alerta.getFecha()));
                    lista.addView(item);
                }
            }
            @Override protected void error(@NonNull ErrorApi e) {
                refrescar.setRefreshing(false);
                estado.setText(e.getError());
                estado.setTextColor(getColor(R.color.linde_error));
                estado.setVisibility(View.VISIBLE);
                findViewById(R.id.alertas_reintentar).setVisibility(View.VISIBLE);
            }
        });
    }
}
