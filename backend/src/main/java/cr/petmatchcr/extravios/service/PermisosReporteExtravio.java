package cr.petmatchcr.extravios.service;

import cr.petmatchcr.mascotas.api.MascotasParaExtravios;
import cr.petmatchcr.mascotas.service.ErrorMascota;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gestiona la comprobación de permisos sobre
 * los reportes de mascotas extraviadas.
 */
@Service
@RequiredArgsConstructor
public class PermisosReporteExtravio {

    private final MascotasParaExtravios mascotas;

    /**
     * Comprueba si el usuario autenticado es
     * el responsable vigente de la mascota.
     *
     * @param mascotaId identificador de la mascota
     * @return true si es el responsable vigente
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean esResponsable(UUID mascotaId) {

        try {
            mascotas.comprobarResponsable(mascotaId);
            return true;

        } catch (ErrorMascota ex) {

            if (ex.getEstado() == 404
                    && "NO_ENCONTRADO".equals(ex.getCodigo())) {
                return false;
            }

            throw ex;
        }
    }
}