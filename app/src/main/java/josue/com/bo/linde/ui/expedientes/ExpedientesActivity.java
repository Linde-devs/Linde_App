package josue.com.bo.linde.ui.expedientes;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LindeApi;
import josue.com.bo.linde.modelo.AlertaExpediente;
import josue.com.bo.linde.modelo.EstadoInforme;
import josue.com.bo.linde.modelo.Expediente;
import josue.com.bo.linde.modelo.InspeccionExpediente;
import josue.com.bo.linde.modelo.ResultadoCapa;
import josue.com.bo.linde.ui.poligono.MapaUtil;
import josue.com.bo.linde.util.Extras;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CU-10. Expedientes del productor: lista, detalle, generación y descarga del GeoJSON.
 * La lista y el detalle están en la misma pantalla (el manifest no se toca).
 * Recibe, opcionales, Extras.PREDIO_ID y Extras.PREDIO_ESTADO (para filtrar y generar) y Extras.CODIGO (abre el detalle).
 */
public class ExpedientesActivity extends AppCompatActivity {

    private static final String ESTADO_CODIGO = "expedientes_codigo_abierto";
    private static final String ESTADO_APROBADO = "Aprobado";
    private static final String ESTADO_CON_ALERTAS = "ConAlertas";
    private static final String TIPO_GEOJSON = "application/geo+json";

    private int predioId;
    private String predioEstado;

    private View raiz;
    private TextView txtTitulo;
    private LinearProgressIndicator progreso;
    private View vistaLista;
    private ScrollView vistaDetalle;
    private View panelGenerar;
    private TextView txtGenerar;
    private MaterialButton btnGenerar;
    private TextView txtEstadoLista;
    private MaterialButton btnReintentar;
    private ExpedientesAdapter adaptador;

    private TextView detCodigo;
    private TextView detFecha;
    private TextView detEstado;
    private TextView detMensajeEstado;
    private TextView detDatos;
    private MapView detMapa;
    private View detTituloAlertas;
    private LinearLayout detAlertas;
    private LinearLayout detValidaciones;
    private LinearLayout detDocumentos;
    private View detTituloInspecciones;
    private LinearLayout detInspecciones;
    private TextView detLeyenda;
    private MaterialButton btnDescargar;
    private MaterialButton btnCompartir;

    private final List<Call<?>> llamadas = new ArrayList<>();
    private final ExecutorService archivos = Executors.newSingleThreadExecutor();
    private OnBackPressedCallback volverALista;

    private boolean ocupado;
    private boolean hayExpedienteDelPredio;
    private Expediente abierto;
    private Overlay capaDetalle;
    private byte[] geojsonPendiente;
    private Uri archivoGuardado;

