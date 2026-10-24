/** Triggers an OIDC session check to restore existing authentication on application start. */
export class CheckAuth {
  static readonly type = '[Auth] Check Auth';
}

/** Initiates the OIDC authorization code flow, redirecting the user to the identity provider. */
export class Login {
  static readonly type = '[Auth] Login';
}

/** Terminates the OIDC session and clears all stored auth state. */
export class Logout {
  static readonly type = '[Auth] Logout';
}

/** Updates the user-data claims stored in {@link AuthState}. */
export class SetUserData {
  static readonly type = '[Auth] Set User Data';

  /**
   * @param userData The raw user-data claims object returned by the OIDC provider.
   */
  constructor(public userData: any) {}
}
