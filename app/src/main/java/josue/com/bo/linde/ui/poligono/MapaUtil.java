package josue.com.bo.linde.ui.poligono;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import josue.com.bo.linde.BuildConfig;
import josue.com.bo.linde.modelo.Coordenada;

/**
 * Ayudas del módulo de Josep (polígono, historial y expedientes): preparar el mapa de osmdroid,
 * dibujar polígonos y puntos, y dar formato a fechas y superficies. No tiene lógica de negocio.
 */
public final class MapaUtil {

    public static final int COLOR_PRIMARIO = 0xFF15524A;
    public static final int COLOR_ACCION = 0xFF2E7D32;
    public static final int COLOR_INFO = 0xFF2F8FB0;
    public static final int COLOR_ERROR = 0xFFB3261E;
    public static final int COLOR_AVISO = 0xFFB26A00;
    public static final int COLOR_GRIS = 0xFF6B6B6B;

    /** Centro por defecto cuando no hay polígono ni permiso de ubicación. */
    public static final GeoPoint SANTA_CRUZ = new GeoPoint(-17.7833, -63.1821);

    private static final Locale BOLIVIA = new Locale("es", "BO");

    private MapaUtil() {
    }

    /** Debe llamarse antes de setContentView: osmdroid exige un User-Agent propio. */
    public static void configurar(Context contexto) {
        Context app = contexto.getApplicationContext();
        Configuration.getInstance().load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
    }

    public static void preparar(MapView mapa) {
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapa.setMinZoomLevel(4.0);
        mapa.getController().setZoom(13.0);
        mapa.getController().setCenter(SANTA_CRUZ);
    }

    /** Respeta la barra de estado y la de navegación (edge-to-edge en Android 15+). */
    public static void ajustarBordes(View raiz) {
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Coordenada de la API (longitud, latitud) a punto de osmdroid (latitud, longitud). */
    public static GeoPoint punto(Coordenada c) {
        return new GeoPoint(c.getLat(), c.getLon());
    }

    public static List<GeoPoint> puntos(List<Coordenada> vertices) {
        List<GeoPoint> lista = new ArrayList<>();
        if (vertices == null) return lista;
        for (Coordenada c : vertices) lista.add(punto(c));
        return lista;
    }

    /**
     * Crea la capa de un polígono (o un marcador si es un solo punto). No la agrega al mapa: quien llama decide
     * en qué posición va. Los toques sobre la capa no se consumen, para que el mapa siga recibiendo puntos.
     */
    public static Overlay crearCapa(MapView mapa, List<Coordenada> vertices, int color, boolean punteado, String titulo) {
        Context contexto = mapa.getContext();
        if (vertices.size() == 1) {
            Marker marcador = new Marker(mapa);
            marcador.setPosition(punto(vertices.get(0)));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setTitle(titulo);
            marcador.setAlpha(punteado ? 0.55f : 1f);
            return marcador;
        }
        Polygon poligono = new Polygon(mapa);
        poligono.setPoints(puntos(vertices));
        poligono.setTitle(titulo);
        Paint borde = poligono.getOutlinePaint();
        borde.setColor(color);
        borde.setStrokeWidth(dp(contexto, punteado ? 3 : 4));
        Paint relleno = poligono.getFillPaint();
        if (punteado) {
            borde.setPathEffect(new DashPathEffect(new float[]{dp(contexto, 10), dp(contexto, 7)}, 0));
            relleno.setColor(0x00000000);
        } else {
            relleno.setColor((color & 0x00FFFFFF) | 0x40000000);
        }
        poligono.setOnClickListener((p, m, posicion) -> false);
        return poligono;
    }

    /** Centra el mapa en los puntos dados; con uno solo, se acerca a ese punto. */
    public static void centrar(MapView mapa, List<GeoPoint> puntos) {
        if (puntos == null || puntos.isEmpty()) return;
        if (puntos.size() == 1) {
            mapa.getController().setZoom(17.0);
            mapa.getController().setCenter(puntos.get(0));
            return;
        }
        BoundingBox caja = BoundingBox.fromGeoPointsSafe(puntos);
        int borde = dp(mapa.getContext(), 48);
        if (mapa.getWidth() == 0 || mapa.getHeight() == 0) {
            mapa.addOnFirstLayoutListener((v, l, t, r, b) -> mapa.zoomToBoundingBox(caja, false, borde));
        } else {
            mapa.zoomToBoundingBox(caja, true, borde);
        }
    }

    /** Círculo con el número del vértice: verde si vino del GPS, azul si se tocó en el mapa. */
    public static Drawable iconoNumerado(Context contexto, int numero, boolean gps) {
        int tam = dp(contexto, 28);
        Bitmap imagen = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(imagen);
        float centro = tam / 2f;

        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(gps ? COLOR_ACCION : COLOR_INFO);
        lienzo.drawCircle(centro, centro, centro - 1, fondo);

        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(dp(contexto, 2));
        borde.setColor(0xFFFFFFFF);
        lienzo.drawCircle(centro, centro, centro - dp(contexto, 2), borde);

        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(0xFFFFFFFF);
        texto.setTextSize(dp(contexto, numero > 99 ? 10 : 12));
        texto.setTypeface(Typeface.DEFAULT_BOLD);
        texto.setTextAlign(Paint.Align.CENTER);
        float y = centro - (texto.descent() + texto.ascent()) / 2f;
        lienzo.drawText(String.valueOf(numero), centro, y, texto);

        return new BitmapDrawable(contexto.getResources(), imagen);
    }

    public static int dp(Context contexto, int valor) {
        return Math.round(valor * contexto.getResources().getDisplayMetrics().density);
    }

    /** Redondeo a 6 decimales (unos 11 cm), como la web. */
    public static double seisDecimales(double valor) {
        return Math.round(valor * 1_000_000d) / 1_000_000d;
    }

    /** «12,3456 ha» con el formato de Bolivia. */
    public static String hectareas(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(BOLIVIA);
        formato.setMaximumFractionDigits(4);
        return formato.format(valor) + " ha";
    }

    /** «±4,2 m». */
    public static String metros(double valor) {
        return String.format(BOLIVIA, "±%.1f m", valor);
    }

    /**
     * Fecha ISO-8601 de la API («2026-11-25T14:03:12.1234567Z») a «25/11/2026 10:03» en la hora del celular.
     * Si no se puede leer, devuelve el texto tal cual.
     */
    public static String fecha(String iso) {
        if (iso == null) return "";
        if (iso.length() < 19) return iso;
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
            Date d = entrada.parse(iso.substring(0, 19));
            if (d == null) return iso;
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", BOLIVIA).format(d);
        } catch (ParseException | IndexOutOfBoundsException e) {
            return iso;
        }
    }

    /** Fecha sin hora («aaaa-mm-dd», DateOnly de la API) a «dd/mm/aaaa». */
    public static String fechaCorta(String aaaaMmDd) {
        if (aaaaMmDd == null) return "";
        String[] partes = aaaaMmDd.split("-");
        if (partes.length != 3) return aaaaMmDd;
        return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    /** Nombre del estado del predio como en la web. */
    public static String estadoPredio(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case "ConAlertas":
                return "Con alertas";
            case "Aprobado":
                return "Aprobado";
            case "Registrado":
                return "Registrado";
            default:
                return estado;
        }
    }
}
