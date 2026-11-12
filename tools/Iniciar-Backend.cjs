// Arranque local reproducible. No instala Java ni Maven globalmente ni cambia politicas de Windows.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { spawnSync } = require('node:child_process');

const raiz = path.resolve(__dirname, '..');
const backend = path.join(raiz, 'backend');

function detener(mensaje) {
  console.error(mensaje);
  process.exit(1);
}

const modo = process.argv[2] || 'iniciar';
if (!['iniciar', 'verificar'].includes(modo) || process.argv.length > 3) detener('Modo permitido: iniciar o verificar.');

// Solo se leen variables de la aplicacion; .env nunca se ejecuta ni se imprime.
const permitidas = new Set([
  'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET', 'GOOGLE_CLIENT_ID',
  'TEST_DB_SCHEMA', 'TEST_DEMO_DB_SCHEMA',
]);
const configuracion = {};
const archivoEnv = path.join(raiz, '.env');
if (fs.existsSync(archivoEnv)) {
  const lineas = fs.readFileSync(archivoEnv, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/);
  for (const [indice, linea] of lineas.entries()) {
    const texto = linea.trim();
    if (!texto || texto.startsWith('#')) continue;
    const entrada = /^(?:export\s+)?([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$/.exec(texto);
    if (!entrada) detener(`Revisa el formato de .env en la linea ${indice + 1}; se espera NOMBRE=valor.`);
    if (!permitidas.has(entrada[1])) continue;
    let valor = entrada[2];
    if (valor.startsWith('"') || valor.startsWith("'")) {
      const comilla = valor[0];
      const cierre = valor.lastIndexOf(comilla);
      if (cierre === 0 || !/^(?:\s*#.*)?$/.test(valor.slice(cierre + 1))) {
        detener(`Revisa las comillas de .env en la linea ${indice + 1}.`);
      }
      valor = valor.slice(1, cierre);
    } else {
      valor = valor.replace(/\s+#.*$/, '').trim();
    }
    configuracion[entrada[1]] = valor;
  }
}

// Una variable explicitamente proporcionada al proceso tiene prioridad sobre .env.
const entorno = { ...process.env };
for (const nombre of permitidas) {
  if (entorno[nombre] === undefined && configuracion[nombre] !== undefined) {
    entorno[nombre] = configuracion[nombre];
  }
}
entorno.DB_URL ||= 'jdbc:postgresql://127.0.0.1:55432/petmatchcr';
entorno.DB_USERNAME ||= 'petmatch_app';
for (const nombre of ['DB_PASSWORD', 'JWT_SECRET', 'GOOGLE_CLIENT_ID']) {
  if (!entorno[nombre]?.trim()) detener(`Falta ${nombre}. Completa .env local; no publiques su contenido.`);
}
if (!/^[A-Za-z0-9+/]+={0,2}$/.test(entorno.JWT_SECRET)
    || Buffer.from(entorno.JWT_SECRET, 'base64').length < 32) {
  detener('JWT_SECRET debe ser una clave Base64 de al menos 32 bytes aleatorios. Revisa .env local.');
}

function esJava21(carpeta) {
  if (!carpeta) return false;
  try {
    const ejecutable = path.join(carpeta, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
    const version = fs.readFileSync(path.join(carpeta, 'release'), 'utf8');
    return fs.existsSync(ejecutable) && /^JAVA_VERSION="21(?:[.\-+"])/m.test(version);
  } catch {
    return false;
  }
}

const carpetaJdks = path.join(os.homedir(), '.jdks');
const candidatos = [entorno.JAVA_HOME, path.join(carpetaJdks, 'jdk-21.0.12.1+1')];
if (fs.existsSync(carpetaJdks)) {
  for (const entrada of fs.readdirSync(carpetaJdks, { withFileTypes: true })) {
    if (entrada.isDirectory()) candidatos.push(path.join(carpetaJdks, entrada.name));
  }
}
const java21 = candidatos.find(esJava21);
if (!java21) detener('No se encontro JDK 21. Configura JAVA_HOME con un JDK 21 o instalalo desde IntelliJ.');
entorno.JAVA_HOME = java21;

// El PATH solo cambia para este proceso; no altera Windows ni los otros proyectos.
const entradaPath = Object.entries(entorno).find(([nombre]) => nombre.toLowerCase() === 'path');
for (const nombre of Object.keys(entorno)) if (nombre.toLowerCase() === 'path') delete entorno[nombre];
entorno[process.platform === 'win32' ? 'Path' : 'PATH'] = path.join(java21, 'bin')
  + path.delimiter + (entradaPath?.[1] ?? '');

// Esta tarea utiliza el esquema independiente y no activa perfiles de identidad ficticia.
if (modo === 'iniciar') entorno.SPRING_PROFILES_ACTIVE = 'local,integracion';
else {
  delete entorno.SPRING_PROFILES_ACTIVE;
  entorno.TEST_DB_SCHEMA ||= 'pruebas_abc';
  entorno.TEST_DEMO_DB_SCHEMA ||= 'pruebas_demo_abc';
  for (const nombre of ['TEST_DB_SCHEMA', 'TEST_DEMO_DB_SCHEMA']) {
    if (!/^pruebas_[a-z0-9_]{1,55}$/.test(entorno[nombre])) {
      detener(`${nombre} debe empezar por pruebas_ y usar solo minusculas, numeros y guion bajo (maximo 63).`);
    }
  }
  if (entorno.TEST_DB_SCHEMA === entorno.TEST_DEMO_DB_SCHEMA) {
    detener('Las pruebas normales y de demostracion deben usar esquemas distintos.');
  }
}
entorno.SERVER_ADDRESS = '127.0.0.1';
entorno.SERVER_PORT = '8083';

// La salida de Maven queda fuera de OneDrive, sin mover el codigo abierto en el IDE.
// La misma ruta se utiliza en iniciar/verificar; distintas copias no comparten clases.
const rutaReal = fs.realpathSync(raiz);
const identidadRuta = process.platform === 'win32' ? rutaReal.toLowerCase() : rutaReal;
const idRepositorio = createHash('sha256').update(identidadRuta).digest('hex').slice(0, 16);
const baseSalida = process.platform === 'win32' && entorno.LOCALAPPDATA
  ? entorno.LOCALAPPDATA : os.tmpdir();
entorno.PETMATCH_BUILD_DIRECTORY = path.resolve(baseSalida, 'PetMatchCR', 'build', idRepositorio);

const wrapper = path.join(backend, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
if (!fs.existsSync(wrapper)) detener('Falta el Maven Wrapper en backend. Recuperalo del repositorio.');

console.log(modo === 'iniciar'
  ? 'Backend: http://127.0.0.1:8083 / perfiles local,integracion / esquema integracion_abc'
  : `Verificacion Maven: esquema ${entorno.TEST_DB_SCHEMA}; demo aislada ${entorno.TEST_DEMO_DB_SCHEMA}.`
    + ' Resultado en backend-verificacion.log');
console.log('Java 21 y Maven Wrapper del proyecto. Para detener, usa Ctrl+C.');
const opciones = { cwd: backend, env: entorno, stdio: 'inherit', windowsHide: true };
// El texto del comando Windows es fijo. Los secretos viajan solo en env, nunca en argumentos.
// Se usa el Wrapper oficial .cmd; no se ejecutan .ps1 del proyecto ni se cambia ExecutionPolicy.
const argumentosMaven = modo === 'iniciar'
  ? ['-B', '-ntp', 'clean', 'spring-boot:run']
  : ['-B', '-ntp', '-l', '../backend-verificacion.log', 'clean', 'verify'];
const resultado = process.platform === 'win32'
  ? spawnSync(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd ' + argumentosMaven.join(' ')],
      { ...opciones, windowsVerbatimArguments: true })
  : spawnSync(wrapper, argumentosMaven, opciones);
if (resultado.error) detener('No se pudo iniciar Maven Wrapper. Revisa Java 21 y el acceso a la carpeta del proyecto.');
process.exit(resultado.status ?? (resultado.signal === 'SIGINT' ? 130 : 1));
