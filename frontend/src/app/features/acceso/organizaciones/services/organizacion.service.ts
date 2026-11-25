import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { timeout } from 'rxjs';
import { EscriturasHttpService } from '../../../../shared/escrituras-http.service';
import {
  Organizacion,
  OrganizacionRequest,
  OrganizacionMiembro,
  OrganizacionMiembroRequest,
  OrganizacionPermisoRequest
} from '../models/organizacion';

/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description Servicio encargado de las organizaciones y sus miembros.
 */

@Injectable({
  providedIn: 'root'
})
export class OrganizacionService {

  private readonly http = inject(HttpClient);
  private readonly escrituras = inject(EscriturasHttpService);
  private readonly baseUrl = '/api/v1/organizaciones';

  listar() {
    return this.http.get<Organizacion[]>(this.baseUrl).pipe(timeout(15000));
  }

  consultar(id: string) {
    return this.http.get<Organizacion>(`${this.baseUrl}/${id}`).pipe(timeout(15000));
  }

  registrar(datos: OrganizacionRequest) {
    return this.escrituras.enviar<Organizacion>('POST', this.baseUrl, datos);
  }

  listarMiembros(id: string) {
    return this.http.get<OrganizacionMiembro[]>(`${this.baseUrl}/${id}/miembros`)
      .pipe(timeout(15000));
  }

  agregarMiembro(id: string, datos: OrganizacionMiembroRequest) {
    return this.escrituras.enviar<void>('POST', `${this.baseUrl}/${id}/miembros`, datos);
  }

  cambiarPermiso(id: string, usuarioId: string, datos: OrganizacionPermisoRequest) {
    const ruta = `${this.baseUrl}/${id}/miembros/${usuarioId}/permiso`;
    return this.escrituras.enviar<void>('PUT', ruta, datos);
  }

  quitarMiembro(id: string, usuarioId: string) {
    const ruta = `${this.baseUrl}/${id}/miembros/${usuarioId}`;
    return this.escrituras.enviar<void>('DELETE', ruta);
  }
}
