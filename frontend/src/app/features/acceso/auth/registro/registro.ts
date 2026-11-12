
import { AfterViewInit, Component, DestroyRef, ElementRef, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';

import { AuthService } from '../services/auth.service';
import { GoogleAuthService } from '../services/google-auth.service';
import { RegistroRequest } from '../models/registro-request';

const PATRON_NOMBRE = /^[\p{L}]+(?: [\p{L}]+)*$/u;

/**
 * @author Daniela Morales
 * @version 1.0
 * @description Componente encargado del registro de usuarios
 */
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrls: ['./registro.css']
})
export class Registro implements AfterViewInit {

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly googleAuthService = inject(GoogleAuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('botonGoogle')
  private botonGoogle!: ElementRef<HTMLDivElement>;

  cargando = false;
  mensaje = '';

  hoy = this.aFechaIso(new Date());

  roles = [
    {
      valor: 'ADOPTANTE',
      titulo: 'Adoptante',
      descripcion: 'Encuentra mascotas disponibles para adopción.'
    },
    {
      valor: 'REFUGIO',
      titulo: 'Refugio',
      descripcion: 'Gestiona mascotas y procesos de adopción.'
    }
  ];

  form = this.fb.group({
    nombreCompleto: ['', [Validators.required, this.validarNombre]],
    fechaNacimiento: ['', [Validators.required, this.validarNoFutura]],
    rol: ['', Validators.required]
  });

  /**
   * Inicializa el btn de Google y recibe el token de identidad
   */
  ngAfterViewInit(): void {
    this.googleAuthService.credencial$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(idToken => {
        this.registrar(idToken);
      });

    this.googleAuthService.inicializarBoton(
      this.botonGoogle.nativeElement,
      'signup_with'
    );
  }

  /**
   * Revisa nombre contenga solo letras
   * y espacios entre palabras.
   */
  private validarNombre(control: AbstractControl<string>): ValidationErrors | null {
    const nombre = control.value.trim();

    if (nombre && !PATRON_NOMBRE.test(nombre)) {
      return { pattern: true };
    }

    return null;
  }

  /**
   * Elimina los espacios al inicio y al final.
   */
  protected normalizarNombre(): void {
    const nombre = this.form.controls.nombreCompleto;
    nombre.setValue(nombre.value.trim());
  }

  /**
   * Verifica que la fecha de nacimiento sea correcta
   */
  private validarNoFutura(control: AbstractControl<string>): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const fecha = new Date(`${control.value}T00:00:00`);
    const hoy = new Date();

    hoy.setHours(23, 59, 59, 999);

    if (Number.isNaN(fecha.getTime())) {
      return { fechaInvalida: true };
    }

    if (fecha > hoy) {
      return { fechaFutura: true };
    }

    return null;
  }

  /**
   * Convierte una fecha al formato YYYY-MM-DD utilizado
   */
  private aFechaIso(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  /**
   * Valida los datos del formulario
  */
  private registrar(idToken: string): void {
    if (this.cargando) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mensaje = 'Completa correctamente los datos antes de continuar.';
      return;
    }

    const datos = this.form.getRawValue();

    const request: RegistroRequest = {
      idToken,
      nombreCompleto: datos.nombreCompleto.trim(),
      fechaNacimiento: datos.fechaNacimiento,
      rol: datos.rol
    };

    this.cargando = true;
    this.mensaje = '';

    this.authService.registrar(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: respuesta => {
          this.cargando = false;

          this.authService.guardarSesion(respuesta.token);
          this.authService.guardarUsuario(respuesta.usuario);

          this.mensaje = `Cuenta creada. Bienvenido a PetMatchCR, ${respuesta.usuario.nombreCompleto}.`;

          this.router.navigateByUrl('/comprobar-entorno');
        },

        error: (error: HttpErrorResponse) => {
          this.cargando = false;
          this.mensaje = this.mensajeDeError(error);
        }
      });
  }

  /**
   * Obtiene un mensaje cuando recibe un error del servidor.
   */
  private mensajeDeError(error: HttpErrorResponse): string {
    const mensajeBackend = error.error?.error;

    if (mensajeBackend) {
      return mensajeBackend;
    }

    if (error.status === 0 || error.status === 503) {
      return 'No se pudo conectar al servidor.';
    }

    return 'Error al registrar la cuenta.';
  }
}
