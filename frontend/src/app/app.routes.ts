
import { Routes } from '@angular/router';

import { Login } from './features/acceso/auth/login/login';
import { Registro } from './features/acceso/auth/registro/registro';
import { loggedInGuard } from './features/acceso/auth/guards/logged-in.guard';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';
import { entradaMascotasGuard, entradaModulosGuard } from './features/mascotas/entrada-mascotas.guard';
import { CatalogoComponent } from './features/adopciones/catalogo/catalogo/catalogo.component';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
    canActivate: [loggedInGuard]
  },
  {
    path: 'registro',
    component: Registro,
    canActivate: [loggedInGuard]
  },
  {
    path: 'comprobar-entorno',
    component: ComprobarEntornoComponent,
    canActivate: [entradaModulosGuard]
  },
  {
    path: 'catalogo',
    component: CatalogoComponent,
    canActivate: [entradaModulosGuard]
  },
  {
    path: 'mascotas',
    canActivate: [entradaMascotasGuard],
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
    canActivate: [entradaModulosGuard],
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
