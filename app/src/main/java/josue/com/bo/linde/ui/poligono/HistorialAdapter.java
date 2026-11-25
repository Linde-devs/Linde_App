package josue.com.bo.linde.ui.poligono;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.modelo.PoligonoVista;

/** Lista de versiones del polígono: «Versión 3 (vigente)», fecha, autor, motivo y superficie. */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.Fila> {

    public interface AlElegir {
        void elegida(PoligonoVista version);
    }

    private final List<PoligonoVista> versiones = new ArrayList<>();
    private final AlElegir alElegir;
    private int idMarcado = -1;

    public HistorialAdapter(AlElegir alElegir) {
        this.alElegir = alElegir;
    }

    public void poner(List<PoligonoVista> nuevas) {
        versiones.clear();
        versiones.addAll(nuevas);
        notifyDataSetChanged();
    }

    public void marcar(int id) {
        idMarcado = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_poligono_historial, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        PoligonoVista p = versiones.get(posicion);
        Context c = fila.itemView.getContext();

        fila.titulo.setText(p.isVigente()
                ? c.getString(R.string.poligono_historial_version_vigente, p.getVersion())
                : c.getString(R.string.poligono_version_titulo, p.getVersion()));
        fila.titulo.setTextColor(p.isVigente() ? MapaUtil.COLOR_ACCION : MapaUtil.COLOR_PRIMARIO);
        fila.fecha.setText(MapaUtil.fecha(p.getFecha()));
        fila.autor.setText(c.getString(R.string.poligono_historial_autor, p.getAutor() == null ? "" : p.getAutor()));
        fila.motivo.setText(c.getString(R.string.poligono_resumen_motivo, p.getMotivo() == null ? "" : p.getMotivo()));

        String superficie = p.isEsPunto() || p.getAreaHa() == null
                ? c.getString(R.string.poligono_es_punto)
                : MapaUtil.hectareas(p.getAreaHa());
        int conGps = 0;
        if (p.getPrecisionesM() != null) {
            for (Double m : p.getPrecisionesM()) if (m != null) conGps++;
        }
        String detalle = c.getString(R.string.poligono_historial_superficie, superficie);
        if (conGps > 0) detalle += " · " + c.getString(R.string.poligono_resumen_gps, conGps);
        fila.superficie.setText(detalle);

        boolean marcada = p.getId() == idMarcado;
        fila.tarjeta.setStrokeWidth(MapaUtil.dp(c, marcada ? 2 : 1));
        fila.tarjeta.setStrokeColor(marcada
                ? (p.isVigente() ? MapaUtil.COLOR_ACCION : MapaUtil.COLOR_GRIS)
                : 0x22163330);
        fila.itemView.setOnClickListener(v -> alElegir.elegida(p));
    }

    @Override
    public int getItemCount() {
        return versiones.size();
    }

    static final class Fila extends RecyclerView.ViewHolder {
        final MaterialCardView tarjeta;
        final TextView titulo;
        final TextView fecha;
        final TextView autor;
        final TextView motivo;
        final TextView superficie;

        Fila(View vista) {
            super(vista);
            tarjeta = (MaterialCardView) vista;
            titulo = vista.findViewById(R.id.poligono_item_titulo);
            fecha = vista.findViewById(R.id.poligono_item_fecha);
            autor = vista.findViewById(R.id.poligono_item_autor);
            motivo = vista.findViewById(R.id.poligono_item_motivo);
            superficie = vista.findViewById(R.id.poligono_item_superficie);
        }
    }
}
