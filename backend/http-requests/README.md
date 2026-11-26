# HTTP Requests for straightmail API

These files contain example HTTP requests for the straightmail API. They can be used directly in
IntelliJ IDEA.

> **Note:** the request files still address the 0.4.x paths (`/email`, `/render`, `/templates`).
> Since 0.5.0 every endpoint lives under `/api/v1` — e.g. `http://localhost:50003/api/v1/email` —
> and the old paths answer `403`. Adjust the URLs before running a request.

## Usage in IntelliJ IDEA

1. Open a `.http` file in IntelliJ IDEA
2. Click the green "Run" arrow next to a request
3. The result will be displayed in the "Run" tool window

## Available Request Files

### 1. mail-controller.http

**Email Sending Endpoints**

- `POST /api/v1/email` - Send email with template ID
- `POST /api/v1/email/inline` - Send email with inline template

**Examples:**

- Simple email sending with template
- Email with inline template
- Email with complex JSON objects
- Emails with CC and BCC

### 2. render-controller.http

**Template Rendering Endpoints**

- `POST /api/v1/render` - Render template to HTML and plain text (without sending email)

**Examples:**

- Render template with simple model
- Render template with complex nested objects
- 404 case for non-existent templates

### 3. templates-controller.http

**Template Endpoints**

- `GET /api/v1/templates` - List templates of all sources (paginated)
- `GET /api/v1/templates/{id}` - Get template by ID
- `POST /api/v1/templates` - Create new template (`database` profile)
- `PUT /api/v1/templates/{id}` - Update existing template (`database` profile)
- `DELETE /api/v1/templates/{id}` - Delete template (`database` profile)

**Examples:**

- List templates with pagination and sorting
- Filter templates by tags
- Create different template types (Newsletter, Welcome, etc.)
- Templates in different languages (de, en)
- CRUD operations with 404 error handling

## Configuration

The requests use the following defaults:

- **Base URL:** `http://localhost:50003`
- **Content-Type:** `application/json`

If your application runs on a different port, adjust the URLs accordingly. Unless the backend runs
with `AUTH_MODE=none`, add the credentials of your auth mode (`Authorization: Bearer …` or
`X-API-KEY`, plus `X-Tenant-ID` where needed).

## Template Variables

The requests use Freemarker template syntax:

- `${variableName}` - Simple variables
- `${object.property}` - Nested properties
- Complex objects and arrays are supported

## Database Profile

Creating, updating and deleting templates is only available when the `database` profile is active
(`SPRING_PROFILES_ACTIVE=database`). Without it, templates are read from files — see
[Templates and translations](../../README.md#templates-and-translations).

## Example Workflow

1. **Create template** (with the `database` profile):
   ```
   POST /api/v1/templates (from templates-controller.http)
   ```

2. **Render template** (preview):
   ```
   POST /api/v1/render (from render-controller.http)
   ```

3. **Send email**:
   ```
   POST /api/v1/email (from mail-controller.http)
   ```

4. **Update template**:
   ```
   PUT /api/v1/templates/{id} (from templates-controller.http)
   ```

## Test Environment

The backend tests start a Mailpit container through Testcontainers on dynamic ports. The Docker
Compose stacks run Mailpit with its web UI on `http://localhost:8025`.
