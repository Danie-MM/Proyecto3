import { DOCUMENT } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, DefaultUrlSerializer, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../acceso/auth/services/auth.service';
import { entradaMascotasGuard, permiteDemoLocal } from './entrada-mascotas.guard';
import { SesionMascotasService } from './sesion-mascotas.service';

describe('Entrada de mascotas para usuarios reales o la demostración local', () => {
  const url = new DefaultUrlSerializer();
  const login = url.parse('/login');
  const demo = { actores: [{ id: 'actor', nombre: 'Responsable de prueba', organizaciones: [] }], actorActualId: null };
  let http: HttpTestingController;
  let sesion: SesionMascotasService;
  let token: string | null;
  const location = { hostname: 'localhost' };

  beforeEach(() => {
    token = null;
    location.hostname = 'localhost';
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: DOCUMENT, useValue: { location, querySelectorAll: () => [] } },
      { provide: AuthService, useValue: { obtenerToken: () => token } },
      { provide: Router, useValue: { createUrlTree: () => login, serializeUrl: (tree: UrlTree) => url.serialize(tree) } },
    ] });
    http = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionMascotasService);
  });
  afterEach(() => http.verify());

  function entrar() {
    return TestBed.runInInjectionContext(() => entradaMascotasGuard(
      {} as ActivatedRouteSnapshot, { url: '/mascotas' } as RouterStateSnapshot,
    ));
  }

  it('acepta la sesión real con el guard normal y no consulta actores', () => {
    token = 'token-existente';
    expect(entrar()).toBe(true);
    expect(sesion.demoDeEntrada).toBeNull();
    http.expectNone(() => true);
  });

  it('mantiene el login fuera de loopback y en producción', () => {
    location.hostname = 'servidor.equipo';
    expect(entrar()).toBe(login);
    expect(permiteDemoLocal(false, 'localhost')).toBeFalse();
    expect(permiteDemoLocal(true, 'localhost.ejemplo.com')).toBeFalse();
    expect(permiteDemoLocal(true, '[::1]')).toBeTrue();
    http.expectNone(() => true);
  });

  it('conserva el login si el backend real no ofrece actores', () => {
    let resultado: boolean | UrlTree | undefined;
    (entrar() as Observable<boolean | UrlTree>).subscribe(res => resultado = res);
    http.expectOne('/api/v1/demo-b01/actores').flush({}, { status: 404, statusText: 'No disponible' });
    expect(resultado).toBe(login);
    (entrar() as Observable<boolean | UrlTree>).subscribe(res => resultado = res);
    http.expectOne('/api/v1/demo-b01/actores').flush({ actores: [] });
    expect(resultado).toBe(login);
    expect(sesion.demoDeEntrada).toBeNull();
  });

  it('muestra el selector antes de consultar información privada y acepta la sesión elegida por el servidor', () => {
    let resultado: boolean | UrlTree | undefined;
    (entrar() as Observable<boolean | UrlTree>).subscribe(res => resultado = res);
    http.expectOne('/api/v1/demo-b01/actores').flush(demo);
    expect(resultado).toBeTrue();
    sesion.preparar();
    expect(sesion.estado.value).toEqual(demo);
    expect(sesion.contexto.value).toBeNull();
    http.expectNone(() => true);

    sesion.seleccionar('actor').subscribe();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf' });
    http.expectOne('/api/v1/demo-b01/sesion').flush({ actorId: 'actor' });
    http.expectOne('/api/v1/demo-b01/actores').flush({ ...demo, actorActualId: 'actor' });
    http.expectOne('/api/v1/mascotas/contexto').flush({ usuarioId: 'actor', organizaciones: [] });
    expect(sesion.estado.value?.actorActualId).toBe('actor');
    expect(sesion.contexto.value?.usuarioId).toBe('actor');
  });

  it('al recargar aprovecha la cookie demo que confirmó el servidor', () => {
    (entrar() as Observable<boolean | UrlTree>).subscribe();
    http.expectOne('/api/v1/demo-b01/actores').flush({ ...demo, actorActualId: 'actor' });
    sesion.preparar();
    http.expectOne('/api/v1/mascotas/contexto').flush({ usuarioId: 'actor', organizaciones: [] });
    expect(sesion.contexto.value?.usuarioId).toBe('actor');
    http.expectNone('/api/v1/demo-b01/actores');
  });
});
