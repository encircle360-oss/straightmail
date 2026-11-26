import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { TenantState, TenantStateModel } from './tenant.state';
import { TenantActions } from './tenant.actions';
import { TenantDTO } from './tenant.model';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';

const mockTenants: TenantDTO[] = [
  {
    slug: 'alpha',
    displayName: 'Alpha',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: true,
    active: true,
  },
  {
    slug: 'beta',
    displayName: 'Beta',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: false,
    active: true,
  },
];

describe('TenantState', () => {
  let store: Store;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([TenantState])],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    store = TestBed.inject(Store);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should have initial state', () => {
    const state: TenantStateModel = store.selectSnapshot((s) => s.tenant);
    expect(state.tenants).toEqual([]);
    expect(state.selectedTenantId).toBeNull();
  });

  it('should load tenants and auto-select first', () => {
    store.dispatch(new TenantActions.LoadTenants());

    const req = httpTesting.expectOne(`${environment.apiUrl}/v1/tenants/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTenants);

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.tenants).toEqual(mockTenants);
    expect(state.selectedTenantId).toBe('alpha');
  });

  it('should keep selectedTenantId when it is still valid after loading', () => {
    store.dispatch(new TenantActions.SelectTenant('beta'));
    store.dispatch(new TenantActions.LoadTenants());

    const req = httpTesting.expectOne(`${environment.apiUrl}/v1/tenants/me`);
    req.flush(mockTenants);

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.selectedTenantId).toBe('beta');
  });

  it('should reset selectedTenantId to first tenant when stale selection is no longer valid', () => {
    store.dispatch(new TenantActions.SelectTenant('old-tenant'));
    store.dispatch(new TenantActions.LoadTenants());

    const req = httpTesting.expectOne(`${environment.apiUrl}/v1/tenants/me`);
    req.flush(mockTenants);

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.selectedTenantId).toBe('alpha');
  });

  it('should select a tenant', () => {
    store.dispatch(new TenantActions.SelectTenant('beta'));

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.selectedTenantId).toBe('beta');
  });

  it('should clear tenants', () => {
    store.dispatch(new TenantActions.SetTenants(mockTenants));
    store.dispatch(new TenantActions.ClearTenants());

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.tenants).toEqual([]);
    expect(state.selectedTenantId).toBeNull();
  });

  it('should set tenants directly without API call', () => {
    store.dispatch(new TenantActions.SetTenants(mockTenants));

    httpTesting.expectNone(`${environment.apiUrl}/v1/tenants/me`);

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.tenants).toEqual(mockTenants);
    expect(state.selectedTenantId).toBe('alpha');
  });

  it('should not overwrite selectedTenantId on SetTenants if already set', () => {
    store.dispatch(new TenantActions.SelectTenant('beta'));
    store.dispatch(new TenantActions.SetTenants(mockTenants));

    const state = store.selectSnapshot((s) => s.tenant);
    expect(state.selectedTenantId).toBe('beta');
  });
});
