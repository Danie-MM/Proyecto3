import { DefaultUrlSerializer, PRIMARY_OUTLET, Router, UrlTree } from '@angular/router';

const serializer = new DefaultUrlSerializer();
const modulos = new Set(['mascotas', 'catalogo', 'mi-ubicacion', 'comprobar-entorno']);

/** Solo destinos del router de la aplicación, incluidos sus filtros y fragmentos. */
export function destinoTrasIngreso(raw: string | null): string {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) return '/mascotas';
  try {
    const tree = serializer.parse(raw);
    const principal = tree.root.children[PRIMARY_OUTLET];
    if (!principal || !modulos.has(principal.segments[0]?.path) ||
        Object.keys(tree.root.children).length !== 1 || principal.hasChildren() ||
        principal.segments.some(s => s.path === '.' || s.path === '..' || /[\\/]/.test(s.path))) {
      return '/mascotas';
    }
    return serializer.serialize(tree);
  } catch {
    return '/mascotas';
  }
}

export function urlIngreso(router: Router, returnUrl: string): UrlTree {
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: destinoTrasIngreso(returnUrl) } });
}
