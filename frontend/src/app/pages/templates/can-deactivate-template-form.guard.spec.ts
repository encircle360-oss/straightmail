import {
  CanDeactivateComponent,
  canDeactivateTemplateForm,
} from './can-deactivate-template-form.guard';

describe('canDeactivateTemplateForm', () => {
  it('should call canDeactivate on the component', () => {
    const component: CanDeactivateComponent = {
      canDeactivate: vi.fn(() => true),
    };

    const result = canDeactivateTemplateForm(component, {} as any, {} as any, {} as any);
    expect(result).toBe(true);
    expect(component.canDeactivate).toHaveBeenCalled();
  });

  it('should return false if component says false', () => {
    const component: CanDeactivateComponent = {
      canDeactivate: vi.fn(() => false),
    };

    const result = canDeactivateTemplateForm(component, {} as any, {} as any, {} as any);
    expect(result).toBe(false);
  });
});
