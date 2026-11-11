import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { MascotasApiService, mensajeError } from './mascotas-api.service';
import { IntentosMascotaService } from './intentos-mascota.service';
import { Mascota, textoCatalogo } from './mascotas.model';
import { SesionMascotasService } from './sesion-mascotas.service';
import { ConfirmacionModalComponent } from '../../shared/components/confirmacion-modal.component';

@Component({
  standalone: true,
  selector: 'app-mascota-detalle',
  imports: [CommonModule, FormsModule, RouterLink, ConfirmacionModalComponent],
  templateUrl: './mascota-detalle.component.html',
  styleUrl: './mascotas.css',
})
export class MascotaDetalleComponent implements OnInit {
  private readonly api = inject(MascotasApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sesion = inject(SesionMascotasService);
  private readonly intentos = inject(IntentosMascotaService);
  readonly texto = textoCatalogo;
  mascota: Mascota | null = null;
  cargando = false;
  ocupado = false;
  error = '';
  confirmacion = '';
  confirmarBaja = false;
  motivo = '';
  conflicto = false;
  // Los errores de la baja permanecen dentro del modal; no se confunden con cargar el perfil.
  errorBaja = '';
  errorMotivo = '';
  avisoBaja = '';
  recargandoBaja = false;
  @ViewChild('motivoCampo') private motivoCampo?: ElementRef<HTMLTextAreaElement>;
  private solicitudes = new Subscription();
  // Invalida operaciones pendientes si antes cambia el perfil o la sesión.
  private generacion = 0;
  ngOnInit() {
    this.confirmacion =
      typeof history.state?.confirmacion === 'string' ? history.state.confirmacion : '';
    // Evita repetir una confirmación antigua al recargar; los datos se consultan siempre al servidor.
    if (this.confirmacion) {
      const estado = { ...history.state };
      delete estado.confirmacion;
      history.replaceState(estado, '');
    }
    this.destroyRef.onDestroy(() => {
      this.generacion++;
      this.solicitudes.unsubscribe();
    });
    this.ruta.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.sesion.cambios.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((estado) => {
      this.confirmacion = '';
      if (estado === 'lista') {
        this.cargar();
        return;
      }
      this.generacion++;
      this.solicitudes.unsubscribe();
      this.mascota = null;
      this.confirmarBaja = false;
      this.motivo = '';
      this.errorBaja = '';
      this.errorMotivo = '';
      this.avisoBaja = '';
      this.recargandoBaja = false;
      this.conflicto = false;
      this.ocupado = false;
      this.cargando = estado === 'invalidada';
      this.error =
        estado === 'error'
          ? 'La sesión cambió, pero no pudimos recuperar sus permisos. Volvé a iniciar sesión o reintentá.'
          : '';
    });
  }
  cargar() {
    this.generacion++;
    this.solicitudes.unsubscribe();
    this.solicitudes = new Subscription();
    this.mascota = null;
    this.error = '';
    this.cargando = true;
    this.ocupado = false;
    this.conflicto = false;
    this.confirmarBaja = false;
    this.motivo = '';
    this.errorBaja = '';
    this.errorMotivo = '';
    this.avisoBaja = '';
    this.recargandoBaja = false;
    const id = this.ruta.snapshot.paramMap.get('id') ?? '';
    this.solicitudes.add(
      this.api
        .consultar(id)
        .pipe(finalize(() => (this.cargando = false)))
        .subscribe({
          next: (mascota) => (this.mascota = mascota),
          error: (error) => {
            this.error = mensajeError(error);
            this.confirmacion = '';
          },
        }),
    );
  }
  abrirBaja(): void {
    if (this.ocupado || !this.mascota?.activa) return;
    this.motivo = '';
    this.errorBaja = '';
    this.errorMotivo = '';
    this.avisoBaja = '';
    this.conflicto = false;
    this.confirmarBaja = true;
  }

  cancelarBaja(): void {
    if (this.ocupado || this.recargandoBaja) return;
    this.confirmarBaja = false;
    this.motivo = '';
    this.errorBaja = '';
    this.errorMotivo = '';
    this.avisoBaja = '';
    this.conflicto = false;
  }

  /** Un 409 no confirma nada: primero consultamos la versión nueva y la persona decide otra vez. */
  recargarParaBaja(): void {
    if (this.ocupado || this.recargandoBaja || !this.confirmarBaja || !this.mascota) return;
    const generacion = this.generacion;
    this.recargandoBaja = true;
    this.errorBaja = '';
    this.avisoBaja = '';
    this.solicitudes.add(
      this.api
        .consultar(this.mascota.id)
        .pipe(finalize(() => (this.recargandoBaja = false)))
        .subscribe({
          next: (mascota) => {
            if (generacion !== this.generacion) return;
            this.mascota = mascota;
            this.conflicto = false;
            this.avisoBaja = mascota.activa
              ? 'Datos actualizados. Revisalos antes de confirmar nuevamente.'
              : 'Este perfil ya está desactivado. Su identificador y sus registros se conservan.';
          },
          error: (error) => {
            if (generacion === this.generacion) this.errorBaja = mensajeError(error);
          },
        }),
    );
  }

  async desactivar(): Promise<void> {
    if (
      !this.confirmarBaja ||
      this.ocupado ||
      this.recargandoBaja ||
      this.conflicto ||
      !this.mascota?.activa
    )
      return;
    const motivo = this.motivo.trim();
    this.errorMotivo = '';
    if (![...motivo].length || [...motivo].length > 500) {
      this.errorMotivo = 'Escribí un motivo de 1 a 500 caracteres.';
      this.motivoCampo?.nativeElement.focus();
      return;
    }
    const actorId = this.sesion.contexto.value?.usuarioId;
    if (!actorId) {
      this.errorBaja = 'No pudimos verificar tu sesión. Recargá la página antes de desactivar.';
      return;
    }
    const mascota = this.mascota;
    const generacion = this.generacion;
    const operacion = 'desactivar.' + mascota.id;
    this.ocupado = true;
    this.errorBaja = '';
    this.avisoBaja = '';
    this.confirmacion = '';
    try {
      const clave = await this.intentos.obtener(actorId, operacion, {
        motivo,
        versionEsperada: mascota.version,
      });
      if (generacion !== this.generacion) return;
      this.solicitudes.add(
        this.api
          .desactivar(mascota.id, motivo, mascota.version, clave)
          .pipe(finalize(() => (this.ocupado = false)))
          .subscribe({
            next: (resultado) => {
              if (resultado?.id !== mascota.id || resultado.activa !== false) {
                this.errorBaja = 'No pudimos confirmar la desactivación. Reintentá.';
                return;
              }
              this.intentos.completar(actorId, operacion);
              this.mascota = resultado;
              this.confirmarBaja = false;
              this.confirmacion =
                'La mascota quedó desactivada. Su perfil y su identificador se conservan.';
            },
            error: (error) => {
              this.errorBaja = mensajeError(error);
              this.conflicto =
                error instanceof HttpErrorResponse &&
                error.status === 409 &&
                error.error?.codigo === 'VERSION_DESACTUALIZADA';
              if (this.conflicto)
                this.errorBaja =
                  'El perfil cambió desde que lo abriste. Recargá los datos antes de volver a intentar la desactivación.';
            },
          }),
      );
    } catch {
      if (generacion === this.generacion) {
        this.ocupado = false;
        this.errorBaja = 'No pudimos preparar la desactivación. Reintentá.';
      }
    }
  }
}
