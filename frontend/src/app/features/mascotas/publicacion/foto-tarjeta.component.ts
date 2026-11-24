import { NgIf } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { Mascota } from '../mascotas.model';
import { SesionMascotasService } from '../sesion-mascotas.service';
import { PublicacionMascotaApi } from './publicacion-mascota.api';

@Component({
  standalone: true,
  selector: 'app-foto-tarjeta',
  imports: [NgIf],
  template: `
    <img *ngIf="vistaFoto; else huella" [src]="vistaFoto" [alt]="'Foto de ' + mascota.nombre"
         (error)="errorImagen($event)" />
    <ng-template #huella>
      <span class="sin-foto" role="img"
            [attr.aria-label]="mascota.fotoUrl ? 'Fotografía no disponible' : 'Sin fotografía'">
        <span class="huella" aria-hidden="true"></span>
      </span>
    </ng-template>
  `,
  styles: [`
    :host {
      position: relative;
      display: block;
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: #e8f1f7;
    }
    img, .sin-foto { position: absolute; inset: 0; display: block; width: 100%; height: 100%; }
    img { object-fit: cover; }
    .sin-foto {
      display: grid;
      place-items: center;
      background: linear-gradient(145deg, #f5f8fc 0 48%, #e8f1f7 48% 74%, #dfebf3 74%);
    }
    .huella {
      width: 30%;
      max-width: 4.25rem;
      aspect-ratio: 1;
      background: #779bad;
      mask: url('/assets/icons/pets-svgrepo-com.svg') center / contain no-repeat;
    }
  `],
})
export class FotoTarjetaComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) mascota!: Mascota;
  private readonly api = inject(PublicacionMascotaApi);
  private readonly sesion = inject(SesionMascotasService);
  private pendiente?: Subscription;
  private sesionSuspendida = false;
  vistaFoto = '';

  constructor() {
    this.sesion.cambios.pipe(takeUntilDestroyed()).subscribe(estado => {
      this.sesionSuspendida = estado !== 'lista';
      this.cargar();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    const previa: Mascota | undefined = changes['mascota']?.previousValue;
    if (previa?.id !== this.mascota.id || previa?.version !== this.mascota.version ||
        previa?.fotoUrl !== this.mascota.fotoUrl) this.cargar();
  }

  ngOnDestroy(): void {
    this.limpiar();
  }

  errorImagen(evento: Event): void {
    const imagen = evento.target as HTMLImageElement;
    if (imagen.getAttribute('src') === this.vistaFoto) this.limpiar();
  }

  private cargar(): void {
    this.limpiar();
    if (this.sesionSuspendida || !this.mascota?.fotoUrl) return;
    this.pendiente = this.api.leerFoto(this.mascota.id).subscribe({
      next: foto => { this.vistaFoto = URL.createObjectURL(foto); },
      error: () => this.limpiar(),
    });
  }

  private limpiar(): void {
    this.pendiente?.unsubscribe();
    if (this.vistaFoto) URL.revokeObjectURL(this.vistaFoto);
    this.vistaFoto = '';
  }
}
