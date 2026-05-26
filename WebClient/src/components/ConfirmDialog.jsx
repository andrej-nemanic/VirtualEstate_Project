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
    <div
      onClick={onCancel}
      style={{
        position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: 'white', borderRadius: 12, padding: 24,
          maxWidth: 460, width: '90%', boxShadow: '0 20px 50px rgba(0,0,0,0.25)'
        }}
        role="dialog" aria-modal="true"
      >
        <h2 style={{ margin: 0, marginBottom: 12 }}>{title}</h2>
        <p style={{ color: '#475569', margin: '8px 0 20px' }}>{message}</p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="secondary" onClick={onCancel}>{cancelLabel}</button>
          <button className={danger ? 'danger' : ''} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
