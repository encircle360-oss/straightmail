import { Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TenantDTO } from '../../../store/tenant/tenant.state';

@Component({
  selector: 'app-tenant-table',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './tenant-table.component.html',
  styleUrl: './tenant-table.component.scss',
})
/**
 * Presentational table component for displaying the list of tenants.
 *
 * Renders the tenants supplied by the parent and emits {@link edit} and {@link delete}
 * events when the user clicks the corresponding action buttons.
 */
export class TenantTableComponent {
  /** The list of tenants to display. */
  readonly tenants = input.required<TenantDTO[]>();
  /** Whether a data request is in progress (disables action buttons while loading). */
  readonly loading = input.required<boolean>();
  /** Emitted when the user clicks the edit button for a tenant. */
  readonly edit = output<TenantDTO>();
  /** Emitted when the user clicks the view button for a config-based (read-only) tenant. */
  readonly view = output<TenantDTO>();
  /** Emitted when the user clicks the delete button for a tenant. */
  readonly delete = output<TenantDTO>();
}
