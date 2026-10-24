import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngxs/store';
import { signal } from '@angular/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TenantFormComponent } from './tenant-form.component';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ApiService } from '../../../core/services/api.service';

describe('TenantFormComponent', () => {
  let component: TenantFormComponent;
  let confirmService: { confirm: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    confirmService = { confirm: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [TenantFormComponent, TranslatePipe],
      providers: [
        provideTranslateService(),
        { provide: ActivatedRoute, useValue: { snapshot: { data: {} } } },
        { provide: HttpClient, useValue: {} },
        {
          provide: Store,
          useValue: { selectSignal: vi.fn().mockReturnValue(signal('smtp')), dispatch: vi.fn() },
        },
        { provide: ApiService, useValue: { getStatus: vi.fn(), triggerGitSync: vi.fn() } },
        { provide: ConfirmService, useValue: confirmService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(TenantFormComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('canDeactivate()', () => {
    it('returns true when the form is pristine', () => {
      expect(component.form.dirty).toBe(false);
      expect(component.canDeactivate()).toBe(true);
    });

    it('returns true when already saved, even if form is dirty', () => {
      component.form.markAsDirty();
      // Access private field via type cast to simulate post-save state
      (component as any).saved = true;
      expect(component.canDeactivate()).toBe(true);
    });

    it('delegates to ConfirmService when form is dirty and not saved', () => {
      const expected = Promise.resolve(true);
      confirmService.confirm.mockReturnValue(expected);
      component.form.markAsDirty();

      const result = component.canDeactivate();

      expect(confirmService.confirm).toHaveBeenCalledWith('common.unsaved_changes', {
        confirmLabel: 'common.discard',
        variant: 'danger',
      });
      expect(result).toBe(expected);
    });

    it('delegates to ConfirmService when brand color is set via button', () => {
      const expected = Promise.resolve(true);
      confirmService.confirm.mockReturnValue(expected);
      component.form.patchValue({ brandColor: '#3a86ff' });
      component.form.controls.brandColor.markAsDirty();

      const result = component.canDeactivate();

      expect(confirmService.confirm).toHaveBeenCalledWith('common.unsaved_changes', {
        confirmLabel: 'common.discard',
        variant: 'danger',
      });
      expect(result).toBe(expected);
    });

    it('delegates to ConfirmService when brand color is cleared via button', () => {
      const expected = Promise.resolve(true);
      confirmService.confirm.mockReturnValue(expected);
      component.form.patchValue({ brandColor: null });
      component.form.controls.brandColor.markAsDirty();

      const result = component.canDeactivate();

      expect(confirmService.confirm).toHaveBeenCalledWith('common.unsaved_changes', {
        confirmLabel: 'common.discard',
        variant: 'danger',
      });
      expect(result).toBe(expected);
    });
  });
});
