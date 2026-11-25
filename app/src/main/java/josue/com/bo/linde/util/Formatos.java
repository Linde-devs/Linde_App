package josue.com.bo.linde.util;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import josue.com.bo.linde.R;
import josue.com.bo.linde.modelo.CapaValidacion;
import josue.com.bo.linde.modelo.EstadoCapa;
import josue.com.bo.linde.modelo.EstadoExpediente;
import josue.com.bo.linde.modelo.EstadoInforme;
import josue.com.bo.linde.modelo.PrioridadAlerta;
import josue.com.bo.linde.modelo.ResultadoInspeccion;
import josue.com.bo.linde.modelo.TipoDocumento;

/** Formatos para mostrar: fechas, hectáreas y nombres legibles de los enums (los mismos textos que la web). */
public final class Formatos {

    public static final Locale BOLIVIA = new Locale("es", "BO");

    private Formatos() {
    }

    // ------------------------------------------------------------------ fechas

    /** Lee una fecha ISO-8601 de la API («2026-11-25T14:03:12.1234567Z»). Sin zona se toma como UTC. */
    @Nullable
    public static Date leerIso(@Nullable String iso) {
        if (iso == null || iso.length() < 19) return null;
        try {
            String resto = iso.substring(19);
            int i = 0;
            if (resto.startsWith(".")) {
                i = 1;
                while (i < resto.length() && Character.isDigit(resto.charAt(i))) i++;
            }
            String zona = resto.substring(i);
            SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            entrada.setTimeZone(zona.isEmpty() || zona.equals("Z")
                    ? TimeZone.getTimeZone("UTC")
                    : TimeZone.getTimeZone("GMT" + zona));
            return entrada.parse(iso.substring(0, 19));
        } catch (ParseException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** «25/11/2026» en la hora del celular. */
    public static String fecha(@Nullable String iso) {
        Date d = leerIso(iso);
        return d == null ? texto(iso) : new SimpleDateFormat("dd/MM/yyyy", BOLIVIA).format(d);
    }

    /** «25/11/2026 10:03» en la hora del celular. */
    public static String fechaHora(@Nullable String iso) {
        Date d = leerIso(iso);
        return d == null ? texto(iso) : new SimpleDateFormat("dd/MM/yyyy HH:mm", BOLIVIA).format(d);
    }

    /** DateOnly «aaaa-mm-dd» a «dd/mm/aaaa». */
    public static String fechaCorta(@Nullable String aaaaMmDd) {
        if (aaaaMmDd == null) return "";
        String[] p = aaaaMmDd.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : aaaaMmDd;
    }

    /** Fecha de hoy como «aaaa-mm-dd» (hora del celular). */
    public static String hoyAaaaMmDd() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // ------------------------------------------------------------------ números

    /** «12,50 ha». */
    public static String hectareas(double valor) {
        NumberFormat f = NumberFormat.getNumberInstance(BOLIVIA);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(valor) + " ha";
    }

    /** Número para poner en un campo de texto al editar («12.5»; vacío si es null). */
    public static String paraEditar(@Nullable Double valor) {
        if (valor == null) return "";
        return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------ enums

    public static String estadoPredio(@Nullable EstadoExpediente estado) {
        if (estado == null) return "";
        switch (estado) {
            case ConAlertas:
                return "Con alertas";
            case Aprobado:
                return "Aprobado";
            case Registrado:
            default:
                return "Registrado";
        }
    }

    /** Registrado gris, Con alertas naranja, Aprobado verde. */
    @ColorRes
    public static int colorEstadoPredio(@Nullable EstadoExpediente estado) {
        if (estado == EstadoExpediente.Aprobado) return R.color.linde_accion;
        if (estado == EstadoExpediente.ConAlertas) return R.color.linde_aviso;
        return R.color.linde_gris;
    }

    public static String estadoInforme(@Nullable EstadoInforme estado) {
        if (estado == null) return "";
        return estado == EstadoInforme.ConAlertas ? "Con alertas" : "Conforme";
    }

    public static String capa(@Nullable CapaValidacion capa) {
        if (capa == null) return "";
        switch (capa) {
            case Geometrica:
                return "Geometría y superposición";
            case Titularidad:
                return "Titularidad (INRA)";
            case UsoSuelo:
                return "Uso de suelo (ABT)";
            case Cobertura:
            default:
                return "Cobertura satelital";
        }
    }

    public static String estadoCapa(@Nullable EstadoCapa estado) {
        if (estado == null) return "";
        switch (estado) {
            case Alerta:
                return "Alerta";
            case Rechazado:
                return "Rechazado";
            case Conforme:
            default:
                return "Conforme";
        }
    }

    public static String prioridad(@Nullable PrioridadAlerta prioridad) {
        if (prioridad == null) return "";
        switch (prioridad) {
            case Alta:
                return "Alta";
            case Media:
                return "Media";
            case Baja:
            default:
                return "Baja";
        }
    }

    public static String resultadoInspeccion(@Nullable ResultadoInspeccion resultado) {
        if (resultado == null) return "";
        switch (resultado) {
            case ConObservaciones:
                return "Con observaciones";
            case NoConforme:
                return "No conforme";
            case Conforme:
            default:
                return "Conforme";
        }
    }

    /** Conforme verde, Con observaciones naranja, No conforme rojo. */
    @ColorRes
    public static int colorResultadoInspeccion(@Nullable ResultadoInspeccion resultado) {
        if (resultado == ResultadoInspeccion.NoConforme) return R.color.linde_error;
        if (resultado == ResultadoInspeccion.ConObservaciones) return R.color.linde_aviso;
        return R.color.linde_accion;
    }

    public static String tipoDocumento(@Nullable TipoDocumento tipo) {
        if (tipo == null) return "";
        switch (tipo) {
            case PlanOrdenamientoPredial:
                return "Plan de ordenamiento predial (ABT)";
            case PlanDesmonte:
                return "Plan de desmonte (ABT)";
            case TituloPropiedad:
            default:
                return "Título de propiedad (INRA)";
        }
    }

    private static String texto(@Nullable String valor) {
        return valor == null ? "" : valor;
    }
}
