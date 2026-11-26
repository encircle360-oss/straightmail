import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { ToastState, ToastStateModel } from './toast.state';
import { RemoveToast, ShowToast } from './toast.actions';

describe('ToastState', () => {
  let store: Store;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([ToastState])],
    });

    store = TestBed.inject(Store);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should have initial state', () => {
    const state: ToastStateModel = store.selectSnapshot((state) => state.toast);
    expect(state.toasts).toEqual([]);
    expect(state.nextId).toBe(1);
  });

  it('should add toast and remove it after timeout', async () => {
    store.dispatch(new ShowToast('Hello', 'success'));

    let state = store.selectSnapshot((state) => state.toast);
    expect(state.toasts.length).toBe(1);
    expect(state.toasts[0].message).toBe('Hello');
    expect(state.nextId).toBe(2);

    vi.advanceTimersByTime(4000);

    state = store.selectSnapshot((state) => state.toast);
    expect(state.toasts.length).toBe(0);
  });

  it('should remove toast manually', () => {
    store.dispatch(new ShowToast('To remove', 'info'));
    const id = store.selectSnapshot((state) => state.toast.toasts[0].id);

    store.dispatch(new RemoveToast(id));

    const state = store.selectSnapshot((state) => state.toast);
    expect(state.toasts.length).toBe(0);
  });
});
