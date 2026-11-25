package josue.com.bo.linde.ui.predios;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import josue.com.bo.linde.R;
import josue.com.bo.linde.modelo.PredioVista;
import josue.com.bo.linde.util.Formatos;

/** Tarjeta de cada predio: nombre, ubicación, cultivo, superficie, polígono y estado. */
public class PrediosAdapter extends RecyclerView.Adapter<PrediosAdapter.Fila> {

    public interface Acciones {
        void abrir(PredioVista predio);

        void eliminar(PredioVista predio);
    }

    private final List<PredioVista> predios = new ArrayList<>();
    private final Acciones acciones;

    public PrediosAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void poner(List<PredioVista> nuevos) {
        predios.clear();
        predios.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_predios, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        PredioVista p = predios.get(posicion);
        Context c = fila.itemView.getContext();
        fila.nombre.setText(p.getNombre());
        fila.ubicacion.setText(ubicacion(p));
        fila.detalle.setText(c.getString(R.string.predios_item_detalle, texto(p.getCultivo()),
                Formatos.hectareas(p.getSuperficieDeclaradaHa())));
        fila.poligono.setText(textoPoligono(c, p));
        pintarEstado(fila.estado, p);
        fila.itemView.setOnClickListener(v -> acciones.abrir(p));
        fila.itemView.setOnLongClickListener(v -> {
            acciones.eliminar(p);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return predios.size();
    }

    static String ubicacion(PredioVista p) {
        String dep = texto(p.getDepartamento());
        String mun = texto(p.getMunicipio());
        return dep.isEmpty() ? mun : mun + ", " + dep;
    }

    /** «Polígono: 3,42 ha», «Punto registrado» o «Sin polígono». */
    static String textoPoligono(Context c, PredioVista p) {
        if (!p.isTienePoligono()) return c.getString(R.string.predios_sin_poligono);
        if (p.isEsPunto() || p.getAreaPoligonoHa() == null) return c.getString(R.string.predios_punto_registrado);
        return c.getString(R.string.predios_poligono_area, Formatos.hectareas(p.getAreaPoligonoHa()));
    }

    /** Etiqueta de estado: Registrado gris, Con alertas naranja, Aprobado verde. */
    static void pintarEstado(TextView etiqueta, PredioVista p) {
        etiqueta.setText(Formatos.estadoPredio(p.getEstado()));
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(etiqueta.getContext(), Formatos.colorEstadoPredio(p.getEstado()))));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    static final class Fila extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView ubicacion;
        final TextView detalle;
        final TextView poligono;
        final TextView estado;

        Fila(View vista) {
            super(vista);
            nombre = vista.findViewById(R.id.predios_item_nombre);
            ubicacion = vista.findViewById(R.id.predios_item_ubicacion);
            detalle = vista.findViewById(R.id.predios_item_detalle);
            poligono = vista.findViewById(R.id.predios_item_poligono);
            estado = vista.findViewById(R.id.predios_item_estado);
        }
    }
}
