import { useEffect, useRef } from 'react'

type Props = {
  open: boolean
  loading?: boolean
  onContinue: () => void
  onCancel: () => void
}

export default function SensitiveDataModal({ open, loading, onContinue, onCancel }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const el = dialogRef.current
    if (!el) return
    if (open && !el.open) el.showModal()
    else if (!open && el.open) el.close()
  }, [open])

  if (!open) return null

  return (
    <dialog
      ref={dialogRef}
      onCancel={onCancel}
      className="fixed inset-0 z-50 m-auto w-full max-w-md rounded-2xl border border-slate-200 bg-white p-0 shadow-2xl backdrop:bg-black/40 backdrop:backdrop-blur-sm"
    >
      <div className="flex flex-col items-center gap-4 px-8 py-8 text-center">
        {/* Warning icon */}
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-amber-100">
          <span className="material-symbols-outlined text-3xl text-amber-600 filled-icon">
            shield_person
          </span>
        </div>

        <h2 className="text-xl font-bold text-slate-900">
          Sensitive Information Access
        </h2>

        <p className="text-sm leading-relaxed text-slate-600">
          You are about to view an <strong>unmasked registration number</strong>.
          This is sensitive information and your access <strong>will be recorded</strong> in the audit log.
        </p>

        <p className="text-sm leading-relaxed text-slate-600">
          Please ensure you are accessing this data with a <strong>proper and valid intent</strong> in accordance with company policy.
        </p>

        {/* Action buttons */}
        <div className="mt-2 flex w-full gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onContinue}
            disabled={loading}
            className="flex-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary/90 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                Loading...
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-base">verified</span>
                Continue
              </>
            )}
          </button>
        </div>
      </div>
    </dialog>
  )
}
