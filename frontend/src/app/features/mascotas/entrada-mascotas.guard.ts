import { DOCUMENT } from '@angular/common';
import { inject, isDevMode } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { authGuard } from '../acceso/auth/guards/auth.guard';
import { MascotasApiService } from './mascotas-api.service';
import { SesionMascotasService } from './sesion-mascotas.service';

export function permiteDemoLocal(desarrollo: boolean, hostname: string): boolean {
  return desarrollo && ['localhost', '127.0.0.1', '[::1]'].includes(hostname);
}

export const entradaMascotasGuard: CanActivateFn = (ruta, estado) => {
  const normal = authGuard(ruta, estado);
  const sesion = inject(SesionMascotasService);
  sesion.demoDeEntrada = null;
  if (!(normal instanceof UrlTree) || inject(Router).serializeUrl(normal) !== '/login' ||
      !permiteDemoLocal(isDevMode(), inject(DOCUMENT).location?.hostname ?? '')) {
    return normal;
  }

  return inject(MascotasApiService).demo().pipe(
    map(demo => {
      if (!Array.isArray(demo?.actores) || !demo.actores.some(actor => actor?.id && actor?.nombre)) {
        return normal;
      }
      // Esta respuesta permite mostrar el selector. El POST del servidor crea la sesión del actor.
      sesion.demoDeEntrada = demo;
      return true;
    }),
    catchError(() => of(normal)),
  );
};
