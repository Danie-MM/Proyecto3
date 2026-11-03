import { Routes } from '@angular/router';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';

export const routes: Routes = [
  { path: 'comprobar-entorno', component: ComprobarEntornoComponent },
  { path: '', pathMatch: 'full', redirectTo: 'comprobar-entorno' },
  { path: '**', redirectTo: 'comprobar-entorno' }
];
