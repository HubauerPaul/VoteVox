import { SelectHTMLAttributes, ReactNode, forwardRef } from 'react';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  hint?: string;
  errorText?: string;
  required?: boolean;
  children: ReactNode;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, hint, errorText, required, className, id, children, ...rest },
  ref
) {
  const selectId = id ?? rest.name;
  return (
    <div className="form-group">
      {label && (
        <label className="form-label" htmlFor={selectId}>
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <select
        ref={ref}
        id={selectId}
        className={`form-select ${className ?? ''}`}
        required={required}
        {...rest}
      >
        {children}
      </select>
      {hint && !errorText && <div className="form-hint">{hint}</div>}
      {errorText && <div className="form-error">{errorText}</div>}
    </div>
  );
});
