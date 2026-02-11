import type { ReactNode } from 'react'
import { SECTION_LABELS, labelForKey, formatValue } from '../utils/vehicleSections'
import type { SectionId } from '../utils/vehicleSections'

const SECTION_ICONS: Record<string, string> = {
  owner: 'person',
  rc: 'description',
  vehicle: 'directions_car',
  insurance: 'shield',
  puc: 'eco',
  loan: 'account_balance',
  permit: 'badge',
  other: 'more_horiz',
}

const SECTION_STYLES: Record<string, { icon: string; header: string }> = {
  owner: { icon: 'bg-pastelPurple text-primary', header: 'bg-pastelPurple/10' },
  rc: { icon: 'bg-pastelGreen text-green-600', header: 'bg-pastelGreen/10' },
  vehicle: { icon: 'bg-pastelBlue text-blue-600', header: 'bg-pastelBlue/10' },
  insurance: { icon: 'bg-pastelOrange text-orange-600', header: 'bg-pastelOrange/10' },
  puc: { icon: 'bg-pastelGreen text-green-600', header: 'bg-pastelGreen/10' },
  loan: { icon: 'bg-pastelPurple text-primary', header: 'bg-pastelPurple/10' },
  permit: { icon: 'bg-pastelBlue text-blue-600', header: 'bg-pastelBlue/10' },
  other: { icon: 'bg-slate-100 text-slate-600', header: 'bg-slate-100' },
}

type Props = {
  sectionId: SectionId
  data: Record<string, unknown>
  highlightTerm?: string
}

function highlightText(text: string, term: string): ReactNode {
  if (!term) return text
  const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const parts = text.split(new RegExp(`(${escaped})`, 'gi'))
  return parts.map((part, i) =>
    i % 2 === 1 ? (
      <mark key={i} className="bg-yellow-200 text-slate-900 px-0.5 rounded">{part}</mark>
    ) : (
      part
    )
  )
}

export default function VehicleResult({ sectionId, data, highlightTerm }: Props) {
  const label = SECTION_LABELS[sectionId] ?? sectionId
  const icon = SECTION_ICONS[sectionId] ?? 'info'
  const style = SECTION_STYLES[sectionId] ?? { icon: 'bg-slate-100 text-slate-600', header: 'bg-slate-100' }

  return (
    <section className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
      <div className={`flex items-center gap-3 px-5 py-4 border-b border-slate-100 ${style.header}`}>
        <div className={`w-10 h-10 flex items-center justify-center rounded-xl ${style.icon}`}>
          <span className="material-symbols-outlined text-xl filled-icon">{icon}</span>
        </div>
        <h3 className="text-lg font-bold text-slate-800">{label}</h3>
      </div>
      <div className="divide-y divide-slate-100">
        {Object.entries(data).map(([key, value]) => (
          <div key={key} className="flex flex-col sm:flex-row sm:items-center px-5 py-3 hover:bg-slate-50 transition-colors">
            <div className="sm:w-1/3 text-sm font-medium text-slate-500 mb-1 sm:mb-0">
              {highlightTerm ? highlightText(labelForKey(key), highlightTerm) : labelForKey(key)}
            </div>
            <div className="sm:w-2/3 text-base font-semibold text-slate-800 break-words">
              {highlightTerm ? highlightText(formatValue(value), highlightTerm) : formatValue(value)}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
