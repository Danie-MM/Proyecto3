import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, map, Observable, of, tap } from 'rxjs';
import { FiltrosCatalogoData, MascotaData, PaginaMascotasData } from '../catalogointerface/CatalogoData';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
    private readonly http = inject(HttpClient);
    readonly url = '/api/v1/catalogo/mascotas';
    totalElementos = 0;
    limite = 20;

    consultarPagina(filtros: FiltrosCatalogoData, pagina = 0, limite = 20): Observable<PaginaMascotasData> {
        const params: Record<string, string | number> = { pagina, limite };
        const nombre = filtros.nombre?.trim();
        if (nombre) params['nombre'] = nombre;
        if (filtros.especie) params['especie'] = filtros.especie;
        if (filtros.edadEstimadaMeses !== undefined) {
            params['edadMinimaMeses'] = filtros.edadEstimadaMeses;
            params['edadMaximaMeses'] = filtros.edadEstimadaMeses;
        }
        if (filtros.tamano) params['tamanos'] = filtros.tamano;
        if (filtros.energia) params['energias'] = filtros.energia;
        return this.http.get<PaginaMascotasData>(this.url, { params });
    }

    async getAllMascotas(): Promise<MascotaData[]> {
        const mascotas: MascotaData[] = [];
        let pagina = 0;
        let totalPaginas: number;
        do {
            const respuesta = await firstValueFrom(this.consultarPagina({}, pagina, 100));
            mascotas.push(...respuesta.elementos);
            totalPaginas = Math.ceil(respuesta.totalElementos / respuesta.limite);
            pagina++;
        } while (pagina < totalPaginas);
        return mascotas;
    }

    getMascotas(pagina = 0, nombre = ''): Observable<MascotaData[]> {
        return this.getMascotasByFilter({ nombre }, pagina);
    }

    getMascotasByFilter(filtros: FiltrosCatalogoData, pagina = 0): Observable<MascotaData[]> {
        return this.consultarPagina(filtros, pagina).pipe(
            tap(respuesta => {
                this.totalElementos = respuesta.totalElementos;
                this.limite = respuesta.limite;
            }),
            map(respuesta => respuesta.elementos)
        );
    }

    getAllMascotasDisponibles(disponible: string, pagina = 0, nombre = ''): Observable<MascotaData[]> {
        return disponible === 'DISPONIBLE' ? this.getMascotas(pagina, nombre) : of([]);
    }

    getMascotasByEdad(edadEstimadaMeses: number): Observable<MascotaData[]> {
        return this.getMascotasByFilter({ edadEstimadaMeses });
    }

    getMascotasByEspecie(especie: string): Observable<MascotaData[]> {
        return this.getMascotasByFilter({ especie });
    }

    getMascotasByTamano(tamano: string): Observable<MascotaData[]> {
        return this.getMascotasByFilter({ tamano });
    }

    getMascotasByEnergia(energia: string): Observable<MascotaData[]> {
        return this.getMascotasByFilter({ energia });
    }
}
