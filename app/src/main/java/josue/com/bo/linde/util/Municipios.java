package josue.com.bo.linde.util;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Los 9 departamentos de Bolivia y sus 340 municipios (assets/municipios.json, copiado de divisionPolitica.js).
 * La API valida que el municipio sea del departamento; aquí solo se usa para las listas y para avisar antes.
 */
public final class Municipios {

    private static Map<String, List<String>> datos;

    private Municipios() {
    }

    private static synchronized Map<String, List<String>> datos(Context contexto) {
        if (datos != null) return datos;
        try (Reader lector = new InputStreamReader(
                contexto.getApplicationContext().getAssets().open("municipios.json"), StandardCharsets.UTF_8)) {
            datos = new Gson().fromJson(lector, new TypeToken<LinkedHashMap<String, List<String>>>() {
            }.getType());
        } catch (IOException | RuntimeException e) {
            datos = new LinkedHashMap<>();
        }
        return datos;
    }

    public static List<String> departamentos(Context contexto) {
        return new ArrayList<>(datos(contexto).keySet());
    }

    public static List<String> de(Context contexto, @Nullable String departamento) {
        if (departamento == null) return Collections.emptyList();
        List<String> lista = datos(contexto).get(departamento);
        return lista == null ? Collections.emptyList() : new ArrayList<>(lista);
    }

    /** Todos los municipios, sin repetir y en orden alfabético (para el registro del productor). */
    public static List<String> todos(Context contexto) {
        Collator orden = Collator.getInstance(Formatos.BOLIVIA);
        TreeSet<String> conjunto = new TreeSet<>(orden);
        for (List<String> lista : datos(contexto).values()) conjunto.addAll(lista);
        return new ArrayList<>(conjunto);
    }

    /** Nombre oficial del municipio si es de ese departamento (sin mayúsculas ni tildes), o null. */
    @Nullable
    public static String municipioDe(Context contexto, @Nullable String departamento, @Nullable String municipio) {
        if (municipio == null) return null;
        String buscado = normalizar(municipio);
        for (String m : de(contexto, departamento)) {
            if (normalizar(m).equals(buscado)) return m;
        }
        return null;
    }

    /** Nombre oficial del municipio en cualquier departamento, o null. */
    @Nullable
    public static String municipioOficial(Context contexto, @Nullable String municipio) {
        if (municipio == null) return null;
        String buscado = normalizar(municipio);
        for (String m : todos(contexto)) {
            if (normalizar(m).equals(buscado)) return m;
        }
        return null;
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Formatos.BOLIVIA)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
