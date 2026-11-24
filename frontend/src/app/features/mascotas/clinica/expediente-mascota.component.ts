import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, Input, OnChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize, timeout } from 'rxjs';
import { IntentosMascotaService } from '../intentos-mascota.service';
import { MascotasApiService, mensajeError } from '../mascotas-api.service';
import { SesionMascotasService } from '../sesion-mascotas.service';
import { AlcanceClinico, EventoVacuna, PaginaClinica, PermisoClinico } from './clinica.model';

const fechaCostaRica = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'America/Costa_Rica', year: 'numeric', month: '2-digit', day: '2-digit',
});

@Component({
  standalone: true,
  selector: 'app-expediente-mascota',
  imports: [CommonModule, FormsModule],
  templateUrl: './expediente-mascota.component.html',
  styleUrl: '../mascotas.css',
})
export class ExpedienteMascotaComponent implements OnChanges {
  @Input({ required: true }) mascotaId = '';
  @Input() gestionaPermisos = false;

  private readonly http = inject(HttpClient);
  private readonly api = inject(MascotasApiService);
  private readonly sesion = inject(SesionMascotasService);
  private readonly intentos = inject(IntentosMascotaService);
  private readonly destroyRef = inject(DestroyRef);
  private generacion = 0;
  private consultaVacunas?: Subscription;
  private consultaPermisos?: Subscription;
  private consultaDetalle?: Subscription;
  private escritura?: Subscription;
  private get base(): string {
    return '/api/v1/mascotas/' + encodeURIComponent(this.mascotaId);
  }

  vacunas: PaginaClinica<EventoVacuna> | null = null;
  permisos: PaginaClinica<PermisoClinico> | null = null;
  detalle: EventoVacuna | null = null;
  permisoARevocar: PermisoClinico | null = null;
  soloActivos = true;
  tipo = '';
  desde = '';
  hasta = '';
  nombre = '';
  fecha = '';
  descripcion = '';
  beneficiario = '';
  alcance: AlcanceClinico = 'LECTURA';
  motivo = '';
  errorVacunas = '';
  errorPermisos = '';
  errorDetalle = '';
  errorEscritura = '';
  confirmacion = '';
  guardando = false;
  cargandoDetalle = false;

  get hoy(): string {
    return fechaCostaRica.format(Date.now());
  }

  constructor() {
    this.sesion.cambios.pipe(takeUntilDestroyed()).subscribe(estado => {
      this.limpiar();
      if (estado === 'lista') this.cargar();
    });
    this.destroyRef.onDestroy(() => this.limpiar());
  }
  ngOnChanges(): void {
    this.limpiar();
    this.cargar();
  }
  private cargar(): void {
    if (!this.mascotaId) return;
    this.cargarVacunas();
    if (this.gestionaPermisos) this.cargarPermisos();
  }

  private limpiar(): void {
    this.generacion++;
    this.consultaVacunas?.unsubscribe();
    this.consultaPermisos?.unsubscribe();
    this.consultaDetalle?.unsubscribe();
    this.escritura?.unsubscribe();
    this.vacunas = this.permisos = this.detalle = this.permisoARevocar = null;
    this.tipo = this.desde = this.hasta = this.nombre = this.fecha = this.descripcion = '';
    this.beneficiario = this.motivo = this.confirmacion = '';
    this.errorVacunas = this.errorPermisos = this.errorDetalle = this.errorEscritura = '';
    this.alcance = 'LECTURA';
    this.soloActivos = true;
    this.guardando = this.cargandoDetalle = false;
  }

  cargarVacunas(pagina = 0): void {
    this.consultaVacunas?.unsubscribe();
    this.cerrarDetalle();
    this.vacunas = null;
    this.errorVacunas = '';
    if (this.desde && this.hasta && this.desde > this.hasta) {
      this.errorVacunas = 'La fecha inicial debe ser anterior o igual a la final.';
      return;
    }
    let params = new HttpParams().set('pagina', pagina).set('limite', 20);
    if (this.tipo) params = params.set('tipo', this.tipo);
    if (this.desde) params = params.set('desde', this.desde);
    if (this.hasta) params = params.set('hasta', this.hasta);
    this.consultaVacunas = this.http.get<PaginaClinica<EventoVacuna>>(
      this.base + '/eventos-veterinarios', { params },
    ).pipe(timeout(15000)).subscribe({
      next: datos => this.vacunas = datos,
      error: error => { this.errorVacunas = this.mensaje(error); },
    });
  }

  abrirDetalle(id: string): void {
    this.cerrarDetalle();
    this.cargandoDetalle = true;
    this.consultaDetalle = this.http.get<EventoVacuna>(
      '/api/v1/eventos-veterinarios/' + encodeURIComponent(id),
    ).pipe(timeout(15000), finalize(() => this.cargandoDetalle = false)).subscribe({
      next: datos => this.detalle = datos,
      error: error => {
        if (this.accesoDenegado(error)) this.cargarVacunas();
        this.errorDetalle = this.mensaje(error);
      },
    });
  }

