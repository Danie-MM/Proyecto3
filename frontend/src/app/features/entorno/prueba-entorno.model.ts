export interface PruebaEntorno {
  id: string;
  texto: string;
  creadoEn: string;
}

export const UUID_VALIDO = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Equivale a String.strip() de Java 21; no elimina NBSP, U+2007 ni U+202F. */
export function normalizarTexto(texto: string): string {
  return texto.replace(/^[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]+|[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]+$/gu, '');
}

export function longitudTexto(texto: string): number {
  return [...normalizarTexto(texto)].length;
}
