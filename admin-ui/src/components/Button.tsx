import { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'link';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: 'sm' | 'md';
  children: ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  children,
  className,
  type = 'button',
  ...rest
}: ButtonProps): JSX.Element {
  const classes = [
    variant === 'link' ? 'btn-link' : 'btn',
    variant !== 'link' ? `btn-${variant}` : '',
    size === 'sm' && variant !== 'link' ? 'btn-sm' : '',
    className ?? '',
  ]
    .filter(Boolean)
    .join(' ');
  return (
    <button type={type} className={classes} {...rest}>
      {children}
    </button>
  );
}
