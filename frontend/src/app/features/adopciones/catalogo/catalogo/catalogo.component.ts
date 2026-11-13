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

  // consumir las funciones del service para implementarlas en el html

  catalogoService: CatalogoService = inject(CatalogoService);
  catalogoList: MascotaData[] = [
    {
    id: 'mascota-1',
    nombre: 'Luna',
    edadEstimadaMeses: 12,
    especie: 'PERRO',
    creadoEn: '2026-11-12T12:00:00Z',
    version: 0,
    tamano: 'PEQUENO',
    energia: 'ALTA',
    responsableUsuarioId: 'usuario-prueba',
    organizacionId: 'organizacion-prueba',
    disponibilidad: 'DISPONIBLE',
    activa: true,
    comportamientos: 'Brusca pero juguetona',
    foto: '/assets/fotos_ejemplo/luna.jpg'
  },
  {
    id: 'mascota-2',
    nombre: 'Mía',
    edadEstimadaMeses: 24,
    especie: 'GATO',
    creadoEn: '2026-11-12T12:00:00Z',
    version: 0,
    tamano: 'MEDIANO',
    energia: 'BAJA',
    responsableUsuarioId: 'usuario-prueba',
    organizacionId: 'organizacion-prueba',
    disponibilidad: 'DISPONIBLE',
    activa: true,
    comportamientos: 'Calmada y bastante dócil',
    foto: '/assets/fotos_ejemplo/mia.jpg'
  }
  ];

  filtros: FiltrosCatalogoData = {};

  private readonly changeDetectorReference = inject(ChangeDetectorRef);

  constructor() {
    // this.loadMascotas();
  }

  loadMascotas() {
    this.catalogoService.getMascotasByFilter(this.filtros)
    .subscribe(mascotas =>{
      this.catalogoList = mascotas;
      this.changeDetectorReference.markForCheck();
    });
  }

}
