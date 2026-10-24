import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TemplateGridComponent } from './template-grid.component';
import { EmailTemplate } from '../../../core/services/api.service';

const TEMPLATE_FIXTURE: EmailTemplate = {
  id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  name: 'Welcome Email',
  locale: 'en',
  tags: ['source:database', 'onboarding'],
  source: 'DATABASE',
  editable: true,
};

const READONLY_FIXTURE: EmailTemplate = {
  id: 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
  name: 'Git Template',
  locale: 'de',
  tags: ['source:git'],
  source: 'GIT',
  editable: false,
};

describe('TemplateGridComponent', () => {
  let component: TemplateGridComponent;
  let fixture: ComponentFixture<TemplateGridComponent>;

  beforeEach(async () => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    });

    await TestBed.configureTestingModule({
      imports: [TemplateGridComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateGridComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('templates', signal([TEMPLATE_FIXTURE, READONLY_FIXTURE]));
    fixture.componentRef.setInput('loading', signal(false));
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('template rendering', () => {
    it('renders a card for each template', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const cards = compiled.querySelectorAll('.template-card');
      expect(cards.length).toBe(2);
    });

    it('renders the truncated ID in each card', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const ids = compiled.querySelectorAll('.tpl-id');
      expect(ids[0]?.textContent?.trim()).toBe('a1b2c3d4');
      expect(ids[1]?.textContent?.trim()).toBe('b2c3d4e5');
    });

    it('shows delete button only for editable templates', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const deleteButtons = compiled.querySelectorAll('.template-card__delete');
      expect(deleteButtons.length).toBe(1);
    });
  });

  describe('onCardClick', () => {
    it('emits edit for editable templates', () => {
      const editSpy = vi.fn();
      component.edit.subscribe(editSpy);
      component.onCardClick(TEMPLATE_FIXTURE);
      expect(editSpy).toHaveBeenCalledWith(TEMPLATE_FIXTURE);
    });

    it('emits view for read-only templates', () => {
      const viewSpy = vi.fn();
      component.view.subscribe(viewSpy);
      component.onCardClick(READONLY_FIXTURE);
      expect(viewSpy).toHaveBeenCalledWith(READONLY_FIXTURE);
    });
  });

  describe('copyId', () => {
    it('writes the full ID to the clipboard', async () => {
      const event = new MouseEvent('click');
      component.copyId(TEMPLATE_FIXTURE.id!, event);
      await Promise.resolve();
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(TEMPLATE_FIXTURE.id);
    });

    it('toggles copy icon to check after copy and back after 1500ms', async () => {
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

  describe('shortId', () => {
    it('returns exactly 8 characters', () => {
      expect(component.shortId('a1b2c3d4-e5f6-7890-abcd-ef1234567890')).toBe('a1b2c3d4');
    });
  });
});