    private final ActivityResultLauncher<String> crearArchivo = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(TIPO_GEOJSON), this::archivoCreado);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaUtil.configurar(this);
        setContentView(R.layout.activity_expedientes);

        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        predioEstado = getIntent().getStringExtra(Extras.PREDIO_ESTADO);

        enlazarVistas();
        MapaUtil.ajustarBordes(raiz);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.expedientes_titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            txtTitulo.setVisibility(View.GONE);
        }

        RecyclerView lista = findViewById(R.id.expedientes_lista);
        adaptador = new ExpedientesAdapter(e -> abrirDetalle(e.getCodigo()));
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        MapaUtil.preparar(detMapa);
        permitirGestosDelMapa();

        volverALista = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                mostrarLista();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, volverALista);

        btnGenerar.setOnClickListener(v -> generar());
        btnReintentar.setOnClickListener(v -> cargarLista());
        btnDescargar.setOnClickListener(v -> descargarGeoJson());
        btnCompartir.setOnClickListener(v -> compartir());

        configurarPanelGenerar();
        cargarLista();

        String codigo = savedInstanceState != null
                ? savedInstanceState.getString(ESTADO_CODIGO)
                : getIntent().getStringExtra(Extras.CODIGO);
        if (codigo != null && !codigo.isEmpty()) abrirDetalle(codigo);
    }

    private void enlazarVistas() {
        raiz = findViewById(R.id.expedientes_raiz);
        txtTitulo = findViewById(R.id.expedientes_titulo);
        progreso = findViewById(R.id.expedientes_progreso);
        vistaLista = findViewById(R.id.expedientes_vista_lista);
        vistaDetalle = findViewById(R.id.expedientes_vista_detalle);
        panelGenerar = findViewById(R.id.expedientes_panel_generar);
        txtGenerar = findViewById(R.id.expedientes_texto_generar);
        btnGenerar = findViewById(R.id.expedientes_btn_generar);
        txtEstadoLista = findViewById(R.id.expedientes_estado_lista);
        btnReintentar = findViewById(R.id.expedientes_btn_reintentar);

        detCodigo = findViewById(R.id.expedientes_det_codigo);
        detFecha = findViewById(R.id.expedientes_det_fecha);
        detEstado = findViewById(R.id.expedientes_det_estado);
        detMensajeEstado = findViewById(R.id.expedientes_det_mensaje_estado);
        detDatos = findViewById(R.id.expedientes_det_datos);
        detMapa = findViewById(R.id.expedientes_det_mapa);
        detTituloAlertas = findViewById(R.id.expedientes_det_titulo_alertas);
        detAlertas = findViewById(R.id.expedientes_det_alertas);
        detValidaciones = findViewById(R.id.expedientes_det_validaciones);
        detDocumentos = findViewById(R.id.expedientes_det_documentos);
        detTituloInspecciones = findViewById(R.id.expedientes_det_titulo_inspecciones);
        detInspecciones = findViewById(R.id.expedientes_det_inspecciones);
        detLeyenda = findViewById(R.id.expedientes_det_leyenda);
        btnDescargar = findViewById(R.id.expedientes_btn_descargar);
        btnCompartir = findViewById(R.id.expedientes_btn_compartir);
    }

    /** El mapa está dentro de un ScrollView: mientras se toca el mapa, el ScrollView no se mueve. */
    @SuppressLint("ClickableViewAccessibility")
    private void permitirGestosDelMapa() {
        detMapa.setOnTouchListener((v, evento) -> {
            int accion = evento.getActionMasked();
            boolean tocando = accion == MotionEvent.ACTION_DOWN || accion == MotionEvent.ACTION_MOVE;
            v.getParent().requestDisallowInterceptTouchEvent(tocando);
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        detMapa.onResume();
    }

    @Override
    protected void onPause() {
        detMapa.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (Call<?> llamada : llamadas) llamada.cancel();
        archivos.shutdownNow();
        if (detMapa != null) detMapa.onDetach();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        if (abierto != null && vistaDetalle.getVisibility() == View.VISIBLE) {
            estado.putString(ESTADO_CODIGO, abierto.getCodigo());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // ---------------------------------------------------------------- lista y generación

    private boolean hayPredio() {
        return predioId > 0;
    }

    private void configurarPanelGenerar() {
        if (!hayPredio()) {
            panelGenerar.setVisibility(View.GONE);
            return;
        }
        panelGenerar.setVisibility(View.VISIBLE);
        if (ESTADO_APROBADO.equals(predioEstado)) {
            txtGenerar.setText(R.string.expedientes_generar_aprobado);
        } else if (ESTADO_CON_ALERTAS.equals(predioEstado)) {
            txtGenerar.setText(R.string.expedientes_generar_con_alertas);
        } else {
            txtGenerar.setText(R.string.expedientes_generar_no_permitido);
        }
        actualizarBotones();
    }

    private boolean sePuedeGenerar() {
        return hayPredio() && (ESTADO_APROBADO.equals(predioEstado) || ESTADO_CON_ALERTAS.equals(predioEstado));
    }

    /** GET expedientes: la API ya devuelve solo los del productor; aquí se filtra por predio si llegó PREDIO_ID. */
    private void cargarLista() {
        ponerOcupado(true);
        txtEstadoLista.setVisibility(View.GONE);
        btnReintentar.setVisibility(View.GONE);
        Call<List<Expediente>> llamada = api().listarExpedientes();
        llamadas.add(llamada);
        llamada.enqueue(new Callback<List<Expediente>>() {
            @Override
            public void onResponse(@NonNull Call<List<Expediente>> call, @NonNull Response<List<Expediente>> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    mostrarLista(respuesta.body());
                } else {
                    mostrarErrorLista(ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Expediente>> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarErrorLista(getString(R.string.expedientes_sin_conexion));
            }
        });
    }

    private void mostrarLista(List<Expediente> todos) {
        List<Expediente> filtrados = new ArrayList<>();
        for (Expediente e : todos) {
            if (!hayPredio() || e.getPredioId() == predioId) filtrados.add(e);
        }
        // Más nuevos primero (las fechas ISO-8601 se ordenan bien como texto).
        Collections.sort(filtrados, (a, b) -> texto(b.getFechaGeneracion()).compareTo(texto(a.getFechaGeneracion())));
        adaptador.poner(filtrados);

        hayExpedienteDelPredio = hayPredio() && !filtrados.isEmpty();
        btnGenerar.setText(hayExpedienteDelPredio ? R.string.expedientes_volver_a_generar : R.string.expedientes_generar);

        if (filtrados.isEmpty()) {
            txtEstadoLista.setText(hayPredio() ? R.string.expedientes_vacio_predio : R.string.expedientes_vacio);
            txtEstadoLista.setTextColor(MapaUtil.COLOR_PRIMARIO);
            txtEstadoLista.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarErrorLista(String mensaje) {
        txtEstadoLista.setText(mensaje);
        txtEstadoLista.setTextColor(MapaUtil.COLOR_ERROR);
        txtEstadoLista.setVisibility(View.VISIBLE);
        btnReintentar.setVisibility(View.VISIBLE);
    }

    /** POST expedientes/predio/{id}. La API decide si se puede (409 con el motivo si no). */
    private void generar() {
        if (!sePuedeGenerar()) return;
        ponerOcupado(true);
        Call<Expediente> llamada = api().generarExpediente(predioId);
        llamadas.add(llamada);
        llamada.enqueue(new Callback<Expediente>() {
            @Override
            public void onResponse(@NonNull Call<Expediente> call, @NonNull Response<Expediente> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    Expediente e = respuesta.body();
                    cargarLista();
                    mostrarDetalle(e);
                    Snackbar.make(raiz, getString(R.string.expedientes_generado, e.getCodigo()), Snackbar.LENGTH_LONG).show();
                } else {
                    mostrarError(getString(R.string.expedientes_no_generado), ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Expediente> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarError(getString(R.string.expedientes_no_generado), getString(R.string.expedientes_sin_conexion));
            }
        });
    }

    // ---------------------------------------------------------------- detalle

    /** GET expedientes/{codigo}. */
    private void abrirDetalle(String codigo) {
        ponerOcupado(true);
        Call<Expediente> llamada = api().obtenerExpediente(codigo);
        llamadas.add(llamada);
        llamada.enqueue(new Callback<Expediente>() {
            @Override
            public void onResponse(@NonNull Call<Expediente> call, @NonNull Response<Expediente> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    mostrarDetalle(respuesta.body());
                } else {
                    mostrarError(getString(R.string.expedientes_no_abierto), ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Expediente> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarError(getString(R.string.expedientes_no_abierto), getString(R.string.expedientes_sin_conexion));
            }
        });
    }

    private void mostrarDetalle(Expediente e) {
        abierto = e;
        archivoGuardado = null;
        geojsonPendiente = null;

        detCodigo.setText(getString(R.string.expedientes_det_titulo, e.getCodigo()));
        detFecha.setText(getString(R.string.expedientes_generado_el, MapaUtil.fecha(e.getFechaGeneracion())));
        detEstado.setText(TextosExpediente.estado(e.getEstado()));
        detEstado.setTextColor(TextosExpediente.colorEstado(e.getEstado()));
        boolean conAlertas = e.getEstado() == EstadoInforme.ConAlertas;
        detMensajeEstado.setText(conAlertas ? R.string.expedientes_mensaje_con_alertas : R.string.expedientes_mensaje_conforme);
        detMensajeEstado.setBackgroundColor(conAlertas ? 0xFFFFF3E0 : 0xFFE8F5E9);

        detDatos.setText(getString(R.string.expedientes_det_datos,
                texto(e.getProductor()), texto(e.getCiProductor()), texto(e.getPredio()),
                texto(e.getDepartamento()), texto(e.getMunicipio()),
                MapaUtil.hectareas(e.getSuperficieDeclaradaHa()), MapaUtil.hectareas(e.getAreaPoligonoHa()),
                e.getVersionPoligono()));

        if (capaDetalle != null) detMapa.getOverlays().remove(capaDetalle);
        capaDetalle = null;
        if (e.getVertices() != null && !e.getVertices().isEmpty()) {
            capaDetalle = MapaUtil.crearCapa(detMapa, e.getVertices(), MapaUtil.COLOR_PRIMARIO, false, e.getPredio());
            detMapa.getOverlays().add(capaDetalle);
            MapaUtil.centrar(detMapa, MapaUtil.puntos(e.getVertices()));
        }
        detMapa.invalidate();

        detAlertas.removeAllViews();
        List<AlertaExpediente> alertas = e.getAlertas();
        boolean hayAlertas = alertas != null && !alertas.isEmpty();
        detTituloAlertas.setVisibility(hayAlertas ? View.VISIBLE : View.GONE);
        detAlertas.setVisibility(hayAlertas ? View.VISIBLE : View.GONE);
        if (hayAlertas) {
            for (AlertaExpediente a : alertas) {
                agregarLinea(detAlertas, getString(R.string.expedientes_linea_alerta,
                        TextosExpediente.prioridad(a.getPrioridad()), TextosExpediente.capa(a.getCapa()), texto(a.getMensaje())),
                        TextosExpediente.colorPrioridad(a.getPrioridad()));
            }
        }

        detValidaciones.removeAllViews();
        List<ResultadoCapa> capas = e.getValidaciones();
        if (capas == null || capas.isEmpty()) {
            agregarLinea(detValidaciones, getString(R.string.expedientes_sin_validaciones), MapaUtil.COLOR_GRIS);
        } else {
            for (ResultadoCapa c : capas) {
                agregarLinea(detValidaciones, getString(R.string.expedientes_linea_capa,
                        TextosExpediente.capa(c.getCapa()), TextosExpediente.estadoCapa(c.getEstado()), texto(c.getMensaje())),
                        TextosExpediente.colorCapa(c.getEstado()));
            }
        }

        detDocumentos.removeAllViews();
        List<String> documentos = e.getDocumentos();
        if (documentos == null || documentos.isEmpty()) {
            agregarLinea(detDocumentos, getString(R.string.expedientes_sin_documentos), MapaUtil.COLOR_GRIS);
        } else {
            for (String d : documentos) agregarLinea(detDocumentos, "• " + TextosExpediente.documento(d), 0xFF163330);
        }

        detInspecciones.removeAllViews();
        List<InspeccionExpediente> inspecciones = e.getInspecciones();
        boolean hayInspecciones = inspecciones != null && !inspecciones.isEmpty();
        detTituloInspecciones.setVisibility(hayInspecciones ? View.VISIBLE : View.GONE);
        detInspecciones.setVisibility(hayInspecciones ? View.VISIBLE : View.GONE);
        if (hayInspecciones) {
            for (InspeccionExpediente i : inspecciones) {
                String linea = getString(R.string.expedientes_linea_inspeccion, MapaUtil.fechaCorta(i.getFecha()),
                        texto(i.getTecnico()), TextosExpediente.resultado(i.getResultado()));
                if (i.getComentario() != null && !i.getComentario().isEmpty()) linea += "\n" + i.getComentario();
                agregarLinea(detInspecciones, linea, 0xFF163330);
            }
        }

        detLeyenda.setText(getString(R.string.expedientes_leyenda, e.getCodigo()));

        vistaLista.setVisibility(View.GONE);
        vistaDetalle.setVisibility(View.VISIBLE);
        vistaDetalle.scrollTo(0, 0);
        volverALista.setEnabled(true);
        actualizarBotones();
    }

    private void mostrarLista() {
        vistaDetalle.setVisibility(View.GONE);
        vistaLista.setVisibility(View.VISIBLE);
        volverALista.setEnabled(false);
        abierto = null;
    }

    private void agregarLinea(LinearLayout contenedor, String texto, int color) {
        TextView linea = new TextView(this);
        linea.setText(texto);
        linea.setTextColor(color);
        linea.setTextSize(14);
        int espacio = MapaUtil.dp(this, 4);
        linea.setPadding(0, espacio, 0, espacio);
        contenedor.addView(linea);
    }

    // ---------------------------------------------------------------- GeoJSON

    /**
     * GET expedientes/{codigo}/geojson (con token). Primero se descarga y luego se pide dónde guardarlo,
     * así nunca queda un archivo vacío si la descarga falla.
     */
    private void descargarGeoJson() {
        if (abierto == null) return;
        String codigo = abierto.getCodigo();
        ponerOcupado(true);
        Call<ResponseBody> llamada = api().descargarGeoJson(codigo);
        llamadas.add(llamada);
        llamada.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                ResponseBody cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    mostrarError(getString(R.string.expedientes_no_descargado), ErrorApi.de(respuesta).getError());
                    return;
                }
                try {
                    // Sin @Streaming Retrofit ya tiene el cuerpo en memoria: leerlo aquí no usa la red.
                    geojsonPendiente = cuerpo.bytes();
                } catch (IOException ex) {
                    mostrarError(getString(R.string.expedientes_no_descargado), getString(R.string.expedientes_sin_conexion));
                    return;
                } finally {
                    cuerpo.close();
                }
                crearArchivo.launch(codigo + ".geojson");
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarError(getString(R.string.expedientes_no_descargado), getString(R.string.expedientes_sin_conexion));
            }
        });
    }

    /** El usuario eligió dónde guardar (sin permiso de almacenamiento). */
    private void archivoCreado(@Nullable Uri destino) {
        byte[] datos = geojsonPendiente;
        geojsonPendiente = null;
        if (destino == null || datos == null) return;
        ponerOcupado(true);
        archivos.execute(() -> {
            boolean bien;
            try (OutputStream salida = getContentResolver().openOutputStream(destino, "wt")) {
                if (salida == null) throw new IOException("Sin destino");
                salida.write(datos);
                bien = true;
            } catch (IOException | SecurityException | IllegalArgumentException ex) {
                bien = false;
            }
            final boolean guardado = bien;
            runOnUiThread(() -> {
                if (cerrada()) return;
                ponerOcupado(false);
                if (!guardado) {
                    mostrarError(getString(R.string.expedientes_no_descargado), getString(R.string.expedientes_no_escrito));
                    return;
                }
                archivoGuardado = destino;
                actualizarBotones();
                Snackbar.make(raiz, R.string.expedientes_guardado, Snackbar.LENGTH_LONG)
                        .setAction(R.string.expedientes_compartir, v -> compartir())
                        .show();
            });
        });
    }

    private void compartir() {
        if (archivoGuardado == null) return;
        Intent enviar = new Intent(Intent.ACTION_SEND);
        enviar.setType(TIPO_GEOJSON);
        enviar.putExtra(Intent.EXTRA_STREAM, archivoGuardado);
        enviar.setClipData(ClipData.newRawUri("", archivoGuardado));
        enviar.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(enviar, getString(R.string.expedientes_compartir_con)));
    }

    // ---------------------------------------------------------------- comunes

    private LindeApi api() {
        return ClienteApi.get(this);
    }

    private boolean cerrada() {
        return isFinishing() || isDestroyed();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private void ponerOcupado(boolean valor) {
        ocupado = valor;
        progreso.setVisibility(valor ? View.VISIBLE : View.GONE);
        actualizarBotones();
    }

    private void actualizarBotones() {
        btnGenerar.setEnabled(!ocupado && sePuedeGenerar());
        btnDescargar.setEnabled(!ocupado && abierto != null);
        btnCompartir.setEnabled(!ocupado && archivoGuardado != null);
    }

    private void mostrarError(String titulo, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(R.string.expedientes_entendido, null)
                .show();
    }
}
