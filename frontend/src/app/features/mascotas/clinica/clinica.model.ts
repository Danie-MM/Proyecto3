export type AlcanceClinico = 'LECTURA' | 'LECTURA_ESCRITURA';
export interface EventoVacuna {
  id: string;
  tipo: 'VACUNA';
  nombre: string;
  fecha: string;
  descripcion: string | null;
  autorUsuarioId: string;
  origen: 'DECLARADO_RESPONSABLE' | 'REGISTRADO_VETERINARIO';
  creadoEn: string;
}
export interface PermisoClinico {
  id: string;
  beneficiarioUsuarioId: string;
  alcance: AlcanceClinico;
  activo: boolean;
  version: number;
  motivoRevocacion: string | null;
}
export interface PaginaClinica<T> {
  elementos: T[];
  pagina: number;
  limite: number;
  totalElementos: number;
}
