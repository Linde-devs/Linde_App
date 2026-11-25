package josue.com.bo.linde.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.LoginDto;
import josue.com.bo.linde.modelo.ProductorVista;
import josue.com.bo.linde.modelo.RegistroProductorDto;
import josue.com.bo.linde.modelo.RespuestaLogin;
import josue.com.bo.linde.sesion.Sesion;
import josue.com.bo.linde.ui.predios.PrediosActivity;
import josue.com.bo.linde.util.Municipios;
import josue.com.bo.linde.util.Pantalla;
import josue.com.bo.linde.util.Validaciones;

/**
 * Registro de productor (POST auth/registro). Valida antes con las reglas de la web; la API vuelve a validar.
 * Al registrarse muestra el código de productor e inicia sesión con el mismo correo y contraseña.
 * El departamento solo sirve para elegir el municipio: la API recibe el municipio.
 */
public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText txtNombre;
    private TextInputEditText txtApellido;
    private TextInputEditText txtCi;
    private TextInputEditText txtRunpa;
    private TextInputEditText txtTelefono;
    private MaterialAutoCompleteTextView txtDepartamento;
    private MaterialAutoCompleteTextView txtMunicipio;
    private TextInputEditText txtCorreo;
    private TextInputEditText txtContrasena;
    private TextInputEditText txtConfirmar;
    private TextView txtMensaje;
    private MaterialButton btnCrear;
    private LinearProgressIndicator progreso;
    private final Map<String, TextInputLayout> capas = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_registro);
        Pantalla.ajustarBordes(findViewById(R.id.login_registro_raiz));

        txtNombre = findViewById(R.id.login_registro_nombre);
        txtApellido = findViewById(R.id.login_registro_apellido);
        txtCi = findViewById(R.id.login_registro_ci);
        txtRunpa = findViewById(R.id.login_registro_runpa);
        txtTelefono = findViewById(R.id.login_registro_telefono);
        txtDepartamento = findViewById(R.id.login_registro_departamento);
        txtMunicipio = findViewById(R.id.login_registro_municipio);
        txtCorreo = findViewById(R.id.login_registro_correo);
        txtContrasena = findViewById(R.id.login_registro_contrasena);
        txtConfirmar = findViewById(R.id.login_registro_confirmar);
        txtMensaje = findViewById(R.id.login_registro_mensaje);
        btnCrear = findViewById(R.id.login_registro_btn_crear);
        progreso = findViewById(R.id.login_registro_progreso);

        capas.put("nombre", findViewById(R.id.login_registro_capa_nombre));
        capas.put("apellido", findViewById(R.id.login_registro_capa_apellido));
        capas.put("ci", findViewById(R.id.login_registro_capa_ci));
        capas.put("runpa", findViewById(R.id.login_registro_capa_runpa));
        capas.put("telefono", findViewById(R.id.login_registro_capa_telefono));
        capas.put("municipio", findViewById(R.id.login_registro_capa_municipio));
        capas.put("correo", findViewById(R.id.login_registro_capa_correo));
        capas.put("contrasena", findViewById(R.id.login_registro_capa_contrasena));
        capas.put("confirmar", findViewById(R.id.login_registro_capa_confirmar));

        List<String> departamentos = Municipios.departamentos(this);
        txtDepartamento.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, departamentos));
        txtDepartamento.setOnItemClickListener((padre, vista, posicion, id) ->
                ponerMunicipios(departamentos.get(posicion)));
        ponerMunicipios(null);

        btnCrear.setOnClickListener(v -> crearCuenta());
        findViewById(R.id.login_registro_btn_login).setOnClickListener(v -> finish());
    }

    /** Sin departamento elegido se ofrecen los 340 municipios. */
    private void ponerMunicipios(@Nullable String departamento) {
        List<String> lista = departamento == null ? Municipios.todos(this) : Municipios.de(this, departamento);
        txtMunicipio.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lista));
        String actual = Validaciones.limpio(texto(txtMunicipio));
        if (departamento != null && !actual.isEmpty() && Municipios.municipioDe(this, departamento, actual) == null) {
            txtMunicipio.setText("", false);
        }
    }

    private void crearCuenta() {
        for (TextInputLayout capa : capas.values()) capa.setError(null);
        txtMensaje.setVisibility(View.GONE);

        String nombre = Validaciones.limpio(texto(txtNombre));
        String apellido = Validaciones.limpio(texto(txtApellido));
        String ci = Validaciones.limpio(texto(txtCi));
        String runpa = Validaciones.limpio(texto(txtRunpa));
        String telefono = Validaciones.telefonoSinEspacios(texto(txtTelefono));
        String municipio = Validaciones.limpio(texto(txtMunicipio));
        String correo = Validaciones.limpio(texto(txtCorreo));
        String contrasena = texto(txtContrasena);
        String confirmar = texto(txtConfirmar);

        Map<String, String> errores = Validaciones.validarRegistro(nombre, apellido, ci, runpa, telefono, municipio,
                correo, contrasena, confirmar);
        if (!errores.isEmpty()) {
            marcar(errores);
            return;
        }
        String oficial = Municipios.municipioOficial(this, municipio);

        RegistroProductorDto datos = new RegistroProductorDto(nombre, apellido, ci, Validaciones.opcional(runpa),
                telefono, oficial != null ? oficial : municipio, correo, contrasena);
        btnCrear.setText(R.string.login_registro_creando);
        ClienteApi.get(this).registrar(datos).enqueue(new LlamadaApi<ProductorVista>(this, progreso, btnCrear) {
            @Override
            protected void exito(@Nullable ProductorVista productor) {
                btnCrear.setText(R.string.login_registro_crear);
                String codigo = productor != null ? productor.getCodigo() : "";
                new MaterialAlertDialogBuilder(RegistroActivity.this)
                        .setTitle(R.string.login_registro_listo_titulo)
                        .setMessage(getString(R.string.login_registro_listo, codigo))
                        .setCancelable(false)
                        .setPositiveButton(R.string.login_registro_continuar, (d, w) -> iniciarSesion(correo, contrasena))
                        .show();
            }

            @Override
            protected void error(@NonNull ErrorApi error) {
                btnCrear.setText(R.string.login_registro_crear);
                if (!error.marcarCampo(capas)) mostrarMensaje(error.getError());
            }
        });
    }

    /** Login automático con el mismo correo y contraseña recién registrados. */
    private void iniciarSesion(String correo, String contrasena) {
        ClienteApi.get(this).login(new LoginDto(correo, contrasena))
                .enqueue(new LlamadaApi<RespuestaLogin>(this, progreso, btnCrear) {
                    @Override
                    protected void exito(@Nullable RespuestaLogin respuesta) {
                        if (respuesta == null || respuesta.getToken() == null) {
                            irAlLogin(correo, getString(R.string.comun_error_inesperado));
                            return;
                        }
                        Sesion.guardar(RegistroActivity.this, respuesta);
                        startActivity(new Intent(RegistroActivity.this, PrediosActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                    }

                    @Override
                    protected void error(@NonNull ErrorApi error) {
                        irAlLogin(correo, error.getError());
                    }
                });
    }

    private void irAlLogin(String correo, String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class)
                .putExtra(LoginActivity.EXTRA_CORREO, correo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
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
