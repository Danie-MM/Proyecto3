import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, finalize, forkJoin, of } from 'rxjs';
import { MascotasApiService, mensajeError } from './mascotas-api.service';
import { CatalogosMascota, DatosMascota, Mascota, textoCatalogo } from './mascotas.model';
import { IntentosMascotaService } from './intentos-mascota.service';
import { SesionMascotasService } from './sesion-mascotas.service';

@Component({
  selector: 'app-mascota-formulario',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mascota-formulario.component.html',
  styleUrl: './mascotas.css',
})
export class MascotaFormularioComponent implements OnInit {
  private readonly api = inject(MascotasApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly intentos = inject(IntentosMascotaService);
  readonly sesion = inject(SesionMascotasService);
  readonly texto = textoCatalogo;
  catalogos: CatalogosMascota | null = null;
  original: Mascota | null = null;
  datos = this.vacios();
  cargando = false;
  ocupado = false;
  error = '';
  conflicto = false;
  errores: Record<string, string> = {};
  private solicitudes = new Subscription();
  // Invalida un guardado en preparación si antes cambia el perfil o la sesión.
  private generacion = 0;
  get editando() {
    return this.ruta.snapshot.data['editar'] === true;
  }
  get especies() {
    return [
      ...new Set([
        ...(this.catalogos?.especies ?? []),
        ...(this.original ? [this.original.especie] : []),
      ]),
    ];
  }
  ngOnInit() {
    this.destroyRef.onDestroy(() => {
      this.generacion++;
      this.solicitudes.unsubscribe();
    });
    this.ruta.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.sesion.cambios.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((estado) => {
      if (estado !== 'invalidada') return;
      this.generacion++;
      this.solicitudes.unsubscribe();
      this.datos = this.vacios();
      this.original = null;
      void this.router.navigate(['/mascotas']);
    });
  }
  cargar() {
    this.generacion++;
    this.solicitudes.unsubscribe();
    this.solicitudes = new Subscription();
    this.cargando = true;
    this.ocupado = false;
    this.error = '';
    this.conflicto = false;
    this.errores = {};
    this.catalogos = null;
    this.original = null;
    this.datos = this.vacios();
    const id = this.ruta.snapshot.paramMap.get('id');
    this.solicitudes.add(
      forkJoin({
        catalogos: this.api.catalogos(),
        mascota: this.editando && id ? this.api.consultar(id) : of(null),
      })
        .pipe(finalize(() => (this.cargando = false)))
        .subscribe({
          next: ({ catalogos, mascota }) => {
            this.catalogos = catalogos;
            this.original = mascota;
            if (mascota)
              this.datos = {
                nombre: mascota.nombre,
                especie: mascota.especie,
                edadEstimadaMeses: mascota.edadEstimadaMeses,
                tamano: mascota.tamano,
                energia: mascota.energia,
                comportamientoIds: [...mascota.comportamientoIds],
                organizacionId: mascota.organizacionId ?? '',
              };
          },
          error: (error) => (this.error = mensajeError(error)),
        }),
    );
  }
  tieneEtiqueta(id: string) {
    return this.datos.comportamientoIds.includes(id);
  }
  cambiarEtiqueta(id: string, seleccionada: boolean) {
    this.datos.comportamientoIds = seleccionada
      ? [...new Set([...this.datos.comportamientoIds, id])]
      : this.datos.comportamientoIds.filter((valor) => valor !== id);
  }
  async guardar(): Promise<void> {
    if (this.ocupado || this.cargando || !this.catalogos || this.original?.activa === false) return;
    this.errores = this.validar();
    this.error = '';
    this.conflicto = false;
    if (Object.keys(this.errores).length) {
      this.error = 'Revisá los campos indicados antes de guardar.';
      return;
    }
    // El actor proviene de /mascotas/contexto, no del propietario indicado en un perfil.
    const actorId = this.sesion.contexto.value?.usuarioId;
    if (!actorId) {
      this.error = 'No pudimos verificar tu sesión. Recargá la página antes de guardar.';
      return;
    }
    const generacion = this.generacion;
    // Solo campos editables: el servidor determina ID, responsable, estado y versión nueva.
    const datos: DatosMascota = {
      nombre: this.datos.nombre.trim(),
      especie: this.datos.especie,
      edadEstimadaMeses: this.datos.edadEstimadaMeses!,
      tamano: this.datos.tamano,
      energia: this.datos.energia,
      comportamientoIds: [...this.datos.comportamientoIds].sort(),
    };
    this.ocupado = true;
    try {
      const alta = { ...datos, organizacionId: this.datos.organizacionId || null };
      const solicitud = this.editando
        ? this.api.editar(this.original!.id, { ...datos, versionEsperada: this.original!.version })
        : this.api.crear(alta, await this.intentos.obtener(actorId, 'crear', alta));
      if (generacion !== this.generacion) return;
      this.solicitudes.add(
        solicitud.pipe(finalize(() => (this.ocupado = false))).subscribe({
          next: (mascota) => {
            if (!mascota?.id || !Number.isInteger(mascota.version)) {
              this.error =
                'El servidor respondió sin confirmar el perfil. Reintentá con los mismos datos.';
              return;
            }
            if (!this.editando) this.intentos.completar(actorId, 'crear');
            void this.router.navigate(['/mascotas', mascota.id], {
              state: {
                confirmacion: this.editando
                  ? 'Los cambios se guardaron.'
                  : 'La mascota se registró correctamente.',
              },
            });
          },
          error: (error) => {
            this.error = mensajeError(error);
            this.conflicto =
              error instanceof HttpErrorResponse &&
              error.status === 409 &&
              error.error?.codigo === 'VERSION_DESACTUALIZADA';
            if (this.conflicto)
              this.error =
                'Otra persona modificó este perfil. Tus cambios no se sobrescribieron. Recargá la versión vigente para revisar y volver a editar.';
            if (Array.isArray(error.error?.erroresCampos))
              for (const campo of error.error.erroresCampos)
                this.errores[campo.campo] = campo.mensaje;
          },
        }),
      );
    } catch {
      this.ocupado = false;
      this.error = 'No pudimos preparar el guardado. Tus datos siguen en el formulario; reintentá.';
    }
  }
  private validar(): Record<string, string> {
    const errores: Record<string, string> = {};
    const longitud = [...this.datos.nombre.trim()].length;
    if (longitud < 1 || longitud > 100)
      errores['nombre'] = 'Escribí un nombre de 1 a 100 caracteres.';
    if (!this.especies.includes(this.datos.especie))
      errores['especie'] = 'Seleccioná una especie del catálogo.';
    if (
      this.datos.edadEstimadaMeses === null ||
      !Number.isInteger(this.datos.edadEstimadaMeses) ||
      this.datos.edadEstimadaMeses < 0 ||
      this.datos.edadEstimadaMeses > 2147483647
    )
      errores['edadEstimadaMeses'] =
        'Indicá una edad estimada en meses enteros, igual o mayor que cero.';
    if (!this.catalogos?.tamanos.includes(this.datos.tamano))
      errores['tamano'] = 'Seleccioná el tamaño.';
    if (!this.catalogos?.energias.includes(this.datos.energia))
      errores['energia'] = 'Seleccioná el nivel de energía.';
    if (!this.datos.comportamientoIds.length)
      errores['comportamientoIds'] = 'Seleccioná al menos un comportamiento.';
    return errores;
  }
  private vacios() {
    return {
      nombre: '',
      especie: '',
      edadEstimadaMeses: null as number | null,
      tamano: '',
      energia: '',
      comportamientoIds: [] as string[],
      organizacionId: '',
    };
  }
}
