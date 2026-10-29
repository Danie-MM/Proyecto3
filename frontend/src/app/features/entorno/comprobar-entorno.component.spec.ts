import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ComprobarEntornoComponent } from './comprobar-entorno.component';
import { PruebaEntorno } from './prueba-entorno.model';
import { PruebaEntornoService } from './prueba-entorno.service';

describe('ComprobarEntornoComponent', () => {
  let fixture: ComponentFixture<ComprobarEntornoComponent>;
  let componente: ComprobarEntornoComponent;
  let servicio: jasmine.SpyObj<PruebaEntornoService>;
  const registro: PruebaEntorno = { id: 'a47d3b84-75ce-4e61-9c35-e03137e60f99', texto: 'Hola, equipo', creadoEn: '2026-10-28T16:20:00Z' };

  beforeEach(async () => {
    servicio = jasmine.createSpyObj<PruebaEntornoService>('PruebaEntornoService', ['guardar', 'consultar']);
    await TestBed.configureTestingModule({
      imports: [ComprobarEntornoComponent],
      providers: [{ provide: PruebaEntornoService, useValue: servicio }]
    }).compileComponents();
    fixture = TestBed.createComponent(ComprobarEntornoComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('no manda solicitudes para texto vacío o de más de 80 puntos de código', () => {
    componente.texto = ' \n\t';
    componente.guardar();
    expect(componente.error).toContain('1 y 80');
    componente.texto = '🐾'.repeat(81);
    componente.guardar();
    expect(servicio.guardar).not.toHaveBeenCalled();
  });

  it('acepta 80 emojis, normaliza espacios y llena el ID con la respuesta', () => {
    const texto = '🐾'.repeat(80);
    servicio.guardar.and.returnValue(of({ ...registro, texto }));
    componente.texto = `  ${texto}  `;
    componente.guardar();
    expect(servicio.guardar).toHaveBeenCalledOnceWith(texto);
    expect(componente.id).toBe(registro.id);
    expect(componente.resultado?.texto).toBe(texto);
    expect(componente.ocupado).toBeFalse();
  });

  it('deshabilita ambas acciones y evita envíos duplicados mientras espera', () => {
    const pendiente = new Subject<PruebaEntorno>();
    servicio.guardar.and.returnValue(pendiente);
    componente.texto = registro.texto;
    componente.id = registro.id;
    componente.guardar();
    fixture.detectChanges();
    const botones = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    expect(botones.length).toBe(2);
    botones.forEach(boton => expect(boton.disabled).toBeTrue());
    componente.guardar();
    componente.consultar();
    expect(servicio.guardar).toHaveBeenCalledTimes(1);
    expect(servicio.consultar).not.toHaveBeenCalled();
    pendiente.next(registro);
    pendiente.complete();
    expect(componente.ocupado).toBeFalse();
  });

  it('valida el UUID antes de hacer una consulta y borra el resultado anterior', () => {
    componente.resultado = registro;
    componente.id = 'ID inválido';
    componente.consultar();
    expect(servicio.consultar).not.toHaveBeenCalled();
    expect(componente.resultado).toBeNull();
    expect(componente.error).toContain('UUID');
  });

  it('consulta el ID completo y presenta texto e instante UTC recibidos', () => {
    servicio.consultar.and.returnValue(of(registro));
    componente.id = ` ${registro.id} `;
    componente.consultar();
    fixture.detectChanges();
    expect(servicio.consultar).toHaveBeenCalledOnceWith(registro.id);
    expect(fixture.nativeElement.textContent).toContain(registro.texto);
    expect(fixture.nativeElement.textContent).toContain(registro.creadoEn);
    expect(componente.resultadoTitulo).toBe('Encontramos tu registro');
  });

  it('quita el éxito anterior y libera los controles cuando el servidor rechaza la consulta', () => {
    servicio.consultar.and.returnValue(throwError(() => new Error('No encontramos ese registro.')));
    componente.resultado = registro;
    componente.resultadoTitulo = 'Tu texto quedó guardado';
    componente.id = registro.id;
    componente.consultar();
    fixture.detectChanges();
    expect(componente.resultado).toBeNull();
    expect(componente.resultadoTitulo).toBe('');
    expect(componente.ocupado).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No encontramos ese registro');
    expect(fixture.nativeElement.querySelector('.result')).toBeNull();
  });
});
