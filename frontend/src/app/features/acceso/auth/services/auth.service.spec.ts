import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { authInterceptor } from '../auth.interceptor';
import { AuthService } from './auth.service';

describe('Acceso integrado con CSRF y JWT', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let cliente: HttpClient;

  beforeEach(() => {
    localStorage.removeItem('petmatch_token');
    localStorage.removeItem('petmatch_usuario');
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])
    ] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
  });
  afterEach(() => { http.verify(); auth.cerrarSesion(); });

  for (const operacion of ['registro', 'login', 'reactivar']) {
    it('obtiene CSRF sin un JWT vencido antes de ' + operacion, () => {
      auth.guardarSesion('jwt-vencido-de-prueba');
      const solicitud = operacion === 'registro'
        ? auth.registrar({ idToken: 'google-ficticio', nombreCompleto: 'Persona Ficticia',
            fechaNacimiento: '2000-01-01', rol: 'ADOPTANTE' })
        : operacion === 'login' ? auth.iniciarSesion({ idToken: 'google-ficticio' })
          : auth.reactivar({ idToken: 'google-ficticio' });
      solicitud.subscribe();
      const csrf = http.expectOne('/api/v1/sesion/csrf');
      expect(csrf.request.headers.has('Authorization')).toBeFalse();
      http.expectNone('/api/auth/' + operacion);
      csrf.flush({ token: 'csrf-renovado' });
      const escritura = http.expectOne({ method: 'POST', url: '/api/auth/' + operacion });
      expect(escritura.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-renovado');
      expect(escritura.request.headers.has('Authorization')).toBeFalse();
      expect(escritura.request.body.idToken).toBe('google-ficticio');
      escritura.flush({ token: 'jwt-nuevo', usuario: {} });
    });
  }

  it('adjunta JWT a la API privada pero nunca al proveedor de mapas', () => {
    auth.guardarSesion('jwt-ficticio');
    cliente.get('/api/extravios/ubicacion').subscribe();
    const privada = http.expectOne('/api/extravios/ubicacion');
    expect(privada.request.headers.get('Authorization')).toBe('Bearer jwt-ficticio');
    privada.flush({ configurada: false });
    cliente.get('https://nominatim.openstreetmap.org/search').subscribe();
    const externa = http.expectOne('https://nominatim.openstreetmap.org/search');
    expect(externa.request.headers.has('Authorization')).toBeFalse();
    externa.flush([]);
  });

  it('conserva la sesión si un recurso público devuelve 401', () => {
    auth.guardarSesion('sesion-actual');
    const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl');
    for (const url of ['/api/v1/demo-b01/actores', '/api/v1/sesion/csrf',
      '/api/v1/catalogo/mascotas?pagina=1', '/api/v1/mascotas/id/foto/publica']) {
      cliente.get(url).subscribe({ error: () => undefined });
      const solicitud = http.expectOne(url);
      expect(solicitud.request.headers.has('Authorization')).toBeFalse();
      solicitud.flush({}, { status: 401, statusText: 'No autorizado' });
      expect(auth.obtenerToken()).toBe('sesion-actual');
    }
    expect(navegar).not.toHaveBeenCalled();
  });

  it('ignora el 401 de una solicitud iniciada antes de reemplazar el token', () => {
    auth.guardarSesion('anterior');
    const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl');
    cliente.get('/api/v1/mascotas').subscribe({ error: () => undefined });
    const anterior = http.expectOne('/api/v1/mascotas');
    expect(anterior.request.headers.get('Authorization')).toBe('Bearer anterior');
    auth.guardarSesion('actual');
    anterior.flush({}, { status: 401, statusText: 'Caducado' });
    expect(auth.obtenerToken()).toBe('actual');
    expect(navegar).not.toHaveBeenCalled();
  });

  it('conserva el destino cuando vence el mismo token enviado a la API privada', () => {
    auth.guardarSesion('caducado');
    const router = TestBed.inject(Router);
    const destino = '/mascotas?expediente=prueba#vacunas';
    spyOnProperty(router, 'url').and.returnValue(destino);
    const navegar = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    cliente.get('/api/v1/mascotas').subscribe({ error: () => undefined });
    http.expectOne('/api/v1/mascotas').flush({}, { status: 401, statusText: 'Caducado' });
    expect(auth.obtenerToken()).toBeNull();
    const [ingreso, opciones] = navegar.calls.mostRecent().args;
    expect((ingreso as UrlTree).queryParams['returnUrl']).toBe(destino);
    expect(router.serializeUrl(ingreso as UrlTree)).toMatch(/^\/login\?/);
    expect(opciones?.replaceUrl).toBeTrue();
  });
});
