import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { Router } from '@angular/router';
import { apiKeyInterceptor } from './api-key.interceptor';
import { ApiKeyState } from '../../store/api-key/api-key.state';
import { TenantState } from '../../store/tenant/tenant.state';
import { SetApiKey } from '../../store/api-key/api-key.actions';
import { TenantActions } from '../../store/tenant/tenant.actions';

describe('apiKeyInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let store: Store;
  let router: any;

  beforeEach(() => {
    router = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([ApiKeyState, TenantState])],
      providers: [
        provideHttpClient(withInterceptors([apiKeyInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    store = TestBed.inject(Store);
  });

  afterEach(() => httpTesting.verify());

  it('should add X-API-KEY header to requests on the API base path', () => {
    store.dispatch(new SetApiKey('my-secret-key'));

    http.get('/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('/api/v1/templates');
    expect(req.request.headers.get('X-API-KEY')).toBe('my-secret-key');
    req.flush([]);
  });

  it('should add X-Tenant-ID header when a tenant is selected', () => {
    store.dispatch(new SetApiKey('my-secret-key'));
    store.dispatch(new TenantActions.SelectTenant('my-tenant'));

    http.get('/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('/api/v1/templates');
    expect(req.request.headers.get('X-Tenant-ID')).toBe('my-tenant');
    req.flush([]);
  });

  it('should not modify requests outside the API base path', () => {
    store.dispatch(new SetApiKey('my-secret-key'));

    http.get('/assets/i18n/en.json').subscribe();

    const req = httpTesting.expectOne('/assets/i18n/en.json');
    expect(req.request.headers.has('X-API-KEY')).toBe(false);
    req.flush({});
  });

  it('should not send the API key to a foreign origin with the same path', () => {
    store.dispatch(new SetApiKey('my-secret-key'));

    // same path, different host — the credential must not travel there
    http.get('https://attacker.example/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('https://attacker.example/api/v1/templates');
    expect(req.request.headers.has('X-API-KEY')).toBe(false);
    expect(req.request.headers.has('X-Tenant-ID')).toBe(false);
    req.flush({});
  });

  it('should navigate to /api-key-login on 401 response', () => {
    store.dispatch(new SetApiKey('my-secret-key'));

    http.get('/api/v1/templates').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/v1/templates');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/api-key-login']);
  });

  it('should clear API key on 401 response', () => {
    store.dispatch(new SetApiKey('my-secret-key'));

    http.get('/api/v1/templates').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/v1/templates');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    const apiKey = store.selectSnapshot(ApiKeyState.apiKey);
    expect(apiKey).toBeNull();
  });

  it('should NOT navigate to /api-key-login on 401 when no API key is stored', () => {
    http.get('/api/v1/tenants/me').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/v1/tenants/me');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should propagate 401 error to subscriber when no API key is stored', () => {
    let receivedStatus: number | undefined;

    http.get('/api/v1/tenants/me').subscribe({ error: (err) => (receivedStatus = err.status) });

    const req = httpTesting.expectOne('/api/v1/tenants/me');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(receivedStatus).toBe(401);
  });
});
