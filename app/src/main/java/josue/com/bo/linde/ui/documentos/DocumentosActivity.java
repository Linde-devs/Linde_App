package josue.com.bo.linde.ui.documentos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import josue.com.bo.linde.R;
import josue.com.bo.linde.api.*;
import josue.com.bo.linde.modelo.*;
import josue.com.bo.linde.util.*;
import okhttp3.MultipartBody;
import retrofit2.Call;

public class DocumentosActivity extends AppCompatActivity {
    private final ExecutorService trabajador = Executors.newSingleThreadExecutor();
    private final List<Documento> documentos = new ArrayList<>();
    private File foto;
    private final ActivityResultLauncher<String> selector = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) subir(uri, null); });
    private final ActivityResultLauncher<Uri> camara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && foto != null) subir(Archivos.uriPara(this, foto), foto);
            });
    private int predioId, seleccionado;
    private LinearLayout lista;
    private TextView estado;
    private View progreso;
    private Button agregar;
    private Call<?> llamada;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) { finish(); return; }
        setContentView(R.layout.activity_documentos);
        Pantalla.ajustarBordes(findViewById(R.id.documentos_raiz));
        Pantalla.barra(this, (MaterialToolbar) findViewById(R.id.documentos_barra), R.string.documentos_titulo, true);
        lista = findViewById(R.id.documentos_lista);
        estado = findViewById(R.id.documentos_estado);
        progreso = findViewById(R.id.documentos_progreso);
        agregar = findViewById(R.id.documentos_agregar);
        agregar.setOnClickListener(v -> formulario());
        findViewById(R.id.documentos_reintentar).setOnClickListener(v -> cargar());
        cargar();
    }

    @Override protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        trabajador.shutdownNow();
        super.onDestroy();
    }

    private void cargar() {
        if (llamada != null) llamada.cancel();
        estado.setVisibility(View.GONE);
        findViewById(R.id.documentos_reintentar).setVisibility(View.GONE);
        Call<List<Documento>> c = ClienteApi.get(this).listarDocumentos(predioId);
        llamada = c;
        c.enqueue(new LlamadaApi<List<Documento>>(this, progreso, agregar) {
            @Override protected void exito(@Nullable List<Documento> datos) {
                documentos.clear();
                if (datos != null) documentos.addAll(datos);
                pintar();
            }
            @Override protected void error(@NonNull ErrorApi e) { errorVisible(e.getError()); }
        });
    }

    private void pintar() {
        lista.removeAllViews();
        estado.setVisibility(documentos.isEmpty() ? View.VISIBLE : View.GONE);
        if (documentos.isEmpty()) estado.setText(R.string.documentos_vacio);
        for (Documento d : documentos) {
            View item = getLayoutInflater().inflate(R.layout.item_documentos, lista, false);
            ((TextView) item.findViewById(R.id.documentos_item_tipo)).setText(Formatos.tipoDocumento(d.getTipo()));
            String detalle = "N.º " + (d.getNumero() == null ? "—" : d.getNumero());
            if (d.getTitularCi() != null) detalle += " · CI " + d.getTitularCi();
            if (d.getSuperficieHa() != null) detalle += " · " + Formatos.hectareas(d.getSuperficieHa());
            ((TextView) item.findViewById(R.id.documentos_item_detalle)).setText(detalle);
            String archivo = d.getArchivoNombre() == null ? "Sin archivo" : "Archivo: " + d.getArchivoNombre()
                    + (d.getArchivoBytes() == null ? "" : String.format(Locale.getDefault(), ", %.1f MB", d.getArchivoBytes() / 1048576.0));
            ((TextView) item.findViewById(R.id.documentos_item_archivo)).setText(archivo);
            item.findViewById(R.id.documentos_item_subir).setOnClickListener(v -> opciones(d.getId()));
            item.findViewById(R.id.documentos_item_eliminar).setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(this).setTitle("Eliminar documento")
                            .setMessage("¿Eliminar este documento y su archivo?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar", (dialog, which) -> eliminar(d.getId())).show());
            lista.addView(item);
        }
    }

    private void formulario() {
        List<TipoDocumento> tipos = new ArrayList<>();
        boolean titulo = false;
        for (Documento d : documentos) if (d.getTipo() == TipoDocumento.TituloPropiedad) titulo = true;
        if (!titulo) tipos.add(TipoDocumento.TituloPropiedad);
        tipos.add(TipoDocumento.PlanOrdenamientoPredial);
        tipos.add(TipoDocumento.PlanDesmonte);
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (24 * getResources().getDisplayMetrics().density);
        caja.setPadding(p, 8, p, 0);
        Spinner tipo = new Spinner(this);
        List<String> nombres = new ArrayList<>();
        for (TipoDocumento t : tipos) nombres.add(Formatos.tipoDocumento(t));
        tipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));
        caja.addView(tipo);
        EditText numero = campo(caja, "Número del documento");
        EditText ci = campo(caja, "CI del titular (solo título)");
        EditText superficie = campo(caja, "Superficie titulada en ha (solo título)");
        superficie.setInputType(8194);
        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Agregar documento").setView(caja).setNegativeButton("Cancelar", null)
                .setPositiveButton("Agregar", null).create();
        dialogo.setOnShowListener(v -> dialogo.getButton(-1).setOnClickListener(b -> {
            TipoDocumento t = tipos.get(tipo.getSelectedItemPosition());
            String n = t == TipoDocumento.TituloPropiedad
                    ? Validaciones.normalizarTitulo(numero.getText().toString())
                    : Validaciones.opcional(numero.getText().toString());
            String titular = t == TipoDocumento.TituloPropiedad ? Validaciones.opcional(ci.getText().toString()) : null;
            Double ha = t == TipoDocumento.TituloPropiedad ? Validaciones.numero(superficie.getText().toString()) : null;
            if (t == TipoDocumento.TituloPropiedad && !Validaciones.tituloValido(n)) {
                numero.setError(Validaciones.MENSAJE_TITULO); return;
            }
            if (t == TipoDocumento.TituloPropiedad && (titular == null || !titular.matches("\\d{5,10}") || ha == null || ha <= 0)) {
                ci.setError("Revisa el CI y la superficie titulada."); return;
            }
            dialogo.dismiss();
            Call<Documento> c = ClienteApi.get(this).agregarDocumento(predioId, new DocumentoDto(t, n, titular, ha));
            llamada = c;
            c.enqueue(new LlamadaApi<Documento>(this, progreso, agregar) {
                @Override protected void exito(@Nullable Documento d) { cargar(); }
                @Override protected void error(@NonNull ErrorApi e) { errorVisible(e.getError()); }
            });
        }));
        dialogo.show();
    }

    private EditText campo(LinearLayout caja, String pista) {
        EditText campo = new EditText(this);
        campo.setHint(pista);
        campo.setSingleLine(true);
        caja.addView(campo);
        return campo;
    }

    private void eliminar(int id) {
        Call<Void> c = ClienteApi.get(this).eliminarDocumento(id);
        llamada = c;
        c.enqueue(new LlamadaApi<Void>(this, progreso, agregar) {
            @Override protected void exito(@Nullable Void vacio) { cargar(); }
            @Override protected void error(@NonNull ErrorApi e) { errorVisible(e.getError()); }
        });
    }

    private void opciones(int id) {
        seleccionado = id;
        new MaterialAlertDialogBuilder(this).setTitle("Subir archivo")
                .setItems(new String[]{"Elegir PDF o imagen", "Tomar foto"}, (d, opcion) -> {
                    if (opcion == 0) selector.launch("*/*");
                    else try {
                        foto = Archivos.crearArchivoFoto(this);
                        camara.launch(Archivos.uriPara(this, foto));
                    } catch (IOException e) { errorVisible(e.getMessage()); }
                }).show();
    }

    private void subir(Uri uri, @Nullable File local) {
        String nombre = local == null ? Archivos.nombreReal(this, uri) : local.getName();
        String ext = Archivos.extension(nombre);
        if (!Arrays.asList(".pdf", ".jpg", ".jpeg", ".png").contains(ext)) {
            errorVisible("Elige un archivo PDF, JPG o PNG."); return;
        }
        long tamano = local == null ? Archivos.tamano(this, uri) : local.length();
        if (tamano > Archivos.MAXIMO_BYTES) { errorVisible("El archivo pesa más de 10 MB."); return; }
        progreso.setVisibility(View.VISIBLE);
        agregar.setEnabled(false);
        int id = seleccionado;
        trabajador.execute(() -> {
            try {
                MultipartBody.Part parte = local == null ? Archivos.parte(this, uri, "archivo")
                        : Archivos.parte(local, "archivo", "image/jpeg");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Call<Documento> c = ClienteApi.get(this).subirArchivoDocumento(id, parte);
                    llamada = c;
                    c.enqueue(new LlamadaApi<Documento>(this, progreso, agregar) {
                        @Override protected void exito(@Nullable Documento d) { cargar(); }
                        @Override protected void error(@NonNull ErrorApi e) { errorVisible(e.getError()); }
                    });
                });
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) {
                    progreso.setVisibility(View.GONE); agregar.setEnabled(true); errorVisible(e.getMessage());
                }});
            }
        });
    }

    private void errorVisible(String mensaje) {
        String m = mensaje == null ? "No se pudo completar la operación." : mensaje;
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
        estado.setText(m);
        estado.setVisibility(View.VISIBLE);
        findViewById(R.id.documentos_reintentar).setVisibility(View.VISIBLE);
    }
}
