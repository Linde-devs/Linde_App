package josue.com.bo.linde.ui.predios;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.PredioVista;
import josue.com.bo.linde.modelo.Usuario;
import josue.com.bo.linde.sesion.Sesion;
import josue.com.bo.linde.ui.login.LoginActivity;
import josue.com.bo.linde.util.Extras;
import josue.com.bo.linde.util.Pantalla;
import retrofit2.Call;

/** Mis predios (GET predios). Se recarga en onResume porque el estado cambia en Polígono y Envío. */
public class PrediosActivity extends AppCompatActivity {

    private SwipeRefreshLayout refrescar;
    private RecyclerView lista;
    private View panelEstado;
    private TextView txtEstado;
    private MaterialButton btnReintentar;
    private PrediosAdapter adaptador;
    private Call<List<PredioVista>> llamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sesion.haySesionVigente(this)) {
            volverAlLogin();
            return;
        }
        setContentView(R.layout.activity_predios);
        Pantalla.ajustarBordes(findViewById(R.id.predios_raiz));
        MaterialToolbar barra = findViewById(R.id.predios_barra);
        Pantalla.barra(this, barra, R.string.predios_titulo, false);
        Usuario usuario = Sesion.usuario(this);
        if (usuario != null && getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(usuario.nombreCompleto());
        }

        refrescar = findViewById(R.id.predios_refrescar);
        lista = findViewById(R.id.predios_lista);
        panelEstado = findViewById(R.id.predios_panel_estado);
        txtEstado = findViewById(R.id.predios_estado);
        btnReintentar = findViewById(R.id.predios_btn_reintentar);
        ExtendedFloatingActionButton btnNuevo = findViewById(R.id.predios_btn_nuevo);

        adaptador = new PrediosAdapter(new PrediosAdapter.Acciones() {
            @Override
            public void abrir(PredioVista predio) {
                startActivity(new Intent(PrediosActivity.this, PredioDetalleActivity.class)
                        .putExtra(Extras.PREDIO_ID, predio.getId()));
            }

            @Override
            public void eliminar(PredioVista predio) {
                EliminarPredio.confirmar(PrediosActivity.this, predio, () -> cargar());
            }
        });
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        refrescar.setColorSchemeResources(R.color.linde_primario);
        refrescar.setOnRefreshListener(this::cargar);
        btnReintentar.setOnClickListener(v -> cargar());
        btnNuevo.setOnClickListener(v -> startActivity(new Intent(this, PredioFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lista != null) cargar();
    }

    @Override
    protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_predios, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.predios_menu_salir) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.predios_salir_titulo)
                    .setMessage(R.string.predios_salir_mensaje)
                    .setPositiveButton(R.string.predios_salir, (d, w) -> {
                        Sesion.cerrar(this);
                        volverAlLogin();
                    })
                    .setNegativeButton(R.string.comun_cancelar, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargar() {
        if (llamada != null) llamada.cancel();
        refrescar.setRefreshing(true);
        btnReintentar.setVisibility(View.GONE);
        llamada = ClienteApi.get(this).listarPredios();
        llamada.enqueue(new LlamadaApi<List<PredioVista>>(this, null) {
            @Override
            protected void exito(@Nullable List<PredioVista> predios) {
                refrescar.setRefreshing(false);
                List<PredioVista> datos = predios == null ? new ArrayList<>() : predios;
                adaptador.poner(datos);
                if (datos.isEmpty()) {
                    mostrarEstado(getString(R.string.predios_vacio), false);
                } else {
                    panelEstado.setVisibility(View.GONE);
                }
            }

            @Override
            protected void error(@NonNull ErrorApi error) {
                refrescar.setRefreshing(false);
                if (adaptador.getItemCount() == 0) {
                    mostrarEstado(error.getError(), true);
                } else {
                    super.error(error);
                }
            }
        });
    }

    private void mostrarEstado(String texto, boolean esError) {
        txtEstado.setText(texto);
        txtEstado.setTextColor(getColor(esError ? R.color.linde_error : R.color.linde_texto));
        btnReintentar.setVisibility(esError ? View.VISIBLE : View.GONE);
        panelEstado.setVisibility(View.VISIBLE);
    }

    private void volverAlLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
