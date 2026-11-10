
import { Injectable, NgZone, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import { GOOGLE_CLIENT_ID } from '../constants';

interface GoogleCredentialResponse {
  credential: string;
}

export type TextoBotonGoogle = 'signup_with' | 'signin_with' | 'continue_with';

interface GoogleButtonOptions {
  type: 'standard';
  theme: 'filled_black' | 'outline' | 'filled_blue';
  size: 'large' | 'medium' | 'small';
  text: TextoBotonGoogle;
  width: number;
  locale: string;
}

interface GoogleIdApi {
  initialize(config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
  }): void;

  renderButton(parent: HTMLElement, options: GoogleButtonOptions): void;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: GoogleIdApi;
      };
    };
  }
}

/**
 * @author Daniela Morales
 * @version 1.0
 * @description Integra Google Identity Services
 */
@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {

  private static readonly SCRIPT_URL = 'https://accounts.google.com/gsi/client';

  private readonly zone = inject(NgZone);
  private readonly credenciales = new Subject<string>();

  private scriptPromise: Promise<void> | null = null;
  private inicializado = false;

  /**
   * Permite que otros componentes reciban el token
   * de identidad proporcionado por Google.
   *
   * @returns Observable que emite las credenciales de Google.
   */
  get credencial$(): Observable<string> {
    return this.credenciales.asObservable();
  }

  /**
   * Carga Google Identity Services e inicializa el botón
   * de autenticación dentro del elemento indicado.
   *
   * @param contenedor Elemento HTML donde se mostrará el botón.
   * @param texto Texto que utilizará el botón de Google.
   */
  async inicializarBoton(
    contenedor: HTMLElement,
    texto: TextoBotonGoogle = 'signup_with'
  ): Promise<void> {
    try {
      await this.cargarScript();
    } catch {
      console.error('No se pudo cargar Google Identity Services.');
      return;
    }

    if (!window.google) {
      return;
    }

    if (!this.inicializado) {
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: respuesta => {
          this.zone.run(() => {
            this.credenciales.next(respuesta.credential);
          });
        }
      });

      this.inicializado = true;
    }

    window.google.accounts.id.renderButton(contenedor, {
      type: 'standard',
      theme: 'filled_black',
      size: 'large',
      text: texto,
      width: 400,
      locale: 'es'
    });
  }

  /**
   * Carga el script de Google Identity Services si todavía
   * no está disponible. Evita cargarlo varias veces.
   *
   * @returns Promesa que se completa cuando el script termina de cargar.
   */
  private cargarScript(): Promise<void> {
    if (window.google) {
      return Promise.resolve();
    }

    if (this.scriptPromise) {
      return this.scriptPromise;
    }

    this.scriptPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');

      script.src = GoogleAuthService.SCRIPT_URL;
      script.async = true;
      script.defer = true;

      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Error al cargar Google'));

      document.head.appendChild(script);
    });

    return this.scriptPromise;
  }
}
