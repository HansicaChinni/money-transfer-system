import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransferComponentComponent } from './transfer-component.component';

describe('TransferComponentComponent', () => {
  let component: TransferComponentComponent;
  let fixture: ComponentFixture<TransferComponentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferComponentComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TransferComponentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
