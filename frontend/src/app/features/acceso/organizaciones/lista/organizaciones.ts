import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { OrganizacionService } from '../services/organizacion.service';
import { Organizacion } from '../models/organizacion';

/**
 * @author Gabriel Barquero González
 * @version 1.0
 * @description Registro y consulta de organizaciones.
 */

@Component({
  selector: 'app-organizaciones',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './organizaciones.html',
  styleUrl: './organizaciones.css'
})
export class Organizaciones implements OnInit {

  private readonly servicio = inject(OrganizacionService);
  readonly auth = inject(AuthService);

  organizaciones = signal<Organizacion[]>([]);
  cargando = signal(false);
  guardando = signal(false);
  mensaje = signal('');
  error = signal('');

  nombre = '';

  ngOnInit(): void {
    this.listar();
  }

  puedeRegistrar(): boolean {
    return this.auth.hasAnyRole(['REFUGIO', 'ADMINISTRADOR']);
  }

  listar(): void {
    this.cargando.set(true);
    this.error.set('');

    this.servicio.listar().subscribe({
      next: datos => {
        this.organizaciones.set(datos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron consultar las organizaciones.');
        this.cargando.set(false);
      }
    });
  }

  registrar(): void {
    if (this.guardando() || !this.puedeRegistrar()) {
      return;
    }

    const nombre = this.nombre.trim();

    this.mensaje.set('');
    this.error.set('');

    if (!nombre || nombre.length > 255) {
      this.error.set('Ingresa un nombre válido.');
      return;
    }

    this.guardando.set(true);

    this.servicio.registrar({ nombre }).subscribe({
      next: () => {
        this.nombre = '';
        this.guardando.set(false);
        this.mensaje.set('Organización registrada correctamente.');
        this.listar();
      },
      error: error => {
        this.error.set(error.error?.mensaje || 'No se pudo registrar la organización.');
        this.guardando.set(false);
      }
    });
  }
}
