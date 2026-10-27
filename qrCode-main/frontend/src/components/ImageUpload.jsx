import { useRef, useState } from 'react'
import { uploadImage } from '../api/client'
import { prepareImage } from '../lib/prepareImage'
import { Spinner, btn, input } from './ui'

/*
 * "Upload photo" box used by the Admin menu forms.
 * Admin picks a photo -> it is resized in the browser -> uploaded to the
 * cafe's storage (local / S3 / Cloudinary) -> the returned URL is passed
 * to onChange and saved with the item/category like before.
 */
export default function ImageUpload({ value, onChange, folder, aspect = 'aspect-[4/3]', label = 'Photo' }) {
  const fileInput = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [showLink, setShowLink] = useState(false)

  const choose = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = '' // allow picking the same file again
    if (!file) return
    setError('')
    setUploading(true)
    try {
      const small = await prepareImage(file)
      const url = await uploadImage(small, folder)
      onChange(url)
    } catch (e) {
      setError(e.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <span className="mb-1.5 block text-sm font-medium text-espresso/80">{label}</span>

      <div className={`relative ${aspect} w-full overflow-hidden rounded-2xl border border-dashed border-espresso/25 bg-stone-50`}>
        {value ? (
          <img src={value} alt="" className="h-full w-full object-cover" />
        ) : (
          <button
            type="button"
            onClick={() => fileInput.current?.click()}
            className="flex h-full w-full flex-col items-center justify-center gap-1 text-sm text-espresso/60"
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <circle cx="9" cy="10" r="1.8" />
              <path d="M21 16l-5-5-8 8" />
            </svg>
            Tap to upload a photo
          </button>
        )}
        {uploading && (
          <div className="absolute inset-0 flex items-center justify-center gap-2 bg-white/80 text-sm font-medium">
            <Spinner /> Uploading…
          </div>
        )}
      </div>

      <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={choose} />

      <div className="mt-2 flex flex-wrap items-center gap-2">
        <button type="button" className={btn.ghost} disabled={uploading} onClick={() => fileInput.current?.click()}>
          {value ? 'Change photo' : 'Upload photo'}
        </button>
        {value && (
          <button type="button" className={btn.danger} disabled={uploading} onClick={() => onChange('')}>
            Remove
          </button>
        )}
        <button
          type="button"
          className="ml-auto text-xs text-espresso/50 underline-offset-2 hover:underline"
          onClick={() => setShowLink((v) => !v)}
        >
          {showLink ? 'Hide link' : 'Use a link instead'}
        </button>
      </div>

      {showLink && (
        <input
          className={`${input} mt-2`}
          type="url"
          maxLength={500}
          placeholder="https://…"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}

      {error && <p className="mt-2 text-sm text-red-700">{error}</p>}
      <p className="mt-1 text-xs text-espresso/50">JPG, PNG or WEBP. Large photos are resized automatically.</p>
    </div>
  )
}
