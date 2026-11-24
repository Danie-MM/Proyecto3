import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { IntentosMascotaService } from '../intentos-mascota.service';
import { Mascota } from '../mascotas.model';
import { SesionMascotasService } from '../sesion-mascotas.service';
import { PublicacionMascotaComponent } from './publicacion-mascota.component';
import { PublicacionMascotaApi } from './publicacion-mascota.api';

describe('Publicación y fotografía', () => {
  const mascota: Mascota = {
    id: 'mascota', nombre: 'Luna', especie: 'GATO', tamano: 'MEDIANO', energia: 'MEDIA',
    edadEstimadaMeses: 10, comportamientoIds: [], comportamientos: [], disponibilidad: 'NO_DISPONIBLE',
    activa: true, fotoUrl: null, version: 3, responsableUsuarioId: 'usuario',
    organizacionId: 'organizacion', creadoEn: '2026-01-01T12:00:00Z',
  };
  let fixture: ComponentFixture<PublicacionMascotaComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [PublicacionMascotaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()] });
    fixture = TestBed.createComponent(PublicacionMascotaComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('mascota', mascota);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('envía la versión y exige recarga explícita después de un conflicto', () => {
    const componente = fixture.componentInstance;
    spyOn(componente.actualizada, 'emit');
    spyOn(componente.recargar, 'emit');
    componente.disponibilidad('DISPONIBLE');
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const req = http.expectOne('/api/v1/mascotas/mascota/disponibilidad');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ disponibilidad: 'DISPONIBLE', versionEsperada: 3 });
    req.flush({ codigo: 'VERSION_DESACTUALIZADA' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(componente.actualizada.emit).not.toHaveBeenCalled();
    const boton = fixture.nativeElement.querySelector('button.btn-primary') as HTMLButtonElement;
    expect(boton.disabled).toBeTrue();
    fixture.nativeElement.querySelector('[role=alert] button').click();
    expect(componente.recargar.emit).toHaveBeenCalled();
    http.expectNone(request => request.method === 'PUT');
  });

  it('rechaza un archivo mayor al límite y permite seleccionar otro', () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type=file]');
    const archivo = new File([new Uint8Array(5_000_001)], 'grande.png');
    Object.defineProperty(input, 'files', { value: [archivo], configurable: true });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(fixture.componentInstance.archivo).toBeNull();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('5 000 000');
    http.expectNone(() => true);
  });

  it('envía la foto como multipart con versión, CSRF y clave de reenvío', () => {
    const archivo = new File(['contenido de prueba'], 'prueba.png', { type: 'image/png' });
    TestBed.inject(PublicacionMascotaApi).subir(mascota, archivo, 'clave-de-prueba').subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-foto' });
    const req = http.expectOne('/api/v1/mascotas/mascota/foto');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.get('archivo').name).toBe('prueba.png');
    expect(req.request.body.get('versionEsperada')).toBe('3');
    expect(req.request.headers.get('Idempotency-Key')).toBe('clave-de-prueba');
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-foto');
    expect(req.request.headers.has('Content-Type')).toBeFalse();
    req.flush({ urlPrivada: '/api/v1/mascotas/mascota/foto', versionMascota: 4 });
  });

  it('conserva la foto y la selección si el servidor rechaza el reemplazo', async () => {
    const api = TestBed.inject(PublicacionMascotaApi);
    const intentos = TestBed.inject(IntentosMascotaService);
    spyOn(api, 'leerFoto').and.returnValue(of(new Blob(['foto anterior'])));
    spyOn(intentos, 'completar');
    TestBed.inject(SesionMascotasService).contexto.next({ usuarioId: 'usuario', organizaciones: [] });
    fixture.componentRef.setInput('mascota', { ...mascota, fotoUrl: '/api/v1/mascotas/mascota/foto' });
    fixture.detectChanges();
    const componente = fixture.componentInstance;
    const anterior = componente.vistaFoto;
    const archivo = new File(['contenido inválido'], 'foto.jpg', { type: 'image/jpeg' });
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type=file]');
    Object.defineProperty(input, 'files', { value: [archivo] });
    componente.elegir(input);
    spyOn(componente.actualizada, 'emit');
    const enviada = new Promise<void>(resolve => {
      spyOn(api, 'subir').and.callFake(() => {
        resolve();
        return throwError(() => new HttpErrorResponse({ status: 415 }));
      });
    });
    componente.subir(input);
    await enviada;
    fixture.detectChanges();
    expect(componente.vistaFoto).toBe(anterior);
    expect(componente.archivo).toBe(archivo);
    expect(componente.actualizada.emit).not.toHaveBeenCalled();
    expect(intentos.completar).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Se conservó la foto anterior');
  });

  it('habilita ocultar una mascota publicada y actualiza con la respuesta del servidor', () => {
    const publicada = { ...mascota, disponibilidad: 'DISPONIBLE', version: 4 };
    fixture.componentRef.setInput('mascota', publicada);
    fixture.detectChanges();
    const emit = spyOn(fixture.componentInstance.actualizada, 'emit');
    fixture.nativeElement.querySelector('.acciones button').click();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const req = http.expectOne('/api/v1/mascotas/mascota/disponibilidad');
    expect(req.request.body).toEqual({ disponibilidad: 'NO_DISPONIBLE', versionEsperada: 4 });
    const oculta = { ...publicada, disponibilidad: 'NO_DISPONIBLE', version: 5 };
    req.flush(oculta);
    expect(emit).toHaveBeenCalledOnceWith(oculta);
  });
});
