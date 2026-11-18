import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { mensajeError } from './mascotas-api.service';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({
  selector: 'app-mascotas-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet],
  host: { class: 'modulo-mascotas' },
  styleUrl: './mascotas.css',
  template: `<main class="ancho">
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
  private readonly destroyRef = inject(DestroyRef);
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
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => (this.cambiando = false)))
      .subscribe({ error: (error) => (this.error = mensajeError(error)) });
  }
}
