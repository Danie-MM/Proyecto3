/** Unifica el origen local autorizado para Google sin alterar otros despliegues. */
export function urlLocalCanonica(href: string): string | null {
  const url = new URL(href);
  if (url.protocol !== 'http:' || url.hostname !== '127.0.0.1' || url.port !== '4200') {
    return null;
  }

  url.hostname = 'localhost';
  return url.href;
}
