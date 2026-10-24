import { Component, inject, input, output, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { environment } from '../../../../environments/environment';

type HealthStatus = 'checking' | 'up' | 'down';

@Component({
  selector: 'app-api-status-card',
  standalone: true,
  templateUrl: './api-status-card.component.html',
  styleUrl: './api-status-card.component.scss',
})
/**
 * Dashboard card that displays the current backend API health status.
 *
 * Shows the resolved API base URL alongside a visual health indicator and exposes a
 * {@link refresh} output so the parent {@link DashboardComponent} can re-trigger a health check.
 */
export class ApiStatusCardComponent {
  protected readonly environment = environment;

  private readonly apiService = inject(ApiService);

  /** The current health state of the backend API. */
  readonly status = input.required<HealthStatus>();
  /** Signal holding the resolved API base URL for display. */
  readonly base = signal(this.apiService.getBase());

  /** Emitted when the user clicks the refresh button to re-check the backend health. */
  readonly refresh = output<void>();
}
