
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './services/auth.service';

/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description  Encargado de agregar el token JWT a los request al backend.
 *  Controla los errores de auth y  cierra la sesión cuando el servidor responde con un estado HTTP 401
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.obtenerToken();
  const esApi = req.url.startsWith('/api/');
  const esAuth = req.url.startsWith('/api/auth/');
  const esCsrf = req.method === 'GET' && req.url === '/api/v1/sesion/csrf';
  let solicitud = req;

  // Obtener CSRF debe permitir reingresar aunque el navegador conserve un JWT vencido.
  if (token && esApi && !esAuth && !esCsrf) {
    solicitud = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(solicitud).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && esApi && !esAuth) {
        authService.cerrarSesion();
        void router.navigateByUrl('/login', { replaceUrl: true });
      }

      return throwError(() => error);
    })
  );
};
