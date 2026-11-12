import { TestBed } from '@angular/core/testing';

import { PreferenciasUbicacionService } from './preferencias-ubicacion.service';

describe('PreferenciasUbicacionService', () => {
  let service: PreferenciasUbicacionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PreferenciasUbicacionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
