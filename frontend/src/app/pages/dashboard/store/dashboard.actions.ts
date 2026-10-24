/** Loads the aggregated sync status from the backend. Errors are swallowed so the sync section is hidden when the database profile is inactive. */
export class LoadSyncStatus {
  static readonly type = '[Dashboard] Load Sync Status';
}

/** Triggers an immediate Git-sync for the given tenant and shows a toast with the result. */
export class TriggerGitSync {
  static readonly type = '[Dashboard] Trigger Git Sync';

  /**
   * @param tenantSlug The slug of the tenant to sync.
   */
  constructor(public tenantSlug: string) {}
}
