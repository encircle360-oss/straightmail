import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TemplateTableComponent } from './template-table.component';
import { EmailTemplate } from '../../../core/services/api.service';

const TEMPLATE_FIXTURE: EmailTemplate = {
  id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  name: 'Welcome Email',
  locale: 'en',
  tags: ['source:database', 'onboarding', 'welcome', 'transactional'],
  source: 'DATABASE',
  editable: true,
};

describe('TemplateTableComponent', () => {
  let component: TemplateTableComponent;
  let fixture: ComponentFixture<TemplateTableComponent>;

  beforeEach(async () => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    });

    await TestBed.configureTestingModule({
      imports: [TemplateTableComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateTableComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('templates', signal([TEMPLATE_FIXTURE]));
    fixture.componentRef.setInput('loading', signal(false));
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('shortId', () => {
    it('returns exactly 8 characters', () => {
      expect(component.shortId('a1b2c3d4-e5f6-7890-abcd-ef1234567890')).toBe('a1b2c3d4');
      expect(component.shortId('a1b2c3d4-e5f6-7890-abcd-ef1234567890').length).toBe(8);
    });
  });

  describe('sortedTags', () => {
    it('returns all tags unchanged when no source tags are present', () => {
      const result = component.sortedTags(['onboarding', 'welcome']);
      expect(result).toEqual(['onboarding', 'welcome']);
    });

    it('sorts source tags to the front', () => {
      const result = component.sortedTags(['onboarding', 'source:database', 'welcome']);
      expect(result[0]).toBe('source:database');
    });

    it('returns an empty array for empty input', () => {
      expect(component.sortedTags([])).toEqual([]);
    });
  });

  describe('hiddenTagCount', () => {
    it('returns 0 when 2 or fewer tags', () => {
      expect(component.hiddenTagCount([])).toBe(0);
      expect(component.hiddenTagCount(['source:database'])).toBe(0);
      expect(component.hiddenTagCount(['source:database', 'onboarding'])).toBe(0);
    });

    it('returns the correct count for more than 2 tags', () => {
      expect(component.hiddenTagCount(['source:database', 'onboarding', 'welcome'])).toBe(1);
      expect(
        component.hiddenTagCount(['source:database', 'onboarding', 'welcome', 'transactional']),
      ).toBe(2);
    });
  });

  describe('hiddenTagsTooltip', () => {
    it('returns tags beyond index 2 joined by newlines', () => {
      const result = component.hiddenTagsTooltip([
        'source:database',
        'onboarding',
        'welcome',
        'transactional',
      ]);
      // source:database and onboarding are the first 2 after sorting; welcome and transactional are hidden
      expect(result).toBe('welcome\ntransactional');
    });

    it('returns an empty string when no tags are hidden', () => {
      expect(component.hiddenTagsTooltip(['source:database', 'onboarding'])).toBe('');
    });
  });

  describe('copyId', () => {
    it('writes the full ID to the clipboard', async () => {
      const event = new MouseEvent('click');
      component.copyId(TEMPLATE_FIXTURE.id!, event);
      await Promise.resolve(); // flush microtask queue
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(TEMPLATE_FIXTURE.id);
    });

    it('adds the ID to copiedIds after copy', async () => {
      const event = new MouseEvent('click');
      component.copyId(TEMPLATE_FIXTURE.id!, event);
      await Promise.resolve();
      expect(component.copiedIds().has(TEMPLATE_FIXTURE.id!)).toBe(true);
    });

    it('removes the ID from copiedIds after 1500ms', async () => {
      vi.useFakeTimers();
      const event = new MouseEvent('click');
      component.copyId(TEMPLATE_FIXTURE.id!, event);
      await Promise.resolve();
      expect(component.copiedIds().has(TEMPLATE_FIXTURE.id!)).toBe(true);
      vi.advanceTimersByTime(1500);
      expect(component.copiedIds().has(TEMPLATE_FIXTURE.id!)).toBe(false);
      vi.useRealTimers();
    });

    it('stops event propagation', () => {
      const event = new MouseEvent('click');
      const stopPropagation = vi.spyOn(event, 'stopPropagation');
      component.copyId(TEMPLATE_FIXTURE.id!, event);
      expect(stopPropagation).toHaveBeenCalled();
    });
  });

  describe('template rendering', () => {
    it('renders a +N badge when tags exceed 2', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const moreBadge = compiled.querySelector('.tag-badge--more');
      expect(moreBadge).toBeTruthy();
      expect(moreBadge?.textContent?.trim()).toContain('+2');
    });

    it('does not render +N badge when 2 or fewer tags', () => {
      fixture.componentRef.setInput(
        'templates',
        signal([{ ...TEMPLATE_FIXTURE, tags: ['source:database', 'onboarding'] }]),
      );
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.tag-badge--more')).toBeNull();
    });

    it('renders the truncated ID in the table', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const idCode = compiled.querySelector('.tpl-id');
      expect(idCode?.textContent?.trim()).toBe('a1b2c3d4');
    });
  });
});
