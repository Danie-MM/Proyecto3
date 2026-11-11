import { IntentosMascotaService } from './intentos-mascota.service';

describe('Claves de reintento de mascotas', () => {
  const operacion = 'prueba-' + crypto.randomUUID();
  const actorA = '11111111-1111-4111-8111-111111111111';
  const actorB = '22222222-2222-4222-8222-222222222222';
  afterEach(() => {
    new IntentosMascotaService().completar(actorA, operacion);
    new IntentosMascotaService().completar(actorB, operacion);
  });
  it('recupera la intención después de recargar sin guardar el perfil', async () => {
    const datos = { nombre: 'Nombre privado ficticio', especie: 'GATO' };
    const clave = await new IntentosMascotaService().obtener(actorA, operacion, datos);
    expect(await new IntentosMascotaService().obtener(actorA, operacion, datos)).toBe(clave);
    const almacenado = sessionStorage.getItem('petmatch.b01.intento.' + actorA + '.' + operacion)!;
    expect(almacenado).not.toContain('Nombre privado ficticio'); expect(almacenado).not.toContain('GATO');
    expect(JSON.parse(almacenado).huella.length).toBe(64);
  });
  it('cambia de clave al modificar datos o iniciar una nueva creación confirmada', async () => {
    const intentos = new IntentosMascotaService();
    const original = await intentos.obtener(actorA, operacion, { edad: 8 });
    const modificada = await intentos.obtener(actorA, operacion, { edad: 9 });
    expect(modificada).not.toBe(original); intentos.completar(actorA, operacion);
    expect(await intentos.obtener(actorA, operacion, { edad: 9 })).not.toBe(modificada);
  });
  it('completar la creación de B no elimina la intención pendiente de A con los mismos datos', async () => {
    const datos = { nombre: 'Mascota de prueba', edad: 8 };
    const intentos = new IntentosMascotaService();
    // A pierde la respuesta, B completa la misma operación y A vuelve después de recargar.
    const pendienteA = await intentos.obtener(actorA, operacion, datos);
    const claveB = await intentos.obtener(actorB, operacion, datos);
    expect(claveB).not.toBe(pendienteA);
    intentos.completar(actorB, operacion);
    const trasRecarga = new IntentosMascotaService();
    expect(await trasRecarga.obtener(actorA, operacion, datos)).toBe(pendienteA);
    expect(await trasRecarga.obtener(actorB, operacion, datos)).not.toBe(claveB);
  });
});
