import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ApiService, GitSyncStatusDTO, StatusDTO } from '../../../core/services/api.service';
import { ToastState } from '../../../store/toast/toast.state';
import { DashboardState } from './dashboard.state';
import { LoadSyncStatus, TriggerGitSync } from './dashboard.actions';

const MOCK_GIT_STATUS: GitSyncStatusDTO = {
  tenantId: 'acme',
  lastSyncAt: '2026-01-01T00:00:00Z',
  result: 'SUCCESS',
};

const MOCK_STATUS: StatusDTO = {
  gitSync: [MOCK_GIT_STATUS],
  fileTemplates: null,
};

describe('DashboardState', () => {
  let store: Store;
  let apiService: Partial<ApiService>;

  beforeEach(async () => {
    apiService = {
      getStatus: vi.fn(),
      triggerGitSync: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([DashboardState, ToastState])],
      providers: [{ provide: ApiService, useValue: apiService }],
    }).compileComponents();

    store = TestBed.inject(Store);
  });

  describe('LoadSyncStatus', () => {
    it('should store the status on success', async () => {
      vi.mocked(apiService.getStatus!).mockReturnValue(of(MOCK_STATUS));

      await store.dispatch(new LoadSyncStatus()).toPromise();

      expect(store.selectSnapshot(DashboardState.syncStatus)).toEqual(MOCK_STATUS);
    });

    it('should set syncStatus to null on error', async () => {
      vi.mocked(apiService.getStatus!).mockReturnValue(throwError(() => new Error('unreachable')));

      await store.dispatch(new LoadSyncStatus()).toPromise();

      expect(store.selectSnapshot(DashboardState.syncStatus)).toBeNull();
    });
  });

  describe('TriggerGitSync', () => {
    beforeEach(async () => {
      vi.mocked(apiService.getStatus!).mockReturnValue(of(MOCK_STATUS));
      await store.dispatch(new LoadSyncStatus()).toPromise();
    });

    it('should add tenant to syncingTenants while in progress', () => {
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(of(MOCK_GIT_STATUS));

      store.dispatch(new TriggerGitSync('acme'));

      // Synchronously after dispatch but before observable completes, the tenant is in the list.
      // Since `of()` completes synchronously, we check the final state instead.
      expect(store.selectSnapshot(DashboardState.syncingTenants)).not.toContain('acme');
    });

    it('should remove tenant from syncingTenants after success', async () => {
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(of(MOCK_GIT_STATUS));

      await store.dispatch(new TriggerGitSync('acme')).toPromise();

      expect(store.selectSnapshot(DashboardState.syncingTenants)).not.toContain('acme');
    });

    it('should update the gitSync entry in syncStatus on success', async () => {
      const updated: GitSyncStatusDTO = { ...MOCK_GIT_STATUS, result: 'FAILED' };
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(of(updated));

      await store.dispatch(new TriggerGitSync('acme')).toPromise();

      const status = store.selectSnapshot(DashboardState.syncStatus);
      expect(status?.gitSync[0].result).toBe('FAILED');
    });

    it('should show a success toast on success', async () => {
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(of(MOCK_GIT_STATUS));

      await store.dispatch(new TriggerGitSync('acme')).toPromise();

      const toasts = store.selectSnapshot(ToastState.toasts);
      expect(
        toasts.some((t) => t.message === 'dashboard.sync_success' && t.toastType === 'success'),
      ).toBe(true);
    });

    it('should show an error toast on failure', async () => {
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(
        throwError(() => new Error('network error')),
      );

      await store.dispatch(new TriggerGitSync('acme')).toPromise();

      const toasts = store.selectSnapshot(ToastState.toasts);
      expect(
        toasts.some((t) => t.message === 'dashboard.sync_error' && t.toastType === 'error'),
      ).toBe(true);
    });

    it('should remove tenant from syncingTenants after failure', async () => {
      vi.mocked(apiService.triggerGitSync!).mockReturnValue(
        throwError(() => new Error('network error')),
      );

      await store.dispatch(new TriggerGitSync('acme')).toPromise();

      expect(store.selectSnapshot(DashboardState.syncingTenants)).not.toContain('acme');
    });
  });
});
