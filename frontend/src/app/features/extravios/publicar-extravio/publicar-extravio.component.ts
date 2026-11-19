
import {
  AfterViewInit,
  Component,
  OnDestroy,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Subject, expand, reduce, switchMap, takeUntil, EMPTY } from 'rxjs';
import * as L from 'leaflet';

import { MascotasApiService } from '../../mascotas/mascotas-api.service';
import { Mascota } from '../../mascotas/mascotas.model';

import {
  ConfirmacionModalComponent
} from '../../../shared/components/confirmacion-modal.component';

import { ReporteExtravioService } from './reporte-extravio.service';

import {
  CrearReporteExtravio,
  ReporteExtravio,
  MedioContacto
} from './reporte-extravio.model';

@Component({
  selector: 'app-publicar-extravio',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ConfirmacionModalComponent
  ],
  providers: [ReporteExtravioService],
  templateUrl: './publicar-extravio.component.html',
  styleUrl: './publicar-extravio.component.css'
})
export class PublicarExtravioComponent
  implements AfterViewInit, OnDestroy {

  private readonly http = inject(HttpClient);
  private readonly mascotasService = inject(MascotasApiService);
  private readonly reporteService = inject(ReporteExtravioService);

  private readonly destruir$ = new Subject<void>();

  private mapa?: L.Map;
  private marcador?: L.CircleMarker;

  // Mascotas del usuario
  mascotas: Mascota[] = [];
  mascotaId = '';
  cargandoMascotas = true;
  errorMascotas = '';

  // Datos del extravío
  fechaExtravio = '';
  latitud: number | null = null;
  longitud: number | null = null;
  descripcion = '';

  // Datos de contacto
  tipoContacto: MedioContacto = 'CORREO';
  contacto = '';
  autorizaContacto = false;

  // Búsqueda de ubicación
  busqueda = '';
  buscando = false;
  mensajeBusqueda = '';
  tipoMensajeBusqueda: 'info' | 'advertencia' | 'error' = 'info';

  // Estado de la publicación
  modalAbierto = false;
  publicando = false;
  errorPublicacion = '';

  reportePublicado: ReporteExtravio | null = null;
  reporteExistente = false;

  private datosPendientes: CrearReporteExtravio | null = null;

  ngAfterViewInit(): void {
    this.inicializarMapa();
    this.cargarMascotas();
  }

  /** Inicializa el mapa para seleccionar el lugar del extravío. */
  private inicializarMapa(): void {

    this.mapa = L.map('mapa-extravio')
      .setView([9.9281, -84.0907], 12);

    L.tileLayer(
      'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
      }
    ).addTo(this.mapa);

    this.mapa.on('click', (evento: L.LeafletMouseEvent) => {

      if (this.publicando || this.modalAbierto || this.reportePublicado) {
        return;
      }

      this.seleccionarUbicacion(
        evento.latlng.lat,
        evento.latlng.lng
      );
    });

    setTimeout(() => {
      this.mapa?.invalidateSize();
    }, 200);
  }

  /** Consulta todas las páginas de mascotas activas del responsable. */
  cargarMascotas(): void {

    if (this.publicando) return;

    this.cargandoMascotas = true;
    this.errorMascotas = '';

    this.mascotasService.contexto().pipe(

      switchMap(contexto =>
        this.mascotasService.listar(0, true).pipe(

          expand(pagina => {

            const siguiente = pagina.pagina + 1;

            return siguiente * pagina.limite < pagina.totalElementos
              ? this.mascotasService.listar(siguiente, true)
              : EMPTY;
          }),

          reduce(
            (acumuladas: Mascota[], pagina) =>
              [...acumuladas, ...pagina.elementos],
            [] as Mascota[]
          ),

          switchMap(mascotas => {

            const propias = mascotas.filter(mascota =>
              mascota.activa &&
              mascota.responsableUsuarioId === contexto.usuarioId
            );

            return [propias];
          })
        )
      ),

      takeUntil(this.destruir$)

    ).subscribe({

      next: mascotas => {
        this.mascotas = mascotas;

        if (!mascotas.some(mascota => mascota.id === this.mascotaId)) {
          this.mascotaId = '';
        }

        this.cargandoMascotas = false;
      },

      error: () => {
        this.mascotas = [];
        this.mascotaId = '';
        this.cargandoMascotas = false;
        this.errorMascotas =
          'No se pudieron cargar tus mascotas. Intentá nuevamente.';
      }
    });
  }

  /** Devuelve la mascota seleccionada para mostrarla en el formulario. */
  get mascotaSeleccionada(): Mascota | undefined {
    return this.mascotas.find(
      mascota => mascota.id === this.mascotaId
    );
  }

  /** Obtiene la fecha máxima permitida en horario local. */
  get fechaMaxima(): string {

    const ahora = new Date();

    const local = new Date(
      ahora.getTime() - ahora.getTimezoneOffset() * 60000
    );

    return local.toISOString().slice(0, 16);
  }

  /** Registra las coordenadas seleccionadas en el mapa. */
  private seleccionarUbicacion(
    latitud: number,
    longitud: number
  ): void {

    this.latitud = Number(latitud.toFixed(6));
    this.longitud = Number(longitud.toFixed(6));

    if (!this.mapa) return;

    const posicion = L.latLng(
      this.latitud,
      this.longitud
    );

    if (this.marcador) {
      this.marcador.setLatLng(posicion);
    } else {
      this.marcador = L.circleMarker(posicion, {
        radius: 9,
        color: '#FFFFFF',
        weight: 3,
        fillColor: '#245B78',
        fillOpacity: 1
      }).addTo(this.mapa);
    }

    this.errorPublicacion = '';
  }

  /** Busca una ubicación en Costa Rica sin seleccionar el punto automáticamente. */
  buscarUbicacion(): void {

    const lugar = this.busqueda.trim();

    if (
      !lugar ||
      this.buscando ||
      this.publicando ||
      this.modalAbierto ||
      this.reportePublicado
    ) {
      return;
    }

    this.buscando = true;
    this.mensajeBusqueda = '';
    this.tipoMensajeBusqueda = 'info';

    const parametros = {
      q: lugar,
      format: 'json',
      limit: '1',
      countrycodes: 'cr'
    };

    this.http.get<Array<{
      lat: string;
      lon: string;
      display_name: string;
    }>>(
      'https://nominatim.openstreetmap.org/search',
      { params: parametros }
    ).pipe(
      takeUntil(this.destruir$)
    ).subscribe({

      next: resultados => {

        this.buscando = false;

        if (resultados.length === 0) {
          this.tipoMensajeBusqueda = 'advertencia';
          this.mensajeBusqueda =
            'No encontramos esa ubicación en Costa Rica.';
          return;
        }

        const latitud = Number(resultados[0].lat);
        const longitud = Number(resultados[0].lon);

        this.mapa?.flyTo(
          [latitud, longitud],
          15,
          { duration: 1.5 }
        );

        this.mensajeBusqueda =
          `Mapa centrado en ${resultados[0].display_name}. ` +
          'Seleccioná el punto donde se extravió tu mascota.';
      },

      error: () => {
        this.buscando = false;
        this.tipoMensajeBusqueda = 'error';
        this.mensajeBusqueda =
          'No se pudo realizar la búsqueda. Intentá nuevamente.';
      }
    });
  }

  /** Valida los datos antes de solicitar la confirmación. */
  private validarFormulario(): string | null {

    if (!this.mascotaSeleccionada) {
      return 'Debés seleccionar una mascota.';
    }

    if (!this.fechaExtravio) {
      return 'Debés indicar la fecha y hora del extravío.';
    }

    const fecha = new Date(this.fechaExtravio);

    if (
      Number.isNaN(fecha.getTime()) ||
      fecha.getTime() > Date.now()
    ) {
      return 'La fecha del extravío no puede estar en el futuro.';
    }

    if (
      this.latitud === null ||
      this.longitud === null ||
      !Number.isFinite(this.latitud) ||
      !Number.isFinite(this.longitud) ||
      Math.abs(this.latitud) > 90 ||
      Math.abs(this.longitud) > 180
    ) {
      return 'Seleccioná una ubicación válida en el mapa.';
    }

    const descripcion = this.descripcion.trim();

    if (descripcion.length < 10 || descripcion.length > 1000) {
      return 'La descripción debe tener entre 10 y 1000 caracteres.';
    }

    const contacto = this.contacto.trim();

    if (!contacto || contacto.length > 254) {
      return 'Debés indicar un medio de contacto válido.';
    }

    if (
      this.tipoContacto === 'CORREO' &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contacto)
    ) {
      return 'El correo electrónico no es válido.';
    }

    if (
      this.tipoContacto === 'TELEFONO' &&
      !/^\+[1-9][0-9]{7,14}$/.test(contacto)
    ) {
      return 'Indicá el teléfono en formato internacional. Ejemplo: +50688888888.';
    }

    if (!this.autorizaContacto) {
      return 'Debés autorizar la publicación de tus datos de contacto.';
    }

    return null;
  }

  /** Valida y prepara el reporte sin enviarlo al servidor. */
  abrirConfirmacion(): void {

    if (
      this.publicando ||
      this.modalAbierto ||
      this.cargandoMascotas ||
      this.errorMascotas ||
      this.reportePublicado
    ) {
      return;
    }

    this.errorPublicacion = '';

    const error = this.validarFormulario();

    if (error) {
      this.errorPublicacion = error;
      return;
    }

    this.datosPendientes = {
      mascotaId: this.mascotaId,
      fechaExtravio: new Date(this.fechaExtravio).toISOString(),
      latitud: this.latitud!,
      longitud: this.longitud!,
      descripcion: this.descripcion.trim(),
      tipoContacto: this.tipoContacto,
      contacto: this.contacto.trim(),
      autorizaContacto: this.autorizaContacto,
      confirmado: true
    };

    this.modalAbierto = true;
  }

  /** Cancela la confirmación sin publicar el reporte. */
  cancelarConfirmacion(): void {

    if (this.publicando) return;

    this.modalAbierto = false;
    this.datosPendientes = null;
  }

  /** Publica únicamente después de la confirmación del usuario. */
  confirmarPublicacion(): void {

    if (
      this.publicando ||
      !this.modalAbierto ||
      !this.datosPendientes ||
      this.reportePublicado
    ) {
      return;
    }

    this.publicando = true;
    this.errorPublicacion = '';

    this.reporteService.crear(this.datosPendientes).pipe(
      takeUntil(this.destruir$)
    ).subscribe({

      next: respuesta => {

        this.publicando = false;
        this.modalAbierto = false;
        this.datosPendientes = null;

        if (!respuesta.body) {
          this.errorPublicacion =
            'No se recibió la información del reporte.';
          return;
        }

        this.reportePublicado = respuesta.body;
        this.reporteExistente = respuesta.status === 200;

        // No conservar el contacto en el formulario.
        this.contacto = '';
        this.autorizaContacto = false;
        this.descripcion = '';
        this.busqueda = '';
        this.mensajeBusqueda = '';
      },

      error: error => {

        this.publicando = false;
        this.modalAbierto = false;
        this.datosPendientes = null;

        this.errorPublicacion = this.obtenerMensajeError(error);
      }
    });
  }

  /** Traduce los errores del backend a mensajes para el usuario. */
  private obtenerMensajeError(error: unknown): string {

    if (error instanceof HttpErrorResponse) {

      if (error.status === 401) {
        return 'Tu sesión no está activa. Volvé a ingresar.';
      }

      if (error.status === 403) {
        return 'Tu sesión no permite realizar esta operación.';
      }

      if (error.status === 404) {
        return 'No encontramos la mascota o no tenés permiso para reportarla.';
      }

      if (error.status === 0 || error.status >= 500) {
        return 'No pudimos confirmar la publicación. Reintentá con los mismos datos.';
      }

      if (typeof error.error?.mensaje === 'string') {
        return error.error.mensaje;
      }

      if (error.status === 409) {
        return 'Existe un conflicto con el reporte de esta mascota.';
      }
    }

    return 'No pudimos confirmar la publicación. Reintentá con los mismos datos.';
  }

  /** Restablece el formulario para una nueva publicación. */
  nuevoReporte(): void {

    if (this.publicando) return;

    this.reportePublicado = null;
    this.reporteExistente = false;
    this.mascotaId = '';
    this.fechaExtravio = '';
    this.latitud = null;
    this.longitud = null;
    this.descripcion = '';
    this.tipoContacto = 'CORREO';
    this.contacto = '';
    this.autorizaContacto = false;
    this.busqueda = '';
    this.mensajeBusqueda = '';
    this.errorPublicacion = '';
    this.datosPendientes = null;
    this.modalAbierto = false;

    this.marcador?.remove();
    this.marcador = undefined;

    this.mapa?.setView([9.9281, -84.0907], 12);

    setTimeout(() => {
      this.mapa?.invalidateSize();
    }, 200);

    this.cargarMascotas();
  }

  ngOnDestroy(): void {

    this.destruir$.next();
    this.destruir$.complete();

    this.datosPendientes = null;
    this.contacto = '';
    this.reportePublicado = null;
    this.reporteExistente = false;

    this.mapa?.remove();
    this.mapa = undefined;
  }
}
