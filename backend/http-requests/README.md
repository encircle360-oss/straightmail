# HTTP Requests for straightmail API

These files contain HTTP requests for all API endpoints of the straightmail application. They can be used directly in
IntelliJ IDEA.

## Usage in IntelliJ IDEA

1. Open a `.http` file in IntelliJ IDEA
2. Click the green "Run" arrow next to a request
3. The result will be displayed in the "Run" tool window

## Available Request Files

### 1. mail-controller.http

**Email Sending Endpoints**

- `POST /email` - Send email with template ID (template from filesystem or MongoDB)
- `POST /email/inline` - Send email with inline template

**Examples:**

- Simple email sending with template
- Email with inline template
- Email with complex JSON objects
- Emails with CC and BCC

### 2. render-controller.http

**Template Rendering Endpoints**

- `POST /render` - Render template to HTML and plain text (without sending email)

**Examples:**

- Render template with simple model
- Render template with complex nested objects
- 404 case for non-existent templates

### 3. templates-controller.http

**Template Management Endpoints** (only active with MongoDB profile)

- `GET /templates` - List all templates (paginated)
- `GET /templates/{id}` - Get template by ID
- `POST /templates` - Create new template
- `PUT /templates/{id}` - Update existing template
- `DELETE /templates/{id}` - Delete template

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

If your application runs on a different port, adjust the URLs accordingly.

## Template Variables

The requests use Freemarker template syntax:

- `${variableName}` - Simple variables
- `${object.property}` - Nested properties
- Complex objects and arrays are supported

## MongoDB Profile

**Important:** The `TemplatesController` is only available when the MongoDB profile is active:

```yaml
spring:
  profiles:
    active: mongodb
```

Without the MongoDB profile, templates are only loaded from the filesystem (`resources/templates`).

## Example Workflow

1. **Create template** (with MongoDB):
   ```
   POST /templates (from templates-controller.http)
   ```

2. **Render template** (preview):
   ```
   POST /render (from render-controller.http)
   ```

3. **Send email**:
   ```
   POST /email (from mail-controller.http)
   ```

4. **Update template**:
   ```
   PUT /templates/{id} (from templates-controller.http)
   ```

## Test Environment

In the test environment, a MailHog container runs automatically on dynamic ports.
Emails can be viewed through the MailHog Web UI (typically on port 8025).
