import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject, Subscription, catchError, forkJoin, of, switchMap, tap, throwError } from 'rxjs';
import { MascotasApiService } from './mascotas-api.service';
import { ContextoMascotas, SesionDemo } from './mascotas.model';

/** Los permisos proceden de la sesión del servidor. La demo es una interfaz opcional. */
@Injectable({ providedIn: 'root' })
export class SesionMascotasService {
  private readonly api = inject(MascotasApiService);
  readonly estado = new BehaviorSubject<SesionDemo | null>(null);
  readonly contexto = new BehaviorSubject<ContextoMascotas | null>(null);
  readonly cambios = new Subject<'invalidada' | 'lista' | 'error'>();
  actualizando = false;
  private iniciada = false;
  private lecturasIniciales = new Subscription();
  preparar() {
    if (this.iniciada) return;
    this.iniciada = true;
    this.lecturasIniciales.add(this.api.contexto().pipe(catchError(() => of(null))).subscribe(contexto => this.contexto.next(contexto)));
    this.lecturasIniciales.add(this.api.demo().pipe(catchError(() => of(null))).subscribe(demo => this.estado.next(demo)));
  }
  seleccionar(actorId: string) {
    let cambioConfirmado = false;
    return this.api.cambiarActorDemo(actorId).pipe(
      tap(() => {
        cambioConfirmado = true; this.lecturasIniciales.unsubscribe(); this.actualizando = true;
        this.contexto.next(null);
        const demo = this.estado.value;
        if (demo) this.estado.next({ ...demo, actorActualId: null });
        // La sesión ya cambió: borrar el perfil ANTES de consultar los metadatos siguientes.
        this.cambios.next('invalidada');
      }),
      switchMap(() => forkJoin({ demo: this.api.demo(), contexto: this.api.contexto() })),
      tap(({ demo, contexto }) => {
        this.estado.next(demo); this.contexto.next(contexto); this.actualizando = false; this.cambios.next('lista');
      }),
      catchError(error => {
        if (cambioConfirmado) { this.actualizando = false; this.cambios.next('error'); }
        return throwError(() => error);
      })
    );
  }
  get organizaciones() { return this.contexto.value?.organizaciones ?? []; }
}
