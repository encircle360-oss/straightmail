import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { AuthState, AuthStateModel } from './auth.state';
import { CheckAuth, Login, Logout, SetUserData } from './auth.actions';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TenantState } from '../tenant/tenant.state';

describe('AuthState', () => {
  let store: Store;
  let oidcSecurityService: OidcSecurityService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([AuthState, TenantState])],
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            checkAuth: vi.fn(),
            authorize: vi.fn(),
            logoff: vi.fn(),
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    store = TestBed.inject(Store);
    oidcSecurityService = TestBed.inject(OidcSecurityService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should have initial state', () => {
    const state: AuthStateModel = store.selectSnapshot((state) => state.auth);
    expect(state.isAuthenticated).toBe(false);
    expect(state.userData).toBeNull();
    expect(state.roles).toEqual([]);
  });

  it('should check auth and update state', () => {
    const authResponse = {
      isAuthenticated: true,
      userData: { name: 'Test User' },
      accessToken: 'token123',
    };
    (oidcSecurityService.checkAuth as any).mockReturnValue(of(authResponse));

    store.dispatch(new CheckAuth());

    const state = store.selectSnapshot((state) => state.auth);
    expect(state.isAuthenticated).toBe(true);
    expect(state.userData).toEqual({ name: 'Test User' });
  });

  it('should not keep a copy of the access token', () => {
    // Silent renew replaces the token in the background, so a stored copy would go stale and
    // every request would 401 before it refreshed. The OIDC library owns the token.
    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({ isAuthenticated: true, userData: {}, accessToken: 'token123' }),
    );

    store.dispatch(new CheckAuth());

    const state = store.selectSnapshot((state) => state.auth);
    expect('accessToken' in state).toBe(false);
  });

  it('should call authorize on login', () => {
    store.dispatch(new Login());
    expect(oidcSecurityService.authorize).toHaveBeenCalled();
  });

  it('should clear auth and tenant state on logout', () => {
    (oidcSecurityService.logoff as any).mockReturnValue(of(null));
    store.dispatch(new Logout());

    const auth = store.selectSnapshot((state) => state.auth);
    expect(auth.isAuthenticated).toBe(false);
    expect(auth.userData).toBeNull();
    expect(auth.roles).toEqual([]);

    const tenant = store.selectSnapshot((state) => state.tenant);
    expect(tenant.selectedTenantId).toBeNull();
    expect(tenant.tenants).toEqual([]);
  });

  it('should set userData via SetUserData action', () => {
    const userData = { name: 'Jane Doe', email: 'jane@example.com' };
    store.dispatch(new SetUserData(userData));

    const state = store.selectSnapshot((state) => state.auth);
    expect(state.userData).toEqual(userData);
  });

  it('should extract roles from JWT realm_access', () => {
    // Encode payload with realm_access.roles
    const payload = { realm_access: { roles: ['ADMIN', 'USER'] } };
    const encoded = btoa(JSON.stringify(payload));
    const accessToken = `header.${encoded}.signature`;

    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({ isAuthenticated: true, userData: {}, accessToken }),
    );

    store.dispatch(new CheckAuth());

    const roles = store.selectSnapshot(AuthState.roles);
    expect(roles).toContain('ADMIN');
    expect(roles).toContain('USER');
  });

  it('should extract roles from userData if not in token', () => {
    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({
        isAuthenticated: true,
        userData: { roles: ['VIEWER'] },
        accessToken: null,
      }),
    );

    store.dispatch(new CheckAuth());

    const roles = store.selectSnapshot(AuthState.roles);
    expect(roles).toContain('VIEWER');
  });
});
