export interface MascotaData {
    id: string;
    nombre: string;
    edadEstimadaMeses: number;
    especie: string;
    tamano: string;
    energia: string;
    disponibilidad: 'DISPONIBLE';
    organizacion: { id: string; nombre: string };
    comportamientos: { id: string; nombre: string }[];
    fotoUrl: string | null;
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
