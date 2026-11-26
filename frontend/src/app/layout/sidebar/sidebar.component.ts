import { Component, inject, input, output, Signal } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngxs/store';
import { NavItem } from '../nav-item.model';
import { NavIconComponent } from '../../shared/components/layout/nav-icon/nav-icon.component';
import { AppLogoComponent } from '../../shared/components/layout/app-logo/app-logo.component';
import { LanguageSelectorComponent } from '../../shared/components/layout/language-selector/language-selector.component';
import { TenantAvatarComponent } from '../../shared/components/layout/tenant-avatar/tenant-avatar.component';
import { version } from '../../../environments/version';
import { TenantDTO, TenantState } from '../../store/tenant/tenant.state';
import { TenantActions } from '../../store/tenant/tenant.actions';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    AsyncPipe,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    TranslatePipe,
    AppLogoComponent,
    LanguageSelectorComponent,
    NavIconComponent,
    TenantAvatarComponent,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
/**
 * Sidebar navigation component displaying the application logo, tenant selector,
 * navigation links, and version.
 *
 * Renders the filtered {@code navItems} as router links and emits {@link navClick} on each
 * navigation so the parent layout can close the sidebar overlay. The tenant selector appears
 * directly below the logo and allows switching between accessible tenants.
 */
export class SidebarComponent {
  /** The list of navigation items to render; role-filtered by the parent layout. */
  readonly navItems = input<NavItem[]>([]);
  /** Whether the sidebar overlay is currently open (controls the CSS open state). */
  readonly open = input(false);
  /** Emitted when the user clicks a navigation link, signalling the parent to close the sidebar. */
  readonly navClick = output<void>();
  /** Application version string displayed at the bottom of the sidebar. */
  readonly version = version;
  /** Observable emitting all tenants accessible to the current user. */
  tenants$: Observable<TenantDTO[]>;
  /** Observable emitting the slug of the currently selected tenant. */
  selectedTenantId$: Observable<string | null>;
  /** Signal emitting the full DTO of the currently selected tenant, or {@code null}. */
  readonly selectedTenant: Signal<TenantDTO | null>;

  private readonly store = inject(Store);

  constructor() {
    this.tenants$ = this.store.select(TenantState.tenants);
    this.selectedTenantId$ = this.store.select(TenantState.selectedTenantId);
    this.selectedTenant = toSignal(this.store.select(TenantState.selectedTenant), {
      initialValue: null,
    });
  }

  /**
   * Selects a tenant as the active one.
   *
   * @param tenantId The slug of the tenant to activate.
   */
  selectTenant(tenantId: string): void {
    this.store.dispatch(new TenantActions.SelectTenant(tenantId));
  }
}
