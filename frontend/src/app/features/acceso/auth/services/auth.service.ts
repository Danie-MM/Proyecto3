
import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { RegistroRequest } from '../models/registro-request';
import { RegistroResponse } from '../models/registro-response';
import { UsuarioDto } from '../models/usuario-dto';
import { LoginRequest } from '../models/login-request';
import { ReactivacionPendiente } from '../models/reactivacion-pendiente';

/**
 * @author Daniela Morales
 * @version 1.0
 * @description Servicio encargado de la autenticación de usuarios
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';

  private static readonly TOKEN_KEY = 'petmatch_token';
  private static readonly USUARIO_KEY = 'petmatch_usuario';

  readonly roles = signal<string[]>([]);
  readonly usuarioEnSesion = signal<UsuarioDto | null>(null);

  private reactivacionPendiente: ReactivacionPendiente | null = null;

  /** Recupera los datos del usuario almacenados en el navegador*/
  constructor() {
    const usuario = this.obtenerUsuario();

    this.usuarioEnSesion.set(usuario);
    this.roles.set(usuario?.rol ? [usuario.rol] : []);
  }

  /** Envía los datos de un nuevo usuario al servidor. */
  registrar(request: RegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${this.baseUrl}/registro`, request);
  }

  /** Solicita el inicio de sesión de un usuario.*/
  iniciarSesion(request: LoginRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${this.baseUrl}/login`, request);
  }

  /**
   * Solicita la reactivación de una cuenta desactivada.*/
  reactivar(request: LoginRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${this.baseUrl}/reactivar`, request);
  }

  /** Guarda el token de autenticación en el almacenamiento local. */
  guardarSesion(token: string): void {
    localStorage.setItem(AuthService.TOKEN_KEY, token);
  }

  /**Obtiene el token almacenado en el navegador.*/
  obtenerToken(): string | null {
    return localStorage.getItem(AuthService.TOKEN_KEY);
  }

  /**
   * Guarda los datos del usuario y actualiza la información de sesión y su rol.*/
  guardarUsuario(usuario: UsuarioDto): void {
    localStorage.setItem(AuthService.USUARIO_KEY, JSON.stringify(usuario));
    this.usuarioEnSesion.set(usuario);
    this.roles.set([usuario.rol]);
  }

  /**Recupera los datos del usuario almacenados en el navegador. */
  obtenerUsuario(): UsuarioDto | null {
    const usuario = localStorage.getItem(AuthService.USUARIO_KEY);

    if (!usuario) {
      return null;
    }

    try {
      return JSON.parse(usuario) as UsuarioDto;
    } catch {
      return null;
    }
  }

  /** Actualiza los roles asociados al usuario en sesión. */
  setRoles(roles: string[]): void {
    this.roles.set(roles);
  }

  /** Comprueba si el usuario tiene un rol específico. */
  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  /** Comprueba si el usuario tiene al menos uno de los roles */
  hasAnyRole(rolesToCheck: string[]): boolean {
    return rolesToCheck.some(role => this.roles().includes(role));
  }

  /** Guarda temporalmente los datos para reactivar una cuenta.*/
  guardarReactivacionPendiente(pendiente: ReactivacionPendiente): void {
    this.reactivacionPendiente = pendiente;
  }

  /** Obtiene los datos de una reactivación*/
  obtenerReactivacionPendiente(): ReactivacionPendiente | null {
    return this.reactivacionPendiente;
  }

  /**Elimina los datos temporales de reactivación */
  limpiarReactivacionPendiente(): void {
    this.reactivacionPendiente = null;
  }

  
  /** Cierra sesión del usuario y elimina el token*/
  cerrarSesion(): void {
    localStorage.removeItem(AuthService.TOKEN_KEY);
    localStorage.removeItem(AuthService.USUARIO_KEY);

    this.usuarioEnSesion.set(null);
    this.roles.set([]);
    this.reactivacionPendiente = null;
  }
}
