/** Map of section id -> keys that belong to that section. Any key not in any list falls into "Other". */
export const SECTION_KEYS: Record<string, string[]> = {
  owner: [
    'owner',
    'ownerFatherName',
    'mobileNumber',
    'presentAddress',
    'permanentAddress',
    'ownerCount',
  ],
  rc: [
    'regNo',
    'vehicleNumber',
    'status',
    'statusAsOn',
    'regAuthority',
    'regDate',
    'rcExpiryDate',
    'vehicleTaxUpto',
    'nocDetails',
    'blacklistStatus',
    'blacklistDetails',
    'dbResult',
    'partialData',
  ],
  vehicle: [
    'vehicleClass',
    'chassis',
    'engine',
    'vehicleManufacturerName',
    'model',
    'vehicleColour',
    'type',
    'normsType',
    'bodyType',
    'vehicleCubicCapacity',
    'grossVehicleWeight',
    'unladenWeight',
    'vehicleCategory',
    'vehicleCylindersNo',
    'vehicleSeatCapacity',
    'vehicleSleeperCapacity',
    'vehicleStandingCapacity',
    'wheelbase',
    'electricVehicle',
    'rtoCode',
  ],
  insurance: [
    'vehicleInsuranceCompanyName',
    'vehicleInsuranceUpto',
    'vehicleInsurancePolicyNumber',
  ],
  puc: ['puccNumber', 'puccUpto'],
  loan: ['rcFinancer', 'financed'],
  permit: [
    'permitIssueDate',
    'permitNumber',
    'permitType',
    'permitValidFrom',
    'permitValidUpto',
    'nationalPermitNumber',
    'nationalPermitUpto',
    'nationalPermitIssuedBy',
    'nonUseStatus',
    'nonUseFrom',
    'nonUseTo',
  ],
}

export const SECTION_LABELS: Record<string, string> = {
  owner: 'Owner Identity',
  rc: 'Registration (RC)',
  vehicle: 'Vehicle Specs',
  insurance: 'Insurance',
  puc: 'PUC',
  loan: 'Loan / Finances',
  permit: 'Permit',
  other: 'Other',
}

export type SectionId = keyof typeof SECTION_LABELS

export function groupDataBySection(data: Record<string, unknown>): Record<string, Record<string, unknown>> {
  const assigned = new Set<string>()
  const result: Record<string, Record<string, unknown>> = {}

  for (const [sectionId, keys] of Object.entries(SECTION_KEYS)) {
    const sectionData: Record<string, unknown> = {}
    for (const key of keys) {
      if (key in data) {
        sectionData[key] = data[key]
        assigned.add(key)
      }
    }
    if (Object.keys(sectionData).length > 0) {
      result[sectionId] = sectionData
    }
  }

  const other: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(data)) {
    if (!assigned.has(key)) other[key] = value
  }
  if (Object.keys(other).length > 0) {
    result['other'] = other
  }

  return result
}

export function labelForKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (s) => s.toUpperCase())
    .trim()
}

export function formatValue(value: unknown): string {
  if (value == null) return '—'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
