package josue.com.bo.linde.modelo;

import java.util.List;

/** GET predios/{id}/requisitos. */
public class RevisionRequisitos {
    private int predioId;
    private boolean completo;
    private List<Requisito> requisitos;

    public int getPredioId() {
        return predioId;
    }

    public boolean isCompleto() {
        return completo;
    }

    public List<Requisito> getRequisitos() {
        return requisitos;
    }
}
