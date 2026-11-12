
import { Routes } from '@angular/router';

import { Login } from './features/acceso/auth/login/login';
import { Registro } from './features/acceso/auth/registro/registro';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';
import { authGuard } from './features/acceso/auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'registro',
    component: Registro
  },
  {
    path: 'comprobar-entorno',
    component: ComprobarEntornoComponent,
    canActivate: [authGuard]
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
