package josue.com.bo.linde.modelo;

/** POST predios/{predioId}/documentos. */
public class DocumentoDto {
    private final TipoDocumento tipo;
    private final String numero;
    private final String titularCi;
    private final Double superficieHa;

    public DocumentoDto(TipoDocumento tipo, String numero, String titularCi, Double superficieHa) {
        this.tipo = tipo;
        this.numero = numero;
        this.titularCi = titularCi;
        this.superficieHa = superficieHa;
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
}
