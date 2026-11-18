
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './services/auth.service';
import { urlIngreso } from './destino-tras-ingreso';

/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description  Encargado de agregar el token JWT a los request al backend.
 *  Cierra la sesión si el servidor rechaza el token vigente en una solicitud autenticada.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.obtenerToken();
  const ruta = req.url.split(/[?#]/, 1)[0];
  const esApi = ruta.startsWith('/api/');
  const esLogout = req.method === 'POST' && ruta === '/api/auth/logout';
  const esLecturaPublica = req.method === 'GET' && (
    ruta === '/api/v1/sesion/csrf' || ruta === '/api/v1/catalogos/mascotas' ||
    ruta === '/api/v1/catalogo/mascotas' || /^\/api\/v1\/mascotas\/[^/]+\/(foto\/)?publica$/.test(ruta)
  );
  const usaBearer = token && esApi && (!ruta.startsWith('/api/auth/') || esLogout) &&
    !ruta.startsWith('/api/v1/demo-b01/') && !esLecturaPublica;
  let solicitud = req;

  if (usaBearer) {
    solicitud = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(solicitud).pipe(
    catchError((error: HttpErrorResponse) => {
      // Un 401 público o tardío de una cuenta anterior no invalida la sesión actual.
      if (error.status === 401 && usaBearer &&
          solicitud.headers.get('Authorization') === `Bearer ${token}` &&
          authService.obtenerToken() === token) {
        authService.cerrarSesion();
        if (!esLogout && !/^\/(login|registro)(?:[/?#;]|$)/.test(router.url)) {
          void router.navigateByUrl(urlIngreso(router, router.url), { replaceUrl: true });
        }
      }

      return throwError(() => error);
    })
  );
};
