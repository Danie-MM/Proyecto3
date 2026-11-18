import { DOCUMENT } from '@angular/common';
import { inject, isDevMode } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot, UrlTree } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { authGuard } from '../acceso/auth/guards/auth.guard';
import { MascotasApiService } from './mascotas-api.service';
import { SesionMascotasService } from './sesion-mascotas.service';

export function permiteDemoLocal(desarrollo: boolean, hostname: string): boolean {
  return desarrollo && ['localhost', '127.0.0.1', '[::1]'].includes(hostname);
}

function comprobarEntrada(ruta: ActivatedRouteSnapshot, estado: RouterStateSnapshot, permiteSelector: boolean) {
  const normal = authGuard(ruta, estado);
  const sesion = inject(SesionMascotasService);
  sesion.demoDeEntrada = null;
  if (normal === true) sesion.estado.next(null);
  if (!(normal instanceof UrlTree) || !permiteDemoLocal(isDevMode(), inject(DOCUMENT).location?.hostname ?? '')) {
    return normal;
  }

  return inject(MascotasApiService).demo().pipe(
    map(demo => {
      if (!Array.isArray(demo?.actores) || !demo.actores.some(actor => actor?.id && actor?.nombre)) {
        return normal;
      }
      if (!permiteSelector && !demo.actores.some(actor => actor.id === demo.actorActualId)) return normal;
      // Solo B puede mostrar el selector todavía anónimo. En otras rutas la cookie debe tener actor.
      if (permiteSelector) sesion.demoDeEntrada = demo;
      else sesion.estado.next(demo);
      return true;
    }),
    catchError(() => of(normal)),
  );
}

export const entradaMascotasGuard: CanActivateFn = (ruta, estado) => comprobarEntrada(ruta, estado, true);
export const entradaModulosGuard: CanActivateFn = (ruta, estado) => comprobarEntrada(ruta, estado, false);
