import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { SplashScreen } from '@capacitor/splash-screen'
import './index.css'
import { AppProvider } from "@/providers/AppProvider"
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AppProvider>
      <App />
    </AppProvider>
  </BrowserRouter>,
)

SplashScreen.hide().catch(() => {})
