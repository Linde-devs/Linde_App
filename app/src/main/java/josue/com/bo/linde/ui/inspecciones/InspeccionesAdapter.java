package josue.com.bo.linde.ui.inspecciones;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.modelo.InspeccionCampo;
import josue.com.bo.linde.util.Formatos;

/** Tarjeta de cada inspección: fecha, técnico, resultado con color, comentario y quién la registró. */
public class InspeccionesAdapter extends RecyclerView.Adapter<InspeccionesAdapter.Fila> {

    public interface AlEliminar {
        void eliminar(InspeccionCampo inspeccion);
    }

    private final List<InspeccionCampo> inspecciones = new ArrayList<>();
    private final AlEliminar alEliminar;

    public InspeccionesAdapter(AlEliminar alEliminar) {
        this.alEliminar = alEliminar;
    }

    public void poner(List<InspeccionCampo> nuevas) {
        inspecciones.clear();
        inspecciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_inspecciones, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        InspeccionCampo i = inspecciones.get(posicion);
        Context c = fila.itemView.getContext();
        fila.fecha.setText(Formatos.fechaCorta(i.getFecha()));
        fila.tecnico.setText(c.getString(R.string.inspecciones_item_tecnico, texto(i.getTecnico())));
        fila.resultado.setText(Formatos.resultadoInspeccion(i.getResultado()));
        fila.resultado.setTextColor(ContextCompat.getColor(c, Formatos.colorResultadoInspeccion(i.getResultado())));
        boolean hayComentario = i.getComentario() != null && !i.getComentario().trim().isEmpty();
        fila.comentario.setVisibility(hayComentario ? View.VISIBLE : View.GONE);
        fila.comentario.setText(hayComentario ? i.getComentario() : "");
        fila.registrada.setText(c.getString(R.string.inspecciones_item_registrada, texto(i.getRegistradaPor()),
                Formatos.fechaHora(i.getFechaRegistro())));
        fila.eliminar.setOnClickListener(v -> alEliminar.eliminar(i));
    }

    @Override
    public int getItemCount() {
        return inspecciones.size();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    static final class Fila extends RecyclerView.ViewHolder {
        final TextView fecha;
        final TextView tecnico;
        final TextView resultado;
        final TextView comentario;
        final TextView registrada;
        final MaterialButton eliminar;

        Fila(View vista) {
            super(vista);
            fecha = vista.findViewById(R.id.inspecciones_item_fecha);
            tecnico = vista.findViewById(R.id.inspecciones_item_tecnico);
            resultado = vista.findViewById(R.id.inspecciones_item_resultado);
            comentario = vista.findViewById(R.id.inspecciones_item_comentario);
            registrada = vista.findViewById(R.id.inspecciones_item_registrada);
            eliminar = vista.findViewById(R.id.inspecciones_item_eliminar);
        }
    }
}
