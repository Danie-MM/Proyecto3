package cr.petmatchcr.config;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.Optional;

/** Sin el adaptador de A se permite solo el ámbito individual: nunca se inventan permisos. */
@Configuration
public class AmbitosMascotasConfiguracion {
    /** Sin integración explícita con A, ni siquiera un nombre UUID concede una identidad. */
    @Bean
    @ConditionalOnMissingBean(IdentidadMascotas.class)
    IdentidadMascotas identidadSinIntegracion() { return Optional::empty; }

    @Bean
    @ConditionalOnMissingBean(AmbitosMascotas.class)
    AmbitosMascotas ambitosSinIntegracion() { return usuarioId -> Set.of(); }
}
