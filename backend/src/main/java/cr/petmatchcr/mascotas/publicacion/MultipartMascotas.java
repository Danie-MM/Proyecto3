package cr.petmatchcr.mascotas.publicacion;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MultipartMascotas {
    @Bean
    MultipartConfigElement multipartConfigElement() {
        // El contenedor admite 5 MB de archivo más el encabezado y el campo de versión.
        return new MultipartConfigElement("", FotografiaVerificada.LIMITE, 6_000_000, 0);
    }
}
