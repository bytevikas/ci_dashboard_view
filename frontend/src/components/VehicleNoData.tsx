type Props = {
  message: string
  onTryAgain: () => void
}

type ErrorType = 'not_found' | 'config' | 'api_down' | 'network' | 'rate_limit' | 'unknown'

function getErrorType(message: string): ErrorType {
  const lower = message.toLowerCase()
  if (lower.includes('api key') || lower.includes('not configured') || lower.includes('vahan')) return 'config'
  if (lower.includes('not found') || lower.includes('no data')) return 'not_found'
  if (lower.includes('too many') || lower.includes('rate') || lower.includes('limit')) return 'rate_limit'
  if (lower.includes('network') || lower.includes('timeout') || lower.includes('connection')) return 'network'
  if (lower.includes('server') || lower.includes('unavailable') || lower.includes('500')) return 'api_down'
  return 'unknown'
}

const ERROR_CONFIG: Record<ErrorType, { icon: string; title: string; color: string; bgColor: string }> = {
  not_found: {
    icon: 'search_off',
    title: 'No Data for This Registration',
    color: 'text-slate-400',
    bgColor: 'bg-slate-100',
  },
  config: {
    icon: 'key_off',
    title: 'Search Not Configured',
    color: 'text-amber-600',
    bgColor: 'bg-amber-50',
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
      <p className="text-slate-500 text-base mb-2 leading-relaxed max-w-md mx-auto">{message}</p>
      {errorType === 'not_found' && (
        <p className="text-slate-400 text-sm mb-6 max-w-md mx-auto">Check the number is correct and try again, or use a different registration number.</p>
      )}
      {errorType === 'config' && (
        <p className="text-amber-700/80 text-sm mb-6 max-w-md mx-auto">In the backend directory run: <code className="bg-amber-100 px-1.5 py-0.5 rounded">export VAHAN_API_KEY=your-key</code> then restart with <code className="bg-amber-100 px-1.5 py-0.5 rounded">mvn spring-boot:run</code>. See RUN.md for details.</p>
      )}
      {(errorType !== 'not_found' && errorType !== 'config') && <div className="mb-6" />}
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
