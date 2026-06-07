import { InputHTMLAttributes, forwardRef } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  errorText?: string;
  required?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, hint, errorText, required, className, id, ...rest },
  ref
) {
  const inputId = id ?? rest.name;
  return (
    <div className="form-group">
      {label && (
        <label className="form-label" htmlFor={inputId}>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        className={`form-input ${className ?? ''}`}
        required={required}
        {...rest}
      />
      {hint && !errorText && <div className="form-hint">{hint}</div>}
      {errorText && <div className="form-error">{errorText}</div>}
    </div>
  );
});
