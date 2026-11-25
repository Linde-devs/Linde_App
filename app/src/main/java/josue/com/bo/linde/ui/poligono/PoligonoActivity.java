package josue.com.bo.linde.ui.poligono;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LindeApi;
import josue.com.bo.linde.modelo.Coordenada;
import josue.com.bo.linde.modelo.CoordenadaDto;
import josue.com.bo.linde.modelo.PoligonoVista;
import josue.com.bo.linde.modelo.VerticesDto;
import josue.com.bo.linde.util.Extras;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CU-04. Polígono del predio: muestra el vigente, permite dibujarlo tocando el mapa, capturarlo caminando con el GPS
 * o importarlo desde un archivo. La API valida la geometría; aquí solo se avisa antes de enviar.
 * Recibe Extras.PREDIO_ID, Extras.PREDIO_ESTADO y Extras.PREDIO_HA.
 */
public class PoligonoActivity extends AppCompatActivity {

    private static final String ESTADO_LON = "poligono_lon";
    private static final String ESTADO_LAT = "poligono_lat";
    private static final String ESTADO_PRECISION = "poligono_precision";
    private static final String ESTADO_PREDIO = "poligono_estado_predio";
    private static final String ESTADO_CAMBIO = "poligono_hubo_cambio";

    private static final double PRECISION_BUENA_M = 10.0;
    private static final double HA_MAXIMA_PUNTO = 4.0;
    private static final long MAX_BYTES_ARCHIVO = 10L * 1024 * 1024;
    private static final String ESTADO_REGISTRADO = "Registrado";
    private static final String MENSAJE_SIN_POLIGONO = "no tiene polígono";

    /** Un vértice marcado: precisionM es null si se tocó en el mapa. */
    private static final class Vertice {
        final double lon;
        final double lat;
        final Double precisionM;

        Vertice(double lon, double lat, Double precisionM) {
            this.lon = lon;
            this.lat = lat;
            this.precisionM = precisionM;
        }
    }

    private int predioId;
    private String predioEstado;
    private double predioHa;

    private View raiz;
    private MapView mapa;
    private LinearProgressIndicator progreso;
    private TextView txtResumen;
    private TextView txtAvisoEstado;
    private View panelPermiso;
    private View panelGpsApagado;
    private TextView txtPrecision;
    private TextView txtPuntos;
    private MaterialButton btnGps;
    private MaterialButton btnGuardar;
    private MaterialButton btnDeshacer;
    private MaterialButton btnBorrar;
    private MaterialButton btnImportar;
    private MaterialButton btnHistorial;
    private MaterialButton btnEditarActual;

    private final List<Vertice> trazo = new ArrayList<>();
    private final List<Overlay> capasTrazo = new ArrayList<>();
    private final List<Call<?>> llamadas = new ArrayList<>();
    private final ExecutorService archivos = Executors.newSingleThreadExecutor();

    private Overlay capaVigente;
    private PoligonoVista vigente;
    private boolean cargaTerminada;
    private boolean ocupado;
    private boolean buscandoGps;
    private boolean escuchandoPrecision;
    private boolean hayPermiso;
    private boolean huboCambio;

    private FusedLocationProviderClient ubicacion;
    private CancellationTokenSource cancelacionGps;
    private OnBackPressedCallback confirmarSalida;

