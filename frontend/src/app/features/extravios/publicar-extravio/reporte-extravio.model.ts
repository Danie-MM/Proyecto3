
/** Medios de contacto permitidos para publicar un reporte. */
export type MedioContacto = 'CORREO' | 'TELEFONO';

/** Estados de un reporte de extravío. */
export type EstadoReporteExtravio =
  | 'ACTIVO'
  | 'ENCONTRADA'
  | 'CANCELADO';

// ======================================================
// C-02: Publicación de reportes de extravío
// ======================================================

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

// ======================================================
// C-03: Consulta y actualización de reportes de extravío
// ======================================================

/** Información resumida para el listado de reportes activos. */
export interface ReporteExtravioResumen {
  id: string;
  mascotaId: string;
  nombreMascota: string | null;
  especie: string | null;
  fechaExtravio: string;
  estado: EstadoReporteExtravio;
  actualizadoEn: string;
}

/** Información permitida para consultar el detalle de un reporte. */
export interface ReporteExtravioDetalle {
  id: string;
  mascotaId: string;
  nombreMascota: string | null;
  especie: string | null;
  fechaExtravio: string;
  latitud: number;
  longitud: number;
  descripcion: string;
  tipoContacto: MedioContacto;
  contacto: string;
  estado: EstadoReporteExtravio;
  creadoEn: string;
  actualizadoEn: string;
  cerradoEn: string | null;
  puedeEditar: boolean;
  version: number;
}

/** Datos enviados al backend para actualizar parcialmente un reporte. */
export interface ActualizarReporteExtravio {
  fechaExtravio?: string;
  latitud?: number;
  longitud?: number;
  descripcion?: string;
  tipoContacto?: MedioContacto;
  contacto?: string;
  autorizaContacto?: boolean;
  motivoAdministrativo?: string;
  version: number;
}
