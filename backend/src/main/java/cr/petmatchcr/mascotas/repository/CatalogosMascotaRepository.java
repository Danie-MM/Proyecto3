package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.dto.CatalogosMascotaRespuesta;
import cr.petmatchcr.mascotas.model.Especie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class CatalogosMascotaRepository {

    private final JdbcTemplate jdbc;

    public CatalogosMascotaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CatalogosMascotaRespuesta.Etiqueta> listarComportamientos() {
        return jdbc.query("SELECT id, nombre, activa FROM ret_all_etiqueta_comportamiento_pr()",
                (fila, indice) -> new CatalogosMascotaRespuesta.Etiqueta(
                        fila.getObject("id", UUID.class), fila.getString("nombre"), fila.getBoolean("activa")));
    }

    /** Consulta V5; la validación definitiva se repite bajo bloqueo al guardar. */
    public boolean existeEspecieActiva(String codigo) {
        Boolean resultado = jdbc.queryForObject(
                "SELECT existe_especie_activa_pr(?)",
                Boolean.class,
                codigo
        );
        return Boolean.TRUE.equals(resultado);
    }

    /** Las especies vienen de PostgreSQL: el código no necesita una lista fija. */
    public List<Especie> listarEspecies() {
        return jdbc.query("SELECT id, codigo, nombre, activa FROM ret_all_especie_pr()",
                (fila, indice) -> new Especie(fila.getObject("id", UUID.class), fila.getString("codigo"),
                        fila.getString("nombre"), fila.getBoolean("activa")));
    }
}
