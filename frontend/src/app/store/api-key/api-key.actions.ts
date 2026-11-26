/** Stores an API key in {@link ApiKeyState}, enabling API-key authentication mode. */
export class SetApiKey {
  static readonly type = '[ApiKey] Set API Key';
  /**
   * @param apiKey The plain-text API key to store (appended to requests as {@code X-API-KEY}).
   */
  constructor(public apiKey: string) {}
}

/** Removes the stored API key from {@link ApiKeyState}, effectively logging the user out. */
export class ClearApiKey {
  static readonly type = '[ApiKey] Clear API Key';
}
