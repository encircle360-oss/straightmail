import '@angular/localize/init';
import { bootstrapApplication } from '@angular/platform-browser';
import { createAppConfig } from './app/app.config';
import { App } from './app/app';

// Fetch runtime configuration from the backend before bootstrapping Angular.
// /v1/info is always publicly accessible regardless of auth.mode.
// On failure (network error, backend not reachable) the static defaults
// in environment.ts are used as fallback.
try {
  const res = await fetch('/api/v1/info');
  if (res.ok) {
    (window as any).__runtimeConfig = await res.json();
  }
} catch {
  // Backend unreachable – environment.ts defaults will be used
}

bootstrapApplication(App, createAppConfig()).catch((err) => console.error(err));
