package josue.com.bo.linde.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import josue.com.bo.linde.BuildConfig;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Archivos para subir a la API. La API decide el tipo por la extensión del nombre y además revisa los primeros
 * bytes, por eso cada parte lleva el NOMBRE REAL del archivo (OpenableColumns.DISPLAY_NAME).
 */
public final class Archivos {

    /** Debe coincidir con android:authorities del FileProvider en el manifest. */
    public static final String AUTORIDAD = BuildConfig.APPLICATION_ID + ".archivos";
    public static final long MAXIMO_BYTES = 10L * 1024 * 1024;

    private Archivos() {
    }

    /** Nombre real del archivo elegido, con su extensión. */
    public static String nombreReal(Context contexto, Uri uri) {
        try (Cursor c = contexto.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String nombre = c.getString(0);
                if (nombre != null && !nombre.isEmpty()) return nombre;
            }
        } catch (RuntimeException ignorada) {
            // Se usa el último segmento de la ruta.
        }
        String segmento = uri.getLastPathSegment();
        return segmento != null ? segmento : "archivo";
    }

    /** Tamaño en bytes, o -1 si no se sabe. */
    public static long tamano(Context contexto, Uri uri) {
        try (Cursor c = contexto.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE},
                null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (RuntimeException ignorada) {
            // Tamaño desconocido.
        }
        return -1;
    }

    /**
     * Crea la parte multipart «campo» con el contenido del Uri y su nombre real.
     * Lee el archivo completo: llámalo desde un hilo de fondo (Executor), no en el hilo principal.
     */
    public static MultipartBody.Part parte(Context contexto, Uri uri, String campo) throws IOException {
        String nombre = nombreReal(contexto, uri);
        String tipo = contexto.getContentResolver().getType(uri);
        byte[] datos = leer(contexto, uri);
        RequestBody cuerpo = RequestBody.create(datos, MediaType.parse(tipo != null ? tipo : "application/octet-stream"));
        return MultipartBody.Part.createFormData(campo, nombre, cuerpo);
    }

    /** Parte multipart desde un archivo local (por ejemplo la foto de la cámara). */
    public static MultipartBody.Part parte(File archivo, String campo, String tipo) {
        RequestBody cuerpo = RequestBody.create(archivo, MediaType.parse(tipo));
        return MultipartBody.Part.createFormData(campo, archivo.getName(), cuerpo);
    }

    public static byte[] leer(Context contexto, Uri uri) throws IOException {
        try (InputStream entrada = contexto.getContentResolver().openInputStream(uri)) {
            if (entrada == null) throw new IOException("No se pudo abrir el archivo.");
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            long total = 0;
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                total += leidos;
                if (total > MAXIMO_BYTES) throw new IOException("El archivo pesa más de 10 MB.");
                salida.write(bloque, 0, leidos);
            }
            return salida.toByteArray();
        }
    }

    /** Archivo vacío «foto_aaaammdd_hhmmss.jpg» en la caché, para ActivityResultContracts.TakePicture. */
    public static File crearArchivoFoto(Context contexto) throws IOException {
        File carpeta = new File(contexto.getCacheDir(), "fotos");
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear la carpeta de fotos.");
        String nombre = "foto_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
        File foto = new File(carpeta, nombre);
        if (!foto.exists() && !foto.createNewFile()) throw new IOException("No se pudo crear el archivo de la foto.");
        return foto;
    }

    /** Uri content:// del archivo para dárselo a la cámara del sistema (sin permiso de cámara). */
    public static Uri uriPara(Context contexto, File archivo) {
        return FileProvider.getUriForFile(contexto, AUTORIDAD, archivo);
    }

    /** Extensión en minúsculas («.pdf») o "" si no tiene. */
    public static String extension(@Nullable String nombre) {
        if (nombre == null) return "";
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto).toLowerCase(Locale.ROOT);
    }
}
