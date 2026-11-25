package josue.com.bo.linde.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import josue.com.bo.linde.R;
import josue.com.bo.linde.api.ClienteApi;
import josue.com.bo.linde.api.ErrorApi;
import josue.com.bo.linde.api.LlamadaApi;
import josue.com.bo.linde.modelo.LoginDto;
import josue.com.bo.linde.modelo.RespuestaLogin;
import josue.com.bo.linde.modelo.RolUsuario;
import josue.com.bo.linde.sesion.Sesion;
import josue.com.bo.linde.ui.consulta.ConsultaActivity;
import josue.com.bo.linde.ui.predios.PrediosActivity;
import josue.com.bo.linde.util.Pantalla;
import josue.com.bo.linde.util.Validaciones;

/**
 * Inicio de sesión del productor (POST auth/login). Si ya hay una sesión vigente, va directo a Mis predios.
 * Un 401 aquí es «Correo o contraseña incorrectos.»: se muestra el mensaje de la API y no se cierra nada.
 */
public class LoginActivity extends AppCompatActivity {

    /** Lo pone ClienteApi cuando el token venció. */
    public static final String EXTRA_SESION_VENCIDA = "login_sesion_vencida";
    /** Correo para precargar (por ejemplo, después del registro). */
    public static final String EXTRA_CORREO = "login_correo";

    private TextInputLayout capaCorreo;
    private TextInputLayout capaContrasena;
    private TextInputEditText txtCorreo;
    private TextInputEditText txtContrasena;
    private TextView txtMensaje;
    private MaterialButton btnIngresar;
    private MaterialButton btnRegistro;
    private LinearProgressIndicator progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Sesion.haySesionVigente(this)) {
            abrirPredios();
            return;
        }
        setContentView(R.layout.activity_login);
        Pantalla.ajustarBordes(findViewById(R.id.login_raiz));

        capaCorreo = findViewById(R.id.login_capa_correo);
        capaContrasena = findViewById(R.id.login_capa_contrasena);
        txtCorreo = findViewById(R.id.login_correo);
        txtContrasena = findViewById(R.id.login_contrasena);
        txtMensaje = findViewById(R.id.login_mensaje);
        btnIngresar = findViewById(R.id.login_btn_ingresar);
        btnRegistro = findViewById(R.id.login_btn_registro);
        progreso = findViewById(R.id.login_progreso);

        String correo = getIntent().getStringExtra(EXTRA_CORREO);
        if (correo != null) txtCorreo.setText(correo);
        if (getIntent().getBooleanExtra(EXTRA_SESION_VENCIDA, false) && savedInstanceState == null) {
            mostrarMensaje(getString(R.string.comun_sesion_vencida));
        }

        txtContrasena.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                ingresar();
                return true;
            }
            return false;
        });
        btnIngresar.setOnClickListener(v -> ingresar());
        btnRegistro.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));
        findViewById(R.id.login_btn_consulta).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultaActivity.class)));
    }

    private void ingresar() {
        txtMensaje.setVisibility(View.GONE);
        capaCorreo.setError(null);
        capaContrasena.setError(null);
        String correo = Validaciones.limpio(texto(txtCorreo));
        String contrasena = texto(txtContrasena);
        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarMensaje(getString(R.string.login_faltan_datos));
            return;
        }

        btnIngresar.setText(R.string.login_ingresando);
        ClienteApi.get(this).login(new LoginDto(correo, contrasena))
                .enqueue(new LlamadaApi<RespuestaLogin>(this, progreso, btnIngresar, btnRegistro) {
                    @Override
                    protected void exito(@Nullable RespuestaLogin respuesta) {
                        btnIngresar.setText(R.string.login_ingresar);
                        if (respuesta == null || respuesta.getToken() == null) {
                            mostrarMensaje(getString(R.string.comun_error_inesperado));
                            return;
                        }
                        if (respuesta.getUsuario() != null
                                && respuesta.getUsuario().getRol() == RolUsuario.Administrador) {
                            // No se guarda la sesión: la app es solo para productores.
                            mostrarMensaje(getString(R.string.login_es_administrador));
                            return;
                        }
                        Sesion.guardar(LoginActivity.this, respuesta);
                        abrirPredios();
                    }

                    @Override
                    protected void error(@NonNull ErrorApi error) {
                        btnIngresar.setText(R.string.login_ingresar);
                        mostrarMensaje(error.getError());
                    }
                });
    }

    private void abrirPredios() {
        startActivity(new Intent(this, PrediosActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void mostrarMensaje(String mensaje) {
        txtMensaje.setText(mensaje);
        txtMensaje.setVisibility(View.VISIBLE);
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
