interface SpendlyLogoProps {
  size?: number
}

export function SpendlyLogo({ size = 32 }: SpendlyLogoProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="0" width="120" height="120" rx="28" fill="#dceeff" />
      <rect x="18" y="32" width="84" height="58" rx="10" fill="#2a7ec8" />
      <rect x="18" y="32" width="84" height="20" rx="10" fill="#1a6ab8" />
      <rect x="18" y="42" width="84" height="10" fill="#1a6ab8" />
      <rect x="72" y="50" width="24" height="16" rx="6" fill="#5ab4f0" opacity="0.9" />
      <circle cx="80" cy="58" r="4" fill="#dceeff" opacity="0.9" />
      <circle cx="90" cy="58" r="4" fill="#1a5a9a" opacity="0.5" />
      <rect x="26" y="52" width="28" height="3" rx="1.5" fill="#9ad0f5" opacity="0.7" />
      <rect x="26" y="60" width="18" height="3" rx="1.5" fill="#9ad0f5" opacity="0.4" />
      <polyline
        points="22,86 36,76 50,81 64,68 78,71 94,58"
        fill="none"
        stroke="#40e08a"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="94" cy="58" r="4.5" fill="#40e08a" />
      <circle cx="64" cy="68" r="3" fill="#40e08a" opacity="0.6" />
    </svg>
  )
}
