import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CatalogoService } from '../catalogoservice/CatalogoService';
import { FiltrosCatalogoData, MascotaData } from '../catalogointerface/CatalogoData';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent {

  catalogoService: CatalogoService = inject(CatalogoService);

  paginaActual = 0;
  totalPaginas = 0;
  cargando = false;
  errorCarga = '';

  allMascotas : MascotaData[] = [];
  catalogoList: MascotaData[] = [];
  limite = 20;
  filtros: FiltrosCatalogoData = {};

  private readonly changeDetectorReference = inject(ChangeDetectorRef);

  constructor() {
    this.cargarCatalogo();
  }
    async cargarCatalogo(): Promise<void> {
    this.cargando = true;
    this.errorCarga = '';

    try {
        this.allMascotas =
            await this.catalogoService.getAllMascotas();

        this.loadMascotas();
    } catch {
        this.errorCarga = 'No se pudo cargar el catálogo.';
    } finally {
        this.cargando = false;
        this.changeDetectorReference.markForCheck();
    }
  }

  loadMascotas(pagina: number = 0): void {
    const nombre = (this.filtros.nombre ?? '').trim().toLowerCase();

    const resultados = this.allMascotas.filter(mascota =>
        mascota.disponibilidad === 'DISPONIBLE' &&
        mascota.nombre.toLowerCase().includes(nombre) &&
        (!this.filtros.especie ||
            mascota.especie === this.filtros.especie) &&
        (this.filtros.edadEstimadaMeses === undefined ||
            mascota.edadEstimadaMeses === this.filtros.edadEstimadaMeses) &&
        (!this.filtros.tamano ||
            mascota.tamano === this.filtros.tamano) &&
        (!this.filtros.energia ||
            mascota.energia === this.filtros.energia)
    );

    this.totalPaginas = Math.ceil(resultados.length / this.limite);

    this.paginaActual = Math.max(
        0,
        Math.min(pagina, this.totalPaginas - 1)
    );

    const inicio = this.paginaActual * this.limite;

    this.catalogoList = resultados.slice(inicio, inicio + this.limite);
  }
}
