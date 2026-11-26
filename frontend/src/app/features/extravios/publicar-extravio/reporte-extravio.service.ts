
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, tap, timeout } from 'rxjs';

import { EscriturasHttpService } from '../../../shared/escrituras-http.service';

import {
  CrearReporteExtravio,
  ReporteExtravio,
  ReporteExtravioResumen,
  ReporteExtravioDetalle,
  ActualizarReporteExtravio
} from './reporte-extravio.model';

/** Gestiona la publicación, consulta y actualización de reportes de extravío. */
@Injectable()
export class ReporteExtravioService {

  private readonly http = inject(HttpClient);
  private readonly escrituras = inject(EscriturasHttpService);

  private readonly ruta = '/api/v1/reportes-extravio';

  private intentoAlta: {
    datos: string;
    clave: string;
  } | null = null;

  // ======================================================
  // C-02: Publicación de reportes
  // ======================================================

  /** Publica un reporte conservando la clave ante un reintento. */
  crear(datos: CrearReporteExtravio): Observable<HttpResponse<ReporteExtravio>> {

    const contenido = JSON.stringify(datos);

    // Si cambian los datos, se considera una nueva operación.
    if (this.intentoAlta?.datos !== contenido) {
      this.intentoAlta = {
        datos: contenido,
        clave: crypto.randomUUID()
      };
    }

    return this.escrituras.enviarConRespuesta<ReporteExtravio>(
      'POST',
      this.ruta,
      datos,
      {
        'Idempotency-Key': this.intentoAlta.clave
      }
    ).pipe(
      tap(() => {
        this.intentoAlta = null;
      })
    );
  }

  // ======================================================
  // C-03: Consulta y actualización de reportes
  // ======================================================

  /** Consulta el listado de reportes de extravío activos. */
  listarActivos(): Observable<ReporteExtravioResumen[]> {

    return this.http.get<ReporteExtravioResumen[]>(
      this.ruta
    ).pipe(
      timeout(15000)
    );
  }

  /** Consulta la información permitida de un reporte específico. */
  consultarDetalle(id: string): Observable<ReporteExtravioDetalle> {

    return this.http.get<ReporteExtravioDetalle>(
      `${this.ruta}/${encodeURIComponent(id)}`
    ).pipe(
      timeout(15000)
    );
  }

  /** Actualiza parcialmente un reporte utilizando su versión vigente. */
  actualizar(
    id: string,
    datos: ActualizarReporteExtravio
  ): Observable<ReporteExtravioDetalle> {

    return this.escrituras.enviar<ReporteExtravioDetalle>(
      'PATCH',
      `${this.ruta}/${encodeURIComponent(id)}`,
      datos
    );
  }
}
