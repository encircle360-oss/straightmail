import { Component, inject } from '@angular/core';
import { Store } from '@ngxs/store';
import { UiState } from '../../../../store/ui/ui.state';
import { UiActions } from '../../../../store/ui/ui.actions';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [],
  templateUrl: './language-selector.component.html',
  styleUrl: './language-selector.component.scss',
})
/**
 * Language selector component displayed in the sidebar footer.
 *
 * Reads the current language from {@link UiState} and dispatches {@link UiActions.SetLanguage}
 * when the user picks a different locale.
 */
export class LanguageSelectorComponent {
  private readonly store = inject(Store);

  /** Signal emitting the currently active UI language code. */
  readonly currentLang = this.store.selectSignal(UiState.language);

  /**
   * Activates the given language in the UI.
   *
   * @param lang BCP 47 language code to switch to (e.g. {@code 'en'}, {@code 'de'}).
   */
  switchLanguage(lang: string) {
    this.store.dispatch(new UiActions.SetLanguage(lang));
  }
}
