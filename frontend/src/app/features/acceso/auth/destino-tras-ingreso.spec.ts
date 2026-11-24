import { destinoTrasIngreso } from './destino-tras-ingreso';

describe('Destino de regreso después de la autenticación', () => {
  it('recupera el módulo, consulta y fragmento solicitados', () => {
    for (const ruta of ['/catalogo', '/mi-ubicacion', '/comprobar-entorno', '/mascotas/id/editar',
      '/mascotas?expediente=id&gestionar=1#vacunas']) {
      expect(destinoTrasIngreso(ruta)).toBe(ruta);
    }
  });

  it('elige Mis mascotas al iniciar desde cero o recibir un destino ajeno al router', () => {
    for (const ruta of [null, '', '/', '/login', '/registro', 'https://example.org', '//example.org',
      '/api/auth/login', '/catalogo/../login', '/catalogo/%2F', '/catalogo/%ZZ']) {
      expect(destinoTrasIngreso(ruta)).withContext(String(ruta)).toBe('/mascotas');
    }
  });
});
