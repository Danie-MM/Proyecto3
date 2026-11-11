import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MascotaDetalleComponent } from './mascota-detalle.component';
import { SesionMascotasService } from './sesion-mascotas.service';

describe('Privacidad del detalle al cambiar sesión', () => {
  let fixture: ComponentFixture<MascotaDetalleComponent>; let http: HttpTestingController; let sesion: SesionMascotasService;
  const mascota = { id: 'mascota', nombre: 'Nombre privado anterior', especie: 'GATO', edadEstimadaMeses: 8, tamano: 'PEQUENO', energia: 'MEDIA',
    comportamientos: [], comportamientoIds: [], version: 1, activa: true, disponibilidad: 'NO_DISPONIBLE', creadoEn: '2026-11-06T12:00:00Z' };
  beforeEach(async () => {
    const paramMap = convertToParamMap({ id: mascota.id });
    await TestBed.configureTestingModule({ imports: [MascotaDetalleComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap }, paramMap: of(paramMap) } }]
    }).compileComponents();
    fixture = TestBed.createComponent(MascotaDetalleComponent); http = TestBed.inject(HttpTestingController); sesion = TestBed.inject(SesionMascotasService);
    fixture.detectChanges(); http.expectOne('/api/v1/mascotas/mascota').flush(mascota); fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('borra el perfil anterior y no lo repone si falla la consulta de nuevos permisos', () => {
    expect(fixture.nativeElement.textContent).toContain('Nombre privado anterior'); sesion.cambios.next('invalidada'); fixture.detectChanges();
    expect(fixture.componentInstance.mascota).toBeNull(); expect(fixture.nativeElement.textContent).not.toContain('Nombre privado anterior'); http.expectNone(() => true);
    sesion.cambios.next('error'); fixture.detectChanges(); expect(fixture.componentInstance.cargando).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Nombre privado anterior'); expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('La sesión cambió');
    http.expectNone(() => true);
  });
});
