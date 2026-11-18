import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './features/acceso/auth/services/auth.service';

@Component({ standalone: true, template: '<p>Pantalla de prueba</p>' })
class VistaPrueba {}

describe('Menú común de módulos', () => {
  let token: string | null;
  const rutas = ['login', 'registro', 'mascotas', 'catalogo', 'mi-ubicacion', 'comprobar-entorno'];
  beforeEach(() => {
    token = null;
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter(rutas.map(path => ({ path, component: VistaPrueba }))),
        { provide: AuthService, useValue: { obtenerToken: () => token, cerrarSesion: () => token = null } },
      ],
    });
  });

  it('mantiene los cuatro enlaces al pasar por los módulos con cookie de demostración', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    for (const url of ['/mascotas', '/catalogo', '/mi-ubicacion', '/comprobar-entorno']) {
      await router.navigateByUrl(url);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const nav: HTMLElement = fixture.nativeElement.querySelector('nav');
      expect(Array.from(nav.querySelectorAll('a')).map(a => a.getAttribute('href'))).toEqual(
        ['/mascotas','/catalogo','/mi-ubicacion','/comprobar-entorno']);
      expect(nav.querySelector('[aria-current="page"]')?.getAttribute('href')).toBe(url);
      expect(fixture.nativeElement.textContent).toContain('Ingresar con Google');
      expect(fixture.nativeElement.querySelector('.cuenta').getAttribute('href')).toContain(
        'returnUrl=' + encodeURIComponent(url));
    }
  });

  it('cierra la cuenta desde Catálogo y oculta la navegación en el ingreso', async () => {
    token = 'sesion-de-prueba';
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    await router.navigateByUrl('/catalogo');
    fixture.detectChanges();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('.cuenta');
    expect(boton.textContent).toContain('Cerrar sesión');
    boton.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(token).toBeNull();
    expect(router.url).toBe('/login');
    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });
});
