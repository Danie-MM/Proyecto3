package cr.petmatchcr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/** Inicia el backend; el bloque A añadirá la autenticación de usuarios. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PetMatchCrApplication {

    /** Arranca Spring con la configuración del perfil elegido. */
    public static void main(String[] args) {
        SpringApplication.run(PetMatchCrApplication.class, args);
    }
}
