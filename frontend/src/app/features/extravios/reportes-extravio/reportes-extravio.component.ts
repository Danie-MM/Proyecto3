
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, takeUntil, finalize, TimeoutError } from 'rxjs';

import { ReporteExtravioService } from '../publicar-extravio/reporte-extravio.service';
import { ReporteExtravioResumen } from '../publicar-extravio/reporte-extravio.model';

/** Presenta los reportes de extravío activos disponibles para consulta. */
@Component({
  selector: 'app-reportes-extravio',
  standalone: true,
  imports: [CommonModule, RouterLink],
  providers: [ReporteExtravioService],
  templateUrl: './reportes-extravio.component.html',
  styleUrl: './reportes-extravio.component.css'
})
export class ReportesExtravioComponent implements OnInit, OnDestroy {

  private readonly reporteService = inject(ReporteExtravioService);
  private readonly destruir$ = new Subject<void>();

  reportes: ReporteExtravioResumen[] = [];

  cargando = false;
  mensajeError = '';

  /** Carga los reportes al ingresar al componente. */
  ngOnInit(): void {
    this.cargarReportes();
  }

  /** Consulta nuevamente el listado de reportes activos. */
  cargarReportes(): void {

    if (this.cargando) {
      return;
    }

    this.cargando = true;
    this.mensajeError = '';
    this.reportes = [];

    this.reporteService.listarActivos()
      .pipe(
        takeUntil(this.destruir$),
        finalize(() => {
          this.cargando = false;
        })
      )
      .subscribe({
        next: (reportes) => {
          this.reportes = reportes;
        },
        error: (error: unknown) => {
          this.mensajeError = this.obtenerMensajeError(error);
        }
      });
  }

  /** Obtiene un mensaje comprensible ante un error de consulta. */
  private obtenerMensajeError(error: unknown): string {

    if (error instanceof TimeoutError) {
      return 'La consulta está tardando demasiado. Intentá nuevamente.';
    }

    if (error instanceof HttpErrorResponse) {

      switch (error.status) {
        case 0:
          return 'No fue posible establecer comunicación con el servidor.';

        case 401:
          return 'Tu sesión ha expirado. Iniciá sesión nuevamente.';

        case 403:
          return 'No tenés permisos para consultar los reportes.';

        default:
          return 'No fue posible cargar los reportes de extravío. Intentá nuevamente.';
      }
    }

    return 'Ocurrió un error inesperado al consultar los reportes.';
  }

  /** Libera las suscripciones al abandonar el componente. */
  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }
}
