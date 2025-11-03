import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import { ErrorBoundary } from './components/ErrorBoundary.tsx'
import { watchOrders } from './utils/orderWatcher'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>,
)

// Use contextBridge
if (window.ipcRenderer) {
  window.ipcRenderer.on('main-process-message', (_event, message) => {
    console.log(message)
  })
}

// Start order watcher for notifications (renderer-side Firestore listener)
if (window.electronAPI) {
  try {
    watchOrders();
  } catch (error) {
    console.error('[main.tsx] Failed to start order watcher:', error);
  }
}
