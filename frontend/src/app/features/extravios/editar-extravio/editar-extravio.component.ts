
import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import {
  Subject,
  Subscription,
  takeUntil,
  finalize,
  timeout,
  TimeoutError
} from 'rxjs';

import * as L from 'leaflet';

import {
  ConfirmacionModalComponent
} from '../../../shared/components/confirmacion-modal.component';

import {
  ReporteExtravioService
} from '../publicar-extravio/reporte-extravio.service';

import {
  ReporteExtravioDetalle,
  ActualizarReporteExtravio,
  MedioContacto
} from '../publicar-extravio/reporte-extravio.model';

interface ResultadoBusquedaUbicacion {
  lat: string;
  lon: string;
  display_name: string;
}

/**
 * Permite corregir un reporte activo de extravío
 * cuando el usuario cuenta con autorización.
 */
@Component({
  selector: 'app-editar-extravio',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ConfirmacionModalComponent
  ],
  providers: [
    ReporteExtravioService
  ],
  templateUrl: './editar-extravio.component.html',
  styleUrl: './editar-extravio.component.css'
})
export class EditarExtravioComponent
  implements OnInit, OnDestroy {

  private readonly ruta = inject(ActivatedRoute);

  private readonly http = inject(HttpClient);

  private readonly reporteService =
    inject(ReporteExtravioService);

  private readonly destruir$ = new Subject<void>();

  private consultaDetalle?: Subscription;

  private consultaBusqueda?: Subscription;

  private consultaGuardado?: Subscription;

  private mapa?: L.Map;

  private marcador?: L.CircleMarker;

  private temporizadorMapa?: ReturnType<typeof setTimeout>;

  private componenteDestruido = false;

  private datosPendientes: ActualizarReporteExtravio | null = null;

  // Información original del reporte

  reporteId = '';

  reporteOriginal: ReporteExtravioDetalle | null = null;

  reporteActualizado: ReporteExtravioDetalle | null = null;

  // Datos editables

  fechaExtravio = '';

  latitud: number | null = null;

  longitud: number | null = null;

  descripcion = '';

  // Información de contacto

  tipoContacto: MedioContacto = 'CORREO';

  contacto = '';

  autorizaContacto = false;

  // Búsqueda de ubicación

  busqueda = '';

  buscando = false;

  mensajeBusqueda = '';

  tipoMensajeBusqueda:
    'info' | 'advertencia' | 'error' = 'info';

  mensajeMapa = '';

  // Estados del formulario

  cargando = false;

  guardando = false;

  modalAbierto = false;

  conflictoVersion = false;

  guardadoIncierto = false;

  mensajeError = '';

  errorValidacion = '';

  errorGuardado = '';

  /**
   * Inicializa el mapa cuando el formulario autorizado
   * se encuentra disponible.
   */
  @ViewChild('mapaEdicion')
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
   * Consulta el reporte indicado en la ruta.
   */
  ngOnInit(): void {

    this.ruta.paramMap
      .pipe(takeUntil(this.destruir$))
      .subscribe(parametros => {

        this.consultaDetalle?.unsubscribe();

        this.consultaBusqueda?.unsubscribe();

        this.consultaGuardado?.unsubscribe();

        this.destruirMapa();

        this.reporteId = parametros.get('id') ?? '';

        this.reporteOriginal = null;

        this.reporteActualizado = null;

        this.limpiarFormulario();

        this.mensajeError = '';

        if (!this.reporteId) {

          this.mensajeError =
            'No se proporcionó un identificador de reporte.';

          return;
        }

        this.cargarReporte();

      });
  }

  /**
   * Consulta la información vigente y verifica
   * que el reporte pueda ser editado.
   */
  cargarReporte(): void {

    if (
      this.cargando ||
      this.guardando ||
      this.modalAbierto ||
      !this.reporteId ||
      this.componenteDestruido
    ) {
      return;
    }

    this.consultaBusqueda?.unsubscribe();

    this.destruirMapa();

    this.reporteOriginal = null;

    this.reporteActualizado = null;

    this.limpiarFormulario();

    this.cargando = true;

    this.mensajeError = '';

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

          if (reporte.estado !== 'ACTIVO') {

            this.mensajeError =
              'Este reporte ya no se encuentra activo y no puede editarse.';

            return;
          }

          if (reporte.puedeEditar !== true) {

            this.mensajeError =
              'No tenés autorización para editar este reporte.';

            return;
          }

          this.reporteOriginal = { ...reporte };

          this.cargarDatosFormulario(reporte);

        },

        error: (error: unknown) => {

          this.mensajeError =
            this.obtenerMensajeConsulta(error);

        }

      });
  }

  /**
   * Carga los valores actuales sin modificar
   * la copia original del reporte.
   */
  private cargarDatosFormulario(
    reporte: ReporteExtravioDetalle
  ): void {

    this.fechaExtravio =
      this.convertirFechaCostaRica(reporte.fechaExtravio);

    this.latitud = reporte.latitud;

    this.longitud = reporte.longitud;

    this.descripcion = reporte.descripcion;

    this.tipoContacto = reporte.tipoContacto;

    this.contacto = reporte.contacto;

    // Solo se solicitará nuevamente si cambia el contacto.
    this.autorizaContacto = false;

  }

  /**
   * Convierte un instante UTC a fecha local de Costa Rica
   * para utilizarlo en un input datetime-local.
   */
  private convertirFechaCostaRica(
    fecha: string
  ): string {

    const instante = new Date(fecha);

    if (!Number.isFinite(instante.getTime())) {
      return '';
    }

    const fechaCostaRica = new Date(
      instante.getTime() - 6 * 60 * 60 * 1000
    );

    return fechaCostaRica.toISOString().slice(0, 16);
  }

  /**
   * Convierte la fecha ingresada en hora de Costa Rica
   * a un instante UTC para el backend.
   */
  private convertirFechaUtc(
    fecha: string
  ): string | null {

    if (
      !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(fecha)
    ) {
      return null;
    }

    const instante = new Date(`${fecha}:00-06:00`);

    if (!Number.isFinite(instante.getTime())) {
      return null;
    }

    const resultado = instante.toISOString();

    if (this.convertirFechaCostaRica(resultado) !== fecha) {
      return null;
    }

    return resultado;
  }

  /**
   * Fecha máxima permitida para corregir el extravío.
   */
  get fechaMaxima(): string {

    if (!this.reporteOriginal) {
      return '';
    }

    return this.convertirFechaCostaRica(
      this.reporteOriginal.creadoEn
    );
  }

  /**
   * Determina si el formulario cuenta con autorización.
   */
  get puedeEditar(): boolean {

    return (
      this.reporteOriginal?.estado === 'ACTIVO' &&
      this.reporteOriginal.puedeEditar === true
    );
  }

  /**
   * Determina si deben bloquearse las acciones de edición.
   */
  get formularioBloqueado(): boolean {

    return (
      !this.puedeEditar ||
      this.cargando ||
      this.guardando ||
      this.modalAbierto ||
      this.conflictoVersion ||
      this.guardadoIncierto ||
      this.reporteActualizado !== null
    );
  }

  /**
   * Detecta cambios en el medio o valor del contacto.
   */
  get contactoModificado(): boolean {

    if (!this.reporteOriginal) {
      return false;
    }

    return (
      this.tipoContacto !== this.reporteOriginal.tipoContacto ||
      this.contacto.trim() !== this.reporteOriginal.contacto.trim()
    );
  }

  /**
   * Detecta si existe al menos un campo modificado.
   */
  get hayCambios(): boolean {

    if (!this.reporteOriginal) {
      return false;
    }

    return (
      this.fechaExtravio !==
        this.convertirFechaCostaRica(
          this.reporteOriginal.fechaExtravio
        ) ||
      this.latitud !== this.reporteOriginal.latitud ||
      this.longitud !== this.reporteOriginal.longitud ||
      this.descripcion.trim() !==
        this.reporteOriginal.descripcion.trim() ||
      this.contactoModificado
    );
  }

  /**
   * Limpia el consentimiento cuando se modifica
   * nuevamente la información de contacto.
   */
  alCambiarContacto(): void {

    this.autorizaContacto = false;

    this.errorValidacion = '';

    this.errorGuardado = '';

  }

  /**
   * Inicializa el mapa con las coordenadas vigentes
   * y permite seleccionar una nueva ubicación.
   */
  private inicializarMapa(
    contenedor: HTMLDivElement
  ): void {

    if (
      !this.puedeEditar ||
      this.componenteDestruido
    ) {
      return;
    }

    if (
      !this.coordenadasValidas(
        this.latitud,
        this.longitud
      )
    ) {

      this.mensajeMapa =
        'Las coordenadas originales no son válidas.';

      return;
    }

    try {

      const posicion = L.latLng(
        this.latitud!,
        this.longitud!
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
            'Las coordenadas seleccionadas se conservan.';

        }

      });

      capaMapa.addTo(this.mapa);

      this.actualizarMarcador();

      this.mapa.on('click', (evento: L.LeafletMouseEvent) => {

        if (
          this.formularioBloqueado ||
          this.buscando
        ) {
          return;
        }

        this.seleccionarUbicacion(
          evento.latlng.lat,
          evento.latlng.lng
        );

      });

      this.temporizadorMapa = setTimeout(() => {

        this.temporizadorMapa = undefined;

        this.mapa?.invalidateSize();

      }, 200);

    } catch {

      this.destruirMapa();

      this.mensajeMapa =
        'No fue posible inicializar el mapa. ' +
        'La ubicación original se conserva.';

    }
  }

  /**
   * Verifica que las coordenadas sean geográficamente válidas.
   */
  private coordenadasValidas(
    latitud: number | null,
    longitud: number | null
  ): boolean {

    return (
      latitud !== null &&
      longitud !== null &&
      Number.isFinite(latitud) &&
      Number.isFinite(longitud) &&
      latitud >= -90 &&
      latitud <= 90 &&
      longitud >= -180 &&
      longitud <= 180
    );
  }

  /**
   * Actualiza las coordenadas seleccionadas
   * con una precisión de seis decimales.
   */
  private seleccionarUbicacion(
    latitud: number,
    longitud: number
  ): void {

    if (
      this.formularioBloqueado ||
      !this.coordenadasValidas(latitud, longitud)
    ) {
      return;
    }

    this.latitud = Number(latitud.toFixed(6));

    this.longitud = Number(longitud.toFixed(6));

    this.actualizarMarcador();

    this.errorValidacion = '';

    this.errorGuardado = '';

    this.mensajeBusqueda = '';

  }

  /**
   * Actualiza el marcador sin crear instancias duplicadas.
   */
  private actualizarMarcador(): void {

    if (
      !this.mapa ||
      this.latitud === null ||
      this.longitud === null
    ) {
      return;
    }

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
      })
        .addTo(this.mapa)
        .bindTooltip('Ubicación seleccionada');

    }
  }

  /**
   * Restablece la ubicación original del reporte.
   */
  restaurarUbicacion(): void {

    if (
      this.formularioBloqueado ||
      this.buscando ||
      !this.reporteOriginal
    ) {
      return;
    }

    this.seleccionarUbicacion(
      this.reporteOriginal.latitud,
      this.reporteOriginal.longitud
    );

    this.mapa?.setView(
      [
        this.reporteOriginal.latitud,
        this.reporteOriginal.longitud
      ],
      15
    );
  }

  /**
   * Busca una dirección en Costa Rica mediante Nominatim.
   * La búsqueda no modifica automáticamente el marcador.
   */
  buscarUbicacion(): void {

    if (
      this.formularioBloqueado ||
      this.buscando
    ) {
      return;
    }

    const consulta = this.busqueda.trim();

    this.mensajeBusqueda = '';

    if (consulta.length < 3) {

      this.tipoMensajeBusqueda = 'advertencia';

      this.mensajeBusqueda =
        'Ingresá al menos tres caracteres para buscar.';

      return;
    }

    if (!this.mapa) {

      this.tipoMensajeBusqueda = 'advertencia';

      this.mensajeBusqueda =
        'El mapa no se encuentra disponible.';

      return;
    }

    this.buscando = true;

    this.consultaBusqueda = this.http
      .get<ResultadoBusquedaUbicacion[]>(
        'https://nominatim.openstreetmap.org/search',
        {
          params: {
            q: consulta,
            format: 'json',
            limit: '1',
            countrycodes: 'cr'
          }
        }
      )
      .pipe(
        timeout(15000),
        takeUntil(this.destruir$),
        finalize(() => {
          this.buscando = false;
        })
      )
      .subscribe({

        next: (resultados) => {

          if (resultados.length === 0) {

            this.tipoMensajeBusqueda = 'advertencia';

            this.mensajeBusqueda =
              'No encontramos una ubicación que coincida con la búsqueda.';

            return;
          }

          const resultado = resultados[0];

          const latitud = Number(resultado.lat);

          const longitud = Number(resultado.lon);

          if (
            !this.coordenadasValidas(latitud, longitud)
          ) {

            this.tipoMensajeBusqueda = 'error';

            this.mensajeBusqueda =
              'La búsqueda devolvió coordenadas no válidas.';

            return;
          }

          this.mapa?.flyTo(
            [latitud, longitud],
            15,
            {
              duration: 1.5
            }
          );

          this.tipoMensajeBusqueda = 'info';

          this.mensajeBusqueda =
            'Ubicación encontrada. Seleccioná el punto exacto en el mapa.';

        },

        error: () => {

          this.tipoMensajeBusqueda = 'error';

          this.mensajeBusqueda =
            'No fue posible realizar la búsqueda. ' +
            'Podés seleccionar la ubicación manualmente.';

        }

      });
  }

  /**
   * Valida los datos antes de solicitar confirmación.
   */
  private validarFormulario(): boolean {

    this.errorValidacion = '';

    if (!this.puedeEditar || !this.reporteOriginal) {

      this.errorValidacion =
        'No tenés autorización para editar este reporte.';

      return false;
    }

    if (
      !Number.isInteger(this.reporteOriginal.version) ||
      this.reporteOriginal.version < 0
    ) {

      this.errorValidacion =
        'La versión del reporte no es válida. Consultá nuevamente.';

      return false;
    }

    if (!this.hayCambios) {

      this.errorValidacion =
        'No existen cambios para guardar.';

      return false;
    }

    const fechaOriginalLocal =
      this.convertirFechaCostaRica(
        this.reporteOriginal.fechaExtravio
      );

    const fechaUtc =
      this.fechaExtravio === fechaOriginalLocal
        ? this.reporteOriginal.fechaExtravio
        : this.convertirFechaUtc(this.fechaExtravio);

    if (!fechaUtc) {

      this.errorValidacion =
        'Ingresá una fecha de extravío válida.';

      return false;
    }

    const fechaSeleccionada = Date.parse(fechaUtc);

    const fechaPublicacion = Date.parse(
      this.reporteOriginal.creadoEn
    );

    if (
      !Number.isFinite(fechaSeleccionada) ||
      !Number.isFinite(fechaPublicacion) ||
      fechaSeleccionada > fechaPublicacion
    ) {

      this.errorValidacion =
        'La fecha de extravío no puede ser posterior a la publicación.';

      return false;
    }

    if (
      !this.coordenadasValidas(
        this.latitud,
        this.longitud
      )
    ) {

      this.errorValidacion =
        'Seleccioná una ubicación válida para el extravío.';

      return false;
    }

    const descripcion = this.descripcion.trim();

    if (
      descripcion.length < 10 ||
      descripcion.length > 1000
    ) {

      this.errorValidacion =
        'La descripción debe contener entre 10 y 1000 caracteres.';

      return false;
    }

    if (this.contactoModificado) {

      const contacto = this.contacto.trim();

      if (
        this.tipoContacto !== 'CORREO' &&
        this.tipoContacto !== 'TELEFONO'
      ) {

        this.errorValidacion =
          'Seleccioná un medio de contacto válido.';

        return false;
      }

      if (
        contacto.length === 0 ||
        contacto.length > 254
      ) {

        this.errorValidacion =
          'Ingresá un contacto válido de hasta 254 caracteres.';

        return false;
      }

      if (
        this.tipoContacto === 'CORREO' &&
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contacto)
      ) {

        this.errorValidacion =
          'Ingresá una dirección de correo electrónico válida.';

        return false;
      }

      if (
        this.tipoContacto === 'TELEFONO' &&
        !/^\+[1-9][0-9]{7,14}$/.test(contacto)
      ) {

        this.errorValidacion =
          'Ingresá un teléfono en formato internacional, por ejemplo +50688888888.';

        return false;
      }

      if (!this.autorizaContacto) {

        this.errorValidacion =
          'Debés autorizar la publicación del nuevo contacto.';

        return false;
      }

    }

    return true;
  }

  /**
   * Construye una solicitud PATCH con la versión
   * y únicamente los campos modificados.
   */
  private construirActualizacion():
    ActualizarReporteExtravio | null {

    if (!this.reporteOriginal) {
      return null;
    }

    const original = this.reporteOriginal;

    const datos: ActualizarReporteExtravio = {
      version: original.version
    };

    if (
      this.fechaExtravio !==
        this.convertirFechaCostaRica(original.fechaExtravio)
    ) {

      const fechaUtc =
        this.convertirFechaUtc(this.fechaExtravio);

      if (!fechaUtc) {
        return null;
      }

      datos.fechaExtravio = fechaUtc;

    }

    if (
      this.latitud !== original.latitud ||
      this.longitud !== original.longitud
    ) {

      if (
        this.latitud === null ||
        this.longitud === null
      ) {
        return null;
      }

      // Las coordenadas siempre se envían juntas.
      datos.latitud = this.latitud;

      datos.longitud = this.longitud;

    }

    if (
      this.descripcion.trim() !==
      original.descripcion.trim()
    ) {

      datos.descripcion = this.descripcion.trim();

    }

    if (this.contactoModificado) {

      datos.tipoContacto = this.tipoContacto;

      datos.contacto = this.contacto.trim();

      datos.autorizaContacto = true;

    }

    return datos;
  }

  /**
   * Valida y prepara los cambios para confirmación.
   */
  solicitarConfirmacion(): void {

    if (
      this.formularioBloqueado ||
      this.buscando
    ) {
      return;
    }

    this.errorGuardado = '';

    if (!this.validarFormulario()) {
      return;
    }

    const datos = this.construirActualizacion();

    if (!datos || Object.keys(datos).length <= 1) {

      this.errorValidacion =
        'No existen cambios válidos para guardar.';

      return;
    }

    this.datosPendientes = datos;

    this.modalAbierto = true;

  }

  /**
   * Cancela la confirmación sin perder los cambios.
   */
  cancelarConfirmacion(): void {

    if (this.guardando) {
      return;
    }

    this.modalAbierto = false;

    this.datosPendientes = null;

  }

  /**
   * Envía los cambios confirmados al backend.
   */
  confirmarActualizacion(): void {

    if (
      !this.modalAbierto ||
      !this.datosPendientes ||
      !this.puedeEditar ||
      this.guardando ||
      this.conflictoVersion ||
      this.guardadoIncierto ||
      this.componenteDestruido
    ) {
      return;
    }

    const datos = { ...this.datosPendientes };

    this.guardando = true;

    this.errorGuardado = '';

    this.consultaGuardado = this.reporteService
      .actualizar(this.reporteId, datos)
      .pipe(
        takeUntil(this.destruir$),
        finalize(() => {

          this.guardando = false;

          this.modalAbierto = false;

          this.datosPendientes = null;

        })
      )
      .subscribe({

        next: (reporte) => {

          this.reporteActualizado = reporte;

          this.reporteOriginal = { ...reporte };

          this.cargarDatosFormulario(reporte);

          this.destruirMapa();

        },

        error: (error: unknown) => {

          this.procesarErrorGuardado(error);

        }

      });
  }

  /**
   * Procesa errores de actualización sin sobrescribir
   * cambios concurrentes ni repetir escrituras inciertas.
   */
  private procesarErrorGuardado(
    error: unknown
  ): void {

    if (error instanceof TimeoutError) {

      this.guardadoIncierto = true;

      this.errorGuardado =
        'No recibimos confirmación del servidor. ' +
        'Consultá el reporte vigente antes de volver a editar.';

      return;
    }

    if (error instanceof HttpErrorResponse) {

      if (error.status === 409) {

        this.conflictoVersion = true;

        this.errorGuardado =
          'El reporte cambió o existe un conflicto de actualización. ' +
          'Consultá la información vigente antes de continuar.';

        return;
      }

      if (
        error.status === 0 ||
        error.status >= 500
      ) {

        this.guardadoIncierto = true;

        this.errorGuardado =
          'No fue posible confirmar el resultado del guardado. ' +
          'Consultá el reporte vigente antes de intentar nuevamente.';

        return;
      }

      if (
        error.status === 401 ||
        error.status === 403 ||
        error.status === 404
      ) {

        this.errorGuardado =
          this.obtenerMensajeConsulta(error);

        // Impide continuar editando con permisos desactualizados.
        this.reporteOriginal = null;

        this.contacto = '';

        this.autorizaContacto = false;

        this.destruirMapa();

        this.mensajeError = this.errorGuardado;

        return;
      }

      if (error.status === 400) {

        this.errorGuardado =
          'El servidor rechazó los datos ingresados. ' +
          'Revisá la información e intentá nuevamente.';

        return;
      }

    }

    this.guardadoIncierto = true;

    this.errorGuardado =
      'Ocurrió un error inesperado durante la actualización. ' +
      'Consultá el reporte vigente antes de continuar.';

  }

  /**
   * Traduce los errores de consulta en mensajes legibles.
   */
  private obtenerMensajeConsulta(
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
          return 'La solicitud contiene datos no válidos.';

        case 401:
          return 'Tu sesión ha expirado. Iniciá sesión nuevamente.';

        case 403:
          return 'No tenés autorización para realizar esta operación.';

        case 404:
          return 'No encontramos el reporte solicitado o ya no está disponible.';

        default:
          return 'No fue posible consultar el reporte. Intentá nuevamente.';

      }
    }

    return 'Ocurrió un error inesperado al consultar el reporte.';
  }

  /**
   * Limpia los datos y estados del formulario.
   */
  private limpiarFormulario(): void {

    this.fechaExtravio = '';

    this.latitud = null;

    this.longitud = null;

    this.descripcion = '';

    this.tipoContacto = 'CORREO';

    this.contacto = '';

    this.autorizaContacto = false;

    this.busqueda = '';

    this.mensajeBusqueda = '';

    this.tipoMensajeBusqueda = 'info';

    this.mensajeMapa = '';

    this.errorValidacion = '';

    this.errorGuardado = '';

    this.conflictoVersion = false;

    this.guardadoIncierto = false;

    this.modalAbierto = false;

    this.datosPendientes = null;

  }

  /**
   * Libera los recursos utilizados por Leaflet.
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

    this.marcador = undefined;

  }

  /**
   * Libera los recursos al abandonar el componente.
   */
  ngOnDestroy(): void {

    this.componenteDestruido = true;

    this.destruir$.next();

    this.destruir$.complete();

    this.consultaDetalle?.unsubscribe();

    this.consultaBusqueda?.unsubscribe();

    this.consultaGuardado?.unsubscribe();

    this.destruirMapa();

    this.reporteOriginal = null;

    this.reporteActualizado = null;

    this.limpiarFormulario();

  }
}
