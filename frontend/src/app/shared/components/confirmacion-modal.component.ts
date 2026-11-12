import { CommonModule, DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, ViewChild, inject } from '@angular/core';

let siguienteModal = 0;

/**
 * Presentación compartida: abre una ventana modal y administra su foco y cancelación.
 * El formulario, las validaciones y la acción de confirmar pertenecen al componente que la usa.
 */
@Component({
  standalone: true,
  selector: 'app-confirmacion-modal',
  imports: [CommonModule],
  templateUrl: './confirmacion-modal.component.html'
})
export class ConfirmacionModalComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() abierta = false;
  @Input() titulo = 'Confirmar';
  @Input() descripcion = '';
  @Input() tamano: 'sm' | 'md' | 'lg' = 'md';
  @Input() ocupado = false;
  // Si la acción elimina el botón que abrió el modal, el padre ofrece otro destino de foco.
  @Input() retornoAlternativo: HTMLElement | null = null;
  @Output() cancelada = new EventEmitter<void>();

  @ViewChild('dialogo', { static: true }) private dialogo!: ElementRef<HTMLDialogElement>;
  @ViewChild('cancelarBoton', { static: true }) private cancelarBoton!: ElementRef<HTMLButtonElement>;
  private readonly documento = inject(DOCUMENT);
  private iniciada = false;
  private focoAnterior: HTMLElement | null = null;
  private restaurarFocoPendiente = false;
  readonly idTitulo = 'confirmacion-titulo-' + ++siguienteModal;
  readonly idDescripcion = this.idTitulo + '-descripcion';

  ngAfterViewInit(): void { this.iniciada = true; this.sincronizar(); }
  ngOnChanges(): void { if (this.iniciada) this.sincronizar(); }
  ngOnDestroy(): void {
    if (this.dialogo.nativeElement.open) this.dialogo.nativeElement.close();
    this.devolverFoco();
  }

  cancelar(evento?: Event): void {
    // Escape produce cancel en el dialog nativo. El padre decide cuándo cambia abierta.
    evento?.preventDefault();
    if (!this.ocupado) this.cancelada.emit();
  }

  cerrado(): void {
    // close se entrega después del cierre; puede haber una apertura nueva entretanto.
    if (this.dialogo.nativeElement.open) return;
    // Protege también un cierre nativo solicitado mientras hay una escritura pendiente.
    if (this.abierta && this.ocupado) { this.sincronizar(); return; }
    if (this.abierta) this.cancelada.emit();
    this.devolverFoco();
  }

  private sincronizar(): void {
    const dialogo = this.dialogo.nativeElement;
    if (this.abierta && !dialogo.open) {
      if (!this.restaurarFocoPendiente) {
        this.focoAnterior = this.documento.activeElement instanceof HTMLElement ? this.documento.activeElement : null;
      }
      this.restaurarFocoPendiente = true;
      // showModal hace inerte el fondo y conserva la navegación del teclado dentro del diálogo.
      dialogo.showModal();
      if (!this.ocupado) this.cancelarBoton.nativeElement.focus();
    } else if (!this.abierta && dialogo.open) {
      dialogo.close();
      this.devolverFoco();
    }
  }

  private devolverFoco(): void {
    if (!this.restaurarFocoPendiente) return;
    this.restaurarFocoPendiente = false;
    const destino = this.focoAnterior?.isConnected ? this.focoAnterior : this.retornoAlternativo;
    if (destino?.isConnected) destino.focus();
    this.focoAnterior = null;
  }
}
