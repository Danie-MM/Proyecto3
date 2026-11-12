import { Routes } from '@angular/router';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';
import { PreferenciasUbicacionComponent } from './features/extravios/preferencias-ubicacion/preferencias-ubicacion.component';

export const routes: Routes = [
  { path: 'comprobar-entorno', component: ComprobarEntornoComponent },
  { path: 'extravios/preferencias-ubicacion', component: PreferenciasUbicacionComponent },
  { path: '', pathMatch: 'full', redirectTo: 'comprobar-entorno' },
  { path: '**', redirectTo: 'comprobar-entorno' }
];
