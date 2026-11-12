
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
    path: 'mascotas',
    canActivate: [authGuard],
    loadComponent: () => import('./features/mascotas/mascotas-layout.component')
      .then(modulo => modulo.MascotasLayoutComponent),
    children: [
      {
        path: '', pathMatch: 'full', title: 'Mis mascotas | PetMatchCR',
        loadComponent: () => import('./features/mascotas/mascotas-lista.component')
          .then(modulo => modulo.MascotasListaComponent)
      },
      {
        path: 'nueva', title: 'Registrar mascota | PetMatchCR',
        loadComponent: () => import('./features/mascotas/mascota-formulario.component')
          .then(modulo => modulo.MascotaFormularioComponent)
      },
      {
        path: ':id/editar', data: { editar: true }, title: 'Editar mascota | PetMatchCR',
        loadComponent: () => import('./features/mascotas/mascota-formulario.component')
          .then(modulo => modulo.MascotaFormularioComponent)
      },
      {
        path: ':id', title: 'Perfil de mascota | PetMatchCR',
        loadComponent: () => import('./features/mascotas/mascota-detalle.component')
          .then(modulo => modulo.MascotaDetalleComponent)
      }
    ]
  },
  {
    path: 'mi-ubicacion',
    title: 'Mi ubicación | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () => import('./features/extravios/preferencias-ubicacion/preferencias-ubicacion.component')
      .then(modulo => modulo.PreferenciasUbicacionComponent)
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
