import { useState, useEffect, useRef, useCallback } from "react";

interface ScrollBlurStyle {
  filter: string;
}

export function useScrollBlur(threshold = 70, maxBlur = 2) {
  const ref = useRef<HTMLDivElement>(null);
  const [blurStyle, setBlurStyle] = useState<ScrollBlurStyle>({
    filter: "none",
  });

  const update = useCallback(() => {
    if (!ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    const top = rect.top;

    let progress = 0;
    if (top < threshold) {
      progress = Math.max(0, Math.min(1, 1 - top / threshold));
    }

    // easeOut quad for smoother, more natural feel
    const eased = 1 - (1 - progress) * (1 - progress);
    const blur = eased * maxBlur;

    setBlurStyle({
      filter: blur > 0.1 ? `blur(${blur.toFixed(2)}px)` : "none",
    });
  }, [threshold, maxBlur]);

  useEffect(() => {
    let raf = 0;
    const onScroll = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(update);
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    update();
    return () => {
      window.removeEventListener("scroll", onScroll);
      cancelAnimationFrame(raf);
    };
  }, [update]);

  return { ref, blurStyle };
}
