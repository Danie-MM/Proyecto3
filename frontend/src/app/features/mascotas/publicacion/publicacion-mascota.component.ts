import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subscription, defer, finalize, map, switchMap, takeUntil, tap } from 'rxjs';
import { IntentosMascotaService } from '../intentos-mascota.service';
import { mensajeError } from '../mascotas-api.service';
import { Mascota } from '../mascotas.model';
import { SesionMascotasService } from '../sesion-mascotas.service';
import { PublicacionMascotaApi } from './publicacion-mascota.api';

@Component({
  standalone: true,
  selector: 'app-publicacion-mascota',
  imports: [CommonModule],
  templateUrl: './publicacion-mascota.component.html',
  styleUrls: ['../mascotas.css', '../bloque-b.css'],
})
export class PublicacionMascotaComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) mascota!: Mascota;
  @Output() readonly actualizada = new EventEmitter<Mascota>();
  @Output() readonly recargar = new EventEmitter<void>();
  private readonly api = inject(PublicacionMascotaApi);
  private readonly sesion = inject(SesionMascotasService);
  private readonly intentos = inject(IntentosMascotaService);
  private readonly destroyRef = inject(DestroyRef);
  private lectura?: Subscription;
  archivo: File | null = null;
  vistaFoto = '';
  errorFoto = '';
  error = '';
  aviso = '';
  cargandoFoto = false;
  ocupado = false;
  conflicto = false;

  ngOnChanges(changes: SimpleChanges) {
    const anterior: Mascota | undefined = changes['mascota']?.previousValue;
    if (anterior?.id !== this.mascota.id || anterior?.version !== this.mascota.version) this.leerFoto();
  }

  ngOnDestroy() {
    this.limpiarFoto();
  }

  leerFoto() {
    this.limpiarFoto();
    this.errorFoto = '';
    if (!this.mascota.fotoUrl) return;
    this.cargandoFoto = true;
    this.lectura = this.api.leerFoto(this.mascota.id).pipe(
      takeUntil(this.sesion.cambios),
      finalize(() => (this.cargandoFoto = false)),
    ).subscribe({
      next: blob => (this.vistaFoto = URL.createObjectURL(blob)),
      error: () => (this.errorFoto = 'No pudimos cargar la fotografía privada.'),
    });
  }

  elegir(campo: HTMLInputElement) {
    this.error = '';
    this.aviso = '';
    this.archivo = campo.files?.[0] ?? null;
    if (this.archivo && (this.archivo.size === 0 || this.archivo.size > 5_000_000)) {
      this.archivo = null;
      campo.value = '';
      this.error = 'Elegí un JPEG o PNG con contenido de hasta 5 000 000 bytes.';
    }
  }

  disponibilidad(estado: 'DISPONIBLE' | 'NO_DISPONIBLE') {
    this.guardar(this.api.disponibilidad(this.mascota, estado),
      estado === 'DISPONIBLE' ? 'La mascota está publicada.' : 'La mascota quedó oculta.');
  }

  subir(campo: HTMLInputElement) {
    const archivo = this.archivo;
    if (!archivo) return;
    const mascota = this.mascota;
    const actor = this.sesion.contexto.value?.usuarioId;
    if (!actor) { this.error = 'Recargá el perfil para verificar tu sesión.'; return; }
    const operacion = 'subirFoto.' + mascota.id;
    // Dos archivos distintos pueden tener el mismo nombre, tamaño y fecha.
    const solicitud = defer(async () => {
      const bytes = await archivo.arrayBuffer();
      const huella = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)),
        b => b.toString(16).padStart(2, '0')).join('');
      return this.intentos.obtener(actor, operacion, { huella, versionEsperada: mascota.version });
    }).pipe(
      switchMap(clave => this.api.subir(mascota, archivo, clave)),
      map(foto => ({ ...mascota, fotoUrl: foto.urlPrivada, version: foto.versionMascota })),
      tap(() => this.completarFoto(actor, operacion, campo)),
    );
    this.guardar(solicitud, 'Fotografía guardada.');
  }

  retirar(campo: HTMLInputElement) {
    const mascota = this.mascota;
    const actor = this.sesion.contexto.value?.usuarioId;
    if (!actor) { this.error = 'Recargá el perfil para verificar tu sesión.'; return; }
    const operacion = 'retirarFoto.' + mascota.id;
    const solicitud = defer(() => this.intentos.obtener(actor, operacion, {
      versionEsperada: mascota.version,
    })).pipe(
      switchMap(clave => this.api.retirar(mascota, clave)),
      map(respuesta => ({ ...mascota, fotoUrl: respuesta.foto, version: respuesta.versionMascota })),
      tap(() => this.completarFoto(actor, operacion, campo)),
    );
    this.guardar(solicitud, 'Fotografía retirada.');
  }

  private completarFoto(actor: string, operacion: string, campo: HTMLInputElement) {
    this.intentos.completar(actor, operacion);
    this.archivo = null;
    campo.value = '';
  }

  private guardar(solicitud: Observable<Mascota>, mensaje: string) {
    if (this.ocupado || this.conflicto || !this.mascota.activa) return;
    this.aviso = '';
    this.error = '';
    this.ocupado = true;
    solicitud.pipe(
      takeUntil(this.sesion.cambios),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => (this.ocupado = false)),
    ).subscribe({
      next: resultado => {
        this.actualizada.emit(resultado);
        this.aviso = mensaje;
      },
      error: error => {
        if (error instanceof HttpErrorResponse && error.status === 409 &&
          error.error?.codigo === 'VERSION_DESACTUALIZADA') {
          this.conflicto = true;
          this.error = 'El perfil cambió. Recargalo antes de volver a guardar.';
        } else if (error instanceof HttpErrorResponse && [413, 415].includes(error.status)) {
          this.error = 'Archivo rechazado. Elegí un JPEG o PNG real de hasta 5 MB. Se conservó la foto anterior.';
        } else {
          this.error = mensajeError(error);
        }
      },
    });
  }

  private limpiarFoto() {
    this.lectura?.unsubscribe();
    if (this.vistaFoto) URL.revokeObjectURL(this.vistaFoto);
    this.vistaFoto = '';
  }
}
