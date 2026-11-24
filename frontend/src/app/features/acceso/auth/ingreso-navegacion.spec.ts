import { HttpErrorResponse } from '@angular/common/http';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Subject, of, throwError } from 'rxjs';
import { routes } from '../../../app.routes';
import { CatalogoComponent } from '../../adopciones/catalogo/catalogo/catalogo.component';
import { Login } from './login/login';
import { Registro } from './registro/registro';
import { AuthService } from './services/auth.service';
import { GoogleAuthService } from './services/google-auth.service';

@Component({ standalone: true, template: '' })
class DestinoDePrueba {}

it('muestra el catálogo público a un visitante sin consultar la sesión demo', async () => {
  TestBed.configureTestingModule({
    providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/catalogo', CatalogoComponent);
  const http = TestBed.inject(HttpTestingController);
  http.expectOne(req => req.url === '/api/v1/catalogo/mascotas')
    .flush({ elementos: [], limite: 20, pagina: 0, totalElementos: 0 });
  http.verify();
});

describe('Navegación entre ingreso, registro y módulo solicitado', () => {
  const sesion = { token: 'prueba', usuario: {
    id: 10, nombreCompleto: 'Persona de Prueba', email: 'persona@example.org',
    fechaNacimiento: '2000-01-01', rol: 'ADOPTANTE', activo: true,
  } };
  let credencial: Subject<string>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    credencial = new Subject();
    auth = jasmine.createSpyObj<AuthService>('Auth', [
      'iniciarSesion', 'registrar', 'guardarSesion', 'guardarUsuario',
    ]);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'login', component: Login }, { path: 'registro', component: Registro },
          { path: 'catalogo', component: DestinoDePrueba }, { path: 'mascotas', component: DestinoDePrueba },
        ]),
        { provide: AuthService, useValue: auth },
        { provide: GoogleAuthService, useValue: { credencial$: credencial, inicializarBoton: () => undefined } },
      ],
    });
    router = TestBed.inject(Router);
  });

  it('mantiene el destino solicitado al ingresar mediante Google', async () => {
    auth.iniciarSesion.and.returnValue(of(sesion));
    const harness = await RouterTestingHarness.create();
    const destino = '/catalogo?pagina=2#filtros';
    await harness.navigateByUrl('/login?returnUrl=' + encodeURIComponent(destino), Login);
    const enlace = harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.login__registro a')!;
    expect(new URL(enlace.href).searchParams.get('returnUrl')).toBe(destino);
    credencial.next('google-ficticio');
    await harness.fixture.whenStable();
    expect(router.url).toBe(destino);
  });

  it('transfiere el destino al registro cuando no existe la cuenta y lo recupera al completar el alta', async () => {
    auth.iniciarSesion.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    auth.registrar.and.returnValue(of(sesion));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login?returnUrl=%2Fcatalogo', Login);
    credencial.next('google-ficticio');
    await harness.fixture.whenStable();
    expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe('/catalogo');
    const registro = await harness.navigateByUrl(router.url, Registro);
    const enlace = harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.registro__login a')!;
    expect(new URL(enlace.href).searchParams.get('returnUrl')).toBe('/catalogo');
    registro.form.setValue({ nombreCompleto: 'Persona de Prueba', fechaNacimiento: '2000-01-01', rol: 'ADOPTANTE' });
    credencial.next('google-ficticio');
    await harness.fixture.whenStable();
    expect(router.url).toBe('/catalogo');
  });
});