  cerrarDetalle(): void {
    this.consultaDetalle?.unsubscribe();
    this.detalle = null;
    this.cargandoDetalle = false;
    this.errorDetalle = '';
  }

  cargarPermisos(pagina = 0): void {
    if (!this.gestionaPermisos) return;
    this.consultaPermisos?.unsubscribe();
    this.permisos = this.permisoARevocar = null;
    this.errorPermisos = this.motivo = '';
    const params = new HttpParams().set('pagina', pagina).set('limite', 20)
      .set('soloActivos', this.soloActivos);
    this.consultaPermisos = this.http.get<PaginaClinica<PermisoClinico>>(
      this.base + '/permisos-clinicos', { params },
    ).pipe(timeout(15000)).subscribe({
      next: datos => this.permisos = datos,
      error: error => { this.errorPermisos = this.mensaje(error); },
    });
  }

  registrar(): void {
    const nombre = this.nombre.trim();
    if (!nombre || [...nombre].length > 2000 || !this.fecha || this.fecha > this.hoy ||
        [...this.descripcion.trim()].length > 2000) {
      this.errorEscritura = 'Revisá el nombre, la descripción y la fecha de la vacuna. La fecha no puede ser futura.';
      return;
    }
    const body = { tipo: 'VACUNA', nombre, fecha: this.fecha, descripcion: this.descripcion.trim() || null };
    void this.guardar('vacuna', this.base + '/eventos-veterinarios', body, () => {
      this.nombre = this.fecha = this.descripcion = '';
      this.confirmacion = 'Vacuna registrada.';
      this.cargarVacunas();
    });
  }

  conceder(): void {
    if (!this.gestionaPermisos) return;
    const beneficiarioUsuarioId = this.beneficiario.trim();
    if (!/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(beneficiarioUsuarioId)) {
      this.errorEscritura = 'Ingresá el identificador de usuario del veterinario (UUID).';
      return;
    }
    const body = { beneficiarioUsuarioId, fundamento: 'VETERINARIO_AUTORIZADO', alcance: this.alcance };
    void this.guardar('permiso', this.base + '/permisos-clinicos', body, () => {
      this.beneficiario = '';
      this.alcance = 'LECTURA';
      this.confirmacion = 'Permiso clínico concedido.';
      this.cargarPermisos();
    });
  }

  revocar(): void {
    if (!this.gestionaPermisos || !this.permisoARevocar) return;
    const permiso = this.permisoARevocar;
    const motivo = this.motivo.trim();
    if (!motivo || [...motivo].length > 500) {
      this.errorEscritura = 'Ingresá un motivo de 1 a 500 caracteres.';
      return;
    }
    const body = { motivo, versionEsperada: permiso.version };
    const ruta = '/api/v1/permisos-clinicos/' + encodeURIComponent(permiso.id) + '/revocacion';
    void this.guardar('revocar.' + permiso.id, ruta, body, () => {
      this.confirmacion = 'Permiso clínico revocado.';
      this.cargarPermisos();
    });
  }

  private async guardar(operacion: string, ruta: string, body: unknown, hecho: () => void): Promise<void> {
    if (this.guardando) return;
    this.errorEscritura = this.confirmacion = '';
    const usuario = this.sesion.contexto.value?.usuarioId;
    if (!usuario) {
      this.errorEscritura = 'Volvé a iniciar sesión para guardar los cambios.';
      return;
    }
    const generacion = this.generacion;
    const intento = operacion + '.' + this.mascotaId;
    this.guardando = true;
    try {
      const clave = await this.intentos.obtener(usuario, intento, body);
      if (this.generacion !== generacion) return;
      if (usuario !== this.sesion.contexto.value?.usuarioId) {
        this.guardando = false;
        return;
      }
      this.escritura = this.api.escribir('POST', ruta, body, clave).pipe(
        finalize(() => this.guardando = false),
      ).subscribe({
        next: () => {
          this.intentos.completar(usuario, intento);
          hecho();
        },
        error: error => {
          if (this.accesoDenegado(error)) {
            this.cargarVacunas();
            if (this.gestionaPermisos) this.cargarPermisos();
          } else if (error.status === 409 && operacion.startsWith('revocar.')) {
            this.cargarPermisos();
          }
          this.errorEscritura = this.mensaje(error);
        },
      });
    } catch {
      if (generacion === this.generacion) {
        this.guardando = false;
        this.errorEscritura = 'No pudimos preparar la operación. Reintentá.';
      }
    }
  }

  private accesoDenegado(error: unknown): boolean {
    return error instanceof HttpErrorResponse && [401, 403, 404].includes(error.status);
  }
  private mensaje(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) return 'El registro no está disponible o no tenés acceso.';
      if (error.status === 503) return 'El servicio necesario para completar la operación no está disponible. Reintentá más tarde.';
    }
    return mensajeError(error);
  }
}
