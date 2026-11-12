
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Datos que enviamos al backend
export interface GuardarPreferenciaUbicacion {
  latitud: number | null;
  longitud: number | null;
  referencia: string;
  aceptaAvisos: boolean;
}

// Datos que recibimos del backend
export interface PreferenciaUbicacion {
  id: string;
  usuarioId: string;
  latitud: number | null;
  longitud: number | null;
  referencia: string | null;
  aceptaAvisos: boolean;
  consentimientoEn: string | null;
  actualizadaEn: string;
  creadoEn: string;
  version: number;
}

@Injectable({
  providedIn: 'root'
})
export class PreferenciasUbicacionService {

  private readonly http = inject(HttpClient);

  private readonly ruta = '/api/extravios/ubicacion';

  // Consultar las preferencias del usuario
  consultar(): Observable<PreferenciaUbicacion> {
    return this.http.get<PreferenciaUbicacion>(this.ruta);
  }

  // Crear preferencias por primera vez
  crear(datos: GuardarPreferenciaUbicacion): Observable<PreferenciaUbicacion> {
    return this.http.post<PreferenciaUbicacion>(this.ruta, datos);
  }

  // Actualizar preferencias existentes
  actualizar(datos: GuardarPreferenciaUbicacion): Observable<PreferenciaUbicacion> {
    return this.http.put<PreferenciaUbicacion>(this.ruta, datos);
  }

  // Eliminar las preferencias
  eliminar(): Observable<void> {
    return this.http.delete<void>(this.ruta);
  }
}
