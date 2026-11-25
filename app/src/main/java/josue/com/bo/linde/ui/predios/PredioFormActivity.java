package josue.com.bo.linde.ui.predios;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.EstadoExpediente;
import josue.com.bo.linde.modelo.PredioDto;
import josue.com.bo.linde.modelo.PredioVista;
import josue.com.bo.linde.modelo.Usuario;
import josue.com.bo.linde.sesion.Sesion;
import josue.com.bo.linde.util.Extras;
import josue.com.bo.linde.util.Formatos;
import josue.com.bo.linde.util.Municipios;
import josue.com.bo.linde.util.Pantalla;
import josue.com.bo.linde.util.Validaciones;

/**
 * Crear (POST predios) o editar (PUT predios/{id}) un predio. Para editar llega Extras.PREDIO_ID.
 * Las validaciones solo avisan: la API decide y su error se muestra junto al campo (por ejemplo, título TIT-0214).
 */
public class PredioFormActivity extends AppCompatActivity {

    private static final List<String> CULTIVOS = Arrays.asList("Soya", "Maíz", "Girasol", "Sorgo", "Trigo", "Arroz",
            "Caña de azúcar", "Café", "Cacao", "Chía", "Sésamo", "Frejol");
    private static final String DEPARTAMENTO_INICIAL = "Santa Cruz";
    private static final double HA_PUNTO = 4.0;

    private int predioId;
    private boolean editando;

