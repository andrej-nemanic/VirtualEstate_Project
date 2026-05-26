import { useEffect } from 'react';

export default function ConfirmDialog({
  open,
  title = 'Potrditev',
  message,
  confirmLabel = 'Potrdi',
  cancelLabel = 'Prekliči',
  danger = false,
  onConfirm,
  onCancel
}) {
  useEffect(() => {
    if (!open) return;
    const handler = (e) => {
      if (e.key === 'Escape') onCancel?.();
      if (e.key === 'Enter') onConfirm?.();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open, onConfirm, onCancel]);

  if (!open) return null;

  return (
    <div className="dialog-backdrop" onClick={onCancel}>
      <div
        className="dialog-card"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
      >
        <h2 id="confirm-dialog-title" style={{ margin: 0, marginBottom: 12 }}>{title}</h2>
        <p className="muted" style={{ margin: '8px 0 20px' }}>{message}</p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="secondary" onClick={onCancel}>{cancelLabel}</button>
          <button className={danger ? 'danger' : ''} onClick={onConfirm} autoFocus>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
