package josue.com.bo.linde.modelo;

/** Documento de un predio. Los campos archivo* son null hasta que se sube el archivo. */
public class Documento {
    private int id;
    private int predioId;
    private TipoDocumento tipo;
    private String numero;
    private String titularCi;
    private Double superficieHa;
    private String fechaRegistro;
    private String archivoNombre;
    private String archivoTipo;
    private Long archivoBytes;
    private String archivoFecha;
    private boolean tieneArchivo;

    public int getId() {
        return id;
    }

    public int getPredioId() {
        return predioId;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getNumero() {
        return numero;
    }

    public String getTitularCi() {
        return titularCi;
    }

    public Double getSuperficieHa() {
        return superficieHa;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public String getArchivoTipo() {
        return archivoTipo;
    }

    public Long getArchivoBytes() {
        return archivoBytes;
    }

    public String getArchivoFecha() {
        return archivoFecha;
    }

    public boolean isTieneArchivo() {
        return tieneArchivo;
    }
}
