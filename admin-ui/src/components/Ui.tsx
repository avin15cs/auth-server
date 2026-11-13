import { useEffect, useRef } from 'react'

export function StatusPill({ enabled }: { enabled: boolean }) {
  return <span className={`status ${enabled ? 'status--on' : 'status--off'}`}><i />{enabled ? 'Enabled' : 'Disabled'}</span>
}

export function PageHeader({ eyebrow, title, description, action }: {
  eyebrow?: string
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </header>
  )
}

export function EmptyState({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="empty-state"><h2>{title}</h2><p>{children}</p></div>
}

export function LoadingBlock({ label = 'Loading secure data' }: { label?: string }) {
  return <div className="loading-block" role="status"><span className="spinner" />{label}</div>
}

export function ErrorBanner({ message }: { message: string }) {
  return <div className="alert alert--error" role="alert"><strong>Request failed</strong><span>{message}</span></div>
}

export function ConfirmDialog({ open, title, description, confirmLabel, dangerous = false, onConfirm, onCancel }: {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  dangerous?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cancelButton = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (open && !dialog.current?.open) {
      dialog.current?.showModal()
      cancelButton.current?.focus()
    } else if (!open && dialog.current?.open) {
      dialog.current.close()
    }
  }, [open])

  return (
    <dialog ref={dialog} className="dialog" onCancel={(event) => { event.preventDefault(); onCancel() }}>
      <div className="dialog__mark" aria-hidden="true">!</div>
      <h2>{title}</h2>
      <p>{description}</p>
      <div className="dialog__actions">
        <button ref={cancelButton} className="button button--ghost" type="button" onClick={onCancel}>Cancel</button>
        <button className={`button ${dangerous ? 'button--danger' : 'button--primary'}`} type="button" onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </dialog>
  )
}

export function Pagination({ total, limit, offset, onChange }: {
  total: number
  limit: number
  offset: number
  onChange: (offset: number) => void
}) {
  const start = total === 0 ? 0 : offset + 1
  const end = Math.min(offset + limit, total)
  return (
    <nav className="pagination" aria-label="Pagination">
      <span>{start}–{end} of {total}</span>
      <div>
        <button className="button button--ghost button--small" disabled={offset === 0} onClick={() => onChange(Math.max(0, offset - limit))}>Previous</button>
        <button className="button button--ghost button--small" disabled={offset + limit >= total} onClick={() => onChange(offset + limit)}>Next</button>
      </div>
    </nav>
  )
}
