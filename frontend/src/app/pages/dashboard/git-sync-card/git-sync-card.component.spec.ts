import { TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { GitSyncCardComponent } from './git-sync-card.component';
import { GitSyncStatusDTO } from '../../../core/services/api.service';
import { TenantDTO } from '../../../store/tenant/tenant.model';

const SUCCESS_STATUS: GitSyncStatusDTO = {
  tenantId: 'acme',
  lastSyncAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
  result: 'SUCCESS',
};

const FAILED_STATUS: GitSyncStatusDTO = {
  tenantId: 'demo',
  lastSyncAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  result: 'FAILED',
  errorMessage: 'Authentication failed',
};

const NEVER_STATUS: GitSyncStatusDTO = {
  tenantId: 'new-tenant',
  lastSyncAt: null,
  result: 'NEVER',
};

const ACME_TENANT: TenantDTO = {
  slug: 'acme',
  displayName: 'Acme Corp',
  smtpTls: false,
  smtpSsl: false,
  hasApiKey: false,
  active: true,
};

describe('GitSyncCardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GitSyncCardComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();
  });

  it('should create with SUCCESS status', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should not show tenant slug when no tenant DTO is provided', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.status-card__host')).toBeNull();
    expect(el.querySelector('.status-card__tenant')).toBeNull();
  });

  it('should show tenant displayName when tenant DTO is provided', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.componentRef.setInput('tenant', ACME_TENANT);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Acme Corp');
  });

  it('should show slug as secondary text when tenant DTO is provided', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.componentRef.setInput('tenant', ACME_TENANT);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const slugEl = el.querySelector('.status-card__tenant-slug') as HTMLElement;
    expect(slugEl?.textContent?.trim()).toBe('acme');
  });

  it('should apply pip--up class for SUCCESS', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.pip--up')).toBeTruthy();
  });

  it('should apply pip--down class for FAILED', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', FAILED_STATUS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.pip--down')).toBeTruthy();
  });

  it('should apply pip--checking class for NEVER', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', NEVER_STATUS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.pip--checking')).toBeTruthy();
  });

  it('should disable sync button when syncing is true', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.componentRef.setInput('syncing', true);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.btn-refresh') as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
  });

  it('should enable sync button when syncing is false', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.componentRef.setInput('syncing', false);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.btn-refresh') as HTMLButtonElement;
    expect(btn.disabled).toBe(false);
  });

  it('should emit sync event with tenantId when button is clicked', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();

    const emitted: string[] = [];
    fixture.componentInstance.sync.subscribe((id: string) => emitted.push(id));

    const btn = fixture.nativeElement.querySelector('.btn-refresh') as HTMLButtonElement;
    btn.click();

    expect(emitted).toEqual(['acme']);
  });

  it('should show sync button when canSync is true (default)', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.btn-refresh');
    expect(btn).toBeTruthy();
  });

  it('should hide sync button when canSync is false', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.componentRef.setInput('canSync', false);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.btn-refresh');
    expect(btn).toBeNull();
  });

  it('should show relative time for SUCCESS', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', SUCCESS_STATUS);
    fixture.detectChanges();
    // relativeTime should produce something like "2m ago"
    expect(fixture.componentInstance.relativeTime).toMatch(/\d+([smh]) ago/);
  });

  it('should return null relativeTime when lastSyncAt is null', () => {
    const fixture = TestBed.createComponent(GitSyncCardComponent);
    fixture.componentRef.setInput('syncStatus', NEVER_STATUS);
    fixture.detectChanges();
    expect(fixture.componentInstance.relativeTime).toBeNull();
  });
});
