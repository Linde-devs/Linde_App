package josue.com.bo.linde.ui.expedientes;

import josue.com.bo.linde.modelo.CapaValidacion;
import josue.com.bo.linde.modelo.EstadoCapa;
import josue.com.bo.linde.modelo.EstadoInforme;
import josue.com.bo.linde.modelo.PrioridadAlerta;
import josue.com.bo.linde.modelo.ResultadoInspeccion;
import josue.com.bo.linde.ui.poligono.MapaUtil;

/** Nombres que ve el productor para los enums del expediente (los mismos textos que la web). */
final class TextosExpediente {

    private TextosExpediente() {
    }

    static String estado(EstadoInforme estado) {
        if (estado == null) return "";
        return estado == EstadoInforme.ConAlertas ? "Con alertas" : "Conforme";
    }

    static int colorEstado(EstadoInforme estado) {
        return estado == EstadoInforme.ConAlertas ? MapaUtil.COLOR_AVISO : MapaUtil.COLOR_ACCION;
    }

    static String capa(CapaValidacion capa) {
        if (capa == null) return "";
        switch (capa) {
            case Geometrica:
                return "Geometría y superposición";
            case Titularidad:
                return "Titularidad (INRA)";
            case UsoSuelo:
                return "Uso de suelo (ABT)";
            case Cobertura:
                return "Cobertura satelital";
            default:
                return capa.name();
        }
    }

    static String estadoCapa(EstadoCapa estado) {
        if (estado == null) return "";
        switch (estado) {
            case Conforme:
                return "Conforme";
            case Alerta:
                return "Alerta";
            case Rechazado:
                return "Rechazado";
            default:
                return estado.name();
        }
    }

    static int colorCapa(EstadoCapa estado) {
        if (estado == EstadoCapa.Rechazado) return MapaUtil.COLOR_ERROR;
        if (estado == EstadoCapa.Alerta) return MapaUtil.COLOR_AVISO;
        return MapaUtil.COLOR_ACCION;
    }

    static String prioridad(PrioridadAlerta prioridad) {
        if (prioridad == null) return "";
        switch (prioridad) {
            case Alta:
                return "Alta";
            case Media:
                return "Media";
            case Baja:
                return "Baja";
            default:
                return prioridad.name();
        }
    }

    static int colorPrioridad(PrioridadAlerta prioridad) {
        if (prioridad == PrioridadAlerta.Alta) return MapaUtil.COLOR_ERROR;
        if (prioridad == PrioridadAlerta.Media) return MapaUtil.COLOR_AVISO;
        return MapaUtil.COLOR_INFO;
    }

    static String resultado(ResultadoInspeccion resultado) {
        if (resultado == null) return "";
        switch (resultado) {
            case Conforme:
                return "Conforme";
            case ConObservaciones:
                return "Con observaciones";
            case NoConforme:
                return "No conforme";
            default:
                return resultado.name();
        }
    }

    /**
     * El expediente guarda cada documento como texto: «TituloPropiedad SPP-NAL-070167» (tipo interno y número).
     * Se cambia el tipo por su nombre, como en la web.
     */
    static String documento(String texto) {
        if (texto == null) return "";
        int espacio = texto.indexOf(' ');
        String tipo = espacio < 0 ? texto : texto.substring(0, espacio);
        String resto = espacio < 0 ? "" : texto.substring(espacio);
        switch (tipo) {
            case "TituloPropiedad":
                return "Título de propiedad (INRA)" + resto;
            case "PlanOrdenamientoPredial":
                return "Plan de ordenamiento predial (ABT)" + resto;
            case "PlanDesmonte":
                return "Plan de desmonte (ABT)" + resto;
            default:
                return texto;
        }
    }
}
