import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SesionMascotasService } from './sesion-mascotas.service';

describe('Contexto de sesión de mascotas', () => {
  let sesion: SesionMascotasService;
  let http: HttpTestingController;
  const demo = {
    actores: [
      { id: 'uno', nombre: 'Uno', organizaciones: [] },
      { id: 'dos', nombre: 'Dos', organizaciones: [] },
    ],
    actorActualId: 'uno',
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    sesion = TestBed.inject(SesionMascotasService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('descarta los ámbitos anteriores al volver al módulo con otra cuenta', () => {
    sesion.preparar();
    http.expectOne('/api/v1/mascotas/contexto').flush({
      usuarioId: 'anterior', organizaciones: [{ id: 'privada', nombre: 'Organización anterior' }]
    });
    http.expectOne('/api/v1/demo-b01/actores').flush({}, { status: 404, statusText: 'Not Found' });
    sesion.preparar();
    expect(sesion.contexto.value).toBeNull();
    expect(sesion.organizaciones).toEqual([]);
    http.expectOne('/api/v1/mascotas/contexto').flush({ usuarioId: 'nueva', organizaciones: [] });
    http.expectOne('/api/v1/demo-b01/actores').flush({}, { status: 404, statusText: 'Not Found' });
    expect(sesion.contexto.value?.usuarioId).toBe('nueva');
    expect(sesion.organizaciones).toEqual([]);
  });
  it('usa los ámbitos de una sesión real aunque no exista la demo', () => {
    sesion.preparar();
    http
      .expectOne('/api/v1/mascotas/contexto')
      .flush({
        usuarioId: 'real',
        organizaciones: [{ id: 'org-real', nombre: 'Organización autorizada' }],
      });
    http.expectOne('/api/v1/demo-b01/actores').flush({}, { status: 404, statusText: 'Not Found' });
    expect(sesion.estado.value).toBeNull();
    expect(sesion.contexto.value?.usuarioId).toBe('real');
    expect(sesion.organizaciones.map((org) => org.id)).toEqual(['org-real']);
  });
  it('invalida inmediatamente tras el POST aunque el GET posterior falle', () => {
    sesion.preparar();
    http
      .expectOne('/api/v1/mascotas/contexto')
      .flush({
        usuarioId: 'uno',
        organizaciones: [{ id: 'privada', nombre: 'Organización anterior' }],
      });
    http.expectOne('/api/v1/demo-b01/actores').flush(demo);
    const cambios: string[] = [];
    sesion.cambios.subscribe((estado) => cambios.push(estado));
    let fallo = false;
    sesion.seleccionar('dos').subscribe({ error: () => (fallo = true) });
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token' });
    http.expectOne('/api/v1/demo-b01/sesion').flush({ usuarioId: 'dos' });
    expect(cambios).toEqual(['invalidada']);
    expect(sesion.contexto.value).toBeNull();
    expect(sesion.organizaciones).toEqual([]);
    expect(sesion.estado.value?.actorActualId).toBeNull();
    const contexto = http.expectOne('/api/v1/mascotas/contexto');
    http
      .expectOne('/api/v1/demo-b01/actores')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    expect(contexto.cancelled).toBeTrue();
    expect(fallo).toBeTrue();
    expect(cambios).toEqual(['invalidada', 'error']);
    expect(sesion.contexto.value).toBeNull();
    expect(sesion.actualizando).toBeFalse();
  });
  it('habilita la recarga solo al obtener el nuevo contexto completo', () => {
    const cambios: string[] = [];
    sesion.cambios.subscribe((estado) => cambios.push(estado));
    sesion.seleccionar('dos').subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token' });
    http.expectOne('/api/v1/demo-b01/sesion').flush({ usuarioId: 'dos' });
    http.expectOne('/api/v1/demo-b01/actores').flush({ ...demo, actorActualId: 'dos' });
    expect(cambios).toEqual(['invalidada']);
    http
      .expectOne('/api/v1/mascotas/contexto')
      .flush({ usuarioId: 'dos', organizaciones: [{ id: 'nueva', nombre: 'Ámbito nuevo' }] });
    expect(cambios).toEqual(['invalidada', 'lista']);
    expect(sesion.organizaciones.map((org) => org.id)).toEqual(['nueva']);
  });
});
