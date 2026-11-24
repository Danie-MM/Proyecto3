package cr.petmatchcr.mascotas.clinica;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Contratos de la primera iteración. Autor, origen y versión solo existen en las respuestas. */
public final class DatosClinicos {
    private DatosClinicos() { }

    public record NuevaVacuna(String tipo, LocalDate fecha, String nombre, String descripcion) { }
    public record Concesion(UUID beneficiarioUsuarioId, String fundamento, String alcance) { }
    public record Revocacion(String motivo, Integer versionEsperada) { }

    public record Evento(UUID id, Instant creadoEn, int version, UUID mascotaId, String tipo,
                         LocalDate fecha, UUID autorUsuarioId, String origen, String nombre,
                         String descripcion, int numeroRevision, BigDecimal dosis, String unidad,
                         String frecuencia, String periodo, String clasificacionEnfermedad,
                         LocalDate fechaFinEnfermedad) { }

    public record Permiso(UUID id, Instant creadoEn, int version, UUID mascotaId,
                          UUID beneficiarioUsuarioId, String fundamento, String alcance,
                          UUID otorgadoPorUsuarioId, boolean activo, Instant revocadoEn,
                          UUID revocadoPorUsuarioId, String motivoRevocacion) { }

    public record Pagina<T>(List<T> elementos, int pagina, int limite, long totalElementos) { }
}
