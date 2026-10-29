import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { longitudTexto, normalizarTexto, PruebaEntorno, UUID_VALIDO } from './prueba-entorno.model';
import { PruebaEntornoService } from './prueba-entorno.service';

@Component({
  selector: 'app-comprobar-entorno',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './comprobar-entorno.component.html',
  styleUrl: './comprobar-entorno.component.css'
})
export class ComprobarEntornoComponent {
  private readonly servicio = inject(PruebaEntornoService);
  private readonly destroyRef = inject(DestroyRef);
  texto = '';
  id = '';
  accion: 'guardar' | 'consultar' | null = null;
  resultado: PruebaEntorno | null = null;
  resultadoTitulo = '';
  error = '';

  get ocupado(): boolean { return this.accion !== null; }
  get caracteres(): number { return longitudTexto(this.texto); }

  guardar(): void {
    if (this.ocupado) return;
    this.limpiarResultado();
    if (this.caracteres < 1 || this.caracteres > 80) {
      this.error = 'Escribí entre 1 y 80 caracteres. Los espacios al inicio y al final no cuentan.';
      return;
    }
    this.accion = 'guardar';
    this.servicio.guardar(normalizarTexto(this.texto)).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.accion = null)
    ).subscribe({
      next: dato => {
        this.resultado = dato;
        this.id = dato.id;
        this.texto = dato.texto;
        this.resultadoTitulo = 'Tu texto quedó guardado';
      },
      error: (error: Error) => this.error = error.message
    });
  }

  consultar(): void {
    if (this.ocupado) return;
    this.limpiarResultado();
    this.id = this.id.trim();
    if (!UUID_VALIDO.test(this.id)) {
      this.error = 'Usá un ID válido con formato UUID. Al guardar un texto, lo completamos por vos.';
      return;
    }
    this.accion = 'consultar';
    this.servicio.consultar(this.id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.accion = null)
    ).subscribe({
      next: dato => {
        this.resultado = dato;
        this.resultadoTitulo = 'Encontramos tu registro';
      },
      error: (error: Error) => this.error = error.message
    });
  }

  private limpiarResultado(): void {
    this.resultado = null;
    this.resultadoTitulo = '';
    this.error = '';
  }
}
