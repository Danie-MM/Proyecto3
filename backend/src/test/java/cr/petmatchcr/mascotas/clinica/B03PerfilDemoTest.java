package cr.petmatchcr.mascotas.clinica;

import static org.assertj.core.api.Assertions.assertThat;

import cr.petmatchcr.mascotas.demo.AccesoClinicoDemo;
import cr.petmatchcr.mascotas.demo.ActoresDemoB01;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Profiles;

class B03PerfilDemoTest {
    @Test
    void conservaLosCuatroActoresYAgregaElVeterinarioDeLaDemo() {
        var actores = new ActoresDemoB01().listar();
        assertThat(actores).extracting(ActoresDemoB01.ActorDemo::nombre).containsExactly(
                "Responsable de Luna","Otro responsable","Representante del Refugio A",
                "Representante del Refugio B","Veterinario de prueba");
        assertThat(actores.get(4).id()).isEqualTo(UUID.fromString("55555555-5555-4555-8555-555555555555"));
    }

    @Test
    void lasFuentesFlywayYElAdaptadorRequierenLaCombinacionDeDemostracion() {
        comprobar(true, "local", "demo-b01");
        comprobar(false, "local", "integracion");
        comprobar(false, "demo-b01");
        comprobar(false, "local", "demo-b01", "integracion");
    }

    private void comprobar(boolean demoEsperada, String... perfiles) {
        try (var ctx = new GenericApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles(perfiles);
            new ConfigDataApplicationContextInitializer().initialize(ctx);
            var ubicaciones = ctx.getEnvironment().getProperty("spring.flyway.locations", "");
            assertThat(ubicaciones.contains("db/demo-b03")).isEqualTo(demoEsperada);
            var perfil = Profiles.of(AccesoClinicoDemo.class.getAnnotation(Profile.class).value());
            assertThat(ctx.getEnvironment().acceptsProfiles(perfil)).isEqualTo(demoEsperada);
        }
    }
}
