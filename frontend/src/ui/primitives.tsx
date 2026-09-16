import type { ReactNode } from 'react';

/** Pecas pequenas e reutilizaveis: skeleton, estado vazio, indicador, confirmacao. */

export function Skeleton({ lines = 3, height = 14 }: { lines?: number; height?: number }) {
  return (
    <div className="skeleton-wrap" aria-hidden>
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} className="skeleton" style={{ height, width: `${100 - (i % 3) * 18}%` }} />
      ))}
    </div>
  );
}

export function EmptyState({ icon, title, hint, action }: { icon: string; title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-icon" aria-hidden>{icon}</div>
      <p className="empty-title">{title}</p>
      {hint && <p className="muted small">{hint}</p>}
      {action}
    </div>
  );
}

export function Stat({ label, value, hint, tone }: { label: string; value: string; hint?: ReactNode; tone?: 'ok' | 'warning' | 'danger' }) {
  return (
    <div className={`stat ${tone ?? ''}`}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

export function Confirm({ open, title, text, onCancel, onConfirm, danger }: {
  open: boolean; title: string; text: string; onCancel: () => void; onConfirm: () => void; danger?: boolean;
}) {
  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={onCancel} role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="confirm-title" onClick={(e) => e.stopPropagation()}>
        <h3 id="confirm-title">{title}</h3>
        <p className="muted">{text}</p>
        <div className="modal-actions">
          <button className="btn ghost" onClick={onCancel}>Cancelar</button>
          <button className={danger ? 'btn danger' : 'btn primary'} onClick={onConfirm} autoFocus>Confirmar</button>
        </div>
      </div>
    </div>
  );
}
