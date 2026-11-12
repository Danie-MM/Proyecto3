
import {
  AfterViewInit,
  Component,
  OnDestroy,
  inject
} from '@angular/core';

import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';

import {
  PreferenciasUbicacionService,
  PreferenciaUbicacion,
  GuardarPreferenciaUbicacion
} from './preferencias-ubicacion.service';

@Component({
  selector: 'app-preferencias-ubicacion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './preferencias-ubicacion.component.html',
  styleUrl: './preferencias-ubicacion.component.css'
})
export class PreferenciasUbicacionComponent
  implements AfterViewInit, OnDestroy {

  private readonly http = inject(HttpClient);
  private readonly preferenciasService =
    inject(PreferenciasUbicacionService);

  private mapa?: L.Map;
  private marcador?: L.CircleMarker;

  latitud: number | null = null;
  longitud: number | null = null;

  referencia = '';
  aceptaAvisos = false;

  busqueda = '';
  buscando = false;
  mensajeBusqueda = '';

  cargando = true;
  guardando = false;
  existePreferencia = false;

  mensajeGuardado = '';
  errorGuardado = '';

  ngAfterViewInit(): void {

    this.mapa = L.map('mapa-ubicacion')
      .setView([9.9281, -84.0907], 12);

    L.tileLayer(
      'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
      }
    ).addTo(this.mapa);

    this.mapa.on('click', (evento: L.LeafletMouseEvent) => {
      this.seleccionarUbicacion(
        evento.latlng.lat,
        evento.latlng.lng
      );
    });

    setTimeout(() => {
      this.mapa?.invalidateSize();
    }, 200);

    this.cargarPreferencias();
  }

  private seleccionarUbicacion(
    latitud: number,
    longitud: number
  ): void {

    this.latitud = Number(latitud.toFixed(6));
    this.longitud = Number(longitud.toFixed(6));

    const posicion = L.latLng(
      this.latitud,
      this.longitud
    );

    if (this.marcador) {
      this.marcador.setLatLng(posicion);
    } else {
      this.marcador = L.circleMarker(posicion, {
        radius: 9,
        color: '#ffffff',
        weight: 3,
        fillColor: '#7c3aed',
        fillOpacity: 1
      }).addTo(this.mapa!);
    }

    this.mensajeGuardado = '';
    this.errorGuardado = '';
  }

  cargarPreferencias(): void {

    this.cargando = true;

    this.preferenciasService.consultar().subscribe({
      next: (preferencia: PreferenciaUbicacion) => {

        this.existePreferencia = true;

        this.referencia = preferencia.referencia ?? '';
        this.aceptaAvisos = preferencia.aceptaAvisos;

        if (
          preferencia.latitud !== null &&
          preferencia.longitud !== null
        ) {
          this.seleccionarUbicacion(
            preferencia.latitud,
            preferencia.longitud
          );

          this.mapa?.setView(
            [preferencia.latitud, preferencia.longitud],
            15
          );
        }

        this.cargando = false;
      },

      error: (error) => {

        this.cargando = false;

        if (error.status === 404) {
          this.existePreferencia = false;
          return;
        }

        this.errorGuardado =
          'No se pudieron cargar tus preferencias. Intentá nuevamente.';
      }
    });
  }

  buscarUbicacion(): void {

    const lugar = this.busqueda.trim();

    if (!lugar || this.buscando) {
      return;
    }

    this.buscando = true;
    this.mensajeBusqueda = '';

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
    ).subscribe({

      next: (resultados) => {

        this.buscando = false;

        if (resultados.length === 0) {
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
          'Seleccioná el punto exacto.';
      },

      error: () => {
        this.buscando = false;
        this.mensajeBusqueda =
          'No se pudo realizar la búsqueda. Intentá nuevamente.';
      }
    });
  }

  guardarUbicacion(): void {

    if (
      this.latitud === null ||
      this.longitud === null ||
      this.guardando ||
      this.cargando
    ) {
      return;
    }

    this.guardando = true;
    this.mensajeGuardado = '';
    this.errorGuardado = '';

    const datos: GuardarPreferenciaUbicacion = {
      latitud: this.latitud,
      longitud: this.longitud,
      referencia: this.referencia.trim(),
      aceptaAvisos: this.aceptaAvisos
    };

    const solicitud = this.existePreferencia
      ? this.preferenciasService.actualizar(datos)
      : this.preferenciasService.crear(datos);

    solicitud.subscribe({

      next: () => {
        this.guardando = false;
        this.existePreferencia = true;
        this.mensajeGuardado =
          '¡Tus preferencias se guardaron correctamente!';
      },

      error: (error) => {
        this.guardando = false;

        if (error.status === 409) {
          this.errorGuardado =
            'Las preferencias ya existen. Recargá la página para actualizarlas.';
          return;
        }

        this.errorGuardado =
          'No se pudieron guardar tus preferencias. Intentá nuevamente.';
      }
    });
  }


eliminarUbicacion(): void {

  if (!this.existePreferencia || this.guardando || this.cargando) {
    return;
  }

  const confirmar = window.confirm(
    '¿Estás seguro de que querés eliminar tu ubicación y tus preferencias de avisos?'
  );

  if (!confirmar) {
    return;
  }

  this.guardando = true;
  this.mensajeGuardado = '';
  this.errorGuardado = '';

  this.preferenciasService.eliminar().subscribe({

    next: () => {

      this.guardando = false;
      this.existePreferencia = false;

      this.latitud = null;
      this.longitud = null;
      this.referencia = '';
      this.aceptaAvisos = false;

      this.marcador?.remove();
      this.marcador = undefined;

      this.mensajeGuardado =
        'Tu ubicación y tus preferencias se eliminaron correctamente.';
    },

    error: () => {
      this.guardando = false;
      this.errorGuardado =
        'No se pudieron eliminar tus preferencias. Intentá nuevamente.';
    }
  });
}


  ngOnDestroy(): void {
    this.mapa?.remove();
  }
}
