import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap, timeout } from 'rxjs';
import { CatalogosMascota, ContextoMascotas, CrearMascota, EditarMascota, Mascota, PaginaMascotas, SesionDemo } from './mascotas.model';

/** La cookie identifica la sesión; el formulario nunca envía un propietario elegido libremente. */
@Injectable({ providedIn: 'root' })
export class MascotasApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/mascotas';
  catalogos() { return this.http.get<CatalogosMascota>('/api/v1/catalogos/mascotas').pipe(timeout(15000)); }
  contexto() { return this.http.get<ContextoMascotas>(this.base + '/contexto').pipe(timeout(10000)); }
  listar(pagina = 0, activas: boolean | null = null, organizacionId: string | null = null) {
    let params = new HttpParams().set('pagina', pagina).set('limite', 20);
    if (activas !== null) params = params.set('activas', activas);
    if (organizacionId) params = params.set('organizacionId', organizacionId);
    return this.http.get<PaginaMascotas>(this.base, { params }).pipe(timeout(15000));
  }
  consultar(id: string) { return this.http.get<Mascota>(`${this.base}/${encodeURIComponent(id)}`).pipe(timeout(15000)); }
  crear(datos: CrearMascota, clave: string) { return this.escribir<Mascota>('POST', this.base, datos, clave); }
  editar(id: string, datos: EditarMascota) { return this.escribir<Mascota>('PUT', `${this.base}/${encodeURIComponent(id)}`, datos); }
  desactivar(id: string, motivo: string, versionEsperada: number, clave: string) {
    return this.escribir<Mascota>('POST', `${this.base}/${encodeURIComponent(id)}/desactivacion`, { motivo, versionEsperada }, clave);
  }
  demo() { return this.http.get<SesionDemo>('/api/v1/demo-b01/actores').pipe(timeout(10000)); }
  cambiarActorDemo(actorId: string) { return this.escribir<unknown>('POST', '/api/v1/demo-b01/sesion', { actorId }); }

  // CSRF se obtiene para cada escritura: nunca reutilizamos un token de otra sesión.
  private escribir<T>(metodo: 'POST' | 'PUT', ruta: string, body: unknown, clave?: string): Observable<T> {
    return this.http.get<{ token: string }>('/api/v1/sesion/csrf').pipe(switchMap(({ token }) => {
      const headers: Record<string, string> = { 'X-CSRF-TOKEN': token };
      if (clave) headers['Idempotency-Key'] = clave;
      return this.http.request<T>(metodo, ruta, { body, headers });
    }), timeout(15000));
  }
}

export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401) return 'Necesitás una sesión activa. Ingresá con tu cuenta; en la demostración local, seleccioná un usuario ficticio.';
    if (error.status === 404) return 'No encontramos esta mascota o no tenés permiso para verla.';
    if (error.status === 0 || error.status >= 500) return 'No pudimos confirmar la operación con el servidor. Revisá la conexión y reintentá.';
    if (typeof error.error?.mensaje === 'string') return error.error.mensaje;
    if (error.status === 403) return 'La sesión no permite esta operación. Volvé a ingresar y reintentá.';
  }
  return 'No pudimos confirmar la operación. Revisá la conexión y reintentá.';
}
