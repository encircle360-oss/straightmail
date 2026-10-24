export const environment = {
  production: false,
  get apiUrl(): string {
    // Relative, like the production default: proxy.conf.json forwards /api to the backend, which
    // keeps every API call same-origin. An absolute http://localhost:50003 would bypass the proxy
    // and make authInterceptor skip the request, since it only attaches credentials same-origin.
    return (window as any).__runtimeConfig?.apiUrl ?? '/api';
  },
  get authMode(): 'oidc' | 'api-key' | 'none' {
    return (window as any).__runtimeConfig?.authMode ?? 'oidc';
  },
  oidc: {
    get authority(): string {
      return (
        (window as any).__runtimeConfig?.oidcAuthority ??
        'http://localhost:8090/realms/straightmail'
      );
    },
    clientId: 'straightmail',
    get redirectUrl(): string {
      return `${window.location.origin}/`;
    },
    get postLogoutRedirectUri(): string {
      return `${window.location.origin}/`;
    },
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    autoUserInfo: true,
    logLevel: 1, // 0 = None, 1 = Debug, 2 = Warn, 3 = Error
  },
};
