import { useEffect, useRef } from "react";
import type { WeatherType } from "@/lib/weather/codes";
import { ASSET_VERSION } from "@/const";

interface WeatherBackgroundProps {
  weatherType: WeatherType;
  isDay: boolean;
  scrollY?: number;
}

interface RainParticle {
  x: number; y: number; speed: number; length: number; opacity: number; angle: number;
}

interface SnowParticle {
  x: number; y: number; speed: number; opacity: number; swayOffset: number; swaySpeed: number; size: number;
}

interface Star {
  x: number; y: number; size: number; baseOpacity: number; twinkleSpeed: number; twinkleOffset: number;
}

export default function WeatherBackground({ weatherType, isDay, scrollY = 0 }: WeatherBackgroundProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const particlesRef = useRef<{
    rain: RainParticle[]; snow: SnowParticle[]; stars: Star[];
  }>({ rain: [], snow: [], stars: [] });
  const animRef = useRef<number>(0);
  const bgImageRef = useRef<HTMLImageElement | null>(null);
  const timeRef = useRef(0);

  const getBgImage = () => {
    if (!isDay) return `/bg-night-clear.jpg?${ASSET_VERSION}`;
    switch (weatherType) {
      case "clear": return `/bg-sunny.jpg?${ASSET_VERSION}`;
      case "cloudy": return `/bg-cloudy.jpg?${ASSET_VERSION}`;
      case "rain": return `/bg-rainy.jpg?${ASSET_VERSION}`;
      case "snow": return `/bg-snowy.jpg?${ASSET_VERSION}`;
      case "fog": return `/bg-foggy.jpg?${ASSET_VERSION}`;
      case "storm": return `/bg-rainy.jpg?${ASSET_VERSION}`;
      default: return `/bg-sunny.jpg?${ASSET_VERSION}`;
    }
  };

  useEffect(() => {
    const img = new Image();
    img.src = getBgImage();
    img.onload = () => {
      bgImageRef.current = img;
    };
  }, [weatherType, isDay]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener("resize", resize);

    // Initialize particles
    const W = canvas.width;
    const H = canvas.height;

    particlesRef.current = {
      rain: Array.from({ length: weatherType === "rain" || weatherType === "storm" ? 600 : 0 }, () => ({
        x: Math.random() * W,
        y: Math.random() * H,
        speed: 8 + Math.random() * 12,
        length: 15 + Math.random() * 20,
        opacity: 0.2 + Math.random() * 0.4,
        angle: weatherType === "storm" ? 0.25 + Math.random() * 0.1 : 0.12 + Math.random() * 0.06,
      })),
      snow: Array.from({ length: weatherType === "snow" ? 250 : 0 }, () => ({
        x: Math.random() * W,
        y: Math.random() * H,
        speed: 0.3 + Math.random() * 0.8,
        opacity: 0.3 + Math.random() * 0.5,
        swayOffset: Math.random() * Math.PI * 2,
        swaySpeed: 0.5 + Math.random() * 1.5,
        size: 1.5 + Math.random() * 2.5,
      })),
      stars: Array.from({ length: !isDay && weatherType === "clear" ? 180 : 0 }, () => ({
        x: Math.random() * W,
        y: Math.random() * H * 0.7,
        size: 0.5 + Math.random() * 1.5,
        baseOpacity: 0.3 + Math.random() * 0.7,
        twinkleSpeed: 0.5 + Math.random() * 2,
        twinkleOffset: Math.random() * Math.PI * 2,
      })),
    };

    const animate = (time: number) => {
      timeRef.current = time;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const parallaxY = scrollY * 0.15;

      // Draw background image with cover + parallax
      if (bgImageRef.current) {
        const img = bgImageRef.current;
        const imgRatio = img.width / img.height;
        const canvasRatio = canvas.width / canvas.height;
        let sx = 0, sy = 0, sw = img.width, sh = img.height;
        if (canvasRatio > imgRatio) {
          sh = img.width / canvasRatio;
          sy = (img.height - sh) / 2;
        } else {
          sw = img.height * canvasRatio;
          sx = (img.width - sw) / 2;
        }
        // Parallax: shift source y slightly based on scroll
        const parallaxSy = Math.max(0, Math.min(sy + parallaxY * 0.5, img.height - sh));
        ctx.drawImage(img, sx, parallaxSy, sw, sh, 0, 0, canvas.width, canvas.height);
      }

      // Draw dark overlay
      const overlayOpacity = weatherType === "rain" || weatherType === "storm" ? 0.35
        : weatherType === "snow" ? 0.2
        : weatherType === "fog" ? 0.3
        : isDay ? 0.08 : 0.5;
      ctx.fillStyle = `rgba(0, 0, 0, ${overlayOpacity})`;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Stars (night clear)
      if (particlesRef.current.stars.length > 0) {
        for (const s of particlesRef.current.stars) {
          const twinkle = Math.sin(time * 0.001 * s.twinkleSpeed + s.twinkleOffset);
          const opacity = s.baseOpacity * (0.5 + twinkle * 0.5);
          ctx.beginPath();
          ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(255, 255, 255, ${opacity})`;
          ctx.fill();
          // Star glow for larger stars
          if (s.size > 1) {
            ctx.beginPath();
            ctx.arc(s.x, s.y, s.size * 3, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(255, 255, 255, ${opacity * 0.08})`;
            ctx.fill();
          }
        }
      }

      // Snow
      if (particlesRef.current.snow.length > 0) {
        for (const p of particlesRef.current.snow) {
          const sway = Math.sin(time * 0.001 * p.swaySpeed + p.swayOffset) * 1.5;
          p.x += sway;
          p.y += p.speed;
          if (p.y > canvas.height) {
            p.y = -10;
            p.x = Math.random() * canvas.width;
          }
          if (p.x > canvas.width + 10) p.x = -10;
          if (p.x < -10) p.x = canvas.width + 10;
          ctx.beginPath();
          ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(255, 255, 255, ${p.opacity})`;
          ctx.fill();
          // Soft glow
          ctx.beginPath();
          ctx.arc(p.x, p.y, p.size * 3, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(255, 255, 255, ${p.opacity * 0.05})`;
          ctx.fill();
        }
      }

      // Rain
      if (particlesRef.current.rain.length > 0) {
        ctx.save();
        for (const p of particlesRef.current.rain) {
          p.x += Math.sin(p.angle) * p.speed;
          p.y += Math.cos(p.angle) * p.speed;
          if (p.y > canvas.height) {
            p.y = -p.length;
            p.x = Math.random() * canvas.width;
          }
          ctx.beginPath();
          ctx.moveTo(p.x, p.y);
          ctx.lineTo(p.x + Math.sin(p.angle) * p.length, p.y + Math.cos(p.angle) * p.length);
          ctx.strokeStyle = `rgba(180, 200, 230, ${p.opacity})`;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
        ctx.restore();
      }

      // Fog overlay
      if (weatherType === "fog") {
        const fogGrad = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
        fogGrad.addColorStop(0, `rgba(200, 210, 220, ${0.12 + Math.sin(time * 0.0005) * 0.04})`);
        fogGrad.addColorStop(0.5, `rgba(220, 225, 230, ${0.06 + Math.sin(time * 0.0007) * 0.03})`);
        fogGrad.addColorStop(1, `rgba(200, 210, 220, ${0.1 + Math.sin(time * 0.0006) * 0.03})`);
        ctx.fillStyle = fogGrad;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
      }

      // Storm lightning flash
      if (weatherType === "storm") {
        const flash = Math.sin(time * 0.003) * Math.sin(time * 0.007) * Math.sin(time * 0.011);
        if (flash > 0.97) {
          ctx.fillStyle = `rgba(255, 255, 255, ${(flash - 0.97) * 15 * 0.15})`;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
        }
      }

      // Vignette overlay
      const vignette = ctx.createRadialGradient(
        canvas.width / 2, canvas.height / 2, canvas.height * 0.3,
        canvas.width / 2, canvas.height / 2, canvas.height * 0.9
      );
      vignette.addColorStop(0, "rgba(0,0,0,0)");
      vignette.addColorStop(1, "rgba(0,0,0,0.25)");
      ctx.fillStyle = vignette;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      animRef.current = requestAnimationFrame(animate);
    };

    animRef.current = requestAnimationFrame(animate);

    return () => {
      cancelAnimationFrame(animRef.current);
      window.removeEventListener("resize", resize);
    };
  }, [weatherType, isDay, scrollY]);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        zIndex: 0,
      }}
    />
  );
}
