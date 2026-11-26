import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportesExtravioComponent } from './reportes-extravio.component';

describe('ReportesExtravioComponent', () => {
  let component: ReportesExtravioComponent;
  let fixture: ComponentFixture<ReportesExtravioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportesExtravioComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ReportesExtravioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
