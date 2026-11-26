package josue.com.bo.linde.ui.envio;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import josue.com.bo.linde.R;
import josue.com.bo.linde.api.*;
import josue.com.bo.linde.modelo.*;
import josue.com.bo.linde.ui.alertas.AlertasActivity;
import josue.com.bo.linde.ui.documentos.DocumentosActivity;
import josue.com.bo.linde.ui.poligono.PoligonoActivity;
import josue.com.bo.linde.util.*;
import retrofit2.Call;

public class EnvioActivity extends AppCompatActivity {
    private int predioId;
    private String estadoPredio;
    private LinearLayout requisitos, historial, resultado;
    private TextView estado, resumen;
    private MaterialButton enviar;
    private View progreso;
    private Call<?> requisitosCall, historialCall, envioCall;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        predioId = getIntent().getIntExtra(Extras.PREDIO_ID, -1);
        if (predioId <= 0) { finish(); return; }
        estadoPredio = getIntent().getStringExtra(Extras.PREDIO_ESTADO);
        setContentView(R.layout.activity_envio);
        Pantalla.ajustarBordes(findViewById(R.id.envio_raiz));
        Pantalla.barra(this, (MaterialToolbar) findViewById(R.id.envio_barra), R.string.envio_titulo, true);
        requisitos = findViewById(R.id.envio_requisitos);
        historial = findViewById(R.id.envio_historial);
        resultado = findViewById(R.id.envio_resultado);
        estado = findViewById(R.id.envio_estado);
        resumen = findViewById(R.id.envio_resumen);
        progreso = findViewById(R.id.envio_progreso);
        enviar = findViewById(R.id.envio_enviar);
        enviar.setEnabled(false);
        enviar.setOnClickListener(v -> enviar());
        findViewById(R.id.envio_reintentar).setOnClickListener(v -> cargar());
        findViewById(R.id.envio_alertas).setOnClickListener(v -> abrir(AlertasActivity.class));
    }

    @Override protected void onResume() {
        super.onResume();
        if (requisitos != null) cargar();
    }

    @Override protected void onDestroy() {
        if (requisitosCall != null) requisitosCall.cancel();
        if (historialCall != null) historialCall.cancel();
        if (envioCall != null) envioCall.cancel();
        super.onDestroy();
    }

    private void cargar() {
        if (requisitosCall != null) requisitosCall.cancel();
        if (historialCall != null) historialCall.cancel();
        estado.setVisibility(View.GONE);
        findViewById(R.id.envio_reintentar).setVisibility(View.GONE);
        enviar.setEnabled(false);
        Call<RevisionRequisitos> c = ClienteApi.get(this).requisitosEnvio(predioId);
        requisitosCall = c;
        c.enqueue(new LlamadaApi<RevisionRequisitos>(this, progreso) {
            @Override protected void exito(@Nullable RevisionRequisitos r) {
                requisitos.removeAllViews();
                if (r == null) { errorVisible("No se recibieron los requisitos."); return; }
                if (r.getRequisitos() != null) for (Requisito requisito : r.getRequisitos()) {
                    TextView fila = fila((requisito.isCumplido() ? "✔ " : "✖ ") + requisito.getNombre()
                            + "\n" + (requisito.getDetalle() == null ? "" : requisito.getDetalle()));
                    if (!requisito.isCumplido()) {
                        fila.setTextColor(getColor(R.color.linde_error));
                        String clave = requisito.getClave() == null ? "" : requisito.getClave().toLowerCase();
                        if (clave.contains("poligono") || clave.contains("geometr")) fila.setOnClickListener(v -> abrir(PoligonoActivity.class));
                        else if (clave.contains("document") || clave.contains("titulo")) fila.setOnClickListener(v -> abrir(DocumentosActivity.class));
                    }
                    requisitos.addView(fila);
                }
                boolean permitido = r.isCompleto() && !"Aprobado".equals(estadoPredio);
                enviar.setEnabled(permitido);
                estado.setText(!r.isCompleto() ? "Completa los requisitos pendientes antes de enviar."
                        : permitido ? "El predio está listo para revisión." : "Este predio ya está aprobado.");
                estado.setVisibility(View.VISIBLE);
            }
            @Override protected void error(@NonNull ErrorApi e) { errorVisible(e.getError()); }
        });
        Call<List<ResultadoValidacion>> h = ClienteApi.get(this).historialValidaciones(predioId);
        historialCall = h;
        h.enqueue(new LlamadaApi<List<ResultadoValidacion>>(this, null) {
            @Override protected void exito(@Nullable List<ResultadoValidacion> datos) {
                historial.removeAllViews();
                if (datos == null || datos.isEmpty()) historial.addView(fila("Aún no hay envíos anteriores."));
                else for (ResultadoValidacion item : datos) {
                    TextView vista = fila(Formatos.fechaHora(item.getFecha()) + " · "
                            + Formatos.estadoPredio(item.getEstadoResultante()));
                    vista.setOnClickListener(v -> mostrar(item));
                    historial.addView(vista);
                }
            }
            @Override protected void error(@NonNull ErrorApi e) {
                historial.removeAllViews();
                historial.addView(fila(e.getError()));
            }
        });
    }

    private void enviar() {
        enviar.setEnabled(false);
        progreso.setVisibility(View.VISIBLE);
        estado.setText("Revisando el predio…");
        estado.setVisibility(View.VISIBLE);
        Call<ResultadoValidacion> c = ClienteApi.get(this).enviarARevision(predioId);
        envioCall = c;
        c.enqueue(new LlamadaApi<ResultadoValidacion>(this, progreso, enviar) {
            @Override protected void exito(@Nullable ResultadoValidacion r) {
                if (r == null) { errorVisible("No se recibió el resultado."); return; }
                estadoPredio = r.getEstadoResultante() == null ? null : r.getEstadoResultante().name();
                mostrar(r);
                cargar();
            }
            @Override protected void error(@NonNull ErrorApi e) {
                errorVisible(e.getError());
                enviar.setEnabled(false);
            }
        });
    }

    private void mostrar(ResultadoValidacion r) {
        resultado.removeAllViews();
        resumen.setText(Formatos.estadoPredio(r.getEstadoResultante()) + "\n"
                + (r.getMensaje() == null ? "" : r.getMensaje()));
        if (r.isCortado()) {
            TextView aviso = fila("El envío se detuvo en la primera capa; corrige el predio y vuelve a enviarlo.");
            aviso.setTextColor(getColor(R.color.linde_error));
            resultado.addView(aviso);
        }
        if (r.getCapas() != null) for (ResultadoCapa c : r.getCapas()) {
            String texto = Formatos.capa(c.getCapa()) + " · " + Formatos.estadoCapa(c.getEstado())
                    + (c.getPrioridad() == null ? "" : " · Prioridad " + Formatos.prioridad(c.getPrioridad()))
                    + "\n" + (c.getMensaje() == null ? "" : c.getMensaje());
            TextView fila = fila(texto);
            if (c.getEstado() == EstadoCapa.Rechazado) fila.setTextColor(getColor(R.color.linde_error));
            else if (c.getEstado() == EstadoCapa.Alerta) fila.setTextColor(getColor(R.color.linde_aviso));
            resultado.addView(fila);
        }
        findViewById(R.id.envio_alertas).setVisibility(
                r.getEstadoResultante() == EstadoExpediente.ConAlertas ? View.VISIBLE : View.GONE);
    }

    private TextView fila(String texto) {
        TextView v = new TextView(this);
        v.setText(texto);
        v.setTextColor(getColor(R.color.linde_texto));
        v.setTextSize(15);
        v.setPadding(12, 16, 12, 16);
        return v;
    }

    private void abrir(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla).putExtra(Extras.PREDIO_ID, predioId)
                .putExtra(Extras.PREDIO_ESTADO, estadoPredio));
    }

    private void errorVisible(String mensaje) {
        estado.setText(mensaje);
        estado.setTextColor(getColor(R.color.linde_error));
        estado.setVisibility(View.VISIBLE);
        findViewById(R.id.envio_reintentar).setVisibility(View.VISIBLE);
    }
}
