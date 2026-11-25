package josue.com.bo.linde.ui.predios;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.EstadoExpediente;
import josue.com.bo.linde.modelo.PredioVista;
import josue.com.bo.linde.ui.alertas.AlertasActivity;
import josue.com.bo.linde.ui.documentos.DocumentosActivity;
import josue.com.bo.linde.ui.envio.EnvioActivity;
import josue.com.bo.linde.ui.expedientes.ExpedientesActivity;
import josue.com.bo.linde.ui.inspecciones.InspeccionesActivity;
import josue.com.bo.linde.ui.poligono.PoligonoActivity;
import josue.com.bo.linde.util.Extras;
import josue.com.bo.linde.util.Formatos;
import josue.com.bo.linde.util.Pantalla;
import retrofit2.Call;

/**
 * Detalle del predio (GET predios/{id}) y acceso a los siete módulos. Cada uno recibe PREDIO_ID, PREDIO_ESTADO
 * (nombre del enum) y PREDIO_HA (superficie declarada). Se recarga en onResume: el estado cambia en Polígono y Envío.
 */
public class PredioDetalleActivity extends AppCompatActivity {

    private int predioId;
    private PredioVista predio;

    private LinearProgressIndicator progreso;
    private View contenido;
    private TextView txtNombre;
    private TextView txtEstado;
    private TextView txtDatos;
    private TextView txtTitulo;
    private TextView txtPoligono;
    private TextView txtError;
    private MaterialButton btnReintentar;
    private MaterialButton[] botones;
    private Call<PredioVista> llamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_predios_detalle);
        Pantalla.ajustarBordes(findViewById(R.id.predios_detalle_raiz));
        MaterialToolbar barra = findViewById(R.id.predios_detalle_barra);
        Pantalla.barra(this, barra, R.string.predios_detalle_titulo, true);

        progreso = findViewById(R.id.predios_detalle_progreso);
        contenido = findViewById(R.id.predios_detalle_contenido);
        txtNombre = findViewById(R.id.predios_detalle_nombre);
        txtEstado = findViewById(R.id.predios_detalle_estado);
        txtDatos = findViewById(R.id.predios_detalle_datos);
        txtTitulo = findViewById(R.id.predios_detalle_titulo_inra);
        txtPoligono = findViewById(R.id.predios_detalle_poligono);
        txtError = findViewById(R.id.predios_detalle_error);
        btnReintentar = findViewById(R.id.predios_detalle_btn_reintentar);

        MaterialButton btnPoligono = findViewById(R.id.predios_detalle_btn_poligono);
        MaterialButton btnDocumentos = findViewById(R.id.predios_detalle_btn_documentos);
        MaterialButton btnEnvio = findViewById(R.id.predios_detalle_btn_envio);
        MaterialButton btnAlertas = findViewById(R.id.predios_detalle_btn_alertas);
        MaterialButton btnInspecciones = findViewById(R.id.predios_detalle_btn_inspecciones);
        MaterialButton btnExpedientes = findViewById(R.id.predios_detalle_btn_expedientes);
        MaterialButton btnEditar = findViewById(R.id.predios_detalle_btn_editar);
        botones = new MaterialButton[]{btnPoligono, btnDocumentos, btnEnvio, btnAlertas, btnInspecciones,
                btnExpedientes, btnEditar};

        btnPoligono.setOnClickListener(v -> abrir(PoligonoActivity.class));
        btnDocumentos.setOnClickListener(v -> abrir(DocumentosActivity.class));
        btnEnvio.setOnClickListener(v -> abrir(EnvioActivity.class));
        btnAlertas.setOnClickListener(v -> abrir(AlertasActivity.class));
        btnInspecciones.setOnClickListener(v -> abrir(InspeccionesActivity.class));
        btnExpedientes.setOnClickListener(v -> abrir(ExpedientesActivity.class));
        btnEditar.setOnClickListener(v -> abrir(PredioFormActivity.class));
        btnReintentar.setOnClickListener(v -> cargar());
        habilitarBotones(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (txtNombre != null) cargar();
    }

    @Override
    protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_predios_detalle, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.predios_menu_eliminar) {
            if (predio != null) EliminarPredio.confirmar(this, predio, this::finish);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargar() {
        if (llamada != null) llamada.cancel();
        txtError.setVisibility(View.GONE);
        btnReintentar.setVisibility(View.GONE);
        llamada = ClienteApi.get(this).obtenerPredio(predioId);
        llamada.enqueue(new LlamadaApi<PredioVista>(this, progreso) {
            @Override
            protected void exito(@Nullable PredioVista p) {
                if (p == null) return;
                predio = p;
                mostrar(p);
            }

            @Override
            protected void error(@NonNull ErrorApi error) {
                if (error.getCodigo() == 404) {
                    // El predio ya no existe (por ejemplo, se eliminó desde la web).
                    Toast.makeText(PredioDetalleActivity.this, error.getError(), Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                txtError.setText(error.getError());
                txtError.setVisibility(View.VISIBLE);
                btnReintentar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void mostrar(PredioVista p) {
        contenido.setVisibility(View.VISIBLE);
        txtNombre.setText(p.getNombre());
        PrediosAdapter.pintarEstado(txtEstado, p);

        StringBuilder datos = new StringBuilder();
        datos.append(getString(R.string.predios_detalle_ubicacion, PrediosAdapter.ubicacion(p)));
        datos.append('\n').append(getString(R.string.predios_detalle_cultivo, texto(p.getCultivo())));
        datos.append('\n').append(getString(R.string.predios_detalle_superficie,
                Formatos.hectareas(p.getSuperficieDeclaradaHa())));
        if (p.getDesmonteDeclaradoHa() != null) {
            datos.append('\n').append(getString(R.string.predios_detalle_desmonte,
                    Formatos.hectareas(p.getDesmonteDeclaradoHa())));
        }
        datos.append('\n').append(getString(R.string.predios_detalle_registrado, Formatos.fecha(p.getFechaRegistro())));
        txtDatos.setText(datos.toString());

        if (p.getNumeroTituloInra() == null || p.getNumeroTituloInra().isEmpty()) {
            txtTitulo.setText(R.string.predios_detalle_sin_titulo);
        } else {
            String titulo = getString(R.string.predios_detalle_titulo_numero, p.getNumeroTituloInra());
            if (p.getTitularCi() != null) titulo += "\n" + getString(R.string.predios_detalle_titular, p.getTitularCi());
            if (p.getSuperficieTituladaHa() != null) {
                titulo += "\n" + getString(R.string.predios_detalle_titulada,
                        Formatos.hectareas(p.getSuperficieTituladaHa()));
            }
            txtTitulo.setText(titulo);
        }
        txtPoligono.setText(PrediosAdapter.textoPoligono(this, p));
        habilitarBotones(true);
    }

    private void abrir(Class<? extends Activity> pantalla) {
        if (predio == null) return;
        EstadoExpediente estado = predio.getEstado() == null ? EstadoExpediente.Registrado : predio.getEstado();
        startActivity(new Intent(this, pantalla)
                .putExtra(Extras.PREDIO_ID, predio.getId())
                .putExtra(Extras.PREDIO_ESTADO, estado.name())
                .putExtra(Extras.PREDIO_HA, predio.getSuperficieDeclaradaHa()));
    }

    private void habilitarBotones(boolean valor) {
        for (MaterialButton b : botones) b.setEnabled(valor);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
