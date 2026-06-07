import { useEffect, useMemo, useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import { Card } from '../components/Card';
import { Select } from '../components/Select';
import { Input } from '../components/Input';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { Table, TableColumn } from '../components/Table';
import { EmptyState } from '../components/EmptyState';
import { AuditEntry } from '../types';
import { fetchAuditLog } from '../api/audit';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';
import { formatDateTime } from '../utils/format';

const PAGE_SIZE = 50;

export function AuditPage(): JSX.Element {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionFilter, setActionFilter] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const { showToast } = useToast();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchAuditLog(page, PAGE_SIZE)
      .then((data) => {
        if (cancelled) return;
        setEntries(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load audit log'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page, showToast]);

  const actionTypes = useMemo(() => {
    const set = new Set<string>();
    entries.forEach((entry) => set.add(entry.actionType));
    return Array.from(set).sort();
  }, [entries]);

  const filtered = useMemo(() => {
    return entries.filter((entry) => {
      if (actionFilter !== 'ALL' && entry.actionType !== actionFilter) return false;
      if (fromDate) {
        const from = new Date(fromDate).getTime();
        if (new Date(entry.timestamp).getTime() < from) return false;
      }
      if (toDate) {
        const to = new Date(toDate).getTime() + 24 * 60 * 60 * 1000 - 1;
        if (new Date(entry.timestamp).getTime() > to) return false;
      }
      return true;
    });
  }, [entries, actionFilter, fromDate, toDate]);

  const columns: TableColumn<AuditEntry>[] = [
    {
      key: 'timestamp',
      header: 'Timestamp',
      render: (row) => formatDateTime(row.timestamp),
      width: '180px',
    },
    {
      key: 'actionType',
      header: 'Action',
      render: (row) => <code>{row.actionType}</code>,
      width: '180px',
    },
    {
      key: 'userType',
      header: 'User Type',
      render: (row) => row.userType,
      width: '120px',
    },
    {
      key: 'userId',
      header: 'User ID',
      render: (row) => row.userId ?? <span className="text-muted">—</span>,
      width: '160px',
    },
    {
      key: 'details',
      header: 'Details',
      render: (row) => row.details,
    },
  ];

  const resetFilters = (): void => {
    setActionFilter('ALL');
    setFromDate('');
    setToDate('');
  };

  return (
    <>
      <PageHeader title="Audit Log" subtitle={`${totalElements} total event(s)`} />

      <Card>
        <div className="inline-form mb-16">
          <Select
            label="Action type"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
          >
            <option value="ALL">All actions</option>
            {actionTypes.map((action) => (
              <option key={action} value={action}>
                {action}
              </option>
            ))}
          </Select>
          <Input
            label="From"
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
          <Input
            label="To"
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
          <Button variant="secondary" onClick={resetFilters}>
            Reset
          </Button>
        </div>

        {loading ? (
          <Spinner centered size="lg" />
        ) : filtered.length === 0 ? (
          <EmptyState title="No audit entries" description="Try adjusting your filters." />
        ) : (
          <>
            <Table columns={columns} data={filtered} rowKey={(row) => row.id} />
            <div className="pagination">
              <span>
                Page {page + 1} of {Math.max(totalPages, 1)} — showing {filtered.length} of{' '}
                {entries.length} on this page
              </span>
              <div className="pagination-controls">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page + 1 >= totalPages}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </>
  );
}
