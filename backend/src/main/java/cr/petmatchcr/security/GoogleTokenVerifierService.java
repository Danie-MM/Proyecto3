package cr.petmatchcr.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Autora: Daniela Morales
 * Versión: 01
 *validar los tokens que nos manda el frontend desde Google.
 * Básicamente se fija que el token sea real y no esté alterado.
 */
@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    // Levantamos el cliente ID desde application.properties
    public GoogleTokenVerifierService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Valida el token y devuelve los datos del perfil.
     * Si no sirve o no se encuentra nada = null.
     */
    public GoogleIdToken.Payload verificar(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdToken token = verifier.verify(idToken);
        if (token == null) {
            return null;
        }
        return token.getPayload();
    }
}
