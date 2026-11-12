package cr.petmatchcr.config;

import cr.petmatchcr.acceso.api.IdentidadActual;
import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/** B comparte la identidad de A; sin un padrón organizativo integrado solo hay ámbito individual. */
@Configuration
public class AmbitosMascotasConfiguracion {
    /** La identidad siempre procede del puerto de A, salvo adaptadores explícitos de demo/test. */
    @Bean
    @ConditionalOnMissingBean(IdentidadMascotas.class)
    IdentidadMascotas identidadIntegrada(IdentidadActual identidad) { return identidad::usuarioActual; }

    @Bean
    @ConditionalOnMissingBean(AmbitosMascotas.class)
    AmbitosMascotas ambitosSinIntegracion() { return usuarioId -> Set.of(); }
}
