package cr.petmatchcr.acceso.services;

import cr.petmatchcr.acceso.api.IdentidadActual;
import cr.petmatchcr.acceso.dto.OrganizacionResponse;
import cr.petmatchcr.acceso.dto.OrganizacionMiembroResponse;
import cr.petmatchcr.acceso.entities.*;
import cr.petmatchcr.acceso.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Versión: 01
 * Autora: Daniela Morales
 *
 * Descripción: Esta clase permite registrar organizaciones
 * y administrar sus miembros y permisos.
 */
@Service
@RequiredArgsConstructor
public class OrganizacionService {

    private final OrganizacionRepository organizaciones;
    private final OrganizacionMiembroRepository miembros;
    private final UsuarioRepository usuarios;
    private final IdentidadActual identidad;
    private final JdbcTemplate jdbc;

    /**
     * Busca al usuario que tiene la sesión iniciada.
     */
    private Usuario usuarioActual() {
        UUID id = identidad.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));

        return usuarios.findByIdPublico(id)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    /**
     * Registra una organización y asigna al creador
     * como su primer administrador.
     */
    @Transactional
    public Organizacion crear(String nombre) {
        Usuario usuario = usuarioActual();

        if (usuario.getRol() != RolUsuario.REFUGIO
                && usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tiene permiso para crear organizaciones");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }

        Organizacion organizacion = new Organizacion();
        organizacion.setNombre(nombre.trim());
        organizacion.setActiva(true);
        organizaciones.save(organizacion);

        OrganizacionMiembro miembro = new OrganizacionMiembro();
        miembro.setOrganizacion(organizacion);
        miembro.setUsuario(usuario);
        miembro.setPuedeAdministrar(true);
        miembro.setActivo(true);
        miembros.saveAndFlush(miembro);

        return organizacion;
    }

    /**
     * Comprueba que el usuario pueda administrar
     * una organización.
     */
    private Organizacion verificarPermiso(UUID organizacionId) {
        Usuario usuario = usuarioActual();

        var encontrados = jdbc.queryForList(
                "SELECT id FROM organizaciones WHERE id = ? FOR UPDATE",
                UUID.class, organizacionId
        );

        if (encontrados.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Organización no encontrada");
        }

        Organizacion organizacion = organizaciones.findById(organizacionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organización no encontrada"));

        if (!organizacion.isActiva()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "La organización está inactiva");
        }

        boolean tienePermiso = miembros
                .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.getId())
                .filter(OrganizacionMiembro::isActivo)
                .filter(OrganizacionMiembro::isPuedeAdministrar)
                .isPresent();

        if (!tienePermiso) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tiene permiso para administrar");
        }

        return organizacion;
    }

    /**
     * Agrega un usuario a una organización.
     */
    @Transactional
    public void agregarMiembro(UUID organizacionId, UUID usuarioId,
                               boolean administrador) {

        Organizacion organizacion = verificarPermiso(organizacionId);

        Usuario usuario = usuarios.findByIdPublico(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        OrganizacionMiembro miembro = miembros
                .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.getId())
                .orElseGet(OrganizacionMiembro::new);

        if (miembro.isActivo() && miembro.isPuedeAdministrar() && !administrador) {
            verificarAdministradores(organizacionId);
        }

        miembro.setOrganizacion(organizacion);
        miembro.setUsuario(usuario);
        miembro.setPuedeAdministrar(administrador);
        miembro.setActivo(true);

        miembros.saveAndFlush(miembro);
    }

    /**
     * Modifica el permiso de un miembro.
     */
    @Transactional
    public void cambiarPermiso(UUID organizacionId, UUID usuarioId,
                               boolean administrador) {

        verificarPermiso(organizacionId);

        Usuario usuario = usuarios.findByIdPublico(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        OrganizacionMiembro miembro = miembros
                .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.getId())
                .filter(OrganizacionMiembro::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Miembro no encontrado"));

        if (usuario.isActivo() && miembro.isPuedeAdministrar() && !administrador) {
            verificarAdministradores(organizacionId);
        }

        miembro.setPuedeAdministrar(administrador);
        miembros.saveAndFlush(miembro);
    }

    /**
     * Desactiva a un miembro de la organización.
     */
    @Transactional
    public void quitarMiembro(UUID organizacionId, UUID usuarioId) {
        verificarPermiso(organizacionId);

        Usuario usuario = usuarios.findByIdPublico(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        OrganizacionMiembro miembro = miembros
                .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.getId())
                .filter(OrganizacionMiembro::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Miembro no encontrado"));

        if (usuario.isActivo() && miembro.isPuedeAdministrar()) {
            verificarAdministradores(organizacionId);
        }

        miembro.setActivo(false);
        miembro.setPuedeAdministrar(false);
        miembros.saveAndFlush(miembro);
    }

    /**
     * Evita quitar al último administrador.
     */
    private void verificarAdministradores(UUID organizacionId) {
        Long cantidad = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM organizacion_miembros m
                JOIN usuarios u ON u.id = m.usuario_id
                WHERE m.organizacion_id = ?
                  AND m.activo = TRUE
                  AND m.puede_administrar = TRUE
                  AND u.activo = TRUE
                """,
                Long.class, organizacionId
        );

        if (cantidad == null || cantidad <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La organización debe tener al menos un administrador");
        }
    }

    /**
     * Consulta las organizaciones del usuario.
     */
    @Transactional(readOnly = true)
    public List<OrganizacionResponse> listarOrganizaciones() {
        Usuario usuario = usuarioActual();

        return miembros
                .findByUsuarioIdAndActivoTrueAndOrganizacionActivaTrue(usuario.getId())
                .stream()
                .map(miembro -> {
                    Organizacion organizacion = miembro.getOrganizacion();

                    return new OrganizacionResponse(
                            organizacion.getId(),
                            organizacion.getNombre(),
                            organizacion.isActiva());
                })
                .toList();
    }

    /**
     * Consulta los datos de una organización.
     */
    @Transactional(readOnly = true)
    public OrganizacionResponse consultarOrganizacion(UUID organizacionId) {
        Usuario usuario = usuarioActual();

        OrganizacionMiembro miembro = miembros
                .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.getId())
                .filter(OrganizacionMiembro::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No pertenece a la organización"));

        Organizacion organizacion = miembro.getOrganizacion();

        if (!organizacion.isActiva()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "La organización está inactiva");
        }

        return new OrganizacionResponse(
                organizacion.getId(),
                organizacion.getNombre(),
                organizacion.isActiva());
    }

    /**
     * Consulta los miembros de una organización.
     */
    @Transactional
    public List<OrganizacionMiembroResponse> listarMiembros(UUID organizacionId) {
        verificarPermiso(organizacionId);

        return miembros
                .findByOrganizacionIdAndActivoTrueAndUsuarioActivoTrue(organizacionId)
                .stream()
                .map(miembro -> new OrganizacionMiembroResponse(
                        miembro.getUsuario().getIdPublico(),
                        miembro.getUsuario().getNombreCompleto(),
                        miembro.isPuedeAdministrar(),
                        miembro.isActivo()))
                .toList();
    }
}