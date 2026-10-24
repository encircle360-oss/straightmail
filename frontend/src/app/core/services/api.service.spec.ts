import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  describe('getTemplates', () => {
    it('should GET templates with default page and size', () => {
      service.getTemplates().subscribe();

      const req = httpTesting.expectOne((r) => r.url === '/api/v1/templates');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.has('tag')).toBe(false);
      req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
    });

    it('should include tag params when provided', () => {
      service.getTemplates(1, 10, ['newsletter', 'promo']).subscribe();

      const req = httpTesting.expectOne((r) => r.url === '/api/v1/templates');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.getAll('tag')).toEqual(['newsletter', 'promo']);
      req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
    });
  });

  describe('createTemplate', () => {
    it('should POST to /api/v1/templates', () => {
      const template = { name: 'New', locale: 'en', tags: [] };
      service.createTemplate(template).subscribe();

      const req = httpTesting.expectOne('/api/v1/templates');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(template);
      req.flush({ id: '1', ...template });
    });
  });

  describe('updateTemplate', () => {
    it('should PUT to /api/v1/templates/:id', () => {
      const template = { name: 'Updated', locale: 'en', tags: [] };
      service.updateTemplate('abc-123', template).subscribe();

      const req = httpTesting.expectOne('/api/v1/templates/abc-123');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(template);
      req.flush({ id: 'abc-123', ...template });
    });
  });

  describe('deleteTemplate', () => {
    it('should DELETE /api/v1/templates/:id', () => {
      service.deleteTemplate('abc-123').subscribe();

      const req = httpTesting.expectOne('/api/v1/templates/abc-123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('sendEmail', () => {
    it('should POST to /api/v1/email with request body', () => {
      const sendReq = {
        recipients: ['a@b.com'],
        sender: 'noreply@b.com',
        emailTemplateId: '1',
      };
      service.sendEmail(sendReq).subscribe();

      const req = httpTesting.expectOne('/api/v1/email');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(sendReq);
      req.flush(null);
    });
  });

  describe('sendInlineEmail', () => {
    it('should POST to /api/v1/email/inline with request body', () => {
      const sendReq = {
        recipients: ['a@b.com'],
        sender: 'noreply@b.com',
        subject: 'Hello',
        emailTemplate: '<p>Hi</p>',
      };
      service.sendInlineEmail(sendReq).subscribe();

      const req = httpTesting.expectOne('/api/v1/email/inline');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(sendReq);
      req.flush(null);
    });
  });

  describe('render', () => {
    it('should POST to /api/v1/render and return RenderResponse', () => {
      const renderReq = { templateId: '1', model: { name: 'World' } };
      const response = { html: '<h1>World</h1>', plain: 'World' };

      service.render(renderReq).subscribe((res) => {
        expect(res).toEqual(response);
      });

      const req = httpTesting.expectOne('/api/v1/render');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(renderReq);
      req.flush(response);
    });
  });

  describe('getInfo', () => {
    it('should GET /api/v1/info and return full response', () => {
      service.getInfo().subscribe();

      const req = httpTesting.expectOne('/api/v1/info');
      expect(req.request.method).toBe('GET');
      req.flush({ status: 'UP' });
    });
  });
});
