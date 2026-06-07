import { TextareaHTMLAttributes, forwardRef } from 'react';

interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  errorText?: string;
  required?: boolean;
}

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(function TextArea(
  { label, hint, errorText, required, className, id, ...rest },
  ref
) {
  const textareaId = id ?? rest.name;
  return (
    <div className="form-group">
      {label && (
        <label className="form-label" htmlFor={textareaId}>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <textarea
        ref={ref}
        id={textareaId}
        className={`form-textarea ${className ?? ''}`}
        required={required}
        {...rest}
      />
      {hint && !errorText && <div className="form-hint">{hint}</div>}
      {errorText && <div className="form-error">{errorText}</div>}
    </div>
  );
});
