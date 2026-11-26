import { TestBed } from '@angular/core/testing';
import { TextFieldComponent } from './text-field.component';
import { FormControl } from '@angular/forms';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';

describe('TextFieldComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextFieldComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Test Label');
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display the label', () => {
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Email Address');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('label')?.textContent).toContain('Email Address');
  });

  it('should sync value from control', () => {
    const control = new FormControl('initial value');
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Test');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.value).toBe('initial value');

    control.setValue('new value');
    fixture.detectChanges();
    expect(input.value).toBe('new value');
  });

  it('should write user input back to the control and mark it dirty', () => {
    const control = new FormControl('');
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Test');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'typed by user';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(control.value).toBe('typed by user');
    expect(control.dirty).toBe(true);
  });

  it('should reflect the disabled state of the control', () => {
    const control = new FormControl({ value: 'locked', disabled: true });
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Test');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });
});
