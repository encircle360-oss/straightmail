import { TestBed } from '@angular/core/testing';
import { FormErrorComponent } from './form-error.component';
import { FormControl, Validators } from '@angular/forms';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';

describe('FormErrorComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormErrorComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(FormErrorComponent);
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should not show errors if control is not touched', () => {
    const control = new FormControl('', Validators.required);
    const fixture = TestBed.createComponent(FormErrorComponent);
    fixture.componentRef.setInput('control', control);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.invalid-feedback')).toBeFalsy();
  });

  it('should show error if control is touched and invalid', () => {
    const control = new FormControl('', Validators.required);
    control.markAsTouched();
    const fixture = TestBed.createComponent(FormErrorComponent);
    fixture.componentRef.setInput('control', control);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.invalid-feedback')).toBeTruthy();
    expect(compiled.querySelector('.invalid-feedback')?.textContent).toContain('common.required');
  });

  it('should show custom error message if provided', () => {
    const control = new FormControl('', Validators.required);
    control.markAsTouched();
    const fixture = TestBed.createComponent(FormErrorComponent);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('errors', { required: 'custom.required' });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.invalid-feedback')?.textContent).toContain('custom.required');
  });
});
