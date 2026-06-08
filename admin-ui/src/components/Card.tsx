import { ReactNode } from 'react';

interface CardProps {
  title?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function Card({ title, actions, children, className }: CardProps): JSX.Element {
  return (
    <div className={`card ${className ?? ''}`}>
      {(title || actions) && (
        <div className={actions ? 'card-header' : 'card-title-only'}>
          {title && <h3>{title}</h3>}
          {actions && <div className="flex-gap-8">{actions}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
