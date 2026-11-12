import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PreferenciasUbicacionService } from './preferencias-ubicacion.service';

describe('PreferenciasUbicacionService', () => {
  const ruta = '/api/extravios/ubicacion';
  const datos = { latitud: 9.93, longitud: -84.09, referencia: 'Parque', aceptaAvisos: true };
  let service: PreferenciasUbicacionService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PreferenciasUbicacionService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('se crea con el proveedor HTTP', () => expect(service).toBeTruthy());

  it('reutiliza la clave de alta tras perder la respuesta y renueva el CSRF', () => {
    service.crear(datos).subscribe({ error: () => {} });
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-inicial' });
    const primera = http.expectOne({ method: 'POST', url: ruta });
    const clave = primera.request.headers.get('Idempotency-Key');
    primera.error(new ProgressEvent('error'));
    service.crear(datos).subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-renovado' });
    const reenvio = http.expectOne({ method: 'POST', url: ruta });
    expect(reenvio.request.headers.get('Idempotency-Key')).toBe(clave);
    expect(reenvio.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-renovado');
    reenvio.flush({ ...datos, configurada: true, version: 0 });
  });

  it('usa otra clave si el usuario cambia los datos tras un fallo', () => {
    service.crear(datos).subscribe({ error: () => {} });
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const primera = http.expectOne(ruta);
    const clave = primera.request.headers.get('Idempotency-Key');
    primera.flush({}, { status: 500, statusText: 'Error' });
    service.crear({ ...datos, referencia: 'Otro punto' }).subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const diferente = http.expectOne(ruta);
    expect(diferente.request.headers.get('Idempotency-Key')).not.toBe(clave);
    diferente.flush({ ...datos, configurada: true, version: 0 });
  });

  it('no envía datos si falla la obtención del CSRF', () => {
    service.crear(datos).subscribe({ error: () => {} });
    http.expectOne('/api/v1/sesion/csrf').flush({}, { status: 403, statusText: 'Forbidden' });
    http.expectNone(ruta);
  });

  it('permite una nueva alta tras confirmar y eliminar una creación cuya respuesta se perdió', () => {
    service.crear(datos).subscribe({ error: () => {} });
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const perdida = http.expectOne(ruta);
    const claveAnterior = perdida.request.headers.get('Idempotency-Key');
    perdida.error(new ProgressEvent('error'));
    service.consultar().subscribe();
    http.expectOne(ruta).flush({ ...datos, configurada: true, version: 0 });
    service.eliminar(0).subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    http.expectOne(ruta + '?versionEsperada=0').flush(null, { status: 204, statusText: 'No Content' });
    service.crear(datos).subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    const nueva = http.expectOne(ruta);
    expect(nueva.request.headers.get('Idempotency-Key')).not.toBe(claveAnterior);
    nueva.flush({ ...datos, configurada: true, version: 0 });
  });
});
