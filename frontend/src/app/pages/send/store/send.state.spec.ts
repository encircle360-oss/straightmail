import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { SendState, SendStateModel } from './send.state';
import { SendActions } from './send.actions';
import { ApiService } from '../../../core/services/api.service';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('SendState', () => {
  let store: Store;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([SendState])],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ApiService,
          useValue: {
            sendEmail: vi.fn(),
            sendInlineEmail: vi.fn(),
          },
        },
      ],
    });

    store = TestBed.inject(Store);
    apiService = TestBed.inject(ApiService);
  });

  it('should have initial state', () => {
    const state: SendStateModel = store.selectSnapshot((state) => state.send);
    expect(state.loading).toBe(false);
    expect(state.activeTab).toBe(1);
    expect(state.error).toBeNull();
    expect(state.lastResult).toBeNull();
  });

  it('should send email by template and clear loading', () => {
    (apiService.sendEmail as any).mockReturnValue(of(null));
    store.dispatch(
      new SendActions.SendByTemplate({
        recipients: ['test@your-domain.tld'],
        sender: 'sender@your-domain.tld',
        emailTemplateId: '123',
      }),
    );

    const state = store.selectSnapshot((state) => state.send);
    expect(state.loading).toBe(false);
    expect(state.lastResult).toBe(true);
    expect(apiService.sendEmail).toHaveBeenCalled();
  });

  it('should send inline email', () => {
    (apiService.sendInlineEmail as any).mockReturnValue(of(null));
    store.dispatch(
      new SendActions.SendInline({
        recipients: ['test@your-domain.tld'],
        sender: 'sender@your-domain.tld',
        subject: 'Hello',
        emailTemplate: '<p>Hello</p>',
      }),
    );

    const state = store.selectSnapshot((state) => state.send);
    expect(state.loading).toBe(false);
    expect(state.lastResult).toBe(true);
    expect(apiService.sendInlineEmail).toHaveBeenCalled();
  });

  describe('error handling for SendByTemplate', () => {
    it('should use backend message from HttpErrorResponse', () => {
      const httpError = new HttpErrorResponse({
        error: { message: 'Template not found' },
        status: 400,
      });
      (apiService.sendEmail as any).mockReturnValue(throwError(() => httpError));

      store.dispatch(
        new SendActions.SendByTemplate({
          recipients: ['test@your-domain.tld'],
          sender: 'sender@your-domain.tld',
          emailTemplateId: 'missing',
        }),
      );

      const state = store.selectSnapshot((state) => state.send);
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Template not found');
    });

    it('should fall back to err.message when no backend message is present', () => {
      const errorMessage = 'API Error';
      (apiService.sendEmail as any).mockReturnValue(throwError(() => new Error(errorMessage)));

      store.dispatch(
        new SendActions.SendByTemplate({
          recipients: ['test@your-domain.tld'],
          sender: 'sender@your-domain.tld',
          emailTemplateId: '123',
        }),
      );

      const state = store.selectSnapshot((state) => state.send);
      expect(state.loading).toBe(false);
      expect(state.error).toBe(errorMessage);
    });
  });

  describe('error handling for SendInline', () => {
    it('should use backend message from HttpErrorResponse', () => {
      const httpError = new HttpErrorResponse({
        error: { message: 'Error while rendering template' },
        status: 400,
      });
      (apiService.sendInlineEmail as any).mockReturnValue(throwError(() => httpError));

      store.dispatch(
        new SendActions.SendInline({
          recipients: ['test@your-domain.tld'],
          sender: 'sender@your-domain.tld',
          subject: 'Hello',
          emailTemplate: '<p>Hello</p>',
        }),
      );

      const state = store.selectSnapshot((state) => state.send);
      expect(state.error).toBe('Error while rendering template');
    });

    it('should fall back to err.message when no backend message is present', () => {
      const errorMessage = 'Inline Error';
      (apiService.sendInlineEmail as any).mockReturnValue(
        throwError(() => new Error(errorMessage)),
      );

      store.dispatch(
        new SendActions.SendInline({
          recipients: ['test@your-domain.tld'],
          sender: 'sender@your-domain.tld',
          subject: 'Hello',
          emailTemplate: '<p>Hello</p>',
        }),
      );

      const state = store.selectSnapshot((state) => state.send);
      expect(state.error).toBe(errorMessage);
    });
  });

  it('should update active tab', () => {
    store.dispatch(new SendActions.UpdateActiveTab(2));
    const state = store.selectSnapshot((state) => state.send);
    expect(state.activeTab).toBe(2);
  });

  it('should save form values for byId tab', () => {
    const values = { recipients: ['a@b.com'], emailTemplateId: '1' };
    store.dispatch(new SendActions.SaveForm('byId', values));

    const state = store.selectSnapshot((state) => state.send);
    expect(state.formById).toEqual(values);
  });

  it('should save form values for inline tab', () => {
    const values = { recipients: ['a@b.com'], subject: 'Hi' };
    store.dispatch(new SendActions.SaveForm('inline', values));

    const state = store.selectSnapshot((state) => state.send);
    expect(state.formInline).toEqual(values);
  });
});
