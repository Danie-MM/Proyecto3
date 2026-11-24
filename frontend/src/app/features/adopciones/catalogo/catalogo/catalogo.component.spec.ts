import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MascotaData } from '../catalogointerface/CatalogoData';
import { CatalogoComponent } from './catalogo.component';

describe('CatalogoComponent', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<CatalogoComponent>;
  const url = '/api/v1/catalogo/mascotas';
  const mascota: MascotaData = {
    id: 'mascota-1', nombre: 'Luna', especie: 'PERRO', edadEstimadaMeses: 8,
    tamano: 'MEDIANO', energia: 'MEDIA', disponibilidad: 'DISPONIBLE',
    organizacion: { id: 'org-1', nombre: 'Refugio de prueba' },
    comportamientos: [{ id: 'etiqueta-1', nombre: 'Sociable' }],
    fotoUrl: '/api/v1/mascotas/mascota-1/foto/publica'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogoComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CatalogoComponent);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('muestra la foto pública y las etiquetas de la respuesta; omite la imagen si falta', () => {
    const inicial = http.expectOne(req => req.url === url);
    expect(inicial.request.method).toBe('GET');
    expect(inicial.request.params.get('pagina')).toBe('0');
    expect(inicial.request.params.get('limite')).toBe('20');
    inicial.flush({
      elementos: [mascota, { ...mascota, id: 'otra', nombre: 'Milo', fotoUrl: null, comportamientos: [] }],
      pagina: 0, limite: 20, totalElementos: 2
    });
    fixture.detectChanges();
    const tarjetas = fixture.nativeElement.querySelectorAll('.tarjeta-mascota');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].querySelector('img').getAttribute('src')).toBe(mascota.fotoUrl);
    expect(tarjetas[0].textContent).toContain('Sociable');
    expect(tarjetas[1].querySelector('img')).toBeNull();
    http.expectNone(req => req.url === '/api/v1/mascotas');
  });

  it('cancela la consulta anterior y conserva los filtros aplicados al paginar', () => {
    const inicial = http.expectOne(req => req.url === url);
    const componente = fixture.componentInstance;
    componente.filtros = { nombre: '  Luna ', especie: 'PERRO', edadEstimadaMeses: 0, tamano: 'MEDIANO', energia: 'MEDIA' };
    componente.buscar();
    expect(inicial.cancelled).toBeTrue();
    const busqueda = http.expectOne(req => req.url === url);
    const params = busqueda.request.params;
    expect(params.get('nombre')).toBe('Luna');
    expect(params.get('especie')).toBe('PERRO');
    expect(params.get('edadMinimaMeses')).toBe('0');
    expect(params.get('edadMaximaMeses')).toBe('0');
    expect(params.get('tamanos')).toBe('MEDIANO');
    expect(params.get('energias')).toBe('MEDIA');
    busqueda.flush({ elementos: [mascota], limite: 20, pagina: 0, totalElementos: 21 });
    fixture.detectChanges();
    expect(componente.totalPaginas).toBe(2);
    componente.filtros.nombre = 'otro borrador';
    const siguiente: HTMLButtonElement = fixture.nativeElement.querySelector('nav[aria-label="Páginas del catálogo"] button:last-child');
    siguiente.click();
    const otraPagina = http.expectOne(req => req.url === url);
    expect(otraPagina.request.params.get('pagina')).toBe('1');
    expect(otraPagina.request.params.get('nombre')).toBe('Luna');
    otraPagina.flush({ elementos: [mascota], limite: 20, pagina: 1, totalElementos: 21 });
    expect(componente.paginaActual).toBe(1);
  });

  it('distingue una consulta fallida de una búsqueda sin resultados', () => {
    http.expectOne(req => req.url === url).flush({}, { status: 503, statusText: 'No disponible' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No se pudo cargar');
    expect(fixture.nativeElement.textContent).not.toContain('No hay mascotas para mostrar');
    fixture.componentInstance.buscar();
    http.expectOne(req => req.url === url).flush({ elementos: [], limite: 20, pagina: 0, totalElementos: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No hay mascotas para mostrar');
  });
});
