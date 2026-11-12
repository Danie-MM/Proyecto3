import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MascotaDetalleComponent } from './mascota-detalle.component';
import { SesionMascotasService } from './sesion-mascotas.service';
import { IntentosMascotaService } from './intentos-mascota.service';

describe('Privacidad del detalle al cambiar sesión', () => {
  let fixture: ComponentFixture<MascotaDetalleComponent>;
  let http: HttpTestingController;
  let sesion: SesionMascotasService;
  const mascota = {
    id: 'mascota',
    nombre: 'Nombre privado anterior',
    especie: 'GATO',
    edadEstimadaMeses: 8,
    tamano: 'PEQUENO',
    energia: 'MEDIA',
    comportamientos: [],
    comportamientoIds: [],
    version: 1,
    activa: true,
    disponibilidad: 'NO_DISPONIBLE',
    creadoEn: '2026-11-06T12:00:00Z',
  };
  beforeEach(async () => {
    const paramMap = convertToParamMap({ id: mascota.id });
    await TestBed.configureTestingModule({
      imports: [MascotaDetalleComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap }, paramMap: of(paramMap) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MascotaDetalleComponent);
    http = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionMascotasService);
    fixture.detectChanges();
    http.expectOne('/api/v1/mascotas/mascota').flush(mascota);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('borra el perfil anterior y no lo repone si falla la consulta de nuevos permisos', () => {
    expect(fixture.nativeElement.textContent).toContain('Nombre privado anterior');
    sesion.cambios.next('invalidada');
    fixture.detectChanges();
    expect(fixture.componentInstance.mascota).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Nombre privado anterior');
    http.expectNone(() => true);
    sesion.cambios.next('error');
    fixture.detectChanges();
    expect(fixture.componentInstance.cargando).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Nombre privado anterior');
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'La sesión cambió',
    );
    http.expectNone(() => true);
  });
});

describe('Confirmación de baja del perfil', () => {
  let fixture: ComponentFixture<MascotaDetalleComponent>;
  let http: HttpTestingController;
  let intentos: IntentosMascotaService;
  const actor = '11111111-1111-4111-8111-111111111111';
  const clave = '55555555-5555-4555-8555-555555555555';
  const mascota = {
    id: 'mascota',
    nombre: 'Luna',
    especie: 'GATO',
    edadEstimadaMeses: 8,
    tamano: 'PEQUENO',
    energia: 'MEDIA',
    comportamientos: [],
    comportamientoIds: [],
    version: 1,
    activa: true,
    disponibilidad: 'NO_DISPONIBLE',
    creadoEn: '2026-11-06T12:00:00Z',
  };

  beforeEach(async () => {
    const paramMap = convertToParamMap({ id: mascota.id });
    await TestBed.configureTestingModule({
      imports: [MascotaDetalleComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap }, paramMap: of(paramMap) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MascotaDetalleComponent);
    http = TestBed.inject(HttpTestingController);
    intentos = TestBed.inject(IntentosMascotaService);
    spyOn(intentos, 'obtener').and.returnValue(Promise.resolve(clave));
    spyOn(intentos, 'completar');
    TestBed.inject(SesionMascotasService).contexto.next({ usuarioId: actor, organizaciones: [] });
    fixture.detectChanges();
    http.expectOne('/api/v1/mascotas/mascota').flush(mascota);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function abrir(): HTMLDialogElement {
    const boton = fixture.nativeElement.querySelector('.baja .btn-danger') as HTMLButtonElement;
    boton.focus();
    boton.click();
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('dialog');
  }

  function enviar() {
    fixture.componentInstance.motivo = 'Registro duplicado';
    fixture.detectChanges();
    (
      fixture.nativeElement.querySelector('dialog button[type=submit]') as HTMLButtonElement
    ).click();
    flushMicrotasks();
    fixture.detectChanges();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token-prueba' });
    return http.expectOne('/api/v1/mascotas/mascota/desactivacion');
  }

  it('abrir, escribir y cancelar no modifica el perfil ni envía una petición', () => {
    const dialogo = abrir();
    fixture.componentInstance.motivo = 'Todavía no confirmado';
    fixture.detectChanges();
    (dialogo.querySelector('.btn-secondary') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(dialogo.open).toBeFalse();
    expect(fixture.componentInstance.mascota?.activa).toBeTrue();
    expect(fixture.componentInstance.motivo).toBe('');
    expect(intentos.obtener).not.toHaveBeenCalled();
    http.expectNone(() => true);
  });

  it('señala el motivo inválido junto al campo y no intenta escribir', fakeAsync(() => {
    const dialogo = abrir();
    fixture.componentInstance.motivo = '   ';
    fixture.componentInstance.desactivar();
    flushMicrotasks();
    fixture.detectChanges();
    const motivo = dialogo.querySelector('textarea')!;
    expect(motivo.getAttribute('aria-invalid')).toBe('true');
    expect(motivo.getAttribute('aria-describedby')).toContain('error-motivo');
    expect(dialogo.querySelector('#error-motivo')?.textContent).toContain('1 a 500');
    expect(document.activeElement).toBe(motivo);
    expect(dialogo.open).toBeTrue();
    expect(intentos.obtener).not.toHaveBeenCalled();
    http.expectNone(() => true);
  }));

  it('impide cerrar mientras guarda y conserva el modal ante un error de red', fakeAsync(() => {
    const dialogo = abrir();
    const peticion = enviar();
    expect(peticion.request.body).toEqual({ motivo: 'Registro duplicado', versionEsperada: 1 });
    expect(peticion.request.headers.get('Idempotency-Key')).toBe(clave);
    const escape = new Event('cancel', { cancelable: true });
    dialogo.dispatchEvent(escape);
    fixture.detectChanges();
    expect(escape.defaultPrevented).toBeTrue();
    expect(dialogo.open).toBeTrue();
    expect((dialogo.querySelector('.btn-secondary') as HTMLButtonElement).disabled).toBeTrue();
    peticion.error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(dialogo.open).toBeTrue();
    expect(dialogo.querySelector('[role=alert]')?.textContent).toContain('No pudimos confirmar');
    expect(fixture.componentInstance.mascota?.activa).toBeTrue();
    expect(intentos.completar).not.toHaveBeenCalled();
    expect((dialogo.querySelector('.btn-secondary') as HTMLButtonElement).disabled).toBeFalse();
  }));

  it('muestra 409 dentro del modal y exige recarga y una nueva confirmación', fakeAsync(() => {
    const dialogo = abrir();
    enviar().flush({ codigo: 'VERSION_DESACTUALIZADA' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(dialogo.open).toBeTrue();
    expect(dialogo.querySelector('[role=alert]')?.textContent).toContain('El perfil cambió');
    expect((dialogo.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBeTrue();
    expect(fixture.componentInstance.mascota?.version).toBe(1);
    const recargar = Array.from(dialogo.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Recargar datos'),
    )!;
    recargar.click();
    fixture.detectChanges();
    http
      .expectOne('/api/v1/mascotas/mascota')
      .flush({ ...mascota, edadEstimadaMeses: 9, version: 2 });
    fixture.detectChanges();
    expect(dialogo.open).toBeTrue();
    expect(dialogo.textContent).toContain('9 meses');
    expect(fixture.componentInstance.motivo).toBe('Registro duplicado');
    expect(
      (dialogo.querySelector('button[type=submit]') as HTMLButtonElement).disabled,
    ).toBeFalse();
    http.expectNone((request) => request.method === 'POST');
    const confirmada = enviar();
    expect(confirmada.request.body.versionEsperada).toBe(2);
    confirmada.flush({ ...mascota, edadEstimadaMeses: 9, version: 3, activa: false });
    fixture.detectChanges();
    expect(dialogo.open).toBeFalse();
    expect(fixture.componentInstance.mascota?.activa).toBeFalse();
    expect(intentos.completar).toHaveBeenCalledOnceWith(actor, 'desactivar.mascota');
    expect(fixture.nativeElement.textContent).toContain('La mascota quedó desactivada');
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('h1'));
  }));

  it('ignora la preparación de una baja si cambia la sesión antes de enviarla', fakeAsync(() => {
    abrir();
    fixture.componentInstance.motivo = 'Registro duplicado';
    fixture.componentInstance.desactivar();
    TestBed.inject(SesionMascotasService).cambios.next('invalidada');
    flushMicrotasks();
    fixture.detectChanges();
    expect(fixture.componentInstance.mascota).toBeNull();
    expect(fixture.componentInstance.confirmarBaja).toBeFalse();
    expect(fixture.nativeElement.querySelector('dialog')).toBeNull();
    http.expectNone(() => true);
  }));
});
