// Prepara la configuracion local sin imprimir claves ni cambiar valores existentes.
const fs = require('node:fs');
const path = require('node:path');
const { randomBytes } = require('node:crypto');

function detener(mensaje) {
  console.error(mensaje);
  process.exit(1);
}

if (process.argv.length > 2) detener('Usa node tools/Preparar-Entorno.cjs sin argumentos.');

const raiz = path.resolve(__dirname, '..');
const archivoEnv = path.join(raiz, '.env');
const archivoEjemplo = path.join(raiz, '.env.example');
if (!fs.existsSync(archivoEjemplo)) detener('Falta .env.example en la raiz del repositorio.');

const ejemplo = fs.readFileSync(archivoEjemplo, 'utf8');
const existia = fs.existsSync(archivoEnv);
const original = existia ? fs.readFileSync(archivoEnv, 'utf8') : ejemplo;
const salto = original.includes('\r\n') ? '\r\n' : '\n';
const lineas = original.split(/\r?\n/);
let actualizado = false;

function completar(nombre, crearValor) {
  const patron = new RegExp(`^([ \\t]*(?:export[ \\t]+)?${nombre}[ \\t]*=[ \\t]*)(.*)$`);
  const entradas = lineas.map((linea, indice) => ({ indice, partes: patron.exec(linea) }))
    .filter(entrada => entrada.partes);
  if (entradas.length > 1) detener(`Revisa .env: ${nombre} aparece mas de una vez. No se modifico el archivo.`);

  if (entradas.length === 1) {
    const { indice, partes } = entradas[0];
    // Solo un valor ausente, vacio o entre comillas vacias se completa.
    if (!/^(?:(?:""|'')[ \t]*)?(?:#.*)?$/.test(partes[2].trim())) return;
    const comentario = partes[2].match(/#.*$/)?.[0];
    lineas[indice] = partes[1] + crearValor() + (comentario ? ' ' + comentario : '');
  } else {
    if (lineas.at(-1) === '') lineas.pop();
    lineas.push(`${nombre}=${crearValor()}`, '');
  }
  actualizado = true;
}

function clienteGoogleDelEquipo() {
  const constantes = path.join(raiz, 'frontend/src/app/features/acceso/auth/constants.ts');
  const contenido = fs.existsSync(constantes) ? fs.readFileSync(constantes, 'utf8') : '';
  const cliente = /\bGOOGLE_CLIENT_ID\s*=\s*['"]([^'"\r\n]+)['"]/.exec(contenido)?.[1]
    || /^GOOGLE_CLIENT_ID=([^\r\n]+)$/m.exec(ejemplo)?.[1].trim();
  if (!cliente || !/^[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$/.test(cliente)) {
    detener('No se encontro el identificador publico de Google del equipo. No se modifico .env.');
  }
  return cliente;
}

completar('JWT_SECRET', () => randomBytes(32).toString('base64'));
completar('GOOGLE_CLIENT_ID', clienteGoogleDelEquipo);

if (!existia || actualizado) {
  const resultado = lineas.join(salto);
  // wx evita reemplazar un archivo creado simultaneamente por otro proceso.
  fs.writeFileSync(archivoEnv, resultado, { encoding: 'utf8', flag: existia ? 'w' : 'wx', mode: 0o600 });
}

console.log(!existia ? 'Configuracion local creada en .env.'
  : actualizado ? 'Configuracion local completada; se conservaron los valores existentes.'
    : 'Configuracion local lista; no fue necesario cambiarla.');
