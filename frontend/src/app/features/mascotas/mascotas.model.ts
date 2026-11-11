/** Contratos del perfil privado: clínica y contactos pertenecen a rutas separadas. */
export interface Etiqueta { id: string; nombre: string; activo?: boolean; }
export interface Organizacion { id: string; nombre: string; }
export interface CatalogosMascota {
  especies: string[]; tamanos: string[]; energias: string[];
  comportamientos: Etiqueta[]; versionCatalogo: string;
}
export interface DatosMascota {
  nombre: string; especie: string; edadEstimadaMeses: number;
  tamano: string; energia: string; comportamientoIds: string[];
}
export interface CrearMascota extends DatosMascota { organizacionId: string | null; }
export interface EditarMascota extends DatosMascota { versionEsperada: number; }
export interface Mascota extends DatosMascota {
  id: string; creadoEn: string; version: number; responsableUsuarioId: string;
  organizacionId: string | null; disponibilidad: string; activa: boolean;
  comportamientos: Etiqueta[]; fotoUrl: string | null;
}
export interface PaginaMascotas { elementos: Mascota[]; pagina: number; limite: number; totalElementos: number; }
export interface ContextoMascotas { usuarioId: string; organizaciones: Organizacion[]; }
export interface ActorDemo { id: string; nombre: string; organizaciones: Organizacion[]; }
export interface SesionDemo { actores: ActorDemo[]; actorActualId: string | null; }

export function textoCatalogo(valor: string): string {
  const nombres: Record<string, string> = { PERRO: 'Perro', GATO: 'Gato', PEQUENO: 'Pequeño', MEDIANO: 'Mediano', GRANDE: 'Grande',
    BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta', NO_DISPONIBLE: 'No disponible', DISPONIBLE: 'Disponible', ADOPTADA: 'Adoptada' };
  return nombres[valor] ?? valor.replace(/_/g, ' ').toLocaleLowerCase('es');
}
