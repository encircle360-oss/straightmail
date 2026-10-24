import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { adminGuard } from './admin.guard';
import { Store } from '@ngxs/store';

describe('adminGuard', () => {
  let router: Router;

  function setupWithRoles(roles: string[]) {
    router = { navigate: vi.fn() } as any;
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        {
          provide: Store,
          useValue: { selectSnapshot: vi.fn().mockReturnValue(roles) },
        },
      ],
    });
  }

  it('should allow access when user has ADMIN role', () => {
    setupWithRoles(['USER', 'ADMIN']);

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and navigate to / when user lacks ADMIN role', () => {
    setupWithRoles(['USER']);

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should deny access and navigate to / when roles are empty', () => {
    setupWithRoles([]);

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
