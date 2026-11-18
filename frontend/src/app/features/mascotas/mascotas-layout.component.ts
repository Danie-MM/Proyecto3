import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { mensajeError } from './mascotas-api.service';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({
  selector: 'app-mascotas-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterOutlet],
  host: { class: 'modulo-mascotas' },
  styleUrl: './mascotas.css',
  template: `<header class="cabecera navbar-top">
      <div class="ancho barra">
        <a class="marca" routerLink="/mascotas"><span class="huella" aria-hidden="true"></span>PetMatchCR</a>
        <nav aria-label="Navegación principal">
          <a routerLink="/mascotas" aria-current="page">Mis mascotas</a><a routerLink="/comprobar-entorno">Entorno</a> <a routerLink="/catalogo">Catálogo</a>
        </nav>
      </div>
    </header>
    <main class="ancho">
      <aside class="demo" *ngIf="sesion.estado | async as demo">
        <label for="actor-demo">Usuario de demostración</label>
        <select
          class="select-single"
          id="actor-demo"
          [ngModel]="demo.actorActualId ?? ''"
          (ngModelChange)="cambiar($event)"
          [disabled]="cambiando"
        >
          <option value="" disabled>Seleccioná un usuario</option>
          <option *ngFor="let actor of demo.actores" [value]="actor.id">{{ actor.nombre }}</option>
        </select>
        <p *ngIf="cambiando" role="status">Cambiando sesión…</p>
        <p *ngIf="error" role="alert">{{ error }}</p>
      </aside>
      <router-outlet *ngIf="!sesion.estado.value || sesion.estado.value.actorActualId" />
    </main>`,
})
export class MascotasLayoutComponent implements OnInit {
  readonly sesion = inject(SesionMascotasService);
  cambiando = false;
  error = '';
  ngOnInit() {
    this.sesion.preparar();
  }
  cambiar(actorId: string) {
    if (this.cambiando || !actorId) return;
    this.cambiando = true;
    this.error = '';
    this.sesion
      .seleccionar(actorId)
      .pipe(finalize(() => (this.cambiando = false)))
      .subscribe({ error: (error) => (this.error = mensajeError(error)) });
  }
}
