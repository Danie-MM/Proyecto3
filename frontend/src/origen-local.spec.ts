import { urlLocalCanonica } from './origen-local';

describe('Origen local de acceso', () => {
  it('conserva la ruta de registro, los parametros codificados y el fragmento', () => {
    const ruta = '/registro?desde=login&destino=%2Fmascotas%3Fpagina%3D2&nombre=Jos%C3%A9#datos';
    expect(urlLocalCanonica('http://127.0.0.1:4200' + ruta))
      .toBe('http://localhost:4200' + ruta);
  });

  it('conserva una ruta de mascota abierta desde un marcador', () => {
    expect(urlLocalCanonica('http://127.0.0.1:4200/mascotas/123/editar'))
      .toBe('http://localhost:4200/mascotas/123/editar');
  });

  it('no vuelve a redirigir el destino localhost', () => {
    const destino = urlLocalCanonica('http://127.0.0.1:4200/login');
    expect(destino).not.toBeNull();
    expect(urlLocalCanonica(destino!)).toBeNull();
  });

  it('no modifica HTTPS ni otros puertos locales', () => {
    for (const origen of ['https://127.0.0.1:4200', 'http://127.0.0.1:4202',
      'http://127.0.0.1:8083', 'http://127.0.0.1']) {
      expect(urlLocalCanonica(origen + '/login')).withContext(origen).toBeNull();
    }
  });

  it('no redirige otros hosts aunque el puerto coincida', () => {
    for (const host of ['example.test', '127.0.0.1.example.test', '192.168.1.10', '[::1]']) {
      expect(urlLocalCanonica('http://' + host + ':4200/login')).withContext(host).toBeNull();
    }
  });
});
