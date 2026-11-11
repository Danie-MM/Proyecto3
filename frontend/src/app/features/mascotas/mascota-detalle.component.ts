import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { MascotasApiService, mensajeError } from './mascotas-api.service';
import { IntentosMascotaService } from './intentos-mascota.service';
import { Mascota, textoCatalogo } from './mascotas.model';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({ standalone: true, selector: 'app-mascota-detalle', imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mascota-detalle.component.html', styleUrl: './mascotas.css' })
export class MascotaDetalleComponent implements OnInit {
  private readonly api = inject(MascotasApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sesion = inject(SesionMascotasService);
  private readonly intentos = inject(IntentosMascotaService);
  readonly texto = textoCatalogo;
  mascota: Mascota | null = null;
  cargando = false; ocupado = false; error = ''; confirmacion = ''; confirmarBaja = false; motivo = ''; conflicto = false;
  private solicitudes = new Subscription();
  private generacion = 0;
  ngOnInit() {
    this.confirmacion = typeof history.state?.confirmacion === 'string' ? history.state.confirmacion : '';
    // Evita repetir una confirmación antigua al recargar; los datos se consultan siempre al servidor.
    if (this.confirmacion) { const estado = { ...history.state }; delete estado.confirmacion; history.replaceState(estado, ''); }
    this.destroyRef.onDestroy(() => { this.generacion++; this.solicitudes.unsubscribe(); });
    this.ruta.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.sesion.cambios.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(estado => {
      this.confirmacion = '';
      if (estado === 'lista') { this.cargar(); return; }
      this.generacion++; this.solicitudes.unsubscribe(); this.mascota = null; this.confirmarBaja = false; this.motivo = '';
      this.ocupado = false; this.cargando = estado === 'invalidada';
      this.error = estado === 'error' ? 'La sesión cambió, pero no pudimos recuperar sus permisos. Volvé a iniciar sesión o reintentá.' : '';
    });
  }
  cargar() {
    this.generacion++; this.solicitudes.unsubscribe(); this.solicitudes = new Subscription();
    this.mascota = null; this.error = ''; this.cargando = true; this.ocupado = false; this.conflicto = false; this.confirmarBaja = false; this.motivo = '';
    const id = this.ruta.snapshot.paramMap.get('id') ?? '';
    this.solicitudes.add(this.api.consultar(id).pipe(finalize(() => this.cargando = false))
      .subscribe({ next: mascota => this.mascota = mascota, error: error => { this.error = mensajeError(error); this.confirmacion = ''; } }));
  }
  async desactivar(): Promise<void> {
    if (this.ocupado || !this.mascota || !this.mascota.activa) return;
    const motivo = this.motivo.trim();
    if (![...motivo].length || [...motivo].length > 500) { this.error = 'Escribí el motivo de desactivación, de 1 a 500 caracteres.'; return; }
    const actorId = this.sesion.contexto.value?.usuarioId;
    if (!actorId) { this.error = 'No pudimos verificar tu sesión. Recargá la página antes de desactivar.'; return; }
    const mascota = this.mascota; const generacion = this.generacion; const operacion = 'desactivar.' + mascota.id;
    this.ocupado = true; this.error = ''; this.confirmacion = '';
    try {
      const clave = await this.intentos.obtener(actorId, operacion, { motivo, versionEsperada: mascota.version });
      if (generacion !== this.generacion) return;
      this.solicitudes.add(this.api.desactivar(mascota.id, motivo, mascota.version, clave).pipe(finalize(() => this.ocupado = false))
        .subscribe({ next: resultado => {
          if (resultado?.id !== mascota.id || resultado.activa !== false) { this.error = 'No pudimos confirmar la desactivación. Reintentá.'; return; }
          this.intentos.completar(actorId, operacion); this.mascota = resultado; this.confirmarBaja = false;
          this.confirmacion = 'La mascota quedó desactivada. Su perfil y su identificador se conservan.';
        }, error: error => {
          this.error = mensajeError(error);
          this.conflicto = error instanceof HttpErrorResponse && error.status === 409 && error.error?.codigo === 'VERSION_DESACTUALIZADA';
          if (this.conflicto) this.error = 'El perfil cambió desde que lo abriste. Recargá los datos antes de volver a intentar la desactivación.';
        } }));
    } catch { this.ocupado = false; this.error = 'No pudimos preparar la desactivación. Reintentá.'; }
  }
}
