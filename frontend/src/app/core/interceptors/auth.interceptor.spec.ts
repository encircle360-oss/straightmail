import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Subject, of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthState } from '../../store/auth/auth.state';
import { TenantState } from '../../store/tenant/tenant.state';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let store: Store;
  let router: { navigate: ReturnType<typeof vi.fn> };
  let oidc: {
    getAccessToken: ReturnType<typeof vi.fn>;
    forceRefreshSession: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    router = { navigate: vi.fn(), navigateByUrl: vi.fn() } as any;
    oidc = {
      getAccessToken: vi.fn(() => of('jwt-token')),
      forceRefreshSession: vi.fn(() =>
        of({ isAuthenticated: true, accessToken: 'refreshed-token' }),
      ),
    };

    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([AuthState, TenantState])],
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: OidcSecurityService, useValue: oidc },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    store = TestBed.inject(Store);
    store.reset({
      ...store.snapshot(),
      auth: { isAuthenticated: true, userData: null, roles: [] },
      tenant: { ...store.snapshot().tenant, selectedTenantId: 'acme' },
    });
  });

  afterEach(() => httpTesting.verify());

  it('should attach the bearer token on the API base path', () => {
    http.get('/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('/api/v1/templates');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('should read the token from the OIDC library, not from a stored copy', () => {
    oidc.getAccessToken.mockReturnValue(of('renewed-by-silent-refresh'));

    http.get('/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('/api/v1/templates');
    expect(req.request.headers.get('Authorization')).toBe('Bearer renewed-by-silent-refresh');
    req.flush({});
  });

  it('should not send the bearer token to a foreign origin with the same path', () => {
    // same path, different host — the token must not travel there
    http.get('https://attacker.example/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('https://attacker.example/api/v1/templates');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.has('X-Tenant-ID')).toBe(false);
    req.flush({});
  });

  it('should not modify requests outside the API base path', () => {
    http.get('/assets/i18n/en.json').subscribe();

    const req = httpTesting.expectOne('/assets/i18n/en.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should keep the tenant header when retrying after a refresh', () => {
    // Dropping X-Tenant-ID on the retry made every call after a token expiry fail with
    // "X-Tenant-ID header required" for users with more than one tenant claim.
    http.get('/api/v1/templates').subscribe();

    httpTesting
      .expectOne('/api/v1/templates')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    const retried = httpTesting.expectOne('/api/v1/templates');
    expect(retried.request.headers.get('Authorization')).toBe('Bearer refreshed-token');
    expect(retried.request.headers.get('X-Tenant-ID')).toBe('acme');
    retried.flush({});
  });

  it('should refresh only once for concurrent 401s', () => {
    // Every separate forceRefreshSession() rotates the refresh token, so parallel refreshes
    // invalidate each other and all but one caller lands on /unauthorized. The refresh is kept
    // pending here so both 401s land while it is still in flight, as they do in the browser.
    const refresh = new Subject<any>();
    oidc.forceRefreshSession.mockReturnValue(refresh);

    http.get('/api/v1/templates').subscribe();
    http.get('/api/v1/tenants').subscribe();

    httpTesting
      .expectOne('/api/v1/templates')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    httpTesting
      .expectOne('/api/v1/tenants')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(oidc.forceRefreshSession).toHaveBeenCalledTimes(1);

    refresh.next({ isAuthenticated: true, accessToken: 'refreshed-token' });
    refresh.complete();

    httpTesting.expectOne('/api/v1/templates').flush({});
    httpTesting.expectOne('/api/v1/tenants').flush({});
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should redirect to /unauthorized when the refresh fails', () => {
    oidc.forceRefreshSession.mockReturnValue(of({ isAuthenticated: false, accessToken: null }));

    http.get('/api/v1/templates').subscribe();

    httpTesting
      .expectOne('/api/v1/templates')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/unauthorized']);
  });
});
