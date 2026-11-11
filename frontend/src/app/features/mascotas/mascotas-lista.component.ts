import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize } from 'rxjs';
import { MascotasApiService, mensajeError } from './mascotas-api.service';
import { PaginaMascotas, textoCatalogo } from './mascotas.model';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({
  standalone: true,
  selector: 'app-mascotas-lista',
  imports: [CommonModule, FormsModule, RouterLink],
  styleUrl: './mascotas.css',
  template: `<div class="titulo">
      <div>
        <p class="eyebrow">PERFILES PRIVADOS</p>
        <h1>Mis mascotas</h1>
        <p>Consultá y mantené los datos de las mascotas que tenés a cargo.</p>
      </div>
      <a class="btn-primary" routerLink="nueva">Registrar mascota</a>
    </div>
    <section class="card-basic tarjeta filtros" aria-label="Filtrar mascotas">
      <div>
        <label for="activas">Estado</label>
        <select class="select-single" id="activas" [(ngModel)]="filtro" (ngModelChange)="cargar(0)">
          <option value="todas">Todas</option>
          <option value="activas">Activas</option>
          <option value="inactivas">Desactivadas</option>
        </select>
      </div>
      <div *ngIf="sesion.organizaciones.length">
        <label for="org-filtro">Ámbito</label
        ><select
          class="select-single"
          id="org-filtro"
          [(ngModel)]="organizacion"
          (ngModelChange)="cargar(0)"
        >
          <option value="">Todos mis ámbitos</option>
          <option *ngFor="let org of sesion.organizaciones" [value]="org.id">
            {{ org.nombre }}
          </option>
        </select>
      </div>
      <button class="btn-secondary" (click)="cargar(pagina)" [disabled]="cargando">
        Actualizar
      </button>
    </section>
    <p *ngIf="cargando" role="status" class="aviso">Cargando mascotas…</p>
    <div *ngIf="error" class="error" role="alert">
      {{ error }} <button class="btn-link" (click)="cargar(pagina)">Reintentar</button>
    </div>
    <ng-container *ngIf="!cargando && !error && resultado as datos"
      ><section class="card-basic tarjeta vacio" *ngIf="!datos.elementos.length">
        <h2>No hay mascotas en esta lista</h2>
        <p>Podés registrar una mascota o cambiar el filtro de estado.</p>
        <a routerLink="nueva">Registrar la primera mascota</a>
      </section>
      <div class="rejilla">
        <article class="card-basic tarjeta mascota" *ngFor="let m of datos.elementos">
          <div class="linea">
            <span class="etiqueta">{{ texto(m.especie) }}</span
            ><span class="estado" [class.inactiva]="!m.activa">{{
              m.activa ? 'Activa' : 'Desactivada'
            }}</span>
          </div>
          <h2>
            <a [routerLink]="[m.id]">{{ m.nombre }}</a>
          </h2>
          <p>Edad estimada: {{ m.edadEstimadaMeses }} meses</p>
          <p>{{ texto(m.tamano) }} · Energía {{ texto(m.energia).toLowerCase() }}</p>
          <p class="muted">{{ texto(m.disponibilidad) }}</p>
          <a [routerLink]="[m.id]">Ver perfil →</a>
        </article>
      </div>
      <nav class="paginacion" aria-label="Páginas de mascotas">
        <button class="btn-secondary" (click)="cargar(pagina - 1)" [disabled]="pagina === 0">
          Anterior
        </button>
        <span>Página {{ pagina + 1 }} · {{ datos.totalElementos }} mascotas</span
        ><button
          class="btn-secondary"
          (click)="cargar(pagina + 1)"
          [disabled]="(pagina + 1) * datos.limite >= datos.totalElementos"
        >
          Siguiente
        </button>
      </nav>
    </ng-container>`,
})
export class MascotasListaComponent implements OnInit {
  private readonly api = inject(MascotasApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly sesion = inject(SesionMascotasService);
  readonly texto = textoCatalogo;
  resultado: PaginaMascotas | null = null;
  pagina = 0;
  filtro = 'todas';
  organizacion = '';
  cargando = false;
  error = '';
  private solicitud?: Subscription;
  ngOnInit() {
    this.sesion.cambios.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((estado) => {
      this.organizacion = '';
      if (estado === 'lista') {
        this.cargar(0);
        return;
      }
      this.solicitud?.unsubscribe();
      this.resultado = null;
      this.pagina = 0;
      this.cargando = estado === 'invalidada';
      this.error =
        estado === 'error'
          ? 'La sesión cambió, pero no pudimos recuperar sus permisos. Volvé a iniciar sesión o reintentá.'
          : '';
    });
    this.destroyRef.onDestroy(() => this.solicitud?.unsubscribe());
    if (this.sesion.actualizando) this.cargando = true;
    else this.cargar(0);
  }
  cargar(pagina: number) {
    // Una respuesta tardía nunca debe reponer el inventario de la sesión anterior.
    this.solicitud?.unsubscribe();
    this.resultado = null;
    this.pagina = pagina;
    this.error = '';
    this.cargando = true;
    const activas = this.filtro === 'todas' ? null : this.filtro === 'activas';
    this.solicitud = this.api
      .listar(pagina, activas, this.organizacion || null)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: (datos) => (this.resultado = datos),
        error: (error) => (this.error = mensajeError(error)),
      });
  }
}
