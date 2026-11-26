
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap, timeout } from 'rxjs';

/** Obtiene un CSRF vigente antes de escribir; la cookie viaja al mismo origen por el proxy. */
@Injectable({ providedIn: 'root' })
export class EscriturasHttpService {

  private readonly http = inject(HttpClient);

  enviar<T>(
    metodo: 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    ruta: string,
    body?: unknown,
    cabeceras: Record<string, string> = {}
  ) {
    return this.http.get<{ token: string }>('/api/v1/sesion/csrf').pipe(
      switchMap(({ token }) => this.http.request<T>(metodo, ruta, {
        body,
        headers: { ...cabeceras, 'X-CSRF-TOKEN': token }
      })),
      timeout(15000)
    );
  }

  /** Permite consultar el cuerpo y el estado HTTP de una escritura. */
  enviarConRespuesta<T>(
    metodo: 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    ruta: string,
    body?: unknown,
    cabeceras: Record<string, string> = {}
  ): Observable<HttpResponse<T>> {

    return this.http.get<{ token: string }>('/api/v1/sesion/csrf').pipe(
      switchMap(({ token }) => this.http.request<T>(metodo, ruta, {
        body,
        headers: { ...cabeceras, 'X-CSRF-TOKEN': token },
        observe: 'response'
      })),
      timeout(15000)
    );
  }
}
