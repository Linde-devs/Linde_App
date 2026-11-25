package josue.com.bo.linde.util;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Las mismas reglas que validaciones.js de la web. Solo sirven para avisar antes de enviar: la API vuelve a
 * validar todo y es la que decide. Cada método devuelve un mapa campo → mensaje (vacío si todo está bien);
 * las claves son los nombres de los campos del DTO, igual que el «campo» de los errores de la API.
 */
public final class Validaciones {

    private static final Pattern CI_NIT = Pattern.compile("^\\d{5,12}(-[0-9A-Za-z]{1,2})?$");
    private static final Pattern RUNPA = Pattern.compile("^[0-9A-Za-z-]{1,20}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?\\d{7,12}$");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern TITULO = Pattern.compile("^[A-Z]{2,5}-NAL-\\d{4,7}$");
    private static final Pattern CI_TITULAR = Pattern.compile("^\\d{5,10}$");

    public static final String MENSAJE_TITULO =
            "Tiene la forma del INRA: tipo de título, NAL y el número, por ejemplo SPP-NAL-070167.";

    private Validaciones() {
    }

    // ------------------------------------------------------------------ ayudas

    public static String limpio(@Nullable String texto) {
        return texto == null ? "" : texto.trim();
    }

    /** null si el texto está vacío (para los opcionales de los DTO). */
    @Nullable
    public static String opcional(@Nullable String texto) {
        String t = limpio(texto);
        return t.isEmpty() ? null : t;
    }

