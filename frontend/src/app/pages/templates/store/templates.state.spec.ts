import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { TemplatesState, TemplatesStateModel } from './templates.state';
import { TemplatesActions } from './templates.actions';
import { TenantActions } from '../../../store/tenant/tenant.actions';
import { ApiService } from '../../../core/services/api.service';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('TemplatesState', () => {
  let store: Store;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([TemplatesState])],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ApiService,
          useValue: {
            getTemplates: vi.fn(),
            createTemplate: vi.fn(),
            updateTemplate: vi.fn(),
            deleteTemplate: vi.fn(),
          },
        },
      ],
    });

    store = TestBed.inject(Store);
    apiService = TestBed.inject(ApiService);
  });

  it('should have initial state', () => {
    const state: TemplatesStateModel = store.selectSnapshot((state) => state.templates);
    expect(state.templates).toEqual([]);
    expect(state.loading).toBe(false);
    expect(state.currentPage).toBe(0);
    expect(state.pageSize).toBe(20);
    expect(state.activeTag).toBeUndefined();
    expect(state.activeSourceTab).toBe('ALL');
    expect(state.allTags).toEqual([]);
  });

  it('should load templates', () => {
    const mockResponse = {
      content: [{ id: '1', name: 'Test', tags: ['newsletter', 'source:database'] }] as any,
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    };
    (apiService.getTemplates as any).mockReturnValue(of(mockResponse));

    store.dispatch(new TemplatesActions.LoadTemplates(0, 20));

    const state = store.selectSnapshot((state) => state.templates);
    expect(state.templates).toEqual(mockResponse.content);
    expect(state.totalElements).toBe(1);
    expect(state.loading).toBe(false);
    expect(apiService.getTemplates).toHaveBeenCalledWith(0, 20, undefined);
    // allTags is updated from unfiltered load, source: tags excluded
    expect(state.allTags).toEqual(['newsletter']);
  });

  it('should not update allTags when a custom tag filter is active', () => {
    // First load without filter to populate allTags
    (apiService.getTemplates as any).mockReturnValue(
      of({ content: [{ id: '1', tags: ['newsletter', 'promo'] }], totalElements: 1 }),
    );
    store.dispatch(new TemplatesActions.LoadTemplates(0, 20));

    // Now activate a tag filter — this triggers a filtered load
    (apiService.getTemplates as any).mockReturnValue(
      of({ content: [{ id: '1', tags: ['newsletter'] }], totalElements: 1 }),
    );
    store.dispatch(new TemplatesActions.SetActiveTag('newsletter'));

    const state = store.selectSnapshot((state) => state.templates);
    // allTags must still contain both tags from the previous unfiltered load
    expect(state.allTags).toEqual(['newsletter', 'promo']);
  });

  it('should pass page, size and tags array to API', () => {
    (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

    store.dispatch(new TemplatesActions.LoadTemplates(2, 10, ['newsletter']));

    expect(apiService.getTemplates).toHaveBeenCalledWith(2, 10, ['newsletter']);
    const state = store.selectSnapshot((state) => state.templates);
    expect(state.currentPage).toBe(2);
    expect(state.pageSize).toBe(10);
  });

  it('should handle error on load', () => {
    (apiService.getTemplates as any).mockReturnValue(throwError(() => new Error('Load error')));

    store.dispatch(new TemplatesActions.LoadTemplates(0, 20));

    const state = store.selectSnapshot((state) => state.templates);
    expect(state.loading).toBe(false);
  });

  it('should create template and reload', () => {
    const newTemplate = { name: 'New', locale: 'en', tags: [] } as any;
    (apiService.createTemplate as any).mockReturnValue(of(newTemplate));
    (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

    store.dispatch(new TemplatesActions.CreateTemplate(newTemplate));

    expect(apiService.createTemplate).toHaveBeenCalledWith(newTemplate);
    expect(apiService.getTemplates).toHaveBeenCalled();
  });

  it('should update template and reload', () => {
    const updated = { id: '1', name: 'Updated', locale: 'en', tags: [] } as any;
    (apiService.updateTemplate as any).mockReturnValue(of(updated));
    (apiService.getTemplates as any).mockReturnValue(of({ content: [updated], totalElements: 1 }));

    store.dispatch(new TemplatesActions.UpdateTemplate('1', updated));

    expect(apiService.updateTemplate).toHaveBeenCalledWith('1', updated);
    expect(apiService.getTemplates).toHaveBeenCalled();
  });

  it('should delete template and reload', () => {
    (apiService.deleteTemplate as any).mockReturnValue(of(void 0));
    (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

    store.dispatch(new TemplatesActions.DeleteTemplate('1'));

    expect(apiService.deleteTemplate).toHaveBeenCalledWith('1');
    expect(apiService.getTemplates).toHaveBeenCalled();
  });

  it('should set active tag and reload with tags array', () => {
    (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

    store.dispatch(new TemplatesActions.SetActiveTag('tag1'));

    const state = store.selectSnapshot((state) => state.templates);
    expect(state.activeTag).toBe('tag1');
    expect(apiService.getTemplates).toHaveBeenCalledWith(0, 20, ['tag1']);
  });

  describe('SetActiveSourceTab', () => {
    it('should filter by source:database when switching to DATABASE tab', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveSourceTab('DATABASE'));

      const state = store.selectSnapshot((state) => state.templates);
      expect(state.activeSourceTab).toBe('DATABASE');
      expect(state.currentPage).toBe(0);
      expect(apiService.getTemplates).toHaveBeenCalledWith(0, 20, ['source:database']);
    });

    it('should filter by source:file when switching to FILE tab', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveSourceTab('FILE'));

      expect(apiService.getTemplates).toHaveBeenCalledWith(0, 20, ['source:file']);
    });

    it('should filter by source:git when switching to GIT tab', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveSourceTab('GIT'));

      expect(apiService.getTemplates).toHaveBeenCalledWith(0, 20, ['source:git']);
    });

    it('should load unfiltered when switching back to ALL tab with no custom tag', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveSourceTab('DATABASE'));
      store.dispatch(new TemplatesActions.SetActiveSourceTab('ALL'));

      const state = store.selectSnapshot((state) => state.templates);
      expect(state.activeSourceTab).toBe('ALL');
      expect(apiService.getTemplates).toHaveBeenLastCalledWith(0, 20, undefined);
    });

    it('should clear custom tag filter when switching to GIT tab', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveTag('marketing'));
      store.dispatch(new TemplatesActions.SetActiveSourceTab('GIT'));

      const state = store.selectSnapshot((state) => state.templates);
      expect(state.activeTag).toBeUndefined();
      expect(state.activeSourceTab).toBe('GIT');
      expect(apiService.getTemplates).toHaveBeenLastCalledWith(0, 20, ['source:git']);
    });

    it('should clear custom tag filter when switching to FILE tab', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveTag('newsletter'));
      store.dispatch(new TemplatesActions.SetActiveSourceTab('DATABASE'));
      store.dispatch(new TemplatesActions.SetActiveSourceTab('FILE'));

      const state = store.selectSnapshot((state) => state.templates);
      expect(state.activeTag).toBeUndefined();
      expect(state.activeSourceTab).toBe('FILE');
      expect(apiService.getTemplates).toHaveBeenLastCalledWith(0, 20, ['source:file']);
    });

    it('should preserve custom tag filter when switching between DATABASE and ALL tabs', () => {
      (apiService.getTemplates as any).mockReturnValue(of({ content: [], totalElements: 0 }));

      store.dispatch(new TemplatesActions.SetActiveTag('newsletter'));
      store.dispatch(new TemplatesActions.SetActiveSourceTab('DATABASE'));

      const state = store.selectSnapshot((state) => state.templates);
      expect(state.activeTag).toBe('newsletter');
      expect(state.activeSourceTab).toBe('DATABASE');
      expect(apiService.getTemplates).toHaveBeenLastCalledWith(0, 20, [
        'source:database',
        'newsletter',
      ]);
    });
  });

  it('should reset state on TenantActions.SelectTenant', () => {
    // Pre-load some templates
    const mockResponse = {
      content: [{ id: '1', name: 'Test', locale: 'en', tags: ['foo'] }] as any,
      totalElements: 1,
    };
    (apiService.getTemplates as any).mockReturnValue(of(mockResponse));
    store.dispatch(new TemplatesActions.LoadTemplates(0, 20));

    // Switch tenant
    store.dispatch(new TenantActions.SelectTenant('other-tenant'));

    const state = store.selectSnapshot((state) => state.templates);
    expect(state.templates).toEqual([]);
    expect(state.totalElements).toBe(0);
    expect(state.currentPage).toBe(0);
    expect(state.activeTag).toBeUndefined();
    expect(state.activeSourceTab).toBe('ALL');
    expect(state.allTags).toEqual([]);
  });
});
