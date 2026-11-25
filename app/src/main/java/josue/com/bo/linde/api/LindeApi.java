package josue.com.bo.linde.api;

import java.util.List;

import josue.com.bo.linde.modelo.AlertaVista;
import josue.com.bo.linde.modelo.Documento;
import josue.com.bo.linde.modelo.DocumentoDto;
import josue.com.bo.linde.modelo.Expediente;
import josue.com.bo.linde.modelo.InspeccionCampo;
import josue.com.bo.linde.modelo.InspeccionDto;
import josue.com.bo.linde.modelo.LoginDto;
import josue.com.bo.linde.modelo.PoligonoVista;
import josue.com.bo.linde.modelo.PredioDto;
import josue.com.bo.linde.modelo.PredioVista;
import josue.com.bo.linde.modelo.ProductorVista;
import josue.com.bo.linde.modelo.RegistroProductorDto;
import josue.com.bo.linde.modelo.RespuestaLogin;
import josue.com.bo.linde.modelo.ResultadoValidacion;
import josue.com.bo.linde.modelo.RevisionRequisitos;
import josue.com.bo.linde.modelo.Usuario;
import josue.com.bo.linde.modelo.VerticesDto;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/**
 * TODOS los endpoints que usa la app (así nadie más toca este archivo). La URL base es BuildConfig.API_URL
 * («.../api/»), por eso las rutas van sin «/» inicial. Los DELETE responden 204 sin cuerpo: Call&lt;Void&gt;.
 */
public interface LindeApi {

    // === JOSUE ===========================================================================

    @POST("auth/login")
    Call<RespuestaLogin> login(@Body LoginDto datos);

    @POST("auth/registro")
    Call<ProductorVista> registrar(@Body RegistroProductorDto datos);

    @GET("auth/yo")
    Call<Usuario> yo();

    @GET("predios")
    Call<List<PredioVista>> listarPredios();

    @GET("predios/{id}")
    Call<PredioVista> obtenerPredio(@Path("id") int id);

    @POST("predios")
    Call<PredioVista> crearPredio(@Body PredioDto datos);

    @PUT("predios/{id}")
    Call<PredioVista> actualizarPredio(@Path("id") int id, @Body PredioDto datos);

    @DELETE("predios/{id}")
    Call<Void> eliminarPredio(@Path("id") int id);

    @GET("predios/{predioId}/inspecciones")
    Call<List<InspeccionCampo>> listarInspecciones(@Path("predioId") int predioId);

    @POST("predios/{predioId}/inspecciones")
    Call<InspeccionCampo> registrarInspeccion(@Path("predioId") int predioId, @Body InspeccionDto datos);

    @DELETE("inspecciones/{id}")
    Call<Void> eliminarInspeccion(@Path("id") int id);

    // === JOSEP ===========================================================================

    @GET("predios/{predioId}/poligono")
    Call<PoligonoVista> obtenerPoligono(@Path("predioId") int predioId);

    @GET("predios/{predioId}/poligono/historial")
    Call<List<PoligonoVista>> historialPoligono(@Path("predioId") int predioId);

    @PUT("predios/{predioId}/poligono")
    Call<PoligonoVista> guardarPoligono(@Path("predioId") int predioId, @Body VerticesDto vertices);

    @Multipart
    @POST("predios/{predioId}/poligono/importar")
    Call<PoligonoVista> importarPoligono(@Path("predioId") int predioId,
                                         @Part MultipartBody.Part archivo,
                                         @Part("motivo") RequestBody motivo);

    @GET("expedientes")
    Call<List<Expediente>> listarExpedientes();

    @POST("expedientes/predio/{predioId}")
    Call<Expediente> generarExpediente(@Path("predioId") int predioId);

    @GET("expedientes/{codigo}")
    Call<Expediente> obtenerExpediente(@Path("codigo") String codigo);

    @GET("expedientes/{codigo}/geojson")
    Call<ResponseBody> descargarGeoJson(@Path("codigo") String codigo);

    // === SEBASTIAN =======================================================================

    @GET("predios/{predioId}/documentos")
    Call<List<Documento>> listarDocumentos(@Path("predioId") int predioId);

    @POST("predios/{predioId}/documentos")
    Call<Documento> agregarDocumento(@Path("predioId") int predioId, @Body DocumentoDto datos);

    @DELETE("documentos/{id}")
    Call<Void> eliminarDocumento(@Path("id") int id);

    /** La API decide el tipo por la extensión del nombre: la parte debe llevar el nombre real (util.Archivos). */
    @Multipart
    @POST("documentos/{id}/archivo")
    Call<Documento> subirArchivoDocumento(@Path("id") int id, @Part MultipartBody.Part archivo);

    @Streaming
    @GET("documentos/{id}/archivo")
    Call<ResponseBody> descargarArchivoDocumento(@Path("id") int id);

    @GET("predios/{id}/requisitos")
    Call<RevisionRequisitos> requisitosEnvio(@Path("id") int predioId);

    /** Puede tardar (consulta satelital): ClienteApi usa 60 s de lectura. */
    @POST("predios/{id}/enviar")
    Call<ResultadoValidacion> enviarARevision(@Path("id") int predioId);

    @GET("predios/{id}/validaciones")
    Call<List<ResultadoValidacion>> historialValidaciones(@Path("id") int predioId);

    @GET("alertas")
    Call<List<AlertaVista>> listarAlertas(@Query("predioId") int predioId);

    /** Consulta pública (sin cuenta): el CI del productor viene enmascarado. */
    @GET("consulta/{codigo}")
    Call<Expediente> consultarExpediente(@Path("codigo") String codigo);
}
