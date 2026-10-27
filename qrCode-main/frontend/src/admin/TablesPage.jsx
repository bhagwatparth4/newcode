import { useCallback, useEffect, useState } from 'react'
import {
  createCounterQr, deleteTable, downloadCounterQr, downloadTableQr, getCounterQr, getTables, saveTable,
} from '../api/staff'
import { ErrorNote, PageLoader, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'

// Tables (one QR each) and the single counter / walk-in QR.
export default function TablesPage() {
  const toast = useToast()
  const [tables, setTables] = useState(null)
  const [counter, setCounter] = useState(undefined) // undefined = loading, null = none
  const [error, setError] = useState('')
  const [newNumber, setNewNumber] = useState('')

  const load = useCallback(() => {
    getTables().then((t) => { setTables(t.sort((a, b) => a.tableNumber.localeCompare(b.tableNumber, undefined, { numeric: true }))) }).catch((e) => setError(e.message))
    getCounterQr().then(setCounter).catch(() => setCounter(null))
  }, [])
  useEffect(() => { load() }, [load])

  const run = async (action, message) => {
    try {
      await action()
      if (message) toast(message)
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const addTable = (e) => {
    e.preventDefault()
    const number = newNumber.trim()
    if (!number) return
    run(() => saveTable({ tableNumber: number, active: true }), `Table ${number.toUpperCase()} added`)
    setNewNumber('')
  }

  const rename = (table) => {
    const number = window.prompt('New table number', table.tableNumber)
    if (number && number.trim()) run(() => saveTable({ ...table, tableNumber: number.trim() }), 'Table renamed. Its printed QR still works.')
  }

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!tables) return <PageLoader label="Loading tables" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-semibold">Tables & QR codes</h1>
        <p className="text-stone-500">Print each table's QR and place it on that table. Renaming a table keeps its QR working.</p>
      </div>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <h2 className="font-semibold">Counter / Order here QR</h2>
        <p className="mt-1 text-sm text-stone-500">One shared QR for walk-in customers. Orders show as “Counter” with the customer's name.</p>
        <div className="mt-4 flex flex-wrap gap-2">
          {counter === undefined && <span className="text-sm text-stone-500">Loading…</span>}
          {counter === null && (
            <button className={btn.primary} onClick={() => run(createCounterQr, 'Counter QR created')}>Create counter QR</button>
          )}
          {counter && (
            <>
              <button className={btn.primary} onClick={() => run(() => downloadCounterQr(counter.id))}>Download counter QR</button>
            </>
          )}
        </div>
      </section>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <form onSubmit={addTable} className="flex flex-wrap items-end gap-2">
          <label className="text-sm">
            <span className="mb-1 block font-medium">Add table</span>
            <input className={`${input} w-40`} placeholder="e.g. T1" maxLength={20} value={newNumber} onChange={(e) => setNewNumber(e.target.value)} />
          </label>
          <button className={btn.primary}>Add</button>
        </form>

        <ul className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {tables.length === 0 && <li className="text-sm text-stone-500">No tables yet.</li>}
          {tables.map((table) => (
            <li key={table.id} className={`rounded-2xl border p-4 ${table.active ? 'border-stone-200' : 'border-dashed border-stone-300 opacity-70'}`}>
              <div className="flex items-center justify-between">
                <span className="font-display text-2xl font-semibold">{table.tableNumber}</span>
                <span className={`text-xs font-semibold ${table.active ? 'text-mint-deep' : 'text-stone-500'}`}>{table.active ? 'Active' : 'Off'}</span>
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-sm">
                <button className={btn.ghost} onClick={() => run(() => downloadTableQr(table))}>Download QR</button>
                <button className={btn.ghost} onClick={() => rename(table)}>Rename</button>
                <button className={btn.ghost} onClick={() => run(() => saveTable({ ...table, active: !table.active }), table.active ? 'Table switched off' : 'Table active')}>
                  {table.active ? 'Switch off' : 'Switch on'}
                </button>
                <button
                  className={btn.danger}
                  onClick={() => window.confirm(`Delete table ${table.tableNumber}?`) && run(() => deleteTable(table.id), 'Table deleted')}
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
