import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PreferenciasUbicacionComponent } from './preferencias-ubicacion.component';
import { PreferenciaUbicacion } from './preferencias-ubicacion.service';

describe('Preferencias de ubicación: guardado y recuperación', () => {
  const ruta = '/api/extravios/ubicacion';
  const ausente: PreferenciaUbicacion = {
    configurada: false, id: null, usuarioId: null, latitud: null, longitud: null,
    referencia: null, aceptaAvisos: false, consentimientoEn: null,
    actualizadaEn: null, creadoEn: null, version: null
  };
  const existente: PreferenciaUbicacion = {
    ...ausente, configurada: true, id: 'preferencia', usuarioId: 'usuario',
    latitud: 9.93, longitud: -84.09, referencia: 'Parque', version: 2
  };
  let fixture: ComponentFixture<PreferenciasUbicacionComponent>;
  let component: PreferenciasUbicacionComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreferenciasUbicacionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(PreferenciasUbicacionComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    // Estos casos prueban HTTP y formulario sin pedir imágenes al proveedor de mapas.
  });
  afterEach(() => { http.verify(); fixture.destroy(); });

  it('crea el componente con sus dependencias HTTP', () => {
    expect(component).toBeTruthy();
  });

  it('guarda una preferencia sin ubicación y conserva la versión devuelta', () => {
    component.cargarPreferencias();
    http.expectOne(ruta).flush(ausente);
    expect(component.existePreferencia).toBeFalse();
    component.guardarUbicacion();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-uno' });
    const alta = http.expectOne({ method: 'POST', url: ruta });
    expect(alta.request.body).toEqual({ latitud: null, longitud: null, referencia: '', aceptaAvisos: false });
    expect(alta.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-uno');
    expect(alta.request.headers.get('Idempotency-Key')).toBeTruthy();
    alta.flush({ ...ausente, configurada: true, version: 0 });
    expect(component.existePreferencia).toBeTrue();
    expect(component.version).toBe(0);
    expect(component.guardando).toBeFalse();
  });

  it('preserva lo escrito ante conflicto y exige recargar antes de sobrescribir', () => {
    component.cargarPreferencias();
    http.expectOne(ruta).flush(existente);
    component.referencia = 'Mi cambio pendiente';
    component.guardarUbicacion();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-dos' });
    const cambio = http.expectOne({ method: 'PUT', url: ruta });
    expect(cambio.request.body.versionEsperada).toBe(2);
    cambio.flush({ codigo: 'VERSION_OBSOLETA' }, { status: 409, statusText: 'Conflict' });
    expect(component.referencia).toBe('Mi cambio pendiente');
    expect(component.conflictoVersion).toBeTrue();
    component.guardarUbicacion();
    http.expectNone(() => true);
    component.cargarPreferencias();
    http.expectOne(ruta).flush({ ...existente, referencia: 'Cambio de otra pestaña', version: 3 });
    expect(component.conflictoVersion).toBeFalse();
    expect(component.version).toBe(3);
    expect(component.referencia).toBe('Cambio de otra pestaña');
  });

  it('permite retirar la ubicación desactivando avisos sin borrar la preferencia', () => {
    component.cargarPreferencias();
    http.expectOne(ruta).flush({ ...existente, aceptaAvisos: true });
    component.omitirUbicacion();
    expect(component.aceptaAvisos).toBeFalse();
    component.guardarUbicacion();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-tres' });
    const cambio = http.expectOne({ method: 'PUT', url: ruta });
    expect(cambio.request.body.latitud).toBeNull();
    expect(cambio.request.body.longitud).toBeNull();
    expect(cambio.request.body.aceptaAvisos).toBeFalse();
    expect(cambio.request.body.versionEsperada).toBe(2);
    cambio.flush({ ...existente, latitud: null, longitud: null, aceptaAvisos: false, version: 3 });
  });

  it('no interpreta una caída de consulta como ausencia ni escribe a ciegas', () => {
    component.cargarPreferencias();
    http.expectOne(ruta).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(component.errorCarga).toBeTrue();
    component.guardarUbicacion();
    http.expectNone(() => true);
  });

  it('no borra una versión modificada por otra sesión', () => {
    component.cargarPreferencias();
    http.expectOne(ruta).flush(existente);
    spyOn(window, 'confirm').and.returnValue(true);
    component.eliminarUbicacion();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-cuatro' });
    http.expectOne({ method: 'DELETE', url: ruta + '?versionEsperada=2' })
      .flush({}, { status: 409, statusText: 'Conflict' });
    expect(component.conflictoVersion).toBeTrue();
    expect(component.existePreferencia).toBeTrue();
    expect(component.referencia).toBe('Parque');
  });
});
