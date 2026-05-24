import { useState, useEffect, useRef, useCallback } from "react";

export function useScrollProgress() {
  const [scrollY, setScrollY] = useState(0);
  const [scrollProgress, setScrollProgress] = useState(0);
  const rafRef = useRef<number>(0);
  const lastScrollRef = useRef(0);

  const handleScroll = useCallback(() => {
    const y = window.scrollY;
    if (y === lastScrollRef.current) return;
    lastScrollRef.current = y;
    const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
    setScrollY(y);
    setScrollProgress(maxScroll > 0 ? Math.min(y / (window.innerHeight * 0.6), 1) : 0);
  }, []);

  useEffect(() => {
    const onScroll = () => {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = requestAnimationFrame(handleScroll);
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    handleScroll();
    return () => {
      window.removeEventListener("scroll", onScroll);
      cancelAnimationFrame(rafRef.current);
    };
  }, [handleScroll]);

  return { scrollY, scrollProgress };
}

export function useMousePosition() {
  const [mousePos, setMousePos] = useState({ x: -1000, y: -1000 });
  const rafRef = useRef<number>(0);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = requestAnimationFrame(() => {
        setMousePos({ x: e.clientX, y: e.clientY });
      });
    };
    window.addEventListener("mousemove", onMove, { passive: true });
    return () => {
      window.removeEventListener("mousemove", onMove);
      cancelAnimationFrame(rafRef.current);
    };
  }, []);

  return mousePos;
}
