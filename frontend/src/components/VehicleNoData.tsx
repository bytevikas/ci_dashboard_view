type Props = {
  message: string
  onTryAgain: () => void
}

type ErrorType = 'not_found' | 'api_down' | 'network' | 'rate_limit' | 'unknown'

function getErrorType(message: string): ErrorType {
  const lower = message.toLowerCase()
  if (lower.includes('not found') || lower.includes('no data')) return 'not_found'
  if (lower.includes('too many') || lower.includes('rate') || lower.includes('limit')) return 'rate_limit'
  if (lower.includes('network') || lower.includes('timeout') || lower.includes('connection')) return 'network'
  if (lower.includes('server') || lower.includes('unavailable') || lower.includes('500')) return 'api_down'
  return 'unknown'
}

const ERROR_CONFIG: Record<ErrorType, { icon: string; title: string; color: string; bgColor: string }> = {
  not_found: {
    icon: 'search_off',
    title: 'Vehicle Not Found',
    color: 'text-slate-400',
    bgColor: 'bg-slate-100',
  },
  api_down: {
    icon: 'cloud_off',
    title: 'Service Unavailable',
    color: 'text-red-400',
    bgColor: 'bg-red-50',
  },
  network: {
    icon: 'wifi_off',
    title: 'Connection Error',
    color: 'text-orange-400',
    bgColor: 'bg-orange-50',
  },
  rate_limit: {
    icon: 'speed',
    title: 'Too Many Requests',
    color: 'text-amber-500',
    bgColor: 'bg-amber-50',
  },
  unknown: {
    icon: 'error_outline',
    title: 'Something Went Wrong',
    color: 'text-slate-400',
    bgColor: 'bg-slate-100',
  },
}

export default function VehicleNoData({ message, onTryAgain }: Props) {
  const errorType = getErrorType(message)
  const config = ERROR_CONFIG[errorType]

  return (
    <div className="px-6 max-w-2xl mx-auto py-16 text-center">
      <div className={`w-24 h-24 ${config.bgColor} rounded-2xl flex items-center justify-center mx-auto mb-6`}>
        <span className={`material-symbols-outlined ${config.color} text-5xl`}>{config.icon}</span>
      </div>
      <h3 className="text-xl font-bold mb-2 text-slate-800">{config.title}</h3>
      <p className="text-slate-500 text-base mb-6 leading-relaxed max-w-md mx-auto">{message}</p>
      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        <button
          type="button"
          onClick={onTryAgain}
          className="bg-primary text-white px-6 py-3 rounded-xl font-semibold hover:shadow-lg transition-all"
        >
          Try Again
        </button>
        {errorType === 'api_down' || errorType === 'network' ? (
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="bg-slate-100 text-slate-600 px-6 py-3 rounded-xl font-semibold hover:bg-slate-200 transition-all"
          >
            Refresh Page
          </button>
        ) : null}
      </div>
    </div>
  )
}
