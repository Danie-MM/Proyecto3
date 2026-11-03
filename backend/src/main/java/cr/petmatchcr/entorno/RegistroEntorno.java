package cr.petmatchcr.entorno;

import java.time.Instant;
import java.util.UUID;

public record RegistroEntorno(UUID id, String texto, Instant creadoEn) {}
