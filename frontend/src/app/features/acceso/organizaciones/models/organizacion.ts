/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description Información de las organizaciones y sus miembros.
 */

export interface Organizacion {
  id: string;
  nombre: string;
  activa: boolean;
}

export interface OrganizacionRequest {
  nombre: string;
}

export interface OrganizacionMiembro {
  usuarioId: string;
  nombreCompleto: string;
  puedeAdministrar: boolean;
  activo: boolean;
}

export interface OrganizacionMiembroRequest {
  usuarioId: string;
  administrador: boolean;
}

export interface OrganizacionPermisoRequest {
  administrador: boolean;
}
