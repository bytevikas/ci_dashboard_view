type Props = {
  message: string
  onTryAgain: () => void
}

type ErrorType = 'not_found' | 'no_response' | 'config' | 'api_down' | 'network' | 'rate_limit' | 'unknown'

function getErrorType(message: string): ErrorType {
  const lower = message.toLowerCase()
  if (lower.includes('no response') || lower.includes('empty response')) return 'no_response'
  if (lower.includes('not found') || lower.includes('no data') || lower.includes('invalid')) return 'not_found'
  if (lower.includes('too many') || lower.includes('rate') || lower.includes('limit') || lower.includes('wait a moment') || lower.includes('slow down')) return 'rate_limit'
  if (lower.includes('api key') || lower.includes('not configured') || lower.includes('not set')) return 'config'
  if (lower.includes('network') || lower.includes('timeout') || lower.includes('timed out') || lower.includes('connection') || lower.includes('cannot reach')) return 'network'
  if (lower.includes('server') || lower.includes('unavailable') || lower.includes('500') || lower.includes('502') || lower.includes('503') || lower.includes('504')) return 'api_down'
  return 'unknown'
}

const ERROR_CONFIG: Record<ErrorType, {
  icon: string
  title: string
  friendlyMessage: string
  color: string
  bgColor: string
}> = {
  not_found: {
    icon: 'search_off',
    title: 'No Data Found',
    friendlyMessage: 'We couldn\'t find any records for this registration number. Please check the number and try again.',
    color: 'text-slate-400',
    bgColor: 'bg-slate-100',
  },
  no_response: {
    icon: 'cloud_off',
    title: 'No Response Received',
    friendlyMessage: 'We didn\'t get a response from the vehicle data service. This can happen during peak hours. Please try again in a moment.',
    color: 'text-slate-400',
    bgColor: 'bg-slate-100',
  },
  config: {
    icon: 'build_circle',
    title: 'Service Setup Required',
    friendlyMessage: 'The vehicle lookup service is not fully configured yet. Our team is aware and working on it.',
    color: 'text-amber-600',
    bgColor: 'bg-amber-50',
  },
  api_down: {
    icon: 'cloud_off',
    title: 'Service Temporarily Unavailable',
    friendlyMessage: 'The vehicle data service is currently experiencing issues. This is usually temporary — please try again in a few minutes.',
    color: 'text-red-400',
    bgColor: 'bg-red-50',
  },
  network: {
    icon: 'wifi_off',
    title: 'Connection Issue',
    friendlyMessage: 'We\'re having trouble reaching the server. Please check your internet connection and try again.',
    color: 'text-orange-400',
    bgColor: 'bg-orange-50',
  },
  rate_limit: {
    icon: 'speed',
    title: 'Too Many Requests',
    friendlyMessage: 'You\'ve made too many requests. Please wait a moment before trying again.',
    color: 'text-amber-500',
    bgColor: 'bg-amber-50',
  },
  unknown: {
    icon: 'error_outline',
    title: 'Something Went Wrong',
    friendlyMessage: 'An unexpected issue occurred while fetching vehicle data. Our team has been notified. Please try again shortly.',
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
      <h3 className="text-xl font-bold mb-3 text-slate-800">{config.title}</h3>
      <p className="text-slate-500 text-base mb-6 leading-relaxed max-w-md mx-auto">
        {config.friendlyMessage}
      </p>

      {(errorType === 'api_down' || errorType === 'network' || errorType === 'unknown') && (
        <p className="text-slate-400 text-xs mb-6 max-w-sm mx-auto">
          If this issue persists, please contact support or report the problem.
        </p>
      )}

      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        <button
          type="button"
          onClick={onTryAgain}
          className="bg-primary text-white px-6 py-3 rounded-xl font-semibold hover:shadow-lg transition-all"
        >
          Try Again
        </button>
        {(errorType === 'api_down' || errorType === 'network' || errorType === 'unknown') && (
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="bg-slate-100 text-slate-600 px-6 py-3 rounded-xl font-semibold hover:bg-slate-200 transition-all"
          >
            Refresh Page
          </button>
        )}
      </div>
    </div>
  )
}
