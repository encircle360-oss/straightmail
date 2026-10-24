export const environment = {
  production: false,
  // Relative, like dev and prod: every API call goes through the same base.
  apiUrl: '/api',
  authMode: 'api-key' as 'oidc' | 'api-key' | 'none',
  oidc: {
    authority: 'https://localhost',
    clientId: 'dummy',
    redirectUrl: 'http://localhost:4200/',
    postLogoutRedirectUri: 'http://localhost:4200/',
    scope: 'openid',
    responseType: 'code',
    silentRenew: false,
    useRefreshToken: false,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    autoUserInfo: false,
    logLevel: 3,
  },
};
