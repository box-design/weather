import { useEffect, useRef } from 'react';
import { App } from '@capacitor/app';
import { useLocation, useNavigate } from 'react-router';
import { toast } from 'sonner';

export function useBackButton() {
  const location = useLocation();
  const navigate = useNavigate();
  const lastBackPress = useRef(0);

  useEffect(() => {
    const handler = App.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        navigate(-1);
      } else {
        const now = Date.now();
        if (now - lastBackPress.current < 2000) {
          App.exitApp();
        } else {
          lastBackPress.current = now;
          toast('再按一次退出应用', {
            duration: 2000,
            position: 'bottom-center',
          });
        }
      }
    });

    return () => {
      handler.remove();
    };
  }, [location, navigate]);
}