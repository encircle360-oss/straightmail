import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccordionItemComponent } from './accordion-item.component';
import { NgxsModule, Store } from '@ngxs/store';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { UiState } from '../../../../store/ui/ui.state';

describe('AccordionItemComponent', () => {
  let component: AccordionItemComponent;
  let fixture: ComponentFixture<AccordionItemComponent>;
  let store: Store;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AccordionItemComponent,
        NgxsModule.forRoot([UiState]),
        TranslatePipe,
        NgbAccordionModule,
      ],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccordionItemComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);

    fixture.componentRef.setInput('stateKey', 'test');
    fixture.componentRef.setInput('label', 'Test Label');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the label', () => {
    const button = fixture.nativeElement.querySelector('.accordion-button');
    expect(button.textContent).toContain('Test Label');
  });
});
