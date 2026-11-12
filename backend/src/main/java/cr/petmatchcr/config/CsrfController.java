package cr.petmatchcr.config;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Angular pide un token ligado a la cookie de sesión antes de escribir. No autentica usuarios. */
@RestController
public class CsrfController {
    @GetMapping("/api/v1/sesion/csrf")
    public ResponseEntity<TokenRespuesta> obtener(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TokenRespuesta(token.getToken()));
    }

    public record TokenRespuesta(String token) {}
}
