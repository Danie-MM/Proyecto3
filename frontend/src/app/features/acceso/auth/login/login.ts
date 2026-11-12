
import { AfterViewInit, Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { GoogleAuthService } from '../services/google-auth.service';

/**
 * @author Daniela Morales
 * @version 1.0
 * @description Clase encargada  de lógica de inicio de sesión utilizando Google
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login implements AfterViewInit {

  private readonly authService = inject(AuthService);
  private readonly googleAuthService = inject(GoogleAuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('botonGoogle')
  private botonGoogle!: ElementRef<HTMLDivElement>;

  protected cargando = signal(false);
  protected mensaje = signal('');
  protected tipoMensaje = signal<'error' | 'exito' | 'advertencia'>('error');

  /**
   * Aplica acción de Google y aplica los credenciales que elusuario seleciono.
   */
  ngAfterViewInit(): void {
    this.googleAuthService.credencial$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(idToken => {
        this.iniciarSesion(idToken);
      });

    this.googleAuthService.inicializarBoton(
      this.botonGoogle.nativeElement,
      'continue_with'
    );
  }

  /**
   * Se enfoca en enviar el token
   */
  private iniciarSesion(idToken: string): void {
    if (this.cargando()) {
      return;
    }

    this.cargando.set(true);
    this.mensaje.set('');

    this.authService.iniciarSesion({ idToken })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: respuesta => {
          this.cargando.set(false);

          this.authService.guardarSesion(respuesta.token);
          this.authService.guardarUsuario(respuesta.usuario);

          this.mostrarMensaje(
            `Bienvenido, ${respuesta.usuario.nombreCompleto}`,
            'exito'
          );

          this.router.navigateByUrl('/comprobar-entorno');
        },

        error: (error: HttpErrorResponse) => {
          this.cargando.set(false);
          this.gestionarError(error, idToken);
        }
      });
  }

  /**
   * Se encarga de la gestión de errores de sesión
   */
  private gestionarError(error: HttpErrorResponse, idToken: string): void {
    const mensajeBackend = error.error?.error;

    if (error.status === 404) {
      this.mostrarMensaje(
        mensajeBackend ?? 'No existe una cuenta registrada.',
        'advertencia'
      );

      this.router.navigateByUrl('/registro');
      return;
    }

    if (error.status === 403) {
      this.authService.guardarReactivacionPendiente({
        idToken,
        nombreCompleto: error.error?.nombreCompleto ?? '',
        email: error.error?.email ?? ''
      });

      this.mostrarMensaje(
        mensajeBackend ?? 'Tu cuenta está desactivada.',
        'advertencia'
      );

      return;
    }

    if (error.status === 0 || error.status === 503) {
      this.mostrarMensaje(
        mensajeBackend ?? 'No se pudo contactar al servidor.',
        'error'
      );

      return;
    }

    this.mostrarMensaje(
      mensajeBackend ?? 'No se pudo iniciar sesión.',
      'error'
    );
  }


  private mostrarMensaje(
    texto: string,
    tipo: 'error' | 'exito' | 'advertencia'
  ): void {
    this.mensaje.set(texto);
    this.tipoMensaje.set(tipo);
  }
}