    /** Número escrito con coma o punto. Devuelve null si está vacío o no es un número. */
    @Nullable
    public static Double numero(@Nullable String texto) {
        String t = limpio(texto).replace(',', '.');
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean tieneMasDeDosDecimales(String texto) {
        String[] partes = limpio(texto).replace(',', '.').split("\\.");
        return partes.length == 2 && partes[1].length() > 2;
    }

    /** Mayúsculas y espacios, guiones bajos o rayas convertidos en «-» (la API lo guarda así). */
    public static String normalizarTitulo(@Nullable String titulo) {
        return limpio(titulo).toUpperCase(Locale.ROOT)
                .replaceAll("[\\s_\\-–]+", "-")
                .replaceAll("^-|-$", "");
    }

    public static boolean tituloValido(@Nullable String titulo) {
        return TITULO.matcher(normalizarTitulo(titulo)).matches();
    }

    // ------------------------------------------------------------------ RF-01 productor

    public static Map<String, String> validarRegistro(String nombre, String apellido, String ci, String runpa,
                                                      String telefono, String municipio, String correo,
                                                      String contrasena, String confirmar) {
        Map<String, String> e = new LinkedHashMap<>();
        if (limpio(nombre).isEmpty()) e.put("nombre", "Ingresa el nombre.");
        if (limpio(apellido).isEmpty()) e.put("apellido", "Ingresa el apellido.");
        if (limpio(ci).isEmpty()) e.put("ci", "Ingresa el CI o NIT.");
        else if (!CI_NIT.matcher(limpio(ci)).matches())
            e.put("ci", "El CI o NIT debe tener entre 5 y 12 dígitos (complemento opcional, por ejemplo 7845123-1A).");
        if (!limpio(runpa).isEmpty() && !RUNPA.matcher(limpio(runpa)).matches())
            e.put("runpa", "Solo letras, números y guiones (hasta 20).");
        if (!TELEFONO.matcher(telefonoSinEspacios(telefono)).matches())
            e.put("telefono", "El teléfono debe tener entre 7 y 12 dígitos.");
        if (limpio(municipio).isEmpty()) e.put("municipio", "Ingresa el municipio.");
        if (limpio(correo).isEmpty()) e.put("correo", "Ingresa el correo.");
        else if (!CORREO.matcher(limpio(correo)).matches()) e.put("correo", "El correo no es válido.");
        if (contrasena == null || contrasena.length() < 8)
            e.put("contrasena", "La contraseña debe tener al menos 8 caracteres.");
        if (confirmar != null && !confirmar.equals(contrasena)) e.put("confirmar", "Las contraseñas no coinciden.");
        return e;
    }

    public static String telefonoSinEspacios(@Nullable String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\s", "");
    }

    // ------------------------------------------------------------------ RF-02 predio

    /**
     * Reglas de validarPredio de la web. Además, si se escribe un número de título, el CI del titular y la
     * superficie titulada son obligatorios (así se registra el título del INRA junto con el predio).
     */
    public static Map<String, String> validarPredio(Context contexto, String nombre, String departamento,
                                                    String municipio, String cultivo, String superficie,
                                                    String desmonte, String numeroTitulo, String titularCi,
                                                    String superficieTitulada) {
        Map<String, String> e = new LinkedHashMap<>();
        if (limpio(nombre).isEmpty()) e.put("nombre", "Ingresa el nombre del predio.");
        if (limpio(departamento).isEmpty()) e.put("departamento", "Selecciona el departamento.");
        if (limpio(municipio).isEmpty()) e.put("municipio", "Ingresa el municipio.");
        else if (!limpio(departamento).isEmpty()
                && Municipios.municipioDe(contexto, limpio(departamento), limpio(municipio)) == null)
            e.put("municipio", "No es un municipio de " + limpio(departamento) + ". Elige uno de la lista.");
        if (limpio(cultivo).isEmpty()) e.put("cultivo", "Ingresa el cultivo.");

        Double sup = numero(superficie);
        if (limpio(superficie).isEmpty()) e.put("superficieDeclaradaHa", "Ingresa la superficie declarada.");
        else if (sup == null || !(sup > 0)) e.put("superficieDeclaradaHa", "La superficie debe ser mayor a cero.");
        else if (tieneMasDeDosDecimales(superficie)) e.put("superficieDeclaradaHa", "Máximo 2 decimales.");

        if (!limpio(desmonte).isEmpty()) {
            Double des = numero(desmonte);
            if (des == null || !(des >= 0)) e.put("desmonteDeclaradoHa", "El desmonte no puede ser negativo.");
            else if (tieneMasDeDosDecimales(desmonte)) e.put("desmonteDeclaradoHa", "Máximo 2 decimales.");
            else if (sup != null && sup > 0 && des > sup)
                e.put("desmonteDeclaradoHa", "No puede ser mayor que la superficie.");
        }

        boolean hayTitulo = !limpio(numeroTitulo).isEmpty();
        if (hayTitulo && !tituloValido(numeroTitulo)) e.put("numeroTituloInra", MENSAJE_TITULO);

        String ci = limpio(titularCi);
        String titulada = limpio(superficieTitulada);
        if (!ci.isEmpty() && !CI_TITULAR.matcher(ci).matches())
            e.put("titularCi", "El CI debe tener de 5 a 10 dígitos.");
        else if (hayTitulo && ci.isEmpty()) e.put("titularCi", "Ingresa el CI del titular.");

        if (!titulada.isEmpty()) {
            Double t = numero(titulada);
            if (t == null || !(t > 0)) e.put("superficieTituladaHa", "Debe ser mayor a cero.");
        } else if (hayTitulo) {
            e.put("superficieTituladaHa", "Ingresa la superficie titulada.");
        }
        if (!titulada.isEmpty() && !hayTitulo) e.put("numeroTituloInra", "Escribe el número de título.");
        return e;
    }

    // ------------------------------------------------------------------ RF-14 inspección

    public static Map<String, String> validarInspeccion(String fecha, String tecnico, boolean esConforme,
                                                        String comentario) {
        Map<String, String> e = new LinkedHashMap<>();
        if (limpio(fecha).isEmpty()) e.put("fecha", "Indica la fecha de la inspección.");
        else if (limpio(fecha).compareTo(Formatos.hoyAaaaMmDd()) > 0)
            e.put("fecha", "La fecha no puede ser posterior a hoy.");
        if (limpio(tecnico).isEmpty()) e.put("tecnico", "Escribe el nombre del técnico.");
        else if (limpio(tecnico).length() > 100) e.put("tecnico", "Máximo 100 caracteres.");
        if (!esConforme && limpio(comentario).isEmpty()) e.put("comentario", "Anota lo que observó el técnico.");
        else if (limpio(comentario).length() > 1000) e.put("comentario", "Máximo 1000 caracteres.");
        return e;
    }
}
