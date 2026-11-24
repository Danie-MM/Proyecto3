package cr.petmatchcr.mascotas.dto;

import java.util.List;

public record PaginaMascotasPublicas(List<MascotaPublicaRespuesta> elementos, int pagina,
                                   int limite, long totalElementos) {}
