import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MascotaFormularioComponent } from './mascota-formulario.component';
import { Mascota } from './mascotas.model';
import { SesionMascotasService } from './sesion-mascotas.service';

describe('Edición de mascota', () => {
  const mascota: Mascota = { id: '11111111-1111-4111-8111-111111111111', creadoEn: '2026-11-06T12:00:00Z', version: 3,
    nombre: 'Luna', especie: 'GATO', edadEstimadaMeses: 8, tamano: 'PEQUENO', energia: 'MEDIA', comportamientoIds: ['etiqueta'],
    comportamientos: [{ id: 'etiqueta', nombre: 'Etiqueta de prueba' }], responsableUsuarioId: 'responsable', organizacionId: null,
    disponibilidad: 'NO_DISPONIBLE', activa: true, fotoUrl: null };
  const catalogos = { especies: ['PERRO', 'GATO'], tamanos: ['PEQUENO', 'MEDIANO', 'GRANDE'], energias: ['BAJA', 'MEDIA', 'ALTA'],
    comportamientos: [{ id: 'etiqueta', nombre: 'Etiqueta de prueba', activo: true }], versionCatalogo: '0.1' };
  let fixture: ComponentFixture<MascotaFormularioComponent>; let componente: MascotaFormularioComponent; let http: HttpTestingController;
  beforeEach(async () => {
    const paramMap = convertToParamMap({ id: mascota.id });
    await TestBed.configureTestingModule({ imports: [MascotaFormularioComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { data: { editar: true }, paramMap }, paramMap: of(paramMap) } }]
    }).compileComponents();
    fixture = TestBed.createComponent(MascotaFormularioComponent); componente = fixture.componentInstance; http = TestBed.inject(HttpTestingController);
    TestBed.inject(SesionMascotasService).contexto.next({ usuarioId: mascota.responsableUsuarioId, organizaciones: [] });
    fixture.detectChanges(); http.expectOne('/api/v1/catalogos/mascotas').flush(catalogos); http.expectOne('/api/v1/mascotas/' + mascota.id).flush(mascota); fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('conserva lo escrito ante conflicto y exige recargar explícitamente la versión', async () => {
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    componente.datos.edadEstimadaMeses = 9; await componente.guardar();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token' });
    const cambio = http.expectOne({ method: 'PUT', url: '/api/v1/mascotas/' + mascota.id });
    expect(cambio.request.body.versionEsperada).toBe(3); expect(cambio.request.body.responsableUsuarioId).toBeUndefined();
    expect(cambio.request.body.organizacionId).toBeUndefined(); expect(cambio.request.body.disponibilidad).toBeUndefined();
    cambio.flush({ codigo: 'VERSION_DESACTUALIZADA', mensaje: 'Conflicto' }, { status: 409, statusText: 'Conflict' }); fixture.detectChanges();
    expect(componente.datos.edadEstimadaMeses).toBe(9); expect(componente.original?.version).toBe(3);
    expect(componente.conflicto).toBeTrue(); expect(componente.ocupado).toBeFalse(); expect(navegar).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Recargar versión vigente'); http.expectNone(request => request.method === 'GET');
    componente.cargar(); http.expectOne('/api/v1/catalogos/mascotas').flush(catalogos);
    http.expectOne('/api/v1/mascotas/' + mascota.id).flush({ ...mascota, version: 4, edadEstimadaMeses: 10 });
    expect(componente.datos.edadEstimadaMeses).toBe(10); expect(componente.original?.version).toBe(4);
  });
  it('bloquea vacíos y edad fraccionaria antes de enviar', async () => {
    componente.datos.nombre = '   '; componente.datos.edadEstimadaMeses = 1.5; componente.datos.comportamientoIds = [];
    await componente.guardar(); http.expectNone(() => true);
    expect(componente.errores['nombre']).toBeTruthy(); expect(componente.errores['edadEstimadaMeses']).toBeTruthy(); expect(componente.errores['comportamientoIds']).toBeTruthy();
  });
  it('no usa el propietario del perfil como identidad si falta el contexto de sesión', async () => {
    TestBed.inject(SesionMascotasService).contexto.next(null);
    await componente.guardar();
    http.expectNone(() => true);
    expect(componente.error).toContain('verificar tu sesión');
    expect(componente.ocupado).toBeFalse();
  });
  it('cuenta caracteres Unicode y no simula éxito al fallar la persistencia', async () => {
    componente.datos.nombre = '🐈'.repeat(100); await componente.guardar();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'token' });
    http.expectOne('/api/v1/mascotas/' + mascota.id).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(componente.datos.nombre).toBe('🐈'.repeat(100)); expect(componente.ocupado).toBeFalse(); expect(componente.error).toContain('reintentá');
  });
});
