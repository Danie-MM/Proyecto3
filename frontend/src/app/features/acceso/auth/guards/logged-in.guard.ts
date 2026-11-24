
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { destinoTrasIngreso } from '../destino-tras-ingreso';

export const loggedInGuard: CanActivateFn = (ruta) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.obtenerToken();

  if (token) {
    return router.parseUrl(destinoTrasIngreso(ruta.queryParamMap.get('returnUrl')));
  }

  return true;
};
