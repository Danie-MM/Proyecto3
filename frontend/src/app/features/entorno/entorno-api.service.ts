import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, timeout } from 'rxjs';

export interface RegistroEntorno {
  id: string;
  texto: string;
  creadoEn: string;
}

export const formatoId = /^[a-f\d]{8}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{12}$/i;

@Injectable({ providedIn: 'root' })
export class EntornoApiService {
  private readonly http = inject(HttpClient);
  private readonly ruta = '/api/v1/pruebas-entorno';

  crear(texto: string) {
    return this.http.post<RegistroEntorno>(this.ruta, { texto }, { observe: 'response' }).pipe(
      timeout(10000), map(respuesta => this.registro(respuesta, 201))
    );
  }

  consultar(id: string) {
    return this.http.get<RegistroEntorno>(this.ruta + '/' + encodeURIComponent(id), { observe: 'response' }).pipe(
      timeout(10000), map(respuesta => this.registro(respuesta, 200))
    );
  }

  private registro(respuesta: HttpResponse<RegistroEntorno>, estado: number): RegistroEntorno {
    const dato = respuesta.body;
    if (respuesta.status !== estado || !dato || !formatoId.test(dato.id)
        || typeof dato.texto !== 'string' || !dato.texto.trim() || [...dato.texto].length > 80
        || typeof dato.creadoEn !== 'string' || !Number.isFinite(Date.parse(dato.creadoEn))) {
      throw new Error('Respuesta de registro incompleta');
    }
    return dato;
  }
}
