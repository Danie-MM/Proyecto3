package cr.petmatchcr.config;

import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CompatibilidadFlywayCatalogoTest {
    private static final Location MIGRACIONES = new Location("classpath:db/migration");
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired CompatibilidadFlywayCatalogo compatibilidad;

    @Test
    void reconoceLaMigracionOriginalSinRepetirEtiquetasNiAlterarSuHistorial() {
        String esquema = esquemaDePrueba();
        try {
            instalarV11(esquema);
            var actual = configuracion(esquema);
            compatibilidad.customize(actual);
            assertThat(actual.getLocations()).contains(MIGRACIONES, CompatibilidadFlywayCatalogo.UBICACION_LEGADA);

            Flyway actualizada = actual.load();
            actualizada.migrate();
            actualizada.validate();
            assertThat(versionesAplicadas(actualizada)).contains("11", "14", "15", "16", "17");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + esquema + ".etiqueta_comportamiento",
                    Long.class)).isEqualTo(3L);
        } finally {
            retirarEsquemaDePrueba(esquema);
        }
    }

    @Test
    void mantieneV11FueraDeUnaBaseNueva() {
        String esquema = esquemaDePrueba();
        try {
            var actual = configuracion(esquema);
            compatibilidad.customize(actual);
            assertThat(actual.getLocations()).containsExactly(MIGRACIONES);
            Flyway inicial = actual.load();
            inicial.migrate();
            assertThat(versionesAplicadas(inicial)).contains("17").doesNotContain("11");
        } finally {
            retirarEsquemaDePrueba(esquema);
        }
    }

    @Test
    void mantieneElErrorDeValidacionParaUnHistorialDistinto() {
        String esquema = esquemaDePrueba();
        try {
            instalarV11(esquema);
            jdbc.update("UPDATE " + esquema + ".flyway_schema_history SET checksum = checksum + 1 WHERE version = '11'");
            var actual = configuracion(esquema);
            compatibilidad.customize(actual);
            assertThat(actual.getLocations()).containsExactly(MIGRACIONES);
            assertThatThrownBy(() -> actual.load().migrate()).isInstanceOf(FlywayException.class)
                    .hasMessageContaining("11");
        } finally {
            retirarEsquemaDePrueba(esquema);
        }
    }

    private void instalarV11(String esquema) {
        configuracion(esquema).locations(MIGRACIONES, CompatibilidadFlywayCatalogo.UBICACION_LEGADA)
                .target(MigrationVersion.fromVersion("11")).load().migrate();
    }

    private FluentConfiguration configuracion(String esquema) {
        return Flyway.configure(flyway.getConfiguration().getClassLoader())
                .configuration(flyway.getConfiguration())
                .locations(MIGRACIONES)
                .schemas(esquema)
                .defaultSchema(esquema);
    }

    private static String esquemaDePrueba() {
        return "pruebas_catalogo_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void retirarEsquemaDePrueba(String esquema) {
        jdbc.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
    }

    private static String[] versionesAplicadas(Flyway instalada) {
        return Arrays.stream(instalada.info().applied()).filter(MigrationInfo::isVersioned)
                .map(migracion -> migracion.getVersion().getVersion()).toArray(String[]::new);
    }
}
