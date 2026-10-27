/*
 * The brand hourglass, used as the preparation timer.
 * `progress` goes from 0 (just started) to 1 (estimated time reached).
 */
export default function Hourglass({ progress = 0, running = true, size = 120 }) {
  const p = Math.min(1, Math.max(0, progress))
  const topY = 18 + 52 * p
  const bottomH = 52 * p

  return (
    <svg width={size} height={(size * 140) / 100} viewBox="0 0 100 140" role="img" aria-label="Preparation progress">
      <defs>
        <clipPath id="hg-top">
          <path d="M22 18 C22 48 44 60 48 70 L52 70 C56 60 78 48 78 18 Z" />
        </clipPath>
        <clipPath id="hg-bottom">
          <path d="M48 70 C44 80 22 92 22 122 L78 122 C78 92 56 80 52 70 Z" />
        </clipPath>
      </defs>

      {/* glass */}
      <path
        d="M22 18 C22 48 44 60 48 70 C44 80 22 92 22 122 L78 122 C78 92 56 80 52 70 C56 60 78 48 78 18 Z"
        fill="#ffffff"
        fillOpacity="0.7"
        stroke="#4a3a30"
        strokeWidth="2.5"
      />

      {/* sand */}
      <rect x="0" y={topY} width="100" height={70 - topY} fill="#6b4a2f" clipPath="url(#hg-top)" />
      <rect x="0" y={122 - bottomH} width="100" height={bottomH} fill="#6b4a2f" clipPath="url(#hg-bottom)" />
      {running && p < 1 && (
        <line x1="50" y1="70" x2="50" y2={122 - bottomH} stroke="#6b4a2f" strokeWidth="1.5" strokeDasharray="2 3">
          <animate attributeName="stroke-dashoffset" from="0" to="-10" dur="0.6s" repeatCount="indefinite" />
        </line>
      )}

      {/* bronze frame */}
      <rect x="12" y="8" width="76" height="10" rx="4" fill="#9a7446" />
      <rect x="12" y="122" width="76" height="10" rx="4" fill="#9a7446" />
    </svg>
  )
}
