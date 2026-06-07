import { ElectionType } from '../types';

export function formatDateTime(
  iso: string | null | undefined,
  mode: 'date' | 'time' | 'full' = 'full'
): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  const dateFmt = new Intl.DateTimeFormat('en-GB', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  });
  const timeFmt = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  if (mode === 'date') return dateFmt.format(date);
  if (mode === 'time') return timeFmt.format(date);
  return `${dateFmt.format(date)} ${timeFmt.format(date)}`;
}

export function electionTypeLabel(type: ElectionType): string {
  switch (type) {
    case 'SCHOOL_REP':
      return 'School Representative';
    case 'DEPARTMENT_REP':
      return 'Department Representative';
    case 'SURVEY':
      return 'Survey';
    default:
      return type;
  }
}

export function toDatetimeLocalValue(iso: string | null | undefined): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (n: number): string => String(n).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

export function fromDatetimeLocalValue(value: string): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toISOString();
}

export function formatPercentage(value: number): string {
  if (Number.isNaN(value)) return '0%';
  return `${value.toFixed(1)}%`;
}
