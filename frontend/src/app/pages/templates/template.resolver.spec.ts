import { TestBed } from '@angular/core/testing';
import { Router, RouterStateSnapshot } from '@angular/router';
import { templateResolver } from './template.resolver';
import { ApiService, EmailTemplate } from '../../core/services/api.service';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';

describe('templateResolver', () => {
  let apiService: any;
  let router: any;

  beforeEach(() => {
    apiService = {
      getTemplate: vi.fn(),
    };
    router = {
      navigate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: ApiService, useValue: apiService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('should resolve template if found', async () => {
    const mockTemplate = { id: '1', name: 'Test' };
    apiService.getTemplate.mockReturnValue(of(mockTemplate));
    const route = { paramMap: { get: () => '1' } } as any;

    const result = TestBed.runInInjectionContext(() =>
      templateResolver(route, {} as RouterStateSnapshot),
    );
    const val = await firstValueFrom(result as Observable<EmailTemplate>);
    expect(val).toEqual(mockTemplate);
  });

  it('should navigate to /templates and return EMPTY if template not found', async () => {
    apiService.getTemplate.mockReturnValue(throwError(() => new Error('Not found')));
    const route = { paramMap: { get: () => '1' } } as any;

    const result = TestBed.runInInjectionContext(() =>
      templateResolver(route, {} as RouterStateSnapshot),
    );
    try {
      await firstValueFrom(result as Observable<EmailTemplate>);
    } catch {
      // EMPTY won't emit anything, so firstValueFrom will throw if it completes without emission
    }
    expect(router.navigate).toHaveBeenCalledWith(['/templates']);
  });
});
