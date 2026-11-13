import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { MascotaData, PaginaMascotasData, FiltrosCatalogoData } from '../catalogointerface/CatalogoData';
import { map, Observable, tap, firstValueFrom } from 'rxjs';

@Injectable ({
    providedIn: 'root',
})
export class CatalogoService {

    private http = inject(HttpClient);

    readonly url = "/api/v1/mascotas"

    totalElementos = 0;
    limite = 20;

    async getAllMascotas(): Promise<MascotaData[]> {
    const mascotas: MascotaData[] = [];
    let pagina = 0;
    let totalPaginas = 1;

    while (pagina < totalPaginas) {
        const respuesta = await firstValueFrom(
            this.http.get<PaginaMascotasData>(this.url, {
                params: { pagina, limite: 100 }
            })
        );

        mascotas.push(...respuesta.elementos);

        totalPaginas = Math.ceil(
            respuesta.totalElementos / respuesta.limite
        );

        pagina++;
    }

    return mascotas;
}

    getMascotas(pagina: number = 0, nombre: string = ''): Observable<MascotaData[]> {
        return this.http.get<PaginaMascotasData>(this.url, {
            params: { pagina, limite: 20, nombre: nombre.trim()}
        }).pipe(
            tap(respuesta => {
                this.totalElementos = respuesta.totalElementos;
                this.limite = respuesta.limite;
            }),
            map(respuesta => respuesta.elementos)
        );
    }

    getMascotasByFilter(
        filtros: FiltrosCatalogoData,
        pagina: number = 0
    ): Observable<MascotaData[]> {
        return this.getAllMascotasDisponibles('DISPONIBLE', pagina, filtros.nombre).pipe(
            map(mascotas => {
                let resultado = mascotas;

                if(filtros.especie) {
                    resultado = resultado.filter(
                        mascota => mascota.especie === filtros.especie
                    );
                }

                if(filtros.edadEstimadaMeses !== undefined) {
                    resultado = resultado.filter(
                        mascota => mascota.edadEstimadaMeses === filtros.edadEstimadaMeses
                    );
                }

                if(filtros.tamano) {
                    resultado = resultado.filter(
                        mascota => mascota.tamano === filtros.tamano
                    );
                }

                if(filtros.energia) {
                    resultado = resultado.filter(
                        mascota => mascota.energia === filtros.energia
                    );
                }

                return resultado;

            })          
        );
    }

    getAllMascotasDisponibles(disponible: string, pagina: number = 0, nombre: string = ''): Observable<MascotaData[]> {
        return this.getMascotas(pagina, nombre).pipe(
            map(mascotas => mascotas.filter(mascota => mascota.disponibilidad === disponible))
        )
    }

    getMascotasByEdad(edadEstimadaMeses: number): Observable<MascotaData[]> {
        return this.getMascotas().pipe(
            map(mascotas => mascotas.filter(mascota => mascota.edadEstimadaMeses === edadEstimadaMeses))
        )
    }

    getMascotasByEspecie(especie: string): Observable<MascotaData[]> {
        return this.getMascotas().pipe(
            map(mascotas => mascotas.filter(mascota => mascota.especie ===  especie))
        )
    }

    getMascotasByTamano(tamano: string): Observable<MascotaData[]> {
        return this.getMascotas().pipe(
            map(mascotas => mascotas.filter(mascota => mascota.tamano ===  tamano))
        )
    }

    getMascotasByEnergia(energia: string): Observable<MascotaData[]> {
        return this.getMascotas().pipe(
            map(mascotas => mascotas.filter(mascota => mascota.energia ===  energia))
        )
    }

}
