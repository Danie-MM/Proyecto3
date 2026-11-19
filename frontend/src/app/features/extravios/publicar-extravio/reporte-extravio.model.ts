
/** Medios de contacto permitidos para publicar un reporte. */
export type MedioContacto = 'CORREO' | 'TELEFONO';

/** Estados de un reporte de extravío. */
export type EstadoReporteExtravio =
  | 'ACTIVO'
  | 'ENCONTRADA'
  | 'CANCELADO';

/** Datos enviados al backend para publicar un reporte. */
export interface CrearReporteExtravio {
  mascotaId: string;
  fechaExtravio: string;
  latitud: number;
  longitud: number;
  descripcion: string;
  tipoContacto: MedioContacto;
  contacto: string;
  autorizaContacto: boolean;
  confirmado: boolean;
}

/** Datos devueltos por el backend al publicar un reporte. */
export interface ReporteExtravio {
  id: string;
  mascotaId: string;
  creadoPorUsuarioId: string;
  fechaExtravio: string;
  latitud: number;
  longitud: number;
  radioMetros: number;
  descripcion: string;
  tipoContacto: MedioContacto;
  contacto: string;
  contactoAutorizadoEn: string;
  estado: EstadoReporteExtravio;
  actualizadoEn: string;
  creadoEn: string;
  version: number;
}
