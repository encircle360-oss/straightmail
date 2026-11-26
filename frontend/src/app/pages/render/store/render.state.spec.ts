import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { RenderState, RenderStateModel } from './render.state';
import { RenderActions } from './render.actions';
import { ApiService } from '../../../core/services/api.service';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('RenderState', () => {
  let store: Store;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([RenderState])],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ApiService,
          useValue: {
            render: vi.fn(),
          },
        },
      ],
    });

    store = TestBed.inject(Store);
    apiService = TestBed.inject(ApiService);
  });

  it('should have initial state', () => {
    const state: RenderStateModel = store.selectSnapshot((state) => state.render);
    expect(state.loading).toBe(false);
    expect(state.result).toBeNull();
  });

  it('should render and update state', () => {
    const mockResult = { html: '<p>Hi</p>', plain: 'Hi' };
    (apiService.render as any).mockReturnValue(of(mockResult));

    store.dispatch(new RenderActions.Render({ templateId: '1' }));

    const state = store.selectSnapshot((state) => state.render);
    expect(state.loading).toBe(false);
    expect(state.result).toEqual(mockResult);
    expect(apiService.render).toHaveBeenCalledWith({ templateId: '1' });
  });

  it('should store backend template error message on render failure', () => {
    const backendError = { error: { message: 'FreeMarker error: foo is undefined at line 1' } };
    (apiService.render as any).mockReturnValue(throwError(() => backendError));

    store.dispatch(new RenderActions.Render({ templateId: '1' }));

    const state = store.selectSnapshot((state) => state.render);
    expect(state.loading).toBe(false);
    expect(state.error).toBe('FreeMarker error: foo is undefined at line 1');
  });

  it('should store null as error when backend provides no error message', () => {
    (apiService.render as any).mockReturnValue(throwError(() => ({ message: 'Http failure' })));

    store.dispatch(new RenderActions.Render({ templateId: '1' }));

    const state = store.selectSnapshot((state) => state.render);
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('should save form values', () => {
    const values = { templateId: '1', model: '{}' };
    store.dispatch(new RenderActions.SaveForm(values));

    const state = store.selectSnapshot((state) => state.render);
    expect(state.formValues).toEqual(values);
  });

  it('should clear results', () => {
    store.dispatch(new RenderActions.Clear());
    const state = store.selectSnapshot((state) => state.render);
    expect(state.result).toBeNull();
    expect(state.error).toBeNull();
  });
});
