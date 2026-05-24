import { useEffect } from 'react';
import { StatusBar, Style } from '@capacitor/status-bar';

export function useStatusBar() {
  useEffect(() => {
    const setupStatusBar = async () => {
      try {
        await StatusBar.setStyle({ style: Style.Dark });
        await StatusBar.setBackgroundColor({ color: '#0f172a' });
        await StatusBar.setOverlaysWebView({ overlays: false });
      } catch {
        // Not running on a native device, silently ignore
      }
    };

    setupStatusBar();
  }, []);
}