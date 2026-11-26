import { TestBed } from '@angular/core/testing';
import { TenantAvatarComponent } from './tenant-avatar.component';
import { TenantDTO } from '../../../../store/tenant/tenant.model';

const TENANT_WITH_LOGO: TenantDTO = {
  slug: 'acme',
  displayName: 'Acme Corp',
  logoUrl: 'https://example.com/logo.png',
  brandColor: '#3a86ff',
  smtpTls: false,
  smtpSsl: false,
  hasApiKey: false,
  active: true,
};

const TENANT_NO_LOGO: TenantDTO = {
  slug: 'demo',
  displayName: 'Demo Tenant',
  smtpTls: false,
  smtpSsl: false,
  hasApiKey: false,
  active: true,
};

describe('TenantAvatarComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TenantAvatarComponent],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_NO_LOGO);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render logo img when tenant has logoUrl', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_WITH_LOGO);
    fixture.detectChanges();
    const img = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(img).toBeTruthy();
    expect(img.src).toContain('logo.png');
    expect(img.alt).toBe('Acme Corp');
  });

  it('should render initials when tenant has no logoUrl', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_NO_LOGO);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.textContent.trim()).toBe('DT');
  });

  it('should render placeholder icon when tenant is null', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', null);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.tenant-avatar__placeholder-icon')).toBeTruthy();
  });

  it('should apply sm size class', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_NO_LOGO);
    fixture.componentRef.setInput('size', 'sm');
    fixture.detectChanges();
    expect(fixture.nativeElement.classList).toContain('tenant-avatar--sm');
  });

  it('should apply lg size class', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_NO_LOGO);
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();
    expect(fixture.nativeElement.classList).toContain('tenant-avatar--lg');
  });

  it('should use brandColor as background when set', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_WITH_LOGO);
    fixture.detectChanges();
    expect(fixture.componentInstance.bgColor()).toBe('#3a86ff');
  });

  it('should derive deterministic color when no brandColor', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', TENANT_NO_LOGO);
    fixture.detectChanges();
    const color = fixture.componentInstance.bgColor();
    expect(color).toMatch(/^hsl\(\d+, 60%, 42%\)$/);
  });

  it('should use gray background for null tenant', () => {
    const fixture = TestBed.createComponent(TenantAvatarComponent);
    fixture.componentRef.setInput('tenant', null);
    fixture.detectChanges();
    expect(fixture.componentInstance.bgColor()).toBe('hsl(0, 0%, 40%)');
  });
});
