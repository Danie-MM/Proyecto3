package cr.petmatchcr.entorno;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Repository
class RepositorioEntorno {
    private final JdbcTemplate jdbc;

    RepositorioEntorno(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    RegistroEntorno guardar(String texto) {
        var registro = new RegistroEntorno(UUID.randomUUID(), texto, Instant.now().truncatedTo(ChronoUnit.MICROS));
        jdbc.update("CALL create_prueba_entorno_pr(?, ?, ?)", registro.id(), texto, Timestamp.from(registro.creadoEn()));
        return registro;
    }

    Optional<RegistroEntorno> consultar(UUID id) {
        return jdbc.query("SELECT id, texto, creado_en FROM ret_id_prueba_entorno_pr(?)",
                (fila, indice) -> new RegistroEntorno(fila.getObject("id", UUID.class),
                        fila.getString("texto"), fila.getTimestamp("creado_en").toInstant()), id)
                .stream().findFirst();
    }
}
