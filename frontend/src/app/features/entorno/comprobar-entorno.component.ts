
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, Observable } from 'rxjs';

import { AuthService } from '../acceso/auth/services/auth.service';
import {
  EntornoApiService,
  formatoId,
  RegistroEntorno
} from './entorno-api.service';

@Component({
  selector: 'app-comprobar-entorno',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './comprobar-entorno.component.html',
  styleUrl: './comprobar-entorno.component.css'
})
export class ComprobarEntornoComponent {
  private readonly api = inject(EntornoApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly usuario = this.authService.usuarioEnSesion;
  readonly roles = this.authService.roles;

  texto = '';
  id = '';
  ocupado = false;
  error = '';
  operacion = '';
  registro: RegistroEntorno | null = null;

  cerrarSesion(): void {
    this.authService.cerrarSesion();
    this.router.navigateByUrl('/login', { replaceUrl: true });
  }

  guardar(): void {
    if (this.ocupado) {
      return;
    }

    this.registro = null;

    const texto = this.texto.trim();

    if (!texto || [...texto].length > 80) {
      this.error = 'Escribí un mensaje de entre 1 y 80 caracteres.';
      return;
    }

    this.mostrar(this.api.crear(texto), 'Mensaje guardado');
  }

  consultar(): void {
    if (this.ocupado) {
      return;
    }

    this.registro = null;

    const id = this.id.trim();

    if (!formatoId.test(id)) {
      this.error = 'Revisá el ID. Debe ser el identificador completo que recibiste al guardar.';
      return;
    }

    this.mostrar(this.api.consultar(id), 'Mensaje encontrado');
  }

  private mostrar(
    peticion: Observable<RegistroEntorno>,
    operacion: string
  ): void {
    this.error = '';
    this.ocupado = true;

    peticion.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.ocupado = false;
      })
    ).subscribe({
      next: registro => {
        this.registro = registro;
        this.id = registro.id;
        this.operacion = operacion;
      },
      error: (fallo: unknown) => {
        if (fallo instanceof HttpErrorResponse) {
          if (fallo.status === 401) {
            this.cerrarSesion();
            return;
          }

          if (fallo.status === 403) {
            this.error = 'No tenés permisos para realizar esta operación.';
            return;
          }

          if (fallo.status === 404) {
            this.error = 'No encontramos un mensaje con ese ID.';
            return;
          }

          if (fallo.status === 400) {
            this.error = 'El servidor rechazó los datos. Revisá el mensaje o el ID.';
            return;
          }
        }

        this.error = 'No pudimos confirmar la operación. Revisá la conexión con el servidor.';
      }
    });
  }
}
