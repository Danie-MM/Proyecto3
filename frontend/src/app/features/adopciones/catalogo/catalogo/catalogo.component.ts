import { ChangeDetectorRef, Component, inject, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { FiltrosCatalogoData, MascotaData } from '../catalogointerface/CatalogoData';
import { CatalogoService } from '../catalogoservice/CatalogoService';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent implements OnDestroy {
  private readonly catalogoService = inject(CatalogoService);
  private readonly cambios = inject(ChangeDetectorRef);
  private peticion?: Subscription;
  private filtrosAplicados: FiltrosCatalogoData = {};

  paginaActual = 0;
  totalPaginas = 0;
  cargando = false;
  errorCarga = '';
  catalogoList: MascotaData[] = [];
  filtros: FiltrosCatalogoData = {};

  constructor() {
    this.buscar();
  }

  buscar(): void {
    this.filtrosAplicados = { ...this.filtros };
    this.cargarPagina(0);
  }

  cargarPagina(pagina: number): void {
    this.peticion?.unsubscribe();
    this.cargando = true;
    this.errorCarga = '';
    this.peticion = this.catalogoService.consultarPagina(this.filtrosAplicados, pagina).subscribe({
      next: resultado => {
        this.catalogoList = resultado.elementos;
        this.paginaActual = resultado.pagina;
        this.totalPaginas = Math.ceil(resultado.totalElementos / resultado.limite);
        this.cargando = false;
        this.cambios.markForCheck();
      },
      error: () => {
        this.catalogoList = [];
        this.totalPaginas = 0;
        this.cargando = false;
        this.errorCarga = 'No se pudo cargar el catálogo.';
        this.cambios.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.peticion?.unsubscribe();
  }
}
