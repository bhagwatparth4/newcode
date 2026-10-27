///*
// * Per-client branding. Change these through the frontend .env file
// * (see .env.example) and replace the logo files in /public.
// * Colours live in src/index.css (@theme block).
// */
//const env = import.meta.env
//
//export const BRAND = {
//  name: env.VITE_CAFE_NAME || 'Rush Hours',
//  tagline: env.VITE_CAFE_TAGLINE || 'Brews · Bites · Tales',
//  address: env.VITE_CAFE_ADDRESS || '',
//  phone: env.VITE_CAFE_PHONE || '',
//  logo: '/logo.png',
//  // Greyscale JPEG used inside the downloadable PDF receipt
//  receiptLogo: '/logo-receipt.jpg',
//  // Colour used by the Razorpay payment window
//  razorpayColor: '#2B211C',
//}
//
//export const API_URL = (env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')
//
//export const WS_URL =
//  env.VITE_WS_URL || API_URL.replace(/^http/, 'ws') + '/ws'




/*
 * Per-client branding. Change these through the frontend .env file
 * (see .env.example) and replace the logo files in /public.
 * Colours live in src/index.css (@theme block).
 */
const env = import.meta.env

export const BRAND = {
  name: env.VITE_CAFE_NAME || 'Rush Hours',
  tagline: env.VITE_CAFE_TAGLINE || 'Brews · Bites · Tales',
  address: env.VITE_CAFE_ADDRESS || '',
  phone: env.VITE_CAFE_PHONE || '',
  logo: '/logo.png',
  // Greyscale JPEG used inside the downloadable PDF receipt
  receiptLogo: '/logo-receipt.jpg',
  // Colour used by the Razorpay payment window
  razorpayColor: '#2B211C',
}

/*
 * Backend address.
 * - Development: leave VITE_API_URL empty. Requests go to the same address
 *   as the website and the Vite dev server forwards them (vite.config.js).
 * - Production: set VITE_API_URL to the real backend, e.g. https://api.mycafe.com
 */
export const API_URL = (env.VITE_API_URL || '').replace(/\/$/, '')

const sameSiteWs = () =>
  `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`

export const WS_URL =
  env.VITE_WS_URL || (API_URL ? API_URL.replace(/^http/, 'ws') + '/ws' : sameSiteWs())