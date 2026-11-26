import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Store } from '@ngxs/store';
import { firstValueFrom, Observable, of } from 'rxjs';

describe('authGuard', () => {
  let oidcSecurityService: any;
  let router: Router;

  beforeEach(() => {
    oidcSecurityService = {
      isAuthenticated$: of({ isAuthenticated: false }),
      authorize: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: OidcSecurityService, useValue: oidcSecurityService },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: Store, useValue: { selectSnapshot: vi.fn().mockReturnValue('test-api-key') } },
      ],
    });

    router = TestBed.inject(Router);
  });

  afterEach(() => {
    // Reset runtime config after each test
    delete (window as any).__runtimeConfig;
  });

  it('should return true if authMode is none', () => {
    (window as any).__runtimeConfig = { authMode: 'none' };
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(result).toBe(true);
  });

  it('should return true if authMode is api-key and key is stored', () => {
    (window as any).__runtimeConfig = { authMode: 'api-key' };
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(result).toBe(true);
  });

  it('should call authorize and return false if oidc and not authenticated', async () => {
    (window as any).__runtimeConfig = { authMode: 'oidc' };
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    if (result instanceof Observable || typeof (result as any).subscribe === 'function') {
      const val = await firstValueFrom(result as Observable<boolean>);
      expect(val).toBe(false);
      expect(oidcSecurityService.authorize).toHaveBeenCalled();
    }
  });

  it('should return true if oidc and authenticated', async () => {
    (window as any).__runtimeConfig = { authMode: 'oidc' };
    oidcSecurityService.isAuthenticated$ = of({ isAuthenticated: true });
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    const val = await firstValueFrom(result as Observable<boolean>);
    expect(val).toBe(true);
  });
});
