package cr.petmatchcr.mascotas.dto;

import java.util.List;

/** Una página incluye el total para que Angular pueda ofrecer anterior y siguiente. */
public record PaginaMascotasRespuesta(List<MascotaPrivadaRespuesta> elementos,
                                     int pagina, int limite, long totalElementos) {}
