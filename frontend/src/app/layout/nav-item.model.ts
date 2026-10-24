import { PageIcon } from '../shared/components/layout/nav-icon/nav-icon.component';

/** Represents a single entry in the application's sidebar navigation menu. */
export interface NavItem {
  /** Translation key used as the visible label for the navigation link. */
  label: string;
  /** Router path this item links to (e.g. {@code '/dashboard'}). */
  path: string;
  /** Icon identifier used to render the navigation icon. */
  icon: PageIcon;
  /** Optional role required to see this item; absent means no role restriction. */
  requiredRole?: string;
}
