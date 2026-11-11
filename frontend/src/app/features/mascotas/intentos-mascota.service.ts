import { Injectable } from '@angular/core';

/** Conserva una intención cuando se pierde la respuesta. Solo almacena una huella y una clave. */
@Injectable({ providedIn: 'root' })
export class IntentosMascotaService {
  private readonly memoria = new Map<string, { huella: string; clave: string }>();
  async obtener(usuarioId: string, operacion: string, datos: unknown): Promise<string> {
    const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(JSON.stringify(datos)));
    const huella = Array.from(new Uint8Array(hash), byte => byte.toString(16).padStart(2, '0')).join('');
    const nombre = this.nombre(usuarioId, operacion);
    let previo = this.memoria.get(nombre);
    try { previo = JSON.parse(sessionStorage.getItem(nombre) ?? 'null') ?? previo; } catch { /* Navegador sin almacenamiento. */ }
    if (previo?.huella === huella) return previo.clave;
    const intento = { huella, clave: crypto.randomUUID() };
    this.memoria.set(nombre, intento);
    try { sessionStorage.setItem(nombre, JSON.stringify(intento)); } catch { /* Sigue disponible en memoria. */ }
    return intento.clave;
  }
  completar(usuarioId: string, operacion: string) {
    const nombre = this.nombre(usuarioId, operacion);
    this.memoria.delete(nombre);
    try { sessionStorage.removeItem(nombre); } catch { /* No se almacenan perfiles ni credenciales. */ }
  }

  private nombre(usuarioId: string, operacion: string): string {
    if (!usuarioId) throw new Error('Se requiere el usuario de la sesión verificada.');
    // Cada actor conserva su intención aunque otro use esta misma pestaña y operación.
    return 'petmatch.b01.intento.' + encodeURIComponent(usuarioId) + '.' + encodeURIComponent(operacion);
  }
}
