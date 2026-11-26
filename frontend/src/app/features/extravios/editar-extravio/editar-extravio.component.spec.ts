import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditarExtravioComponent } from './editar-extravio.component';

describe('EditarExtravioComponent', () => {
  let component: EditarExtravioComponent;
  let fixture: ComponentFixture<EditarExtravioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditarExtravioComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EditarExtravioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
