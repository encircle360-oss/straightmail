import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Store } from '@ngxs/store';
import { UiState } from './store/ui/ui.state';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
/**
 * Root application component.
 *
 * Bootstraps the router outlet and restores the user's preferred UI language from
 * {@link UiState} on init so the correct locale is active from the very first render.
 */
export class App implements OnInit {
  private readonly store = inject(Store);
  private readonly translate = inject(TranslateService);

  /** Restores the persisted UI language on application start. */
  ngOnInit() {
    const lang = this.store.selectSnapshot(UiState.language);
    if (lang) {
      this.translate.use(lang);
    }
  }
}
