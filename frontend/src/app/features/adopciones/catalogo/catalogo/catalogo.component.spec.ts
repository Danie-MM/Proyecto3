import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CatalogoComponent } from './catalogo.component';

describe('CatalogoComponent', () => {
  let component: CatalogoComponent;
  let fixture: ComponentFixture<CatalogoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogoComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CatalogoComponent);
    component = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(req => req.url === '/api/v1/mascotas').flush({
      elementos: [], pagina: 0, limite: 100, totalElementos: 0
    });
    await fixture.whenStable();
    http.verify();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
