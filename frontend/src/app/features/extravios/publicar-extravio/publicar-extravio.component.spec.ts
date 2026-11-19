import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PublicarExtravioComponent } from './publicar-extravio.component';

describe('PublicarExtravioComponent', () => {
  let component: PublicarExtravioComponent;
  let fixture: ComponentFixture<PublicarExtravioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicarExtravioComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PublicarExtravioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
