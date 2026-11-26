
import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';

import {
  Subject,
  Subscription,
  takeUntil,
  finalize,
  TimeoutError
} from 'rxjs';

import * as L from 'leaflet';

import {
  ReporteExtravioService
} from '../publicar-extravio/reporte-extravio.service';

import {
  ReporteExtravioDetalle,
  EstadoReporteExtravio
} from '../publicar-extravio/reporte-extravio.model';

/**
 * Presenta la información permitida de un reporte de extravío
 * y su ubicación vigente mediante un mapa de solo consulta.
 */
@Component({
  selector: 'app-detalle-extravio',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  providers: [
    ReporteExtravioService
  ],
  templateUrl: './detalle-extravio.component.html',
  styleUrl: './detalle-extravio.component.css'
})
export class DetalleExtravioComponent
  implements OnInit, OnDestroy {

  private readonly ruta = inject(ActivatedRoute);

  private readonly reporteService =
    inject(ReporteExtravioService);

  private readonly destruir$ = new Subject<void>();

  private consultaDetalle?: Subscription;

  private mapa?: L.Map;

  private temporizadorMapa?: ReturnType<typeof setTimeout>;

  private componenteDestruido = false;

  reporteId = '';

  reporte: ReporteExtravioDetalle | null = null;

  cargando = false;

  mensajeError = '';

  mensajeMapa = '';

  /**
   * Inicializa el mapa cuando su contenedor aparece
   * y libera los recursos cuando desaparece.
   */
  @ViewChild('mapaDetalle')
  set contenedorMapa(
    elemento: ElementRef<HTMLDivElement> | undefined
  ) {

    this.destruirMapa();

    if (!elemento || this.componenteDestruido) {
      return;
    }

    this.temporizadorMapa = setTimeout(() => {

      this.temporizadorMapa = undefined;

      if (!this.componenteDestruido) {
        this.inicializarMapa(elemento.nativeElement);
      }

    }, 0);
  }

  /**
   * Consulta el reporte correspondiente al identificador
   * recibido mediante la ruta.
   */
  ngOnInit(): void {

    this.ruta.paramMap
      .pipe(takeUntil(this.destruir$))
      .subscribe(parametros => {

        const nuevoId = parametros.get('id') ?? '';

        this.consultaDetalle?.unsubscribe();

        this.destruirMapa();

        this.reporteId = nuevoId;

        this.reporte = null;

        this.mensajeError = '';

        this.mensajeMapa = '';

        if (!this.reporteId) {

          this.mensajeError =
            'No se proporcionó un identificador de reporte.';

          return;
        }

        this.cargarDetalle();

      });
  }

  /**
   * Consulta la información vigente del reporte.
   */
  cargarDetalle(): void {

    if (
      this.cargando ||
      !this.reporteId ||
      this.componenteDestruido
    ) {
      return;
    }

    this.destruirMapa();

    this.cargando = true;

    this.mensajeError = '';

    this.mensajeMapa = '';

    this.reporte = null;

    this.consultaDetalle = this.reporteService
      .consultarDetalle(this.reporteId)
      .pipe(
        takeUntil(this.destruir$),
        finalize(() => {
          this.cargando = false;
        })
      )
      .subscribe({

        next: (reporte) => {

          this.reporte = reporte;

        },

        error: (error: unknown) => {

          this.mensajeError =
            this.obtenerMensajeError(error);

        }

      });
  }

  /**
   * Indica si el reporte vigente puede ser editado
   * por el usuario autenticado.
   */
  get puedeEditar(): boolean {

    return (
      this.reporte?.estado === 'ACTIVO' &&
      this.reporte.puedeEditar === true
    );
  }

  /**
   * Indica si corresponde mostrar la ubicación
   * y los datos de contacto autorizados.
   */
  get mostrarInformacionActiva(): boolean {

    return this.reporte?.estado === 'ACTIVO';
  }

  /**
   * Inicializa un mapa de solo consulta utilizando
   * las coordenadas vigentes del reporte.
   */
  private inicializarMapa(
    contenedor: HTMLDivElement
  ): void {

    if (
      !this.reporte ||
      !this.mostrarInformacionActiva ||
      this.componenteDestruido
    ) {
      return;
    }

    const latitud = this.reporte.latitud;

    const longitud = this.reporte.longitud;

    if (
      !Number.isFinite(latitud) ||
      !Number.isFinite(longitud) ||
      latitud < -90 ||
      latitud > 90 ||
      longitud < -180 ||
      longitud > 180
    ) {

      this.mensajeMapa =
        'No se dispone de coordenadas válidas para mostrar el mapa.';

      return;
    }

    this.mensajeMapa = '';

    try {

      const posicion = L.latLng(
        latitud,
        longitud
      );

      this.mapa = L.map(contenedor, {
        zoomControl: true,
        scrollWheelZoom: false,
        dragging: true,
        doubleClickZoom: true,
        keyboard: true
      });

      this.mapa.setView(posicion, 15);

      const capaMapa = L.tileLayer(
        'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
        {
          maxZoom: 19,
          attribution: '&copy; OpenStreetMap contributors'
        }
      );

      capaMapa.on('tileerror', () => {

        if (!this.componenteDestruido) {

          this.mensajeMapa =
            'No fue posible cargar parte del mapa. ' +
            'Las coordenadas del reporte siguen disponibles.';

        }

      });

      capaMapa.addTo(this.mapa);

      L.circleMarker(posicion, {
        radius: 9,
        color: '#FFFFFF',
        weight: 3,
        fillColor: '#245B78',
        fillOpacity: 1
      })
        .addTo(this.mapa)
        .bindTooltip('Ubicación vigente del extravío');

      this.temporizadorMapa = setTimeout(() => {

        this.temporizadorMapa = undefined;

        this.mapa?.invalidateSize();

      }, 200);

    } catch {

      this.destruirMapa();

      this.mensajeMapa =
        'No fue posible inicializar el mapa. ' +
        'Podés consultar las coordenadas del reporte.';

    }
  }

  /**
   * Libera los recursos del mapa y cancela
   * cualquier inicialización pendiente.
   */
  private destruirMapa(): void {

    if (this.temporizadorMapa !== undefined) {

      clearTimeout(this.temporizadorMapa);

      this.temporizadorMapa = undefined;

    }

    if (this.mapa) {

      this.mapa.remove();

      this.mapa = undefined;

    }
  }

  /**
   * Devuelve una descripción legible del estado del reporte.
   */
  obtenerEstado(
    estado: EstadoReporteExtravio
  ): string {

    switch (estado) {

      case 'ACTIVO':
        return 'Activo';

      case 'ENCONTRADA':
        return 'Mascota encontrada';

      case 'CANCELADO':
        return 'Cancelado';

      default:
        return 'Estado desconocido';

    }
  }

  /**
   * Traduce los errores de consulta en mensajes
   * comprensibles para el usuario.
   */
  private obtenerMensajeError(
    error: unknown
  ): string {

    if (error instanceof TimeoutError) {

      return 'La consulta está tardando demasiado. Intentá nuevamente.';

    }

    if (error instanceof HttpErrorResponse) {

      switch (error.status) {

        case 0:
          return 'No fue posible establecer comunicación con el servidor.';

        case 400:
          return 'El identificador del reporte no es válido.';

        case 401:
          return 'Tu sesión ha expirado. Iniciá sesión nuevamente.';

        case 403:
          return 'No tenés permisos para consultar este reporte.';

        case 404:
          return 'No encontramos el reporte de extravío solicitado.';

        default:
          return 'No fue posible consultar el reporte. Intentá nuevamente.';

      }
    }

    return 'Ocurrió un error inesperado al consultar el reporte.';
  }

  /**
   * Libera las suscripciones, el mapa y los datos
   * del reporte al abandonar el componente.
   */
  ngOnDestroy(): void {

    this.componenteDestruido = true;

    this.destruir$.next();

    this.destruir$.complete();

    this.consultaDetalle?.unsubscribe();

    this.destruirMapa();

    this.reporte = null;

  }
}
