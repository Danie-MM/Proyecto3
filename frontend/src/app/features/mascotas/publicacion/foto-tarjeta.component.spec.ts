import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { Mascota } from '../mascotas.model';
import { SesionMascotasService } from '../sesion-mascotas.service';
import { FotoTarjetaComponent } from './foto-tarjeta.component';
import { PublicacionMascotaApi } from './publicacion-mascota.api';

describe('Miniatura privada de la tarjeta de mascota', () => {
  const mascota: Mascota = {
    id: 'mascota-1', nombre: 'Luna', especie: 'PERRO', edadEstimadaMeses: 9, tamano: 'MEDIANO',
    energia: 'MEDIA', comportamientoIds: [], comportamientos: [], creadoEn: '2026-10-01',
    version: 1, responsableUsuarioId: 'actor-1', organizacionId: null,
    disponibilidad: 'NO_DISPONIBLE', activa: true, fotoUrl: null,
  };
  let fixture: ComponentFixture<FotoTarjetaComponent>;
  let leerFoto: jasmine.Spy;
  let cambios: Subject<'invalidada' | 'lista' | 'error'>;
  let respuestas: Subject<Blob>[];
  let crearUrl: jasmine.Spy;
  let liberarUrl: jasmine.Spy;

  beforeEach(() => {
    respuestas = [];
    leerFoto = jasmine.createSpy().and.callFake(() => {
      const respuesta = new Subject<Blob>();
      respuestas.push(respuesta);
      return respuesta.asObservable();
    });
    cambios = new Subject();
    crearUrl = spyOn(URL, 'createObjectURL').and.returnValues('blob:foto-1', 'blob:foto-2');
    liberarUrl = spyOn(URL, 'revokeObjectURL');
    TestBed.configureTestingModule({
      imports: [FotoTarjetaComponent],
      providers: [
        { provide: PublicacionMascotaApi, useValue: { leerFoto } },
        { provide: SesionMascotasService, useValue: { cambios } },
      ],
    });
    fixture = TestBed.createComponent(FotoTarjetaComponent);
  });

  function mostrar(datos: Mascota): void {
    fixture.componentRef.setInput('mascota', datos);
    fixture.detectChanges();
  }

  it('muestra una huella accesible y evita consultas si no hay foto', () => {
    mostrar(mascota);
    expect(leerFoto).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('[role=img]').getAttribute('aria-label')).toBe('Sin fotografía');
  });

  it('carga por ID, libera la URL al reemplazar la foto y cancela una lectura antigua', () => {
    mostrar({ ...mascota, fotoUrl: 'https://ejemplo.invalid/no-se-visita' });
    expect(leerFoto).toHaveBeenCalledOnceWith(mascota.id);
    respuestas[0].next(new Blob(['1']));
    fixture.detectChanges();
    const imagen: HTMLImageElement = fixture.nativeElement.querySelector('img');
    expect(imagen.getAttribute('src')).toBe('blob:foto-1');
    expect(imagen.alt).toBe('Foto de Luna');
    mostrar({ ...mascota, fotoUrl: '/foto', version: 2 });
    expect(liberarUrl).toHaveBeenCalledOnceWith('blob:foto-1');
    mostrar({ ...mascota, id: 'mascota-2', fotoUrl: '/foto' });
    respuestas[1].next(new Blob(['anterior']));
    expect(crearUrl).toHaveBeenCalledTimes(1);
    respuestas[2].next(new Blob(['2']));
    fixture.destroy();
    expect(liberarUrl).toHaveBeenCalledWith('blob:foto-2');
  });

  it('borra la foto al invalidarse la sesión y solo relee cuando los permisos están listos', () => {
    mostrar({ ...mascota, fotoUrl: '/foto' });
    respuestas[0].next(new Blob(['privado']));
    cambios.next('invalidada');
    fixture.detectChanges();
    expect(liberarUrl).toHaveBeenCalledOnceWith('blob:foto-1');
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    mostrar({ ...mascota, fotoUrl: '/foto', version: 4 });
    cambios.next('error');
    expect(leerFoto).toHaveBeenCalledTimes(1);
    cambios.next('lista');
    expect(leerFoto).toHaveBeenCalledTimes(2);
    respuestas[1].error(new Error('Acceso denegado'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=img]').getAttribute('aria-label')).toBe('Fotografía no disponible');
  });
});
