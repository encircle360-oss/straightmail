/** Adds a toast notification to the queue. It is auto-removed after a fixed timeout. */
export class ShowToast {
  static readonly type = '[Toast] Show';

  /**
   * @param message Translation key (or literal string) for the toast body.
   * @param toastType Visual variant: {@code 'success'}, {@code 'error'}, or {@code 'info'} (default).
   */
  constructor(
    public message: string,
    public toastType: 'success' | 'error' | 'info' = 'info',
  ) {}
}

/** Removes a toast notification from the queue by its unique auto-assigned ID. */
export class RemoveToast {
  static readonly type = '[Toast] Remove';

  /**
   * @param id The numeric identifier of the toast to remove.
   */
  constructor(public id: number) {}
}
