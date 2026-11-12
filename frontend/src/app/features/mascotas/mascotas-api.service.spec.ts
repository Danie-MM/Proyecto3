import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MascotasApiService } from './mascotas-api.service';

describe('API de perfiles privados', () => {
  let api: MascotasApiService;
  let http: HttpTestingController;
  const datos = {
    nombre: 'Luna',
    especie: 'GATO',
    edadEstimadaMeses: 8,
    tamano: 'PEQUENO',
    energia: 'MEDIA',
    comportamientoIds: ['etiqueta'],
    organizacionId: null,
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(MascotasApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('obtiene CSRF y conserva la clave original al reintentar un alta', () => {
    api.crear(datos, 'misma-clave').subscribe({ error: () => undefined });
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token-1' });
    const primera = http.expectOne({ method: 'POST', url: '/api/v1/mascotas' });
    expect(primera.request.headers.get('X-CSRF-TOKEN')).toBe('token-1');
    expect(primera.request.headers.get('Idempotency-Key')).toBe('misma-clave');
    expect(primera.request.body).toEqual(datos);
    primera.error(new ProgressEvent('error'));
    api.crear(datos, 'misma-clave').subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token-2' });
    const reintento = http.expectOne('/api/v1/mascotas');
    expect(reintento.request.headers.get('Idempotency-Key')).toBe('misma-clave');
    expect(reintento.request.headers.get('X-CSRF-TOKEN')).toBe('token-2');
    reintento.flush({ id: 'mascota', version: 0 }, { status: 201, statusText: 'Created' });
  });
  it('no envía escrituras si falla la obtención del token', () => {
    let error = false;
    api.crear(datos, 'clave').subscribe({ error: () => (error = true) });
    http.expectOne('/api/v1/sesion/csrf').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(error).toBeTrue();
    http.expectNone('/api/v1/mascotas');
  });
  it('combina filtros y conserva la paginación acordada', () => {
    api.listar(2, false, 'organizacion').subscribe();
    const peticion = http.expectOne((request) => request.url === '/api/v1/mascotas');
    expect(peticion.request.params.get('pagina')).toBe('2');
    expect(peticion.request.params.get('limite')).toBe('20');
    expect(peticion.request.params.get('activas')).toBe('false');
    expect(peticion.request.params.get('organizacionId')).toBe('organizacion');
    peticion.flush({ elementos: [], pagina: 2, limite: 20, totalElementos: 0 });
  });
});
