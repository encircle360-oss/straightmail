import { TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { FileSyncCardComponent } from './file-sync-card.component';

describe('FileSyncCardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileSyncCardComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();
  });

  it('should create when accessible', () => {
    const fixture = TestBed.createComponent(FileSyncCardComponent);
    fixture.componentRef.setInput('status', { accessible: true });
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should apply pip--up when accessible', () => {
    const fixture = TestBed.createComponent(FileSyncCardComponent);
    fixture.componentRef.setInput('status', { accessible: true });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.pip--up')).toBeTruthy();
    expect(el.querySelector('.pip--down')).toBeFalsy();
  });

  it('should apply pip--down when not accessible', () => {
    const fixture = TestBed.createComponent(FileSyncCardComponent);
    fixture.componentRef.setInput('status', { accessible: false });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.pip--down')).toBeTruthy();
    expect(el.querySelector('.pip--up')).toBeFalsy();
  });

  it('should show status-badge--up badge when accessible', () => {
    const fixture = TestBed.createComponent(FileSyncCardComponent);
    fixture.componentRef.setInput('status', { accessible: true });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.status-badge--up')).toBeTruthy();
  });

  it('should show status-badge--down badge when not accessible', () => {
    const fixture = TestBed.createComponent(FileSyncCardComponent);
    fixture.componentRef.setInput('status', { accessible: false });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.status-badge--down')).toBeTruthy();
  });
});
