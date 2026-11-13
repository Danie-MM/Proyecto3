import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { MascotaData, PaginaMascotasData, FiltrosCatalogoData } from '../catalogointerface/CatalogoData';
import { map, Observable } from 'rxjs';

@Injectable ({
    providedIn: 'root',
})
export class CatalogoService {
    // crear las funciones para consumir los endpoints del backend

    private http = inject(HttpClient);

    readonly url = "/api/v1/mascotas"

    getMascotas(pagina: number = 0): Observable<MascotaData[]> {
        return this.http.get<PaginaMascotasData>(this.url, {
            params: { pagina, limite: 20 }
        }).pipe(
            map(respuesta => respuesta.elementos)
        );
    }

    getMascotasByFilter(
        filtros: FiltrosCatalogoData
    ): Observable<MascotaData[]> {
        return this.getAllMascotasDisponibles('DISPONIBLE').pipe(
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

    getAllMascotasDisponibles(disponible: string): Observable<MascotaData[]> {
        return this.getMascotas().pipe(
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
