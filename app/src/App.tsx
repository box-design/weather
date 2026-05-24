import { Routes, Route } from 'react-router'
import Home from './pages/Home'
import NotFound from "./pages/NotFound"
import { useStatusBar } from './hooks/useStatusBar'
import { useBackButton } from './hooks/useBackButton'

export default function App() {
  useStatusBar()
  useBackButton()

  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
