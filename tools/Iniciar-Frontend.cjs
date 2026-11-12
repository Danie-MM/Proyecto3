// Inicia Angular con las versiones del equipo. No cambia instalaciones ni permisos globales.
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { createHash } = require('node:crypto');
const modo = process.argv[2] || 'start:local';
if (!['start:local', 'build', 'test'].includes(modo)) throw new Error('Modo permitido: start:local, build o test.');

const frontend = path.resolve(__dirname, '../frontend');
const carpetaNode = path.dirname(process.execPath);
const npmCli = path.join(carpetaNode, 'node_modules/npm/bin/npm-cli.js');

function detener(mensaje) {
  console.error(mensaje);
  process.exit(1);
}

// La tarea busca primero el Node preparado para PetMatchCR y despues el de PATH.
if (process.version !== 'v20.11.1') {
  detener(`Se necesita Node 20.11.1; se encontro ${process.version}. Consulta docs/EJECUTAR_EN_MI_PC.md.`);
}
if (!fs.existsSync(npmCli)) {
  detener('No se encontro npm junto a Node. Usa la distribucion completa de Node 20.11.1, que incluye npm 10.2.4.');
}

// Windows trata Path y PATH como la misma variable. Creamos una sola clave para los hijos.
// Este entorno solo se pasa a npm: no modifica el PATH de Windows ni el de otras aplicaciones.
const entradaPath = Object.entries(process.env).find(([nombre]) => nombre.toLowerCase() === 'path');
const entorno = Object.fromEntries(Object.entries(process.env).filter(([nombre]) => nombre.toLowerCase() !== 'path'));
entorno.Path = carpetaNode + path.delimiter + (entradaPath?.[1] ?? '');

function ejecutarNpm(argumentos, capturar = false) {
  // Se ejecuta el mismo node.exe con argumentos separados. No usamos un shell ni npm.ps1.
  const resultado = spawnSync(process.execPath, [npmCli, ...argumentos], {
    cwd: frontend,
    env: entorno,
    stdio: capturar ? ['ignore', 'pipe', 'pipe'] : 'inherit',
    encoding: 'utf8'
  });
  if (resultado.error) detener(`No se pudo ejecutar npm: ${resultado.error.message}`);
  if (resultado.status !== 0) {
    if (capturar && resultado.stderr) console.error(resultado.stderr.trim());
    process.exit(resultado.status ?? (resultado.signal === 'SIGINT' ? 130 : 1));
  }
  return resultado;
}

const versionNpm = ejecutarNpm(['--version'], true).stdout.trim();
if (versionNpm !== '10.2.4') {
  detener(`Se necesita npm 10.2.4; se encontro ${versionNpm}. No se instalaran dependencias con otra version.`);
}
if (!fs.existsSync(path.join(frontend, 'package-lock.json'))) {
  detener('Falta frontend/package-lock.json. Recuperalo del repositorio antes de instalar las dependencias.');
}

// node_modules pertenece a esta computadora. npm ci respeta las versiones del archivo compartido.
const dependenciasNecesarias = [
  'node_modules/@angular/core/package.json',
  'node_modules/@angular/cli/bin/ng.js',
  'node_modules/typescript/package.json',
  'node_modules/.bin/ng.cmd',
  'node_modules/leaflet/package.json',
  'node_modules/@types/leaflet/package.json'
];
const huellaLock = createHash('sha256').update(fs.readFileSync(path.join(frontend, 'package-lock.json'))).digest('hex');
const archivoHuella = path.join(frontend, 'node_modules/.petmatch-lock.sha256');
const huellaInstalada = fs.existsSync(archivoHuella) ? fs.readFileSync(archivoHuella, 'utf8').trim() : '';
if (huellaInstalada !== huellaLock || dependenciasNecesarias.some(archivo => !fs.existsSync(path.join(frontend, archivo)))) {
  console.log('Preparando las dependencias locales con npm ci...');
  ejecutarNpm(['ci']);
  fs.writeFileSync(archivoHuella, huellaLock);
}

console.log(`Node ${process.version} / npm ${versionNpm}`);
if (modo === 'start:local') console.log('Frontend: http://127.0.0.1:4200/login');
console.log('Backend esperado: http://127.0.0.1:8083. Para detener Angular usa Ctrl+C.');
// Angular 17 admite --prebundle=false: evita la cache Vite/deps que OneDrive puede bloquear.
// Solo afecta al servidor local. Angular sigue compilando y observando los cambios normalmente.
const argumentos = modo === 'test'
  ? ['run', 'test', '--', '--watch=false', '--browsers=ChromeHeadless', '--progress=false']
  : modo === 'start:local'
    ? ['run', 'start:local', '--', '--prebundle=false']
    : ['run', modo];
ejecutarNpm(argumentos);