    private final LocationCallback escuchaPrecision = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            Location ultima = resultado.getLastLocation();
            if (ultima != null) mostrarPrecision(ultima);
        }
    };

    private final ActivityResultLauncher<String[]> pedirPermiso = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                hayPermiso = tienePermiso();
                actualizarGps();
                if (hayPermiso && cargaTerminada && vigente == null && trazo.isEmpty()) centrarEnUbicacion();
            });

    private final ActivityResultLauncher<String> elegirArchivo = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::archivoElegido);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaUtil.configurar(this);
        setContentView(R.layout.activity_poligono);

        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        predioEstado = getIntent().getStringExtra(Extras.PREDIO_ESTADO);
        predioHa = getIntent().getDoubleExtra(Extras.PREDIO_HA, -1);
        if (predioId <= 0) {
            Toast.makeText(this, R.string.poligono_sin_predio, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        raiz = findViewById(R.id.poligono_raiz);
        mapa = findViewById(R.id.poligono_mapa);
        progreso = findViewById(R.id.poligono_progreso);
        txtResumen = findViewById(R.id.poligono_resumen);
        txtAvisoEstado = findViewById(R.id.poligono_aviso_estado);
        panelPermiso = findViewById(R.id.poligono_panel_permiso);
        panelGpsApagado = findViewById(R.id.poligono_panel_gps_apagado);
        txtPrecision = findViewById(R.id.poligono_precision);
        txtPuntos = findViewById(R.id.poligono_puntos);
        btnGps = findViewById(R.id.poligono_btn_gps);
        btnGuardar = findViewById(R.id.poligono_btn_guardar);
        btnDeshacer = findViewById(R.id.poligono_btn_deshacer);
        btnBorrar = findViewById(R.id.poligono_btn_borrar);
        btnImportar = findViewById(R.id.poligono_btn_importar);
        btnHistorial = findViewById(R.id.poligono_btn_historial);
        btnEditarActual = findViewById(R.id.poligono_btn_editar_actual);
        TextView txtTitulo = findViewById(R.id.poligono_titulo);

        MapaUtil.ajustarBordes(raiz);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.poligono_titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            txtTitulo.setVisibility(View.GONE);
        }

        MapaUtil.preparar(mapa);
        // Va primero en la lista: recibe el toque solo si ninguna otra capa lo usó.
        mapa.getOverlays().add(0, new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (ocupado) return false;
                agregarVertice(p.getLongitude(), p.getLatitude(), null);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        ubicacion = LocationServices.getFusedLocationProviderClient(this);

        confirmarSalida = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                preguntarDescartar();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, confirmarSalida);

        btnGps.setOnClickListener(v -> agregarPuntoGps());
        btnGuardar.setOnClickListener(v -> guardar());
        btnDeshacer.setOnClickListener(v -> deshacer());
        btnBorrar.setOnClickListener(v -> preguntarBorrarTodo());
        btnImportar.setOnClickListener(v -> importar());
        btnHistorial.setOnClickListener(v -> abrirHistorial());
        btnEditarActual.setOnClickListener(v -> partirDelVigente());
        findViewById(R.id.poligono_btn_permitir).setOnClickListener(v -> volverAPedirPermiso());
        findViewById(R.id.poligono_btn_ajustes_gps).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));

        if (savedInstanceState != null) restaurar(savedInstanceState);
        mostrarAvisoEstado();
        redibujarTrazo();
        if (!trazo.isEmpty()) MapaUtil.centrar(mapa, puntosTrazo());

        hayPermiso = tienePermiso();
        if (!hayPermiso && savedInstanceState == null) {
            pedirPermiso.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        cargarVigente();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
        hayPermiso = tienePermiso();
        actualizarGps();
    }

    @Override
    protected void onPause() {
        mapa.onPause();
        detenerPrecision();
        if (cancelacionGps != null) cancelacionGps.cancel();
        terminarBusquedaGps();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (Call<?> llamada : llamadas) llamada.cancel();
        archivos.shutdownNow();
        if (mapa != null) mapa.onDetach();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        double[] lon = new double[trazo.size()];
        double[] lat = new double[trazo.size()];
        double[] precision = new double[trazo.size()];
        for (int i = 0; i < trazo.size(); i++) {
            Vertice v = trazo.get(i);
            lon[i] = v.lon;
            lat[i] = v.lat;
            precision[i] = v.precisionM == null ? Double.NaN : v.precisionM;
        }
        estado.putDoubleArray(ESTADO_LON, lon);
        estado.putDoubleArray(ESTADO_LAT, lat);
        estado.putDoubleArray(ESTADO_PRECISION, precision);
        estado.putString(ESTADO_PREDIO, predioEstado);
        estado.putBoolean(ESTADO_CAMBIO, huboCambio);
    }

    private void restaurar(Bundle estado) {
        double[] lon = estado.getDoubleArray(ESTADO_LON);
        double[] lat = estado.getDoubleArray(ESTADO_LAT);
        double[] precision = estado.getDoubleArray(ESTADO_PRECISION);
        if (lon != null && lat != null && precision != null) {
            for (int i = 0; i < lon.length; i++) {
                trazo.add(new Vertice(lon[i], lat[i], Double.isNaN(precision[i]) ? null : precision[i]));
            }
        }
        predioEstado = estado.getString(ESTADO_PREDIO, predioEstado);
        huboCambio = estado.getBoolean(ESTADO_CAMBIO, false);
        if (huboCambio) avisarCambioAlLlamador();
    }

    // ---------------------------------------------------------------- Fase 1: polígono vigente

    private void cargarVigente() {
        ponerOcupado(true);
        Call<PoligonoVista> llamada = api().obtenerPoligono(predioId);
        llamadas.add(llamada);
        llamada.enqueue(new Callback<PoligonoVista>() {
            @Override
            public void onResponse(@NonNull Call<PoligonoVista> call, @NonNull Response<PoligonoVista> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                cargaTerminada = true;
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    mostrarVigente(respuesta.body(), true);
                    return;
                }
                String error = ErrorApi.de(respuesta).getError();
                if (respuesta.code() == 404 && error != null && error.contains(MENSAJE_SIN_POLIGONO)) {
                    // No es un error para el productor: la pantalla queda lista para capturar.
                    mostrarVigente(null, false);
                    if (trazo.isEmpty()) centrarEnUbicacion();
                } else {
                    mostrarErrorCarga(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PoligonoVista> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarErrorCarga(getString(R.string.poligono_sin_conexion));
            }
        });
    }

    private void mostrarVigente(@Nullable PoligonoVista poligono, boolean centrar) {
        vigente = poligono;
        if (capaVigente != null) mapa.getOverlays().remove(capaVigente);
        capaVigente = null;

        List<Coordenada> vertices = poligono == null ? null : poligono.getVertices();
        if (vertices == null || vertices.isEmpty()) {
            txtResumen.setText(R.string.poligono_vacio);
            txtResumen.setTextColor(MapaUtil.COLOR_PRIMARIO);
        } else {
            String titulo = getString(R.string.poligono_version_titulo, poligono.getVersion());
            capaVigente = MapaUtil.crearCapa(mapa, vertices, MapaUtil.COLOR_PRIMARIO, false, titulo);
            // Justo encima de la capa de toques y debajo de los puntos nuevos.
            mapa.getOverlays().add(1, capaVigente);
            txtResumen.setText(resumen(poligono));
            txtResumen.setTextColor(MapaUtil.COLOR_PRIMARIO);
            if (centrar && trazo.isEmpty()) MapaUtil.centrar(mapa, MapaUtil.puntos(vertices));
        }
        actualizarBotones();
        mapa.invalidate();
    }

    private String resumen(PoligonoVista p) {
        String superficie = p.isEsPunto() || p.getAreaHa() == null
                ? getString(R.string.poligono_es_punto)
                : MapaUtil.hectareas(p.getAreaHa());
        String texto = getString(R.string.poligono_resumen, p.getVersion(), superficie, MapaUtil.fecha(p.getFecha()));
        if (p.getMotivo() != null && !p.getMotivo().isEmpty()) {
            texto += "\n" + getString(R.string.poligono_resumen_motivo, p.getMotivo());
        }
        int conGps = 0;
        if (p.getPrecisionesM() != null) {
            for (Double m : p.getPrecisionesM()) if (m != null) conGps++;
        }
        if (conGps > 0) texto += "\n" + getString(R.string.poligono_resumen_gps, conGps);
        return texto;
    }

    private void mostrarErrorCarga(String mensaje) {
        txtResumen.setText(mensaje);
        txtResumen.setTextColor(MapaUtil.COLOR_ERROR);
        Snackbar.make(raiz, mensaje, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.poligono_reintentar, v -> cargarVigente())
                .show();
    }

    @SuppressLint("MissingPermission")
    private void centrarEnUbicacion() {
        if (!tienePermiso()) return;
        try {
            ubicacion.getLastLocation().addOnSuccessListener(this, ultima -> {
                if (ultima == null || vigente != null || !trazo.isEmpty()) return;
                mapa.getController().setZoom(17.0);
                mapa.getController().setCenter(new GeoPoint(ultima.getLatitude(), ultima.getLongitude()));
            });
        } catch (SecurityException e) {
            hayPermiso = false;
        }
    }

    private void mostrarAvisoEstado() {
        if (predioEstado != null && !ESTADO_REGISTRADO.equals(predioEstado)) {
            txtAvisoEstado.setText(getString(R.string.poligono_aviso_estado, MapaUtil.estadoPredio(predioEstado)));
            txtAvisoEstado.setVisibility(View.VISIBLE);
        } else {
            txtAvisoEstado.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------- Fase 2: dibujo en el mapa

    private void agregarVertice(double lon, double lat, @Nullable Double precisionM) {
        trazo.add(new Vertice(MapaUtil.seisDecimales(lon), MapaUtil.seisDecimales(lat), precisionM));
        redibujarTrazo();
    }

    private void deshacer() {
        if (trazo.isEmpty()) return;
        trazo.remove(trazo.size() - 1);
        redibujarTrazo();
    }

    private void preguntarBorrarTodo() {
        if (trazo.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.poligono_borrar_titulo)
                .setMessage(getString(R.string.poligono_borrar_mensaje, trazo.size()))
                .setPositiveButton(R.string.poligono_borrar_si, (d, w) -> {
                    trazo.clear();
                    redibujarTrazo();
                })
                .setNegativeButton(R.string.poligono_cancelar, null)
                .show();
    }

    /** Copia los vértices del polígono vigente para corregirlos (sin el punto de cierre). */
    private void partirDelVigente() {
        if (vigente == null || vigente.getVertices() == null || vigente.getVertices().isEmpty()) return;
        List<Coordenada> v = vigente.getVertices();
        List<Double> precisiones = vigente.getPrecisionesM();
        // precisionesM puede venir vacía (archivo o dibujo): solo se usa si es paralela a los vértices.
        boolean paralela = precisiones != null && precisiones.size() == v.size();
        int cantidad = v.size();
        if (cantidad > 1 && mismoPunto(v.get(0), v.get(cantidad - 1))) cantidad--;
        trazo.clear();
        for (int i = 0; i < cantidad; i++) {
            trazo.add(new Vertice(v.get(i).getLon(), v.get(i).getLat(), paralela ? precisiones.get(i) : null));
        }
        redibujarTrazo();
        Snackbar.make(raiz, R.string.poligono_editando_actual, Snackbar.LENGTH_LONG).show();
    }

    private static boolean mismoPunto(Coordenada a, Coordenada b) {
        return a.getLon() == b.getLon() && a.getLat() == b.getLat();
    }

    private void redibujarTrazo() {
        for (Overlay capa : capasTrazo) mapa.getOverlays().remove(capa);
        capasTrazo.clear();

        if (trazo.size() >= 2) {
            List<GeoPoint> linea = puntosTrazo();
            if (trazo.size() >= 3) linea.add(linea.get(0));
            Polyline union = new Polyline(mapa);
            union.setPoints(linea);
            union.getOutlinePaint().setColor(MapaUtil.COLOR_INFO);
            union.getOutlinePaint().setStrokeWidth(MapaUtil.dp(this, 3));
            union.setOnClickListener((p, m, posicion) -> false);
            mapa.getOverlays().add(union);
            capasTrazo.add(union);
        }

        for (int i = 0; i < trazo.size(); i++) {
            Vertice v = trazo.get(i);
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(v.lat, v.lon));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marcador.setIcon(MapaUtil.iconoNumerado(this, i + 1, v.precisionM != null));
            marcador.setTitle(getString(R.string.poligono_punto_titulo, i + 1,
                    v.precisionM == null ? getString(R.string.poligono_manual) : MapaUtil.metros(v.precisionM)));
            mapa.getOverlays().add(marcador);
            capasTrazo.add(marcador);
        }

        if (trazo.isEmpty()) {
            txtPuntos.setText(R.string.poligono_instrucciones);
        } else {
            int conGps = 0;
            for (Vertice v : trazo) if (v.precisionM != null) conGps++;
            txtPuntos.setText(getString(R.string.poligono_puntos_marcados, trazo.size(), conGps));
        }
        confirmarSalida.setEnabled(!trazo.isEmpty());
        actualizarBotones();
        mapa.invalidate();
    }

    private List<GeoPoint> puntosTrazo() {
        List<GeoPoint> lista = new ArrayList<>();
        for (Vertice v : trazo) lista.add(new GeoPoint(v.lat, v.lon));
        return lista;
    }

    private void guardar() {
        int cantidad = trazo.size();
        if (cantidad == 0) {
            avisar(getString(R.string.poligono_sin_puntos));
            return;
        }
        if (cantidad == 1 && !(predioHa > 0 && predioHa <= HA_MAXIMA_PUNTO)) {
            avisar(getString(R.string.poligono_punto_no_permitido));
            return;
        }
        if (cantidad == 2 || (cantidad >= 3 && puntosDistintos() < 3)) {
            avisar(getString(R.string.poligono_minimo_tres));
            return;
        }
        boolean conGps = false;
        for (Vertice v : trazo) if (v.precisionM != null) conGps = true;
        pedirMotivo(getString(conGps ? R.string.poligono_motivo_gps : R.string.poligono_motivo_mapa));
    }

    private int puntosDistintos() {
        Set<String> distintos = new HashSet<>();
        for (Vertice v : trazo) distintos.add(v.lon + "," + v.lat);
        return distintos.size();
    }

    private void pedirMotivo(String porDefecto) {
        EditText campo = new EditText(this);
        campo.setText(porDefecto);
        campo.setSelectAllOnFocus(true);
        campo.setSingleLine(true);
        campo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        FrameLayout contenedor = new FrameLayout(this);
        int margen = MapaUtil.dp(this, 20);
        contenedor.setPadding(margen, MapaUtil.dp(this, 8), margen, 0);
        contenedor.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.poligono_motivo_titulo)
                .setView(contenedor)
                .setPositiveButton(R.string.poligono_guardar, (d, w) -> {
                    String motivo = campo.getText().toString().trim();
                    enviar(motivo.isEmpty() ? porDefecto : motivo);
                })
                .setNegativeButton(R.string.poligono_cancelar, null)
                .show();
    }

    /** PUT predios/{id}/poligono. Con 3 o más puntos se repite el primero al final para cerrar el anillo. */
    private void enviar(String motivo) {
        List<CoordenadaDto> lista = new ArrayList<>();
        for (Vertice v : trazo) lista.add(new CoordenadaDto(v.lon, v.lat, v.precisionM));
        if (trazo.size() >= 3) {
            Vertice primero = trazo.get(0);
            lista.add(new CoordenadaDto(primero.lon, primero.lat, primero.precisionM));
        }

        ponerOcupado(true);
        Call<PoligonoVista> llamada = api().guardarPoligono(predioId, new VerticesDto(lista, motivo));
        llamadas.add(llamada);
        llamada.enqueue(new Callback<PoligonoVista>() {
            @Override
            public void onResponse(@NonNull Call<PoligonoVista> call, @NonNull Response<PoligonoVista> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    PoligonoVista p = respuesta.body();
                    guardado(p, getString(p.isEsPunto() ? R.string.poligono_guardado_punto : R.string.poligono_guardado,
                            p.getVersion()));
                } else {
                    // Los puntos se conservan para que el productor los corrija.
                    mostrarError(getString(R.string.poligono_no_guardado), ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PoligonoVista> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarError(getString(R.string.poligono_no_guardado), getString(R.string.poligono_sin_conexion));
            }
        });
    }

    private void guardado(PoligonoVista poligono, String mensaje) {
        trazo.clear();
        redibujarTrazo();
        mostrarVigente(poligono, true);
        huboCambio = true;
        if (predioEstado != null && !ESTADO_REGISTRADO.equals(predioEstado)) {
            mensaje += " " + getString(R.string.poligono_volvio_registrado);
            predioEstado = ESTADO_REGISTRADO;
            mostrarAvisoEstado();
        }
        avisarCambioAlLlamador();
        Snackbar aviso = Snackbar.make(raiz, mensaje, Snackbar.LENGTH_LONG);
        aviso.setTextMaxLines(4);
        aviso.show();
    }

    /** La pantalla que abrió esta (detalle del predio) recarga el predio y su nuevo estado. */
    private void avisarCambioAlLlamador() {
        setResult(RESULT_OK, new Intent().putExtra(Extras.PREDIO_ESTADO, predioEstado));
    }

    private void preguntarDescartar() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.poligono_descartar_titulo)
                .setMessage(getString(R.string.poligono_descartar_mensaje, trazo.size()))
                .setPositiveButton(R.string.poligono_descartar_si, (d, w) -> {
                    confirmarSalida.setEnabled(false);
                    finish();
                })
                .setNegativeButton(R.string.poligono_seguir, null)
                .show();
    }

    // ---------------------------------------------------------------- Fase 3: captura con GPS

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean gpsEncendido() {
        LocationManager gestor = (LocationManager) getSystemService(LOCATION_SERVICE);
        return gestor != null && LocationManagerCompat.isLocationEnabled(gestor);
    }

    private void volverAPedirPermiso() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            pedirPermiso.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            // El usuario marcó «No volver a preguntar»: solo se puede activar desde los ajustes de la app.
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        }
    }

    private void actualizarGps() {
        if (!hayPermiso) {
            panelPermiso.setVisibility(View.VISIBLE);
            panelGpsApagado.setVisibility(View.GONE);
            txtPrecision.setVisibility(View.GONE);
            detenerPrecision();
        } else if (!gpsEncendido()) {
            panelPermiso.setVisibility(View.GONE);
            panelGpsApagado.setVisibility(View.VISIBLE);
            txtPrecision.setVisibility(View.GONE);
            detenerPrecision();
        } else {
            panelPermiso.setVisibility(View.GONE);
            panelGpsApagado.setVisibility(View.GONE);
            txtPrecision.setVisibility(View.VISIBLE);
            if (!escuchandoPrecision) {
                txtPrecision.setText(R.string.poligono_buscando_senal);
                txtPrecision.setTextColor(MapaUtil.COLOR_AVISO);
            }
            iniciarPrecision();
        }
        actualizarBotones();
    }

    /** Precisión en vivo, cada 1 a 2 segundos, solo mientras la pantalla está visible. */
    @SuppressLint("MissingPermission")
    private void iniciarPrecision() {
        if (escuchandoPrecision || !tienePermiso()) return;
        LocationRequest pedido = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500)
                .setMinUpdateIntervalMillis(1000)
                .build();
        try {
            ubicacion.requestLocationUpdates(pedido, escuchaPrecision, Looper.getMainLooper());
            escuchandoPrecision = true;
        } catch (SecurityException e) {
            hayPermiso = false;
        }
    }

    private void detenerPrecision() {
        if (!escuchandoPrecision) return;
        ubicacion.removeLocationUpdates(escuchaPrecision);
        escuchandoPrecision = false;
    }

    private void mostrarPrecision(Location lugar) {
        if (!lugar.hasAccuracy()) {
            txtPrecision.setText(R.string.poligono_precision_desconocida);
            txtPrecision.setTextColor(MapaUtil.COLOR_AVISO);
            return;
        }
        float metros = lugar.getAccuracy();
        String texto = getString(R.string.poligono_precision, MapaUtil.metros(metros));
        if (metros <= PRECISION_BUENA_M) {
            txtPrecision.setText(texto);
            txtPrecision.setTextColor(MapaUtil.COLOR_ACCION);
        } else {
            txtPrecision.setText(texto + " · " + getString(R.string.poligono_precision_espera));
            txtPrecision.setTextColor(MapaUtil.COLOR_AVISO);
        }
    }

    @SuppressLint("MissingPermission")
    private void agregarPuntoGps() {
        if (!tienePermiso()) {
            hayPermiso = false;
            actualizarGps();
            return;
        }
        if (!gpsEncendido()) {
            actualizarGps();
            return;
        }
        buscandoGps = true;
        btnGps.setText(R.string.poligono_buscando_gps);
        actualizarBotones();
        cancelacionGps = new CancellationTokenSource();
        try {
            ubicacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacionGps.getToken())
                    .addOnSuccessListener(this, lugar -> {
                        terminarBusquedaGps();
                        if (lugar == null) {
                            avisar(getString(R.string.poligono_gps_sin_ubicacion));
                            return;
                        }
                        Double precision = null;
                        if (lugar.hasAccuracy()) {
                            double redondeada = Math.round(lugar.getAccuracy() * 10) / 10.0;
                            if (redondeada > 0) precision = redondeada;
                        }
                        agregarVertice(lugar.getLongitude(), lugar.getLatitude(), precision);
                        if (mapa.getZoomLevelDouble() < 17) mapa.getController().setZoom(18.0);
                        mapa.getController().animateTo(new GeoPoint(lugar.getLatitude(), lugar.getLongitude()));
                    })
                    .addOnFailureListener(this, e -> {
                        terminarBusquedaGps();
                        avisar(getString(R.string.poligono_gps_sin_ubicacion));
                    });
        } catch (SecurityException e) {
            terminarBusquedaGps();
            hayPermiso = false;
            actualizarGps();
        }
    }

    private void terminarBusquedaGps() {
        buscandoGps = false;
        if (btnGps != null) btnGps.setText(R.string.poligono_agregar_punto);
        actualizarBotones();
    }

    // ---------------------------------------------------------------- Fase 4: importar archivo

    private void importar() {
        if (trazo.isEmpty()) {
            elegirArchivo.launch("*/*");
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.poligono_importar)
                .setMessage(R.string.poligono_importar_descarta)
                .setPositiveButton(R.string.poligono_continuar, (d, w) -> elegirArchivo.launch("*/*"))
                .setNegativeButton(R.string.poligono_cancelar, null)
                .show();
    }

    private void archivoElegido(@Nullable Uri uri) {
        if (uri == null) return;
        String nombre = nombreReal(uri);
        String minuscula = nombre.toLowerCase(Locale.ROOT);
        if (!(minuscula.endsWith(".geojson") || minuscula.endsWith(".json") || minuscula.endsWith(".kml"))) {
            mostrarError(getString(R.string.poligono_no_importado), getString(R.string.poligono_extension_invalida, nombre));
            return;
        }
        String tipo = getContentResolver().getType(uri);
        ponerOcupado(true);
        archivos.execute(() -> {
            byte[] datos = leer(uri);
            runOnUiThread(() -> {
                if (cerrada()) return;
                if (datos == null) {
                    ponerOcupado(false);
                    mostrarError(getString(R.string.poligono_no_importado), getString(R.string.poligono_archivo_ilegible));
                } else {
                    subirArchivo(nombre, tipo, datos);
                }
            });
        });
    }

    /** POST predios/{id}/poligono/importar. La API elige el lector por la extensión del nombre real. */
    private void subirArchivo(String nombre, @Nullable String tipo, byte[] datos) {
        MediaType tipoArchivo = MediaType.parse(tipo != null ? tipo : "application/octet-stream");
        RequestBody cuerpo = RequestBody.create(datos, tipoArchivo);
        MultipartBody.Part archivo = MultipartBody.Part.createFormData("archivo", nombre, cuerpo);
        RequestBody motivo = RequestBody.create(getString(R.string.poligono_motivo_importado),
                MediaType.parse("text/plain; charset=utf-8"));

        Call<PoligonoVista> llamada = api().importarPoligono(predioId, archivo, motivo);
        llamadas.add(llamada);
        llamada.enqueue(new Callback<PoligonoVista>() {
            @Override
            public void onResponse(@NonNull Call<PoligonoVista> call, @NonNull Response<PoligonoVista> respuesta) {
                if (cerrada()) return;
                ponerOcupado(false);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    guardado(respuesta.body(), getString(R.string.poligono_importado, respuesta.body().getVersion()));
                } else {
                    mostrarError(getString(R.string.poligono_no_importado), ErrorApi.de(respuesta).getError());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PoligonoVista> call, @NonNull Throwable t) {
                if (cerrada() || call.isCanceled()) return;
                ponerOcupado(false);
                mostrarError(getString(R.string.poligono_no_importado), getString(R.string.poligono_sin_conexion));
            }
        });
    }

    /** Nombre real del archivo elegido (OpenableColumns.DISPLAY_NAME), con su extensión. */
    private String nombreReal(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String nombre = cursor.getString(0);
                if (nombre != null && !nombre.isEmpty()) return nombre;
            }
        } catch (RuntimeException ignorada) {
            // Se usa el último segmento de la ruta.
        }
        String segmento = uri.getLastPathSegment();
        return segmento != null ? segmento : "archivo";
    }

    @Nullable
    private byte[] leer(Uri uri) {
        try (InputStream entrada = getContentResolver().openInputStream(uri)) {
            if (entrada == null) return null;
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            long total = 0;
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                total += leidos;
                if (total > MAX_BYTES_ARCHIVO) return null;
                salida.write(bloque, 0, leidos);
            }
            return salida.toByteArray();
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- Fase 5: historial

    private void abrirHistorial() {
        startActivity(new Intent(this, HistorialPoligonoActivity.class).putExtra(Extras.PREDIO_ID, predioId));
    }

    // ---------------------------------------------------------------- comunes

    private LindeApi api() {
        return ClienteApi.get(this);
    }

    private boolean cerrada() {
        return isFinishing() || isDestroyed();
    }

    private void ponerOcupado(boolean valor) {
        ocupado = valor;
        progreso.setVisibility(valor ? View.VISIBLE : View.GONE);
        actualizarBotones();
    }

    private void actualizarBotones() {
        if (btnGps == null) return;
        boolean hayPuntos = !trazo.isEmpty();
        btnGps.setEnabled(!ocupado && !buscandoGps && hayPermiso);
        btnGuardar.setEnabled(!ocupado && hayPuntos);
        btnDeshacer.setEnabled(!ocupado && hayPuntos);
        btnBorrar.setEnabled(!ocupado && hayPuntos);
        btnImportar.setEnabled(!ocupado);
        btnHistorial.setEnabled(!ocupado);
        btnEditarActual.setVisibility(vigente != null && !hayPuntos ? View.VISIBLE : View.GONE);
        btnEditarActual.setEnabled(!ocupado);
    }

    private void avisar(String mensaje) {
        Snackbar aviso = Snackbar.make(raiz, mensaje, Snackbar.LENGTH_LONG);
        aviso.setTextMaxLines(4);
        aviso.show();
    }

    private void mostrarError(String titulo, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(R.string.poligono_entendido, null)
                .show();
    }
}