    private TextView txtAviso;
    private TextView txtMensaje;
    private TextInputEditText txtNombre;
    private MaterialAutoCompleteTextView txtDepartamento;
    private MaterialAutoCompleteTextView txtMunicipio;
    private MaterialAutoCompleteTextView txtCultivo;
    private TextInputEditText txtSuperficie;
    private TextInputEditText txtDesmonte;
    private TextInputEditText txtTitulo;
    private TextInputEditText txtTitularCi;
    private TextInputEditText txtTitulada;
    private TextInputLayout capaSuperficie;
    private MaterialButton btnGuardar;
    private MaterialButton btnCancelar;
    private LinearProgressIndicator progreso;
    private final Map<String, TextInputLayout> capas = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predios_form);
        Pantalla.ajustarBordes(findViewById(R.id.predios_form_raiz));

        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        editando = predioId > 0;
        MaterialToolbar barra = findViewById(R.id.predios_form_barra);
        Pantalla.barra(this, barra, editando ? R.string.predios_form_editar : R.string.predios_form_registrar, true);

        txtAviso = findViewById(R.id.predios_form_aviso);
        txtMensaje = findViewById(R.id.predios_form_mensaje);
        txtNombre = findViewById(R.id.predios_form_nombre);
        txtDepartamento = findViewById(R.id.predios_form_departamento);
        txtMunicipio = findViewById(R.id.predios_form_municipio);
        txtCultivo = findViewById(R.id.predios_form_cultivo);
        txtSuperficie = findViewById(R.id.predios_form_superficie);
        txtDesmonte = findViewById(R.id.predios_form_desmonte);
        txtTitulo = findViewById(R.id.predios_form_titulo);
        txtTitularCi = findViewById(R.id.predios_form_titular_ci);
        txtTitulada = findViewById(R.id.predios_form_titulada);
        capaSuperficie = findViewById(R.id.predios_form_capa_superficie);
        btnGuardar = findViewById(R.id.predios_form_btn_guardar);
        btnCancelar = findViewById(R.id.predios_form_btn_cancelar);
        progreso = findViewById(R.id.predios_form_progreso);

        capas.put("nombre", findViewById(R.id.predios_form_capa_nombre));
        capas.put("departamento", findViewById(R.id.predios_form_capa_departamento));
        capas.put("municipio", findViewById(R.id.predios_form_capa_municipio));
        capas.put("cultivo", findViewById(R.id.predios_form_capa_cultivo));
        capas.put("superficieDeclaradaHa", capaSuperficie);
        capas.put("desmonteDeclaradoHa", findViewById(R.id.predios_form_capa_desmonte));
        capas.put("numeroTituloInra", findViewById(R.id.predios_form_capa_titulo));
        capas.put("titularCi", findViewById(R.id.predios_form_capa_titular_ci));
        capas.put("superficieTituladaHa", findViewById(R.id.predios_form_capa_titulada));

        List<String> departamentos = Municipios.departamentos(this);
        txtDepartamento.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, departamentos));
        txtDepartamento.setOnItemClickListener((p, v, posicion, id) -> ponerMunicipios(departamentos.get(posicion), true));
        txtCultivo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CULTIVOS));
        txtSuperficie.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                actualizarAyudaSuperficie();
            }
        });

        btnGuardar.setText(editando ? R.string.predios_form_guardar_cambios : R.string.predios_form_btn_registrar);
        btnGuardar.setOnClickListener(v -> guardar());
        btnCancelar.setOnClickListener(v -> finish());

        if (editando) {
            if (savedInstanceState == null) cargar();
        } else if (savedInstanceState == null) {
            txtDepartamento.setText(DEPARTAMENTO_INICIAL, false);
            ponerMunicipios(DEPARTAMENTO_INICIAL, false);
            // Como en la web: el CI del titular viene precargado con el del productor.
            Usuario usuario = Sesion.usuario(this);
            if (usuario != null && usuario.getCi() != null) txtTitularCi.setText(usuario.getCi());
        } else {
            ponerMunicipios(Validaciones.limpio(texto(txtDepartamento)), false);
        }
        actualizarAyudaSuperficie();
    }

    private void ponerMunicipios(String departamento, boolean revisarActual) {
        txtMunicipio.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Municipios.de(this, departamento)));
        String actual = Validaciones.limpio(texto(txtMunicipio));
        if (revisarActual && !actual.isEmpty() && Municipios.municipioDe(this, departamento, actual) == null) {
            txtMunicipio.setText("", false);
        }
    }

    private void actualizarAyudaSuperficie() {
        Double ha = Validaciones.numero(texto(txtSuperficie));
        if (ha == null || ha <= 0) {
            capaSuperficie.setHelperText(null);
        } else {
            capaSuperficie.setHelperText(getString(ha <= HA_PUNTO ? R.string.predios_form_ayuda_punto
                    : R.string.predios_form_ayuda_poligono));
        }
    }

    /** GET predios/{id} para llenar el formulario al editar. */
    private void cargar() {
        ClienteApi.get(this).obtenerPredio(predioId)
                .enqueue(new LlamadaApi<PredioVista>(this, progreso, btnGuardar) {
                    @Override
                    protected void exito(@Nullable PredioVista p) {
                        if (p != null) llenar(p);
                    }

                    @Override
                    protected void error(@NonNull ErrorApi error) {
                        mostrarMensaje(error.getError());
                        btnGuardar.setEnabled(false);
                    }
                });
    }

    private void llenar(PredioVista p) {
        txtNombre.setText(p.getNombre());
        String departamento = p.getDepartamento() == null || p.getDepartamento().isEmpty()
                ? DEPARTAMENTO_INICIAL : p.getDepartamento();
        txtDepartamento.setText(departamento, false);
        ponerMunicipios(departamento, false);
        txtMunicipio.setText(p.getMunicipio(), false);
        txtCultivo.setText(p.getCultivo(), false);
        txtSuperficie.setText(Formatos.paraEditar(p.getSuperficieDeclaradaHa()));
        txtDesmonte.setText(Formatos.paraEditar(p.getDesmonteDeclaradoHa()));
        txtTitulo.setText(p.getNumeroTituloInra());
        txtTitularCi.setText(p.getTitularCi());
        txtTitulada.setText(Formatos.paraEditar(p.getSuperficieTituladaHa()));

        if (p.getEstado() == EstadoExpediente.Aprobado || p.getEstado() == EstadoExpediente.ConAlertas) {
            txtAviso.setText(getString(R.string.predios_form_aviso_enviado,
                    p.getEstado() == EstadoExpediente.Aprobado ? "aprobado" : "con alertas"));
            txtAviso.setVisibility(View.VISIBLE);
        }
    }

    private void guardar() {
        for (TextInputLayout capa : capas.values()) capa.setError(null);
        txtMensaje.setVisibility(View.GONE);

        String nombre = texto(txtNombre);
        String departamento = texto(txtDepartamento);
        String municipio = texto(txtMunicipio);
        String cultivo = texto(txtCultivo);
        String superficie = texto(txtSuperficie);
        String desmonte = texto(txtDesmonte);
        String titulo = texto(txtTitulo);
        String titularCi = texto(txtTitularCi);
        String titulada = texto(txtTitulada);

        Map<String, String> errores = Validaciones.validarPredio(this, nombre, departamento, municipio, cultivo,
                superficie, desmonte, titulo, titularCi, titulada);
        if (!errores.isEmpty()) {
            marcar(errores);
            return;
        }

        String dep = Validaciones.limpio(departamento);
        String oficial = Municipios.municipioDe(this, dep, Validaciones.limpio(municipio));
        boolean hayTitulo = !Validaciones.limpio(titulo).isEmpty();
        Double sup = Validaciones.numero(superficie);
        PredioDto datos = new PredioDto(
                Validaciones.limpio(nombre),
                dep,
                oficial != null ? oficial : Validaciones.limpio(municipio),
                Validaciones.limpio(cultivo),
                sup == null ? 0 : sup,
                Validaciones.numero(desmonte),
                hayTitulo ? Validaciones.normalizarTitulo(titulo) : null,
                hayTitulo ? Validaciones.opcional(titularCi) : null,
                hayTitulo ? Validaciones.numero(titulada) : null);

        btnGuardar.setText(R.string.comun_guardando);
        LlamadaApi<PredioVista> respuesta = new LlamadaApi<PredioVista>(this, progreso, btnGuardar, btnCancelar) {
            @Override
            protected void exito(@Nullable PredioVista guardado) {
                restaurarBoton();
                if (guardado == null) {
                    finish();
                    return;
                }
                if (editando) {
                    Toast.makeText(PredioFormActivity.this, R.string.predios_form_actualizado, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(PredioFormActivity.this,
                            getString(R.string.predios_form_registrado, guardado.getNombre()), Toast.LENGTH_LONG).show();
                    startActivity(new Intent(PredioFormActivity.this, PredioDetalleActivity.class)
                            .putExtra(Extras.PREDIO_ID, guardado.getId()));
                }
                finish();
            }

            @Override
            protected void error(@NonNull ErrorApi error) {
                restaurarBoton();
                if (!error.marcarCampo(capas)) mostrarMensaje(error.getError());
            }
        };
        if (editando) {
            ClienteApi.get(this).actualizarPredio(predioId, datos).enqueue(respuesta);
        } else {
            ClienteApi.get(this).crearPredio(datos).enqueue(respuesta);
        }
    }

    private void restaurarBoton() {
        btnGuardar.setText(editando ? R.string.predios_form_guardar_cambios : R.string.predios_form_btn_registrar);
    }

    private void marcar(Map<String, String> errores) {
        TextInputLayout primera = null;
        for (Map.Entry<String, String> e : errores.entrySet()) {
            TextInputLayout capa = capas.get(e.getKey());
            if (capa == null) continue;
            capa.setError(e.getValue());
            if (primera == null) primera = capa;
        }
        if (primera != null) primera.requestFocus();
    }

    private void mostrarMensaje(String mensaje) {
        txtMensaje.setText(mensaje);
        txtMensaje.setVisibility(View.VISIBLE);
    }

    private static String texto(TextView campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
