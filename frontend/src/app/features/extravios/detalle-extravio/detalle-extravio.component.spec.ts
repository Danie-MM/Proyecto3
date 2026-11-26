import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetalleExtravioComponent } from './detalle-extravio.component';

describe('DetalleExtravioComponent', () => {
  let component: DetalleExtravioComponent;
  let fixture: ComponentFixture<DetalleExtravioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalleExtravioComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DetalleExtravioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
