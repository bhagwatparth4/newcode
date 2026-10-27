/*
 * Makes a phone photo small and fast before uploading:
 *  - longest side at most 1600 px
 *  - saved as JPEG (quality 0.85), usually 150–500 KB instead of 3–8 MB
 *  - removes hidden photo data (EXIF), including the GPS location
 * Customers' menus load much faster on mobile data because of this.
 */
const MAX_SIDE = 1600

export async function prepareImage(file) {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose a photo (JPG, PNG or WEBP).')
  }

  const bitmap = await loadImage(file)
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
  const width = Math.round(bitmap.width * scale)
  const height = Math.round(bitmap.height * scale)

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#ffffff' // transparent PNGs get a white background
  ctx.fillRect(0, 0, width, height)
  ctx.drawImage(bitmap, 0, 0, width, height)

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85))
  if (!blob) throw new Error('Could not process this photo. Please try another one.')

  return new File([blob], 'photo.jpg', { type: 'image/jpeg' })
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('This photo format is not supported. Please use JPG or PNG.'))
    }
    img.src = url
  })
}
