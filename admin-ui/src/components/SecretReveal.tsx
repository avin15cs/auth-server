import { useState } from 'react'
import { CopyIcon, ShieldIcon } from './Icons'

export function SecretReveal({ clientId, secret, onAcknowledge }: { clientId: string; secret: string; onAcknowledge: () => void }) {
  const [copied, setCopied] = useState(false)
  const [acknowledged, setAcknowledged] = useState(false)

  async function copy() {
    await navigator.clipboard.writeText(secret)
    setCopied(true)
  }

  return (
    <section className="secret-card" aria-labelledby="secret-title">
      <span className="secret-card__icon"><ShieldIcon /></span>
      <p className="eyebrow">Client created</p>
      <h1 id="secret-title">Save this secret now.</h1>
      <p>This plaintext value is available once. Store it directly in the workload's secret manager; it cannot be recovered later.</p>
      <dl className="secret-values"><div><dt>Client ID</dt><dd><code>{clientId}</code></dd></div><div><dt>Client secret</dt><dd><code data-testid="one-time-secret">{secret}</code><button type="button" className="copy-button" onClick={copy}><CopyIcon />{copied ? 'Copied' : 'Copy'}</button></dd></div></dl>
      <label className="acknowledge"><input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} /><span>I saved this secret in an approved secure location.</span></label>
      <button type="button" className="button button--primary button--wide" disabled={!acknowledged} onClick={onAcknowledge}>Finish registration</button>
    </section>
  )
}
