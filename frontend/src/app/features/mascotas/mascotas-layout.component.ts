import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { mensajeError } from './mascotas-api.service';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({ selector: 'app-mascotas-layout', standalone: true, imports: [CommonModule, FormsModule, RouterLink, RouterOutlet], styleUrl: './mascotas.css',
  template: `<header class="cabecera navbar-top"><div class="ancho barra"><a class="marca" routerLink="/mascotas">PetMatch<span>CR</span></a>
    <nav aria-label="Navegación principal"><a routerLink="/mascotas">Mis mascotas</a><a routerLink="/comprobar-entorno">Entorno</a></nav></div></header>
    <main class="ancho"><aside class="demo" *ngIf="sesion.estado | async as demo"><strong>Demostración local: usuarios ficticios</strong>
      <p>Este selector permite comprobar permisos. La sesión real del equipo pertenece al bloque A.</p>
      <label for="actor-demo">Usuario de prueba</label><select class="select-single" id="actor-demo" [ngModel]="demo.actorActualId ?? ''" (ngModelChange)="cambiar($event)" [disabled]="cambiando">
        <option value="" disabled>Seleccioná un usuario</option><option *ngFor="let actor of demo.actores" [value]="actor.id">{{ actor.nombre }}</option></select>
      <p *ngIf="cambiando" role="status">Cambiando sesión…</p><p *ngIf="error" role="alert">{{ error }}</p></aside><router-outlet /></main>` })
export class MascotasLayoutComponent implements OnInit {
  readonly sesion = inject(SesionMascotasService);
  cambiando = false; error = '';
  ngOnInit() { this.sesion.preparar(); }
  cambiar(actorId: string) {
    if (this.cambiando || !actorId) return;
    this.cambiando = true; this.error = '';
    this.sesion.seleccionar(actorId).pipe(finalize(() => this.cambiando = false)).subscribe({ error: error => this.error = mensajeError(error) });
  }
}
