import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ExpedienteMascotaComponent } from './expediente-mascota.component';
import { IntentosMascotaService } from '../intentos-mascota.service';
import { SesionMascotasService } from '../sesion-mascotas.service';

describe('Expediente privado de vacunas', () => {
  const base = '/api/v1/mascotas/mascota-1';
  const clave = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb';
  const beneficiario = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
  const pagina = { pagina: 0, limite: 20, totalElementos: 0, elementos: [] };
  let http: HttpTestingController;
  let fixture: ComponentFixture<ExpedienteMascotaComponent>;
  let vista: ExpedienteMascotaComponent;
  let intentos: IntentosMascotaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ExpedienteMascotaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    intentos = TestBed.inject(IntentosMascotaService);
    spyOn(intentos, 'obtener').and.resolveTo(clave);
    spyOn(intentos, 'completar');
    TestBed.inject(SesionMascotasService).contexto.next({ usuarioId: 'actor-1', organizaciones: [] });
    fixture = TestBed.createComponent(ExpedienteMascotaComponent);
    fixture.componentRef.setInput('mascotaId', 'mascota-1');
    vista = fixture.componentInstance;
  });
  afterEach(() => http.verify());

  function abrir(responsable = false) {
    fixture.componentRef.setInput('gestionaPermisos', responsable);
    fixture.detectChanges();
    http.expectOne(r => r.url === base + '/eventos-veterinarios').flush(pagina);
    if (responsable) http.expectOne(r => r.url === base + '/permisos-clinicos').flush(pagina);
    fixture.detectChanges();
  }
  function csrf() {
    flushMicrotasks();
    http.expectOne('/api/v1/sesion/csrf').flush({ token: 'csrf-prueba' });
  }

  it('rechaza mañana antes de la medianoche de Costa Rica aunque en UTC ya sea ese día', () => {
    abrir();
    spyOn(Date, 'now').and.returnValue(Date.parse('2026-11-19T05:59:59Z'));
    expect(vista.hoy).toBe('2026-11-18');
    vista.nombre = 'Vacuna de prueba';
    vista.fecha = '2026-11-19';
    vista.registrar();
    expect(vista.errorEscritura).toContain('fecha no puede ser futura');
    expect(intentos.obtener).not.toHaveBeenCalled();
  });

  it('distingue el historial vacío de un fallo y combina los filtros en la API', () => {
    abrir();
    expect(fixture.nativeElement.textContent).toContain('Todavía no hay vacunas');
    expect(fixture.nativeElement.textContent).not.toContain('Autorizar veterinario');
    vista.tipo = 'VACUNA';
    vista.desde = '2026-01-01';
    vista.hasta = '2026-11-01';
    vista.cargarVacunas();
    const req = http.expectOne(r => r.url === base + '/eventos-veterinarios');
    expect(req.request.params.get('desde')).toBe(vista.desde);
    expect(req.request.params.get('hasta')).toBe(vista.hasta);
    expect(req.request.params.get('tipo')).toBe('VACUNA');
    req.flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('no hay vacunas');
    vista.hasta = '2025-11-01';
    vista.cargarVacunas();
    expect(vista.errorVacunas).toContain('fecha inicial');
    http.expectNone(() => true);
  });

  it('conserva el intento de registro tras perder la respuesta y manda solo datos del evento', fakeAsync(() => {
    abrir();
    vista.fecha = '2025-01-01';
    vista.nombre = ' Antirrábica ';
    vista.registrar();
    csrf();
    let req = http.expectOne(base + '/eventos-veterinarios');
    const body = { tipo: 'VACUNA', fecha: '2025-01-01', nombre: 'Antirrábica', descripcion: null };
    expect(req.request.body).toEqual(body);
    expect(req.request.headers.get('Idempotency-Key')).toBe(clave);
    req.error(new ProgressEvent('error'));
    expect(intentos.completar).not.toHaveBeenCalled();
    expect(vista.nombre.trim()).toBe('Antirrábica');
    vista.registrar();
    csrf();
    req = http.expectOne(base + '/eventos-veterinarios');
    expect(req.request.headers.get('Idempotency-Key')).toBe(clave);
    req.flush({ id: 'vacuna-1' });
    http.expectOne(r => r.url === base + '/eventos-veterinarios').flush(pagina);
    expect(intentos.completar).toHaveBeenCalledOnceWith('actor-1', 'vacuna.mascota-1');
    expect(vista.nombre).toBe('');
  }));

  it('envía el alcance y revoca con motivo y versión; un conflicto exige releer', fakeAsync(() => {
    abrir(true);
    vista.beneficiario = beneficiario;
    vista.alcance = 'LECTURA_ESCRITURA';
    vista.conceder();
    csrf();
    const alta = http.expectOne(base + '/permisos-clinicos');
    expect(alta.request.body).toEqual({ beneficiarioUsuarioId: beneficiario,
      fundamento: 'VETERINARIO_AUTORIZADO', alcance: 'LECTURA_ESCRITURA' });
    alta.flush({});
    const permiso = { id: 'permiso-1', beneficiarioUsuarioId: beneficiario,
      alcance: 'LECTURA_ESCRITURA' as const, activo: true, version: 2, motivoRevocacion: null };
    http.expectOne(r => r.url === base + '/permisos-clinicos').flush({ ...pagina, elementos: [permiso], totalElementos: 1 });
    vista.permisoARevocar = permiso;
    vista.motivo = 'Fin de atención';
    vista.revocar();
    csrf();
    const baja = http.expectOne('/api/v1/permisos-clinicos/permiso-1/revocacion');
    expect(baja.request.body).toEqual({ motivo: 'Fin de atención', versionEsperada: 2 });
    baja.flush({ mensaje: 'Versión anterior' }, { status: 409, statusText: 'Conflicto' });
    http.expectOne(r => r.url === base + '/permisos-clinicos').flush(pagina);
    expect(vista.permisoARevocar).toBeNull();
    expect(vista.errorEscritura).toBe('Versión anterior');
  }));

  it('invalida el detalle y cancela solicitudes anteriores al cambiar de mascota', () => {
    abrir();
    vista.abrirDetalle('vacuna-1');
    const detalle = http.expectOne('/api/v1/eventos-veterinarios/vacuna-1');
    fixture.componentRef.setInput('mascotaId', 'mascota-2');
    fixture.detectChanges();
    expect(detalle.cancelled).toBeTrue();
    const lista = http.expectOne(r => r.url === '/api/v1/mascotas/mascota-2/eventos-veterinarios');
    lista.flush({}, { status: 404, statusText: 'No disponible' });
    expect(vista.detalle).toBeNull();
    expect(vista.vacunas).toBeNull();
  });

  it('descarta el detalle mostrado tras revocarse el acceso con un formulario abierto', fakeAsync(() => {
    abrir();
    vista.abrirDetalle('vacuna-1');
    http.expectOne('/api/v1/eventos-veterinarios/vacuna-1').flush({ id: 'vacuna-1', nombre: 'Privada' });
    vista.fecha = '2025-01-01';
    vista.nombre = 'Intento';
    vista.registrar();
    csrf();
    http.expectOne(base + '/eventos-veterinarios').flush({}, { status: 404, statusText: 'No disponible' });
    http.expectOne(r => r.url === base + '/eventos-veterinarios').flush({}, { status: 404, statusText: 'No disponible' });
    expect(vista.detalle).toBeNull();
    expect(vista.vacunas).toBeNull();
    expect(vista.errorEscritura).toContain('no tenés acceso');
  }));
});
