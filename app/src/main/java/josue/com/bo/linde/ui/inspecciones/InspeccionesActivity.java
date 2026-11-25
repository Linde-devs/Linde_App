package josue.com.bo.linde.ui.inspecciones;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.InspeccionCampo;
import josue.com.bo.linde.modelo.InspeccionDto;
import josue.com.bo.linde.modelo.ResultadoInspeccion;
import josue.com.bo.linde.util.Extras;
import josue.com.bo.linde.util.Formatos;
import josue.com.bo.linde.util.Pantalla;
import josue.com.bo.linde.util.Validaciones;
import retrofit2.Call;

/**
 * Inspecciones de campo del predio (RF-14): listar, registrar y eliminar. Registrarlas no cambia el estado del predio.
 * Recibe Extras.PREDIO_ID.
 */
public class InspeccionesActivity extends AppCompatActivity {

    private static final List<ResultadoInspeccion> RESULTADOS = Arrays.asList(
            ResultadoInspeccion.Conforme, ResultadoInspeccion.ConObservaciones, ResultadoInspeccion.NoConforme);

    private int predioId;
    private View raiz;
    private LinearProgressIndicator progreso;
    private TextView txtEstado;
    private MaterialButton btnReintentar;
    private ExtendedFloatingActionButton btnRegistrar;
    private InspeccionesAdapter adaptador;
    private Call<List<InspeccionCampo>> llamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_inspecciones);
        raiz = findViewById(R.id.inspecciones_raiz);
        Pantalla.ajustarBordes(raiz);
        MaterialToolbar barra = findViewById(R.id.inspecciones_barra);
        Pantalla.barra(this, barra, R.string.inspecciones_titulo, true);

        progreso = findViewById(R.id.inspecciones_progreso);
        txtEstado = findViewById(R.id.inspecciones_estado);
        btnReintentar = findViewById(R.id.inspecciones_btn_reintentar);
        btnRegistrar = findViewById(R.id.inspecciones_btn_registrar);
        RecyclerView lista = findViewById(R.id.inspecciones_lista);

        adaptador = new InspeccionesAdapter(this::confirmarEliminar);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        btnReintentar.setOnClickListener(v -> cargar());
        btnRegistrar.setOnClickListener(v -> abrirFormulario());
        cargar();
    }

    @Override
    protected void onDestroy() {
        if (llamada != null) llamada.cancel();
        super.onDestroy();
    }

    /** GET predios/{id}/inspecciones, la más reciente primero. */
    private void cargar() {
        if (llamada != null) llamada.cancel();
        txtEstado.setVisibility(View.GONE);
        btnReintentar.setVisibility(View.GONE);
        llamada = ClienteApi.get(this).listarInspecciones(predioId);
        llamada.enqueue(new LlamadaApi<List<InspeccionCampo>>(this, progreso, btnRegistrar) {
            @Override
            protected void exito(@Nullable List<InspeccionCampo> respuesta) {
                List<InspeccionCampo> datos = respuesta == null ? new ArrayList<>() : new ArrayList<>(respuesta);
                Collections.sort(datos, (a, b) -> {
                    int porFecha = texto(b.getFecha()).compareTo(texto(a.getFecha()));
                    return porFecha != 0 ? porFecha : texto(b.getFechaRegistro()).compareTo(texto(a.getFechaRegistro()));
                });
                adaptador.poner(datos);
                if (datos.isEmpty()) {
                    txtEstado.setText(R.string.inspecciones_vacio);
                    txtEstado.setTextColor(getColor(R.color.linde_texto));
                    txtEstado.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void error(@NonNull ErrorApi error) {
                txtEstado.setText(error.getError());
                txtEstado.setTextColor(getColor(R.color.linde_error));
                txtEstado.setVisibility(View.VISIBLE);
                btnReintentar.setVisibility(View.VISIBLE);
            }
        });
    }

    // ------------------------------------------------------------------ registrar

    private void abrirFormulario() {
        View vista = LayoutInflater.from(this).inflate(R.layout.item_inspecciones_formulario, null, false);
        TextInputLayout capaFecha = vista.findViewById(R.id.inspecciones_form_capa_fecha);
        TextInputLayout capaTecnico = vista.findViewById(R.id.inspecciones_form_capa_tecnico);
        TextInputLayout capaResultado = vista.findViewById(R.id.inspecciones_form_capa_resultado);
        TextInputLayout capaComentario = vista.findViewById(R.id.inspecciones_form_capa_comentario);
        TextInputEditText txtFecha = vista.findViewById(R.id.inspecciones_form_fecha);
        TextInputEditText txtTecnico = vista.findViewById(R.id.inspecciones_form_tecnico);
        MaterialAutoCompleteTextView txtResultado = vista.findViewById(R.id.inspecciones_form_resultado);
        TextInputEditText txtComentario = vista.findViewById(R.id.inspecciones_form_comentario);
        TextView txtMensaje = vista.findViewById(R.id.inspecciones_form_mensaje);
        LinearProgressIndicator progresoForm = vista.findViewById(R.id.inspecciones_form_progreso);

        Map<String, TextInputLayout> capas = new HashMap<>();
        capas.put("fecha", capaFecha);
        capas.put("tecnico", capaTecnico);
        capas.put("resultado", capaResultado);
        capas.put("comentario", capaComentario);

        // Fecha: hoy por defecto, nunca posterior a hoy. Se envía como «aaaa-mm-dd».
        final String[] fecha = {Formatos.hoyAaaaMmDd()};
        txtFecha.setText(Formatos.fechaCorta(fecha[0]));
        View.OnClickListener elegirFecha = v -> {
            CalendarConstraints limites = new CalendarConstraints.Builder()
                    .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                    .setValidator(DateValidatorPointBackward.now())
                    .build();
            MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.inspecciones_form_fecha)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(limites)
                    .build();
            selector.addOnPositiveButtonClickListener(milis -> {
                SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                formato.setTimeZone(TimeZone.getTimeZone("UTC"));
                fecha[0] = formato.format(new Date(milis));
                txtFecha.setText(Formatos.fechaCorta(fecha[0]));
                capaFecha.setError(null);
            });
            selector.show(getSupportFragmentManager(), "inspecciones_fecha");
        };
        txtFecha.setOnClickListener(elegirFecha);
        capaFecha.setEndIconOnClickListener(elegirFecha);

        // Resultado: Conforme por defecto; el comentario es obligatorio si no es Conforme.
        final ResultadoInspeccion[] resultado = {ResultadoInspeccion.Conforme};
        List<String> nombres = new ArrayList<>();
        for (ResultadoInspeccion r : RESULTADOS) nombres.add(Formatos.resultadoInspeccion(r));
        txtResultado.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        txtResultado.setText(nombres.get(0), false);
        capaComentario.setHint(R.string.inspecciones_form_comentario_opcional);
        txtResultado.setOnItemClickListener((p, v, posicion, id) -> {
            resultado[0] = RESULTADOS.get(posicion);
            capaComentario.setHint(resultado[0] == ResultadoInspeccion.Conforme
                    ? R.string.inspecciones_form_comentario_opcional
                    : R.string.inspecciones_form_comentario);
        });

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.inspecciones_registrar)
                .setView(vista)
                .setPositiveButton(R.string.inspecciones_form_guardar, null)
                .setNegativeButton(R.string.comun_cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            for (TextInputLayout capa : capas.values()) capa.setError(null);
            txtMensaje.setVisibility(View.GONE);
            String tecnico = Validaciones.limpio(texto(txtTecnico));
            String comentario = Validaciones.limpio(texto(txtComentario));
            Map<String, String> errores = Validaciones.validarInspeccion(fecha[0], tecnico,
                    resultado[0] == ResultadoInspeccion.Conforme, comentario);
            if (!errores.isEmpty()) {
                for (Map.Entry<String, String> e : errores.entrySet()) {
                    TextInputLayout capa = capas.get(e.getKey());
                    if (capa != null) capa.setError(e.getValue());
                }
                return;
            }
            guardar(new InspeccionDto(fecha[0], tecnico, resultado[0], Validaciones.opcional(comentario)),
                    dialogo, capas, txtMensaje, progresoForm);
        }));
        dialogo.show();
    }

    /** POST predios/{id}/inspecciones. La API revalida (por ejemplo, el comentario si no es Conforme). */
    private void guardar(InspeccionDto datos, AlertDialog dialogo, Map<String, TextInputLayout> capas,
                         TextView txtMensaje, LinearProgressIndicator progresoForm) {
        View btnGuardar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        ClienteApi.get(this).registrarInspeccion(predioId, datos)
                .enqueue(new LlamadaApi<InspeccionCampo>(this, progresoForm, btnGuardar) {
                    @Override
                    protected void exito(@Nullable InspeccionCampo nueva) {
                        dialogo.dismiss();
                        Snackbar.make(raiz, R.string.inspecciones_registrada, Snackbar.LENGTH_LONG).show();
                        cargar();
                    }

                    @Override
                    protected void error(@NonNull ErrorApi error) {
                        if (!error.marcarCampo(capas)) {
                            txtMensaje.setText(error.getError());
                            txtMensaje.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    // ------------------------------------------------------------------ eliminar

    private void confirmarEliminar(InspeccionCampo inspeccion) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.inspecciones_eliminar_titulo)
                .setMessage(getString(R.string.inspecciones_eliminar_mensaje,
                        Formatos.fechaCorta(inspeccion.getFecha()), texto(inspeccion.getTecnico())))
                .setPositiveButton(R.string.comun_eliminar, (d, w) -> eliminar(inspeccion))
                .setNegativeButton(R.string.comun_cancelar, null)
                .show();
    }

    /** DELETE inspecciones/{id} (204 sin cuerpo). */
    private void eliminar(InspeccionCampo inspeccion) {
        ClienteApi.get(this).eliminarInspeccion(inspeccion.getId())
                .enqueue(new LlamadaApi<Void>(this, progreso, btnRegistrar) {
                    @Override
                    protected void exito(@Nullable Void nada) {
                        Snackbar.make(raiz, R.string.inspecciones_eliminada, Snackbar.LENGTH_SHORT).show();
                        cargar();
                    }
                });
    }

    private static String texto(@Nullable Object valor) {
        if (valor instanceof TextView) {
            CharSequence t = ((TextView) valor).getText();
            return t == null ? "" : t.toString();
        }
        return valor == null ? "" : valor.toString();
    }
}
