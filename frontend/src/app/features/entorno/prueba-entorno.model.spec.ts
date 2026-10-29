import { longitudTexto, normalizarTexto, UUID_VALIDO } from './prueba-entorno.model';

describe('Validación compartida con Java', () => {
  it('cuenta un emoji como un punto de código y elimina los espacios de Java strip', () => {
    expect(longitudTexto(`\t${'🐾'.repeat(80)}\u3000`)).toBe(80);
    expect(normalizarTexto('\u2002 hola \n')).toBe('hola');
  });

  it('conserva espacios no separables que Java strip no elimina', () => {
    expect(normalizarTexto('\u00a0texto\u2007\u202f')).toBe('\u00a0texto\u2007\u202f');
    expect(longitudTexto('\u00a0')).toBe(1);
  });

  it('requiere los 36 caracteres del UUID en lugar del formato abreviado que Java permite', () => {
    expect(UUID_VALIDO.test('a47d3b84-75ce-4e61-9c35-e03137e60f99')).toBeTrue();
    expect(UUID_VALIDO.test('1-1-1-1-1')).toBeFalse();
    expect(UUID_VALIDO.test('../../otro-endpoint')).toBeFalse();
  });
});
