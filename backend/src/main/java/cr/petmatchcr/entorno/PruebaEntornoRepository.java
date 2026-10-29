package cr.petmatchcr.entorno;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/** Se comunica con PostgreSQL mediante las rutinas creadas por Flyway. */
@Repository
@Profile({"local", "test"})
public class PruebaEntornoRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Recibe la conexión administrada por Spring, sin contraseñas en el código. */
    public PruebaEntornoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Guarda con parámetros: el texto recibido nunca se concatena como SQL. */
    public void crear(UUID id, String texto, Instant creadoEn) {
        jdbcTemplate.update(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "CALL create_prueba_entorno_pr(?, ?, ?)");
            statement.setObject(1, id);
            statement.setString(2, texto);
            statement.setObject(3, OffsetDateTime.ofInstant(creadoEn, ZoneOffset.UTC));
            return statement;
        });
    }

    /** Lee un registro; Optional vacío significa que el UUID no existe. */
    public Optional<PruebaEntornoRespuesta> consultar(UUID id) {
        return jdbcTemplate.query(
                "SELECT id, texto, creado_en FROM ret_id_prueba_entorno_pr(?)",
                (resultSet, rowNumber) -> new PruebaEntornoRespuesta(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("texto"),
                        resultSet.getObject("creado_en", OffsetDateTime.class).toInstant()),
                id).stream().findFirst();
    }
}
