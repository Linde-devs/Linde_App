package josue.com.bo.linde.modelo;

import java.util.List;

public class VerticesDto {
    private final List<CoordenadaDto> vertices;
    private final String motivo;

    public VerticesDto(List<CoordenadaDto> vertices, String motivo) {
        this.vertices = vertices;
        this.motivo = motivo;
    }

    public List<CoordenadaDto> getVertices() {
        return vertices;
    }

    public String getMotivo() {
        return motivo;
    }
}
