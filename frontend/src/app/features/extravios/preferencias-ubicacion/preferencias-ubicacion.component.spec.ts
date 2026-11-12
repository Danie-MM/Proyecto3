import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreferenciasUbicacionComponent } from './preferencias-ubicacion.component';

describe('PreferenciasUbicacionComponent', () => {
  let component: PreferenciasUbicacionComponent;
  let fixture: ComponentFixture<PreferenciasUbicacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreferenciasUbicacionComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PreferenciasUbicacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
