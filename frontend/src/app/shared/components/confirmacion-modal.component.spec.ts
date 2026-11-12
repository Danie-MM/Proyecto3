import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmacionModalComponent } from './confirmacion-modal.component';

@Component({
  standalone: true,
  imports: [ConfirmacionModalComponent],
  template: `
    <button id="abrir-prueba" (click)="abierta = true">Abrir confirmación</button>
    <app-confirmacion-modal [abierta]="abierta" [ocupado]="ocupado" titulo="Confirmación de prueba"
      descripcion="Contenido de prueba." (cancelada)="cancelar()">
      <label for="dato-prueba">Dato</label><input id="dato-prueba">
      <button accionesModal type="button">Confirmar</button>
    </app-confirmacion-modal>`
})
class AnfitrionModal {
  abierta = false;
  ocupado = false;
  cancelaciones = 0;
  cancelar(): void { this.cancelaciones++; this.abierta = false; }
}

describe('Confirmación modal compartida', () => {
  let fixture: ComponentFixture<AnfitrionModal>;
  let dialogo: HTMLDialogElement;
  let abrir: HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AnfitrionModal] }).compileComponents();
    fixture = TestBed.createComponent(AnfitrionModal);
    fixture.detectChanges();
    dialogo = fixture.nativeElement.querySelector('dialog');
    abrir = fixture.nativeElement.querySelector('#abrir-prueba');
    abrir.focus(); abrir.click(); fixture.detectChanges();
  });

  it('abre modal nativo con nombre accesible y foco en la opción segura', () => {
    expect(dialogo.open).toBeTrue();
    expect(dialogo.matches(':modal')).toBeTrue();
    const titulo = document.getElementById(dialogo.getAttribute('aria-labelledby')!);
    expect(titulo?.textContent).toBe('Confirmación de prueba');
    expect(document.activeElement).toBe(dialogo.querySelector('.btn-secondary'));
  });

  it('cancelar no confirma nada y devuelve el foco al botón de apertura', () => {
    (dialogo.querySelector('.btn-secondary') as HTMLButtonElement).click(); fixture.detectChanges();
    expect(dialogo.open).toBeFalse();
    expect(fixture.componentInstance.cancelaciones).toBe(1);
    expect(document.activeElement).toBe(abrir);
  });

  it('atiende el evento cancel que produce Escape y devuelve el foco', () => {
    const escape = new Event('cancel', { cancelable: true });
    dialogo.dispatchEvent(escape); fixture.detectChanges();
    expect(escape.defaultPrevented).toBeTrue();
    expect(dialogo.open).toBeFalse();
    expect(fixture.componentInstance.cancelaciones).toBe(1);
    expect(document.activeElement).toBe(abrir);
  });

  it('bloquea Escape y Cancelar mientras el padre espera una respuesta', () => {
    fixture.componentInstance.ocupado = true; fixture.detectChanges();
    const escape = new Event('cancel', { cancelable: true });
    dialogo.dispatchEvent(escape); fixture.detectChanges();
    const cancelar = dialogo.querySelector('.btn-secondary') as HTMLButtonElement;
    cancelar.click(); fixture.detectChanges();
    expect(escape.defaultPrevented).toBeTrue();
    expect(cancelar.disabled).toBeTrue();
    expect(dialogo.open).toBeTrue();
    expect(fixture.componentInstance.cancelaciones).toBe(0);
  });

  it('permite que el padre cierre al terminar sin emitir una cancelación', () => {
    fixture.componentInstance.abierta = false; fixture.detectChanges();
    expect(dialogo.open).toBeFalse();
    expect(fixture.componentInstance.cancelaciones).toBe(0);
    expect(document.activeElement).toBe(abrir);
  });
});
