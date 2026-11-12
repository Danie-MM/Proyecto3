import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './features/acceso/auth/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    @if (auth.usuarioEnSesion()) {
      <nav class="navegacion-modulos" aria-label="Secciones de PetMatchCR">
        <a routerLink="/mascotas" routerLinkActive="actual">Mis mascotas</a>
        <a routerLink="/mi-ubicacion" routerLinkActive="actual">Mi ubicación</a>
        <a routerLink="/comprobar-entorno" routerLinkActive="actual">Comprobar entorno</a>
      </nav>
    }
    <router-outlet />`,
  styles: [`
    .navegacion-modulos { display: flex; flex-wrap: wrap; gap: .5rem; padding: .75rem 1rem;
      background: #f3e9d7; border-bottom: 1px solid #c9a27a; }
    a { color: #594735; padding: .5rem .75rem; border-radius: .375rem; text-decoration: none; }
    a.actual, a:hover { background: #7c6349; color: #fff; }
    a:focus-visible { outline: 3px solid #7c6349; outline-offset: 2px; }
  `]
})
export class AppComponent {
  readonly auth = inject(AuthService);
}
