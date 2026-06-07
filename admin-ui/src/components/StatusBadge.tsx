import { ElectionStatus } from '../types';

interface StatusBadgeProps {
  status: ElectionStatus;
}

const STATUS_LABELS: Record<ElectionStatus, string> = {
  PLANNED: 'Planned',
  RUNNING: 'Running',
  FINISHED: 'Finished',
};

const STATUS_CLASSES: Record<ElectionStatus, string> = {
  PLANNED: 'status-planned',
  RUNNING: 'status-running',
  FINISHED: 'status-finished',
};

export function StatusBadge({ status }: StatusBadgeProps): JSX.Element {
  return (
    <span className={`status-badge ${STATUS_CLASSES[status]}`}>{STATUS_LABELS[status]}</span>
  );
}
