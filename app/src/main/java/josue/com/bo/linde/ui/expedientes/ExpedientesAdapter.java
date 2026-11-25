package josue.com.bo.linde.ui.expedientes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.modelo.Expediente;
import josue.com.bo.linde.ui.poligono.MapaUtil;

/** Fila de la lista: código, predio, fecha de generación y estado. */
public class ExpedientesAdapter extends RecyclerView.Adapter<ExpedientesAdapter.Fila> {

    public interface AlElegir {
        void elegido(Expediente expediente);
    }

    private final List<Expediente> expedientes = new ArrayList<>();
    private final AlElegir alElegir;

    public ExpedientesAdapter(AlElegir alElegir) {
        this.alElegir = alElegir;
    }

    public void poner(List<Expediente> nuevos) {
        expedientes.clear();
        expedientes.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_expedientes, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Expediente e = expedientes.get(posicion);
        fila.codigo.setText(e.getCodigo());
        fila.predio.setText(e.getPredio());
        fila.fecha.setText(fila.itemView.getContext().getString(R.string.expedientes_generado_el,
                MapaUtil.fecha(e.getFechaGeneracion())));
        fila.estado.setText(TextosExpediente.estado(e.getEstado()));
        fila.estado.setTextColor(TextosExpediente.colorEstado(e.getEstado()));
        fila.itemView.setOnClickListener(v -> alElegir.elegido(e));
    }

    @Override
    public int getItemCount() {
        return expedientes.size();
    }

    static final class Fila extends RecyclerView.ViewHolder {
        final TextView codigo;
        final TextView predio;
        final TextView fecha;
        final TextView estado;

        Fila(View vista) {
            super(vista);
            codigo = vista.findViewById(R.id.expedientes_item_codigo);
            predio = vista.findViewById(R.id.expedientes_item_predio);
            fecha = vista.findViewById(R.id.expedientes_item_fecha);
            estado = vista.findViewById(R.id.expedientes_item_estado);
        }
    }
}
