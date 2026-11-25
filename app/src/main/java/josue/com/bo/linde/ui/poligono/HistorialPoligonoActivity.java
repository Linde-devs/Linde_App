package josue.com.bo.linde.ui.poligono;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.modelo.PoligonoVista;
import josue.com.bo.linde.util.Extras;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Historial de versiones del polígono (solo consulta). La vigente se dibuja en verde; al tocar una versión anterior
 * se dibuja encima en gris punteado para ver qué lindero se movió. Recibe Extras.PREDIO_ID.
 */
public class HistorialPoligonoActivity extends AppCompatActivity {

    private int predioId;

    private View raiz;
    private MapView mapa;
    private LinearProgressIndicator progreso;
    private TextView txtNota;
    private TextView txtEstado;
    private MaterialButton btnReintentar;
    private RecyclerView lista;
    private HistorialAdapter adaptador;

    private Overlay capaVigente;
    private Overlay capaComparada;
    private PoligonoVista vigente;
    private Call<List<PoligonoVista>> llamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaUtil.configurar(this);
        setContentView(R.layout.activity_poligono_historial);

        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) {
            Toast.makeText(this, R.string.poligono_sin_predio, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        raiz = findViewById(R.id.poligono_historial_raiz);
        mapa = findViewById(R.id.poligono_historial_mapa);
        progreso = findViewById(R.id.poligono_historial_progreso);
        txtNota = findViewById(R.id.poligono_historial_nota);
        txtEstado = findViewById(R.id.poligono_historial_estado);
        btnReintentar = findViewById(R.id.poligono_historial_btn_reintentar);
        lista = findViewById(R.id.poligono_historial_lista);
        TextView txtTitulo = findViewById(R.id.poligono_historial_titulo);

        MapaUtil.ajustarBordes(raiz);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.poligono_historial_titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            txtTitulo.setVisibility(View.GONE);
        }

        MapaUtil.preparar(mapa);
        adaptador = new HistorialAdapter(this::elegir);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        btnReintentar.setOnClickListener(v -> cargar());

        cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
    }

    @Override
    protected void onPause() {
        mapa.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        if (mapa != null) mapa.onDetach();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** GET predios/{id}/poligono/historial: la API las entrega de la más nueva a la más antigua. */
    private void cargar() {
        progreso.setVisibility(View.VISIBLE);
        txtEstado.setVisibility(View.GONE);
        btnReintentar.setVisibility(View.GONE);
        llamada = ClienteApi.get(this).historialPoligono(predioId);
        llamada.enqueue(new Callback<List<PoligonoVista>>() {
            @Override
            public void onResponse(@NonNull Call<List<PoligonoVista>> call,
                                   @NonNull Response<List<PoligonoVista>> respuesta) {
                if (cerrada()) return;
                progreso.setVisibility(View.GONE);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    mostrar(respuesta.body());
                } else {
                    mostrarError(ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PoligonoVista>> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                progreso.setVisibility(View.GONE);
                mostrarError(getString(R.string.poligono_sin_conexion));
            }
        });
    }

    private void mostrar(List<PoligonoVista> versiones) {
        adaptador.poner(versiones);
        if (versiones.isEmpty()) {
            txtEstado.setText(R.string.poligono_historial_vacio);
            txtEstado.setTextColor(MapaUtil.COLOR_PRIMARIO);
            txtEstado.setVisibility(View.VISIBLE);
            return;
        }
        vigente = versiones.get(0);
        for (PoligonoVista p : versiones) {
            if (p.isVigente()) {
                vigente = p;
                break;
            }
        }
        if (capaVigente != null) mapa.getOverlays().remove(capaVigente);
        capaVigente = null;
        if (vigente.getVertices() != null && !vigente.getVertices().isEmpty()) {
            capaVigente = MapaUtil.crearCapa(mapa, vigente.getVertices(), MapaUtil.COLOR_ACCION, false,
                    getString(R.string.poligono_historial_version_vigente, vigente.getVersion()));
            mapa.getOverlays().add(capaVigente);
            MapaUtil.centrar(mapa, MapaUtil.puntos(vigente.getVertices()));
        }
        adaptador.marcar(vigente.getId());
        txtNota.setText(R.string.poligono_historial_ayuda);
        mapa.invalidate();
    }

    private void elegir(PoligonoVista version) {
        if (capaComparada != null) mapa.getOverlays().remove(capaComparada);
        capaComparada = null;

        if (version.isVigente() || version.getVertices() == null || version.getVertices().isEmpty()) {
            adaptador.marcar(version.getId());
            txtNota.setText(R.string.poligono_historial_ayuda);
            if (vigente != null && vigente.getVertices() != null) {
                MapaUtil.centrar(mapa, MapaUtil.puntos(vigente.getVertices()));
            }
            mapa.invalidate();
            return;
        }

        capaComparada = MapaUtil.crearCapa(mapa, version.getVertices(), MapaUtil.COLOR_GRIS, true,
                getString(R.string.poligono_version_titulo, version.getVersion()));
        mapa.getOverlays().add(capaComparada);
        adaptador.marcar(version.getId());
        txtNota.setText(getString(R.string.poligono_historial_comparada, version.getVersion(),
                version.getMotivo() == null ? "" : version.getMotivo()));

        // Centra en las dos versiones para ver la diferencia completa.
        List<GeoPoint> ambas = new ArrayList<>(MapaUtil.puntos(version.getVertices()));
        if (vigente != null) ambas.addAll(MapaUtil.puntos(vigente.getVertices()));
        MapaUtil.centrar(mapa, ambas);
        mapa.invalidate();
    }

    private void mostrarError(String mensaje) {
        txtEstado.setText(mensaje);
        txtEstado.setTextColor(MapaUtil.COLOR_ERROR);
        txtEstado.setVisibility(View.VISIBLE);
        btnReintentar.setVisibility(View.VISIBLE);
    }

    private boolean cerrada() {
        return isFinishing() || isDestroyed();
    }
}
