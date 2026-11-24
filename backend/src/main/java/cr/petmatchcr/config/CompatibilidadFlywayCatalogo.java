package cr.petmatchcr.config;

import java.util.Arrays;
import org.flywaydb.core.api.CoreMigrationType;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

@Component
class CompatibilidadFlywayCatalogo implements FlywayConfigurationCustomizer {
    private static final MigrationVersion VERSION_LEGADA = MigrationVersion.fromVersion("11");
    private static final String SCRIPT_LEGADO = "V11__datos_iniciales_comportamientos.sql";
    private static final Integer CHECKSUM_LEGADO = 1042505516;
    static final Location UBICACION_LEGADA = new Location("classpath:db/compatibilidad-catalogo-v11");

    @Override
    public void customize(FluentConfiguration configuracion) {
        // Consultar el historial en el esquema configurado conserva la validación normal de Flyway.
        // V17 instala los datos en bases nuevas; V11 solo se resuelve donde ya se ejecutó ese archivo.
        boolean aplicada = Arrays.stream(configuracion.load().info().applied())
                .anyMatch(migracion -> migracion.getState() == MigrationState.MISSING_SUCCESS
                        && VERSION_LEGADA.equals(migracion.getVersion())
                        && CoreMigrationType.SQL == migracion.getType()
                        && SCRIPT_LEGADO.equals(migracion.getScript())
                        && CHECKSUM_LEGADO.equals(migracion.getAppliedChecksum()));
        if (aplicada) {
            Location[] ubicaciones = configuracion.getLocations();
            Location[] compatibles = Arrays.copyOf(ubicaciones, ubicaciones.length + 1);
            compatibles[ubicaciones.length] = UBICACION_LEGADA;
            configuracion.locations(compatibles);
        }
    }
}
