
import { Injectable, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { EscriturasHttpService } from '../../../shared/escrituras-http.service';

import {
  CrearReporteExtravio,
  ReporteExtravio
} from './reporte-extravio.model';

/** Gestiona la publicación de reportes y sus reintentos. */
@Injectable()
export class ReporteExtravioService {

  private readonly escrituras = inject(EscriturasHttpService);

  private readonly ruta = '/api/v1/reportes-extravio';

  private intentoAlta: {
    datos: string;
    clave: string;
  } | null = null;

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
}
