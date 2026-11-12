import { Routes } from '@angular/router';
import { ComprobarEntornoComponent } from './features/entorno/comprobar-entorno.component';
import { MascotasLayoutComponent } from './features/mascotas/mascotas-layout.component';
import { MascotasListaComponent } from './features/mascotas/mascotas-lista.component';
import { MascotaFormularioComponent } from './features/mascotas/mascota-formulario.component';
import { MascotaDetalleComponent } from './features/mascotas/mascota-detalle.component';

export const routes: Routes = [
  { path: 'comprobar-entorno', component: ComprobarEntornoComponent, title: 'Comprobar entorno | PetMatchCR' },
  { path: 'mascotas', component: MascotasLayoutComponent, children: [
    { path: '', pathMatch: 'full', component: MascotasListaComponent, title: 'Mis mascotas | PetMatchCR' },
    { path: 'nueva', component: MascotaFormularioComponent, title: 'Registrar mascota | PetMatchCR' },
    { path: ':id/editar', component: MascotaFormularioComponent, data: { editar: true }, title: 'Editar mascota | PetMatchCR' },
    { path: ':id', component: MascotaDetalleComponent, title: 'Perfil de mascota | PetMatchCR' }
  ] },
  { path: '', pathMatch: 'full', redirectTo: 'comprobar-entorno' },
  { path: '**', redirectTo: 'comprobar-entorno' }
];
