import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { timeout } from 'rxjs';
import { MascotasApiService } from '../mascotas-api.service';
import { Mascota } from '../mascotas.model';

interface FotoGuardada {
  urlPrivada: string;
  versionMascota: number;
}

@Injectable({ providedIn: 'root' })
export class PublicacionMascotaApi {
  private readonly http = inject(HttpClient);
  private readonly api = inject(MascotasApiService);

  disponibilidad(mascota: Mascota, disponibilidad: 'DISPONIBLE' | 'NO_DISPONIBLE') {
    return this.api.escribir<Mascota>('PUT', this.base(mascota.id) + '/disponibilidad', {
      disponibilidad,
      versionEsperada: mascota.version,
    });
  }

  leerFoto(id: string) {
    return this.http.get(this.base(id) + '/foto', { responseType: 'blob' }).pipe(timeout(15000));
  }

  subir(mascota: Mascota, archivo: File, clave: string) {
    const body = new FormData();
    body.set('archivo', archivo);
    body.set('versionEsperada', String(mascota.version));
    return this.api.escribir<FotoGuardada>('POST', this.base(mascota.id) + '/foto', body, clave);
  }

  retirar(mascota: Mascota, clave: string) {
    return this.api.escribir<{ foto: null; versionMascota: number }>(
      'POST', this.base(mascota.id) + '/foto/retiro', { versionEsperada: mascota.version }, clave,
    );
  }

  private base(id: string) {
    return '/api/v1/mascotas/' + encodeURIComponent(id);
  }
}
