package cr.petmatchcr.mascotas.publicacion;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;

class FotografiasInicialesDemoTest {
    @Test
    void verificaLosCuatroArchivosYLasTresTarjetasDistintasDelTitularDeLuna() throws Exception {
        var huellas = new HashMap<String,String>();
        for (var mascota : List.of("luna","sol","nube","coco")) {
            var recurso = new ClassPathResource("mascotas-demo/" + mascota + ".jpg");
            var foto = FotografiaVerificada.leer(recurso.getContentAsByteArray());
            assertThat(foto.mime()).isEqualTo("image/jpeg");
            byte[] huella = java.security.MessageDigest.getInstance("SHA-256").digest(foto.contenido());
            huellas.put(mascota, HexFormat.of().formatHex(huella));
        }
        // Milo y Kira reutilizan los recursos de Nube y Sol, respectivamente.
        assertThat(List.of(huellas.get("luna"),huellas.get("nube"),huellas.get("sol"))).doesNotHaveDuplicates();
    }

    @Test
    void elInicializadorSoloSeActivaEnLaDemoLocal() {
        var expresion = Profiles.of(FotografiasInicialesDemo.class.getAnnotation(Profile.class).value());
        var entorno = new StandardEnvironment();
        entorno.setActiveProfiles("local","demo-b01");
        assertThat(entorno.acceptsProfiles(expresion)).isTrue();
        for (var perfiles : List.of("local", "demo-b01", "local,integracion", "local,demo-b01,integracion")) {
            entorno.setActiveProfiles(perfiles.split(","));
            assertThat(entorno.acceptsProfiles(expresion)).isFalse();
        }
    }
}
