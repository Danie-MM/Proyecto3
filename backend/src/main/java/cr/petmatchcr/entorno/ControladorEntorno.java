package cr.petmatchcr.entorno;

import cr.petmatchcr.comun.ErrorApi;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.UUID;

@RestController
@Profile({"local", "test"})
@RequestMapping("/api/v1/pruebas-entorno")
class ControladorEntorno {
    private final RepositorioEntorno registros;

    ControladorEntorno(RepositorioEntorno registros) {
        this.registros = registros;
    }

    @PostMapping(consumes = "application/json")
    ResponseEntity<?> guardar(@RequestBody JsonNode datos) {
        if (!datos.isObject() || datos.size() != 1 || !datos.path("texto").isString()) {
            return invalido("texto", "Enviá únicamente el campo texto como una cadena.");
        }
        String texto = datos.get("texto").stringValue().strip();
        if (texto.isEmpty() || texto.indexOf(0) >= 0 || texto.codePointCount(0, texto.length()) > 80) {
            return invalido("texto", "Usá de 1 a 80 caracteres sin caracteres nulos.");
        }
        RegistroEntorno registro = registros.guardar(texto);
        return ResponseEntity.created(URI.create("/api/v1/pruebas-entorno/" + registro.id())).body(registro);
    }

    @GetMapping("/{id}")
    ResponseEntity<?> consultar(@PathVariable String id) {
        UUID clave;
        try {
            clave = UUID.fromString(id);
        } catch (IllegalArgumentException invalida) {
            return invalido("id", "Usá un UUID válido.");
        }
        if (!clave.toString().equalsIgnoreCase(id)) {
            return invalido("id", "Usá un UUID válido.");
        }
        var registro = registros.consultar(clave);
        if (registro.isEmpty()) {
            return ResponseEntity.status(404).body(ErrorApi.crear("NO_ENCONTRADO", "No encontramos ese registro"));
        }
        return ResponseEntity.ok(registro.get());
    }

    private ResponseEntity<ErrorApi> invalido(String campo, String mensaje) {
        return ResponseEntity.badRequest().body(ErrorApi.crear("VALIDACION", "Revisá los datos enviados.",
                new ErrorApi.Campo(campo, mensaje)));
    }
}
