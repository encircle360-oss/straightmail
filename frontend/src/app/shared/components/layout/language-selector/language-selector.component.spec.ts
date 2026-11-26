import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LanguageSelectorComponent } from './language-selector.component';
import { provideStore, Store } from '@ngxs/store';
import { UiState } from '../../../../store/ui/ui.state';
import { UiActions } from '../../../../store/ui/ui.actions';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';

describe('LanguageSelectorComponent', () => {
  let component: LanguageSelectorComponent;
  let fixture: ComponentFixture<LanguageSelectorComponent>;
  let store: Store;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LanguageSelectorComponent],
      providers: [provideStore([UiState]), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(LanguageSelectorComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch SetLanguage action when switchLanguage is called', () => {
    const dispatchSpy = vi.spyOn(store, 'dispatch');
    component.switchLanguage('en');
    expect(dispatchSpy).toHaveBeenCalledWith(new UiActions.SetLanguage('en'));
  });

  it('should have correct default language', () => {
    expect(component.currentLang()).toBe('en'); // 'en' is default in UiState
  });
});
