import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ComprobarEntornoComponent } from './comprobar-entorno.component';

describe('Comprobación de entorno', () => {
  const ruta = '/api/v1/pruebas-entorno';
  const dato = { id: '86ddf395-a109-42b7-9754-b8ec35d28b71', texto: 'Prueba del equipo', creadoEn: '2026-11-02T18:00:00Z' };
  let fixture: ComponentFixture<ComprobarEntornoComponent>;
  let pantalla: ComprobarEntornoComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComprobarEntornoComponent], providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(ComprobarEntornoComponent);
    pantalla = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('guarda con POST, espera la confirmación y permite recuperar el mismo registro', () => {
    pantalla.texto = '  Prueba del equipo  ';
    pantalla.guardar();
    const alta = http.expectOne({ method: 'POST', url: ruta });
    expect(alta.request.body).toEqual({ texto: dato.texto });
    expect(pantalla.ocupado).toBeTrue();
    pantalla.guardar();
    pantalla.consultar();
    http.expectNone(() => true);
    alta.flush(dato, { status: 201, statusText: 'Creado' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.resultado').textContent).toContain('Mensaje guardado');
    expect(pantalla.id).toBe(dato.id);
    pantalla.consultar();
    expect(pantalla.registro).toBeNull();
    http.expectOne({ method: 'GET', url: ruta + '/' + dato.id }).flush(dato);
    fixture.detectChanges();
    const resultado = fixture.nativeElement.querySelector('.resultado').textContent;
    expect(resultado).toContain('Mensaje encontrado');
    expect(resultado).toContain(dato.texto);
    expect(resultado).toContain(dato.creadoEn);
  });

  it('valida el ID y los límites del texto antes de enviarlos', () => {
    pantalla.id = 'id-incompleto';
    pantalla.consultar();
    expect(pantalla.error).toContain('ID');
    for (const texto of ['   ', '🐾'.repeat(81)]) {
      pantalla.texto = texto;
      pantalla.guardar();
      expect(pantalla.error).toContain('80');
    }
    http.expectNone(() => true);
    pantalla.texto = '🐾'.repeat(80);
    pantalla.guardar();
    http.expectOne({ method: 'POST', url: ruta }).flush({ ...dato, texto: pantalla.texto }, { status: 201, statusText: 'Creado' });
    expect(pantalla.registro?.texto).toBe(pantalla.texto);
  });

  it('muestra un error al buscar un ID inexistente y deja libre el formulario', () => {
    pantalla.registro = dato;
    pantalla.id = dato.id;
    pantalla.consultar();
    http.expectOne(ruta + '/' + dato.id).flush({}, { status: 404, statusText: 'No encontrado' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('No encontramos');
    expect(fixture.nativeElement.querySelector('.resultado')).toBeNull();
    expect(pantalla.ocupado).toBeFalse();
  });

  it('rechaza una respuesta incompleta y permite reintentar tras un fallo de conexión', () => {
    pantalla.texto = dato.texto;
    pantalla.guardar();
    http.expectOne(ruta).flush({ texto: dato.texto }, { status: 201, statusText: 'Creado' });
    expect(pantalla.registro).toBeNull();
    expect(pantalla.error).toContain('No pudimos confirmar');
    pantalla.guardar();
    http.expectOne(ruta).error(new ProgressEvent('error'));
    expect(pantalla.ocupado).toBeFalse();
    expect(pantalla.registro).toBeNull();
  });
});
