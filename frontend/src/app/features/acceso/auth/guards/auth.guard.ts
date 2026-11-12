
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/** @author: Daniela Morales
 *  @version 1.0
 *  @description Verificación de sesión de usuario, la cual debe estar activa antes de permitir el accesso a rutas protegidas. 
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.obtenerToken()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
