
import { Routes } from '@angular/router';

import { Login } from './features/acceso/auth/login/login';
import { Registro } from './features/acceso/auth/registro/registro';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';
import { authGuard } from './features/acceso/auth/guards/auth.guard';
import { CatalogoComponent } from './features/adopciones/catalogo/catalogo/catalogo.component';

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
    path: 'catalogo',
    component: CatalogoComponent,
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

  // C-01: Preferencias de ubicación
  {
    path: 'mi-ubicacion',
    title: 'Mi ubicación | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () => import('./features/extravios/preferencias-ubicacion/preferencias-ubicacion.component')
      .then(modulo => modulo.PreferenciasUbicacionComponent)
  },

  // C-02: Publicación de reportes de extravío
  {
    path: 'publicar-extravio',
    title: 'Publicar extravío | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/extravios/publicar-extravio/publicar-extravio.component')
        .then(modulo => modulo.PublicarExtravioComponent)
  },

  // C-03: Listado de reportes activos
  {
    path: 'reportes-extravio',
    pathMatch: 'full',
    title: 'Reportes de extravío | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/extravios/reportes-extravio/reportes-extravio.component')
        .then(modulo => modulo.ReportesExtravioComponent)
  },

  // C-03: Edición de un reporte
  {
    path: 'reportes-extravio/:id/editar',
    title: 'Editar reporte de extravío | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/extravios/editar-extravio/editar-extravio.component')
        .then(modulo => modulo.EditarExtravioComponent)
  },

  // C-03: Consulta del detalle de un reporte
  {
    path: 'reportes-extravio/:id',
    title: 'Detalle del extravío | PetMatchCR',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/extravios/detalle-extravio/detalle-extravio.component')
        .then(modulo => modulo.DetalleExtravioComponent)
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
