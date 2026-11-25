import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrganizacionService } from '../services/organizacion.service';
import { Organizacion, OrganizacionMiembro } from '../models/organizacion';

/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description Administración de los miembros de una organización.
 */

@Component({
  selector: 'app-miembros',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './miembros.html',
  styleUrl: './miembros.css'
})
export class Miembros implements OnInit {

  private readonly servicio = inject(OrganizacionService);
  private readonly ruta = inject(ActivatedRoute);

  organizacion = signal<Organizacion | null>(null);
  miembros = signal<OrganizacionMiembro[]>([]);
  puedeAdministrar = signal(false);
  cargando = signal(true);
  guardando = signal(false);
  mensaje = signal('');
  error = signal('');

  id = '';
  usuarioId = '';
  administrador = false;

  ngOnInit(): void {
    this.id = this.ruta.snapshot.paramMap.get('id') || '';
    this.consultar();
  }

  consultar(): void {
    this.servicio.consultar(this.id).subscribe({
      next: datos => {
        this.organizacion.set(datos);
        this.listarMiembros();
      },
      error: () => {
        this.error.set('No se pudo consultar la organización.');
        this.cargando.set(false);
      }
    });
  }

  listarMiembros(): void {
    this.cargando.set(true);

    this.servicio.listarMiembros(this.id).subscribe({
      next: datos => {
        this.miembros.set(datos);
        this.puedeAdministrar.set(true);
        this.cargando.set(false);
      },
      error: error => {
        this.miembros.set([]);
        this.puedeAdministrar.set(false);
        this.cargando.set(false);

        if (error.status !== 403) {
          this.error.set('No se pudieron consultar los miembros.');
        }
      }
    });
  }

  agregarMiembro(): void {
    if (this.guardando() || !this.puedeAdministrar()) {
      return;
    }

    const usuarioId = this.usuarioId.trim();
    const formato = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

    if (!formato.test(usuarioId)) {
      this.error.set('Ingresa un identificador de usuario válido.');
      return;
    }

    this.iniciarOperacion();

    this.servicio.agregarMiembro(this.id, {
      usuarioId,
      administrador: this.administrador
    }).subscribe({
      next: () => {
        this.usuarioId = '';
        this.administrador = false;
        this.finalizarOperacion('Miembro agregado correctamente.');
      },
      error: error => this.mostrarError(error)
    });
  }

  cambiarPermiso(miembro: OrganizacionMiembro): void {
    if (this.guardando() || !this.puedeAdministrar()) {
      return;
    }

    this.iniciarOperacion();

    this.servicio.cambiarPermiso(this.id, miembro.usuarioId, {
      administrador: !miembro.puedeAdministrar
    }).subscribe({
      next: () => this.finalizarOperacion('Permiso actualizado.'),
      error: error => this.mostrarError(error)
    });
  }

  quitarMiembro(miembro: OrganizacionMiembro): void {
    if (this.guardando() || !this.puedeAdministrar()) {
      return;
    }

    if (!confirm(`¿Deseas quitar a ${miembro.nombreCompleto}?`)) {
      return;
    }

    this.iniciarOperacion();

    this.servicio.quitarMiembro(this.id, miembro.usuarioId).subscribe({
      next: () => this.finalizarOperacion('Miembro eliminado de la organización.'),
      error: error => this.mostrarError(error)
    });
  }

  private iniciarOperacion(): void {
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
  }

  private finalizarOperacion(mensaje: string): void {
    this.guardando.set(false);
    this.mensaje.set(mensaje);
    this.listarMiembros();
  }

  private mostrarError(error: any): void {
    this.guardando.set(false);

    if (error.status === 409) {
      this.error.set('La organización debe tener al menos un administrador.');
    } else if (error.status === 403) {
      this.error.set('No tienes permiso para realizar esta operación.');
    } else if (error.status === 404) {
      this.error.set('No se encontró el usuario o miembro.');
    } else {
      this.error.set('No se pudo completar la operación.');
    }
  }
}
