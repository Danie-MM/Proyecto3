import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError, timeout, TimeoutError } from 'rxjs';
import { longitudTexto, normalizarTexto, PruebaEntorno, UUID_VALIDO } from './prueba-entorno.model';

@Injectable({ providedIn: 'root' })
export class PruebaEntornoService {
  private readonly http = inject(HttpClient);
  private readonly ruta = '/api/v1/pruebas-entorno';

  guardar(texto: string): Observable<PruebaEntorno> {
    return this.http.post<unknown>(this.ruta, { texto: normalizarTexto(texto) }, { observe: 'response' }).pipe(
      timeout(10_000),
      map(respuesta => this.validarRespuesta(respuesta, 201)),
      catchError(error => throwError(() => new Error(this.mensajeError(error, true))))
    );
  }

  consultar(id: string): Observable<PruebaEntorno> {
    return this.http.get<unknown>(`${this.ruta}/${encodeURIComponent(id)}`, { observe: 'response' }).pipe(
      timeout(10_000),
      map(respuesta => this.validarRespuesta(respuesta, 200)),
      catchError(error => throwError(() => new Error(this.mensajeError(error, false))))
    );
  }

  private validarRespuesta(respuesta: HttpResponse<unknown>, estado: number): PruebaEntorno {
    const dato = respuesta.body as Partial<PruebaEntorno> | null;
    if (respuesta.status !== estado || !dato ||
      typeof dato.id !== 'string' || !UUID_VALIDO.test(dato.id) ||
      typeof dato.texto !== 'string' || dato.texto !== normalizarTexto(dato.texto) ||
      longitudTexto(dato.texto) < 1 || longitudTexto(dato.texto) > 80 ||
      typeof dato.creadoEn !== 'string' || !dato.creadoEn.endsWith('Z') ||
      !Number.isFinite(Date.parse(dato.creadoEn))) {
      throw new Error('RESPUESTA_INVALIDA');
    }
    return dato as PruebaEntorno;
  }

  private mensajeError(error: unknown, escritura: boolean): string {
    if (error instanceof TimeoutError) {
      return escritura
        ? 'No recibimos respuesta a tiempo. El texto podría haberse guardado; revisá la conexión antes de repetir el envío.'
        : 'La consulta tardó demasiado. Revisá la conexión e intentá de nuevo.';
    }
    if (error instanceof HttpErrorResponse) {
      const cuerpo: unknown = error.error;
      const esErrorJson = cuerpo !== null && typeof cuerpo === 'object' &&
        'codigo' in cuerpo && typeof cuerpo.codigo === 'string' &&
        'mensaje' in cuerpo && typeof cuerpo.mensaje === 'string';
      if (error.status === 0 || !esErrorJson || error.status === 502 || error.status === 504) {
        return 'No pudimos conectar con el servidor. Revisá que el proyecto esté encendido.';
      }
      if (error.status === 404) return 'No encontramos ese registro. Revisá el ID e intentá de nuevo.';
      if (error.status === 503) return 'La base de datos no está disponible. Intentá de nuevo en unos momentos.';
      if (error.status === 400) return 'Revisá el texto o el ID enviado. El texto debe tener entre 1 y 80 caracteres.';
      return 'No pudimos completar la solicitud. Intentá de nuevo en unos momentos.';
    }
    return 'El servidor devolvió una respuesta inesperada. No podemos confirmar el resultado.';
  }
}
