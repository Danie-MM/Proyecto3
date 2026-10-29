import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { PruebaEntorno } from './prueba-entorno.model';
import { PruebaEntornoService } from './prueba-entorno.service';

describe('PruebaEntornoService: contrato HTTP', () => {
  let servicio: PruebaEntornoService;
  let http: HttpTestingController;
  const ruta = '/api/v1/pruebas-entorno';
  const registro: PruebaEntorno = {
    id: 'a47d3b84-75ce-4e61-9c35-e03137e60f99',
    texto: 'Hola, equipo 🐾',
    creadoEn: '2026-10-28T16:20:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(PruebaEntornoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía texto normalizado y acepta una creación con estado 201', () => {
    const recibido = jasmine.createSpy('respuesta');
    servicio.guardar(`  ${registro.texto}\n`).subscribe(recibido);
    const solicitud = http.expectOne(ruta);
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({ texto: registro.texto });
    solicitud.flush(registro, { status: 201, statusText: 'Created' });
    expect(recibido).toHaveBeenCalledOnceWith(registro);
  });

  it('consulta el ID mediante GET y acepta estado 200', () => {
    const recibido = jasmine.createSpy('respuesta');
    servicio.consultar(registro.id).subscribe(recibido);
    const solicitud = http.expectOne(`${ruta}/${registro.id}`);
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(registro);
    expect(recibido).toHaveBeenCalledOnceWith(registro);
  });

  it('no muestra éxito cuando POST devuelve 200 en vez de 201', () => {
    let mensaje = '';
    servicio.guardar(registro.texto).subscribe({ next: () => fail('No debe confirmar la creación'), error: (error: Error) => mensaje = error.message });
    http.expectOne(ruta).flush(registro);
    expect(mensaje).toContain('respuesta inesperada');
  });

  it('rechaza una respuesta sin ID o fecha válidos', () => {
    let mensaje = '';
    servicio.consultar(registro.id).subscribe({ next: () => fail('No debe aceptar datos incompletos'), error: (error: Error) => mensaje = error.message });
    http.expectOne(`${ruta}/${registro.id}`).flush({ texto: registro.texto });
    expect(mensaje).toContain('respuesta inesperada');
  });

  [
    { estado: 400, codigo: 'VALIDACION', esperado: 'Revisá el texto o el ID' },
    { estado: 404, codigo: 'NO_ENCONTRADO', esperado: 'No encontramos ese registro' },
    { estado: 503, codigo: 'BD_NO_DISPONIBLE', esperado: 'La base de datos no está disponible' }
  ].forEach(caso => {
    it(`explica el error JSON ${caso.estado} sin volver a enviar la solicitud`, () => {
      let mensaje = '';
      servicio.consultar(registro.id).subscribe({ error: (error: Error) => mensaje = error.message });
      http.expectOne(`${ruta}/${registro.id}`).flush({ codigo: caso.codigo, mensaje: 'Detalle técnico', erroresCampos: [], trazaId: 'prueba' }, { status: caso.estado, statusText: 'Error' });
      expect(mensaje).toContain(caso.esperado);
      http.expectNone(`${ruta}/${registro.id}`);
    });
  });

  it('maneja HTML del proxy sin asumir que el error contiene JSON', () => {
    let mensaje = '';
    servicio.consultar(registro.id).subscribe({ error: (error: Error) => mensaje = error.message });
    http.expectOne(`${ruta}/${registro.id}`).flush('<html>Bad gateway</html>', { status: 502, statusText: 'Bad Gateway' });
    expect(mensaje).toContain('No pudimos conectar');
    expect(mensaje).not.toContain('<html>');
  });

  it('explica una desconexión de red', () => {
    let mensaje = '';
    servicio.consultar(registro.id).subscribe({ error: (error: Error) => mensaje = error.message });
    http.expectOne(`${ruta}/${registro.id}`).error(new ProgressEvent('error'));
    expect(mensaje).toContain('No pudimos conectar');
  });

  it('cancela una creación después de 10 segundos sin reenviarla ni afirmar que falló el guardado', fakeAsync(() => {
    let mensaje = '';
    servicio.guardar(registro.texto).subscribe({ error: (error: Error) => mensaje = error.message });
    const solicitud = http.expectOne(ruta);
    tick(9_999);
    expect(mensaje).toBe('');
    tick(1);
    expect(solicitud.cancelled).toBeTrue();
    expect(mensaje).toContain('podría haberse guardado');
    http.expectNone(ruta);
  }));

  it('permite recuperarse de una consulta que supera los 10 segundos', fakeAsync(() => {
    let mensaje = '';
    servicio.consultar(registro.id).subscribe({ error: (error: Error) => mensaje = error.message });
    const solicitud = http.expectOne(`${ruta}/${registro.id}`);
    tick(10_000);
    expect(solicitud.cancelled).toBeTrue();
    expect(mensaje).toContain('La consulta tardó demasiado');
  }));
});
