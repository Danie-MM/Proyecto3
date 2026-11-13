export interface MascotaData {
    // el contrato de mascotas
    id: string;
    nombre: string;
    edadEstimadaMeses: number;
    especie: string;
    creadoEn: string;
    version: number;
    tamano: string;
    energia: string;
    responsableUsuarioId: string;
    organizacionId: string;
    disponibilidad: string;
    activa: boolean;
    comportamientos: string;
    foto: string;
}

export interface PaginaMascotasData {
    // página que muestra las mascotas. muestra 20 elementos por página
    elementos: MascotaData[];
    pagina: number;
    limite: number;
    totalElementos: number;
}

export interface FiltrosCatalogoData {
    nombre?: string;
    especie?: string;
    edadEstimadaMeses?: number;
    tamano?: string;
    energia?: string;
}