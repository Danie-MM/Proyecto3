
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, timeout } from 'rxjs';
import { EscriturasHttpService } from '../../../shared/escrituras-http.service';

// Datos que enviamos al backend
export interface GuardarPreferenciaUbicacion {
  latitud: number | null;
  longitud: number | null;
  referencia: string;
  aceptaAvisos: boolean;
}

// Datos que recibimos del backend
export interface PreferenciaUbicacion {
  configurada: boolean;
  id: string | null;
  usuarioId: string | null;
  latitud: number | null;
  longitud: number | null;
  referencia: string | null;
  aceptaAvisos: boolean;
  consentimientoEn: string | null;
  actualizadaEn: string | null;
  creadoEn: string | null;
  version: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class PreferenciasUbicacionService {

  private readonly http = inject(HttpClient);
  private readonly escrituras = inject(EscriturasHttpService);
  private intentoAlta: { datos: string; clave: string } | null = null;

  private readonly ruta = '/api/extravios/ubicacion';

  // Consultar las preferencias del usuario
  consultar(): Observable<PreferenciaUbicacion> {
    return this.http.get<PreferenciaUbicacion>(this.ruta).pipe(
      tap(preferencia => { if (preferencia.configurada) this.intentoAlta = null; }),
      timeout(15000)
    );
  }

  // Crear preferencias por primera vez
  crear(datos: GuardarPreferenciaUbicacion): Observable<PreferenciaUbicacion> {
    const contenido = JSON.stringify(datos);
    // Ante una respuesta perdida, reenviar los mismos datos conserva la misma operación.
    if (this.intentoAlta?.datos !== contenido) {
      this.intentoAlta = { datos: contenido, clave: crypto.randomUUID() };
    }
    return this.escrituras.enviar<PreferenciaUbicacion>('POST', this.ruta, datos, {
      'Idempotency-Key': this.intentoAlta.clave
    }).pipe(tap(() => { this.intentoAlta = null; }));
  }

  // Actualizar preferencias existentes
  actualizar(datos: GuardarPreferenciaUbicacion, versionEsperada: number): Observable<PreferenciaUbicacion> {
    return this.escrituras.enviar<PreferenciaUbicacion>('PUT', this.ruta, { ...datos, versionEsperada });
  }

  // Eliminar las preferencias
  eliminar(versionEsperada: number): Observable<void> {
    return this.escrituras.enviar<void>('DELETE', `${this.ruta}?versionEsperada=${versionEsperada}`)
      .pipe(tap(() => { this.intentoAlta = null; }));
  }
}
