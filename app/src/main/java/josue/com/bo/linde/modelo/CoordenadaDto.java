package josue.com.bo.linde.modelo;

/** Vértice que se envía: precisionM es null si no viene del GPS. */
public class CoordenadaDto {
    private final double lon;
    private final double lat;
    private final Double precisionM;

    public CoordenadaDto(double lon, double lat, Double precisionM) {
        this.lon = lon;
        this.lat = lat;
        this.precisionM = precisionM;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public Double getPrecisionM() {
        return precisionM;
    }
}
