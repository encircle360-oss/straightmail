import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { Store } from '@ngxs/store';
import { NgProgressbar } from 'ngx-progressbar';
import { NgProgressHttp } from 'ngx-progressbar/http';
import { ToastContainerComponent } from '../shared/components/overlays/toast-container/toast-container.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { TopbarComponent } from './topbar/topbar.component';
import { FooterComponent } from './footer/footer.component';
import { NavItem } from './nav-item.model';
import { AuthState } from '../store/auth/auth.state';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    NgProgressbar,
    NgProgressHttp,
    ToastContainerComponent,
    SidebarComponent,
    TopbarComponent,
    FooterComponent,
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
/**
 * Root layout shell component wrapping all authenticated pages.
 *
 * Composes the {@link SidebarComponent}, {@link TopbarComponent}, {@link FooterComponent},
 * {@link ToastContainerComponent}, and a {@code RouterOutlet}. Manages sidebar open/close state
 * and filters the full navigation item list to only those the current user is authorised to see
 * (based on {@link AuthState} roles). Role-filtered items are computed reactively via a signal.
 */
export class LayoutComponent {
  private readonly store = inject(Store);

  /** Signal tracking whether the sidebar overlay is currently open. */
  readonly sidebarOpen = signal(false);

  private readonly allNavItems: NavItem[] = [
    { label: 'nav.dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'nav.templates', path: '/templates', icon: 'templates' },
    { label: 'nav.render', path: '/render', icon: 'preview' },
    { label: 'nav.send', path: '/send', icon: 'send' },
    { label: 'nav.tenants', path: '/tenants', icon: 'building', requiredRole: 'ADMIN' },
  ];

  readonly roles = toSignal(this.store.select(AuthState.roles), { initialValue: [] as string[] });

  readonly navItems = computed(() =>
    this.allNavItems.filter(
      (item) =>
        !item.requiredRole ||
        environment.authMode !== 'oidc' ||
        this.roles().includes(item.requiredRole),
    ),
  );

  /** Toggles the sidebar between open and closed. */
  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  /** Closes the sidebar (called on navigation or backdrop click). */
  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
