import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'comprobar-entorno' },
  {
    path: 'comprobar-entorno',
    loadComponent: () => import('./features/entorno/comprobar-entorno.component')
      .then(module => module.ComprobarEntornoComponent)
  },
  { path: '**', redirectTo: 'comprobar-entorno' }
];
