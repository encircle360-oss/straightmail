import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideStore } from '@ngxs/store';
import { UiState } from './store/ui/ui.state';
import { provideTranslateService } from '@ngx-translate/core';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideStore([UiState]), provideTranslateService()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
