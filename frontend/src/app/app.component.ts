import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from './features/acceso/auth/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    @if (mostrarNavegacion()) {
      <header class="cabecera">
        <div class="barra">
          <a class="marca" routerLink="/mascotas"><span class="huella" aria-hidden="true"></span>PetMatchCR</a>
          <nav aria-label="Secciones de PetMatchCR">
            <a routerLink="/mascotas" routerLinkActive="actual" ariaCurrentWhenActive="page">Mis mascotas</a>
            <a routerLink="/catalogo" routerLinkActive="actual" ariaCurrentWhenActive="page">Catálogo</a>
            <a routerLink="/mi-ubicacion" routerLinkActive="actual" ariaCurrentWhenActive="page">Mi ubicación</a>
            <a routerLink="/comprobar-entorno" routerLinkActive="actual" ariaCurrentWhenActive="page">Entorno</a>
          </nav>
          @if (auth.obtenerToken()) {
            <button class="cuenta" type="button" (click)="cerrarSesion()">Cerrar sesión</button>
          } @else {
            <a class="cuenta" routerLink="/login" [queryParams]="{ returnUrl: router.url }">Ingresar con Google</a>
          }
        </div>
      </header>
    }
    <router-outlet />`,
  styles: [`
    .cabecera { color: #233c4b; background: white; border-bottom: 1px solid #ccd9e1; }
    .barra { display: flex; align-items: center; justify-content: space-between; gap: 1rem;
      width: min(1140px, calc(100% - 3rem)); min-height: 4.25rem; margin: auto; }
    .marca { display: inline-flex; align-items: center; gap: .55rem; color: #19445c;
      font-size: clamp(1.35rem, 3vw, 1.7rem); font-weight: 800; letter-spacing: -.05em; text-decoration: none; }
    .huella { width: 1.8rem; height: 1.8rem; background: #245b78;
      mask: url('/assets/icons/pets-svgrepo-com.svg') center / contain no-repeat; }
    nav { display: flex; align-self: stretch; align-items: stretch; flex-wrap: wrap; gap: .25rem 1.2rem; }
    nav a { display: inline-flex; align-items: center; color: #233c4b; text-decoration: none;
      font-size: .95rem; font-weight: 600; border-bottom: 3px solid transparent; }
    nav a:hover, nav a.actual { color: #245b78; border-bottom-color: #245b78; }
    .cuenta { display: inline-flex; align-items: center; padding: .35rem .8rem; border: 0; border-radius: .6rem;
      color: #643626; background: #f9ded5; font: inherit; font-size: .9rem; font-weight: 650; text-decoration: none; }
    .cuenta:hover { background: #e29782; }
    :is(a, button):focus-visible { outline: 3px solid #245b78; outline-offset: 2px; }
    @media (max-width: 720px) {
      .barra { flex-wrap: wrap; gap: .3rem 1rem; width: calc(100% - 1.7rem); padding-top: .6rem; }
      nav { order: 1; flex-basis: 100%; gap: .25rem 1rem; min-height: 2.5rem; }
    }
  `]
})
export class AppComponent {
  readonly auth = inject(AuthService);
  protected readonly router = inject(Router);
  // La navegación aparece en las vistas de trabajo que ya aceptó su guard, incluso con cookie de demo.
  readonly mostrarNavegacion = toSignal(this.router.events.pipe(
    filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
    map(evento => /^\/(mascotas|catalogo|mi-ubicacion|comprobar-entorno)(?:$|[/?#;])/.test(evento.urlAfterRedirects)),
  ), { initialValue: false });

  cerrarSesion(): void {
    this.auth.cerrarSesion();
    void this.router.navigateByUrl('/login', { replaceUrl: true });
  }
}
