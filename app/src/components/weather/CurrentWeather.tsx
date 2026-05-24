import { useEffect, useRef, useState } from "react";
import {
  Droplets, Wind, Eye, Gauge, Sun, CloudRain, Snowflake,
  Cloud, CloudLightning, CloudFog, CloudSun,
} from "lucide-react";
import { getWeatherInfo, getAqiLabel } from "@/lib/weather/codes";
import type { WeatherType } from "@/lib/weather/codes";

interface CurrentWeatherProps {
  current: {
    temperature: number;
    humidity: number;
    apparentTemperature: number;
    weatherCode: number;
    windSpeed: number;
    windDirection: number;
    windGusts: number;
    pressure: number;
    cloudCover: number;
    isDay: boolean;
    precipitation: number;
    rain: number;
    showers: number;
    snowfall: number;
  };
  airQuality?: {
    pm10: number;
    pm2_5: number;
    euAqi: number;
    usAqi: number;
  };
  daily?: {
    tempMax: number;
    tempMin: number;
  };
  cityName: string;
  timezone?: string;
  scrollY?: number;
  isWideLayout?: boolean;
}

function getWeatherIcon(type: WeatherType, isDay: boolean, className: string) {
  if (type === "clear" && !isDay) return <Sun className={className} />;
  const iconMap: Record<WeatherType, React.ReactNode> = {
    clear: isDay ? <Sun className={className} /> : <Sun className={className} />,
    cloudy: <CloudSun className={className} />,
    rain: <CloudRain className={className} />,
    snow: <Snowflake className={className} />,
    fog: <CloudFog className={className} />,
    storm: <CloudLightning className={className} />,
  };
  return iconMap[type] || <Cloud className={className} />;
}

function AnimatedNumber({ value, suffix = "" }: { value: number; suffix?: string }) {
  const [display, setDisplay] = useState(value);
  const prevRef = useRef(value);

  useEffect(() => {
    const prev = prevRef.current;
    if (prev === value) return;
    prevRef.current = value;
    const duration = 800;
    const start = performance.now();
    const animate = (now: number) => {
      const t = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 3);
      setDisplay(Math.round(prev + (value - prev) * ease));
      if (t < 1) requestAnimationFrame(animate);
    };
    requestAnimationFrame(animate);
  }, [value]);

  return <>{display}{suffix}</>;
}

export default function CurrentWeather({ current, airQuality, daily, cityName, timezone, scrollY = 0, isWideLayout = false }: CurrentWeatherProps) {
  const cardRef = useRef<HTMLDivElement>(null);
  const info = getWeatherInfo(current.weatherCode);
  const aqiLabel = airQuality ? getAqiLabel(airQuality.euAqi) : null;

  const localTime = new Date().toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: timezone || undefined,
  });

  const localDate = new Date().toLocaleDateString("zh-CN", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
    timeZone: timezone || undefined,
  });

  const rawProgress = Math.min(scrollY / 380, 1);
  // easeOut quad for smoother, more natural fade
  const scrollProgress = 1 - (1 - rawProgress) * (1 - rawProgress);
  const blurAmount = scrollProgress * 12;
  const scaleValue = 1 - scrollProgress * 0.07;
  const opacityValue = 1 - scrollProgress * 0.88;
  const translateY = scrollProgress * -28;

  useEffect(() => {
    if (cardRef.current && !isWideLayout) {
      cardRef.current.style.opacity = "0";
      cardRef.current.style.transform = "translateY(30px) scale(0.95)";
      requestAnimationFrame(() => {
        if (cardRef.current) {
          cardRef.current.style.transition = "all 0.8s cubic-bezier(0.22, 1, 0.36, 1)";
          cardRef.current.style.opacity = "1";
          cardRef.current.style.transform = "translateY(0) scale(1)";
        }
      });
    }
  }, [current.weatherCode, isWideLayout]);

  // Wide layout: plain text design without card background
  if (isWideLayout) {
    return (
      <div
        ref={cardRef}
        className="transition-all duration-500"
        style={{
          filter: `blur(${blurAmount}px)`,
          transform: `scale(${scaleValue}) translateY(${translateY}px)`,
          opacity: opacityValue,
          pointerEvents: opacityValue < 0.15 ? "none" : "auto",
        }}
      >
        {/* Location and date */}
        <div className="mb-1">
          <span className="text-white/40 text-xs tracking-wide">{localDate}</span>
        </div>
        <h2 className="text-white text-lg font-medium mb-8 opacity-90">{cityName}</h2>

        {/* Large temperature display */}
        <div className="flex items-start gap-2 mb-3">
          {getWeatherIcon(info.type, current.isDay, "w-8 h-8 text-white mt-2")}
          <span
            className="text-white font-bold leading-none tabular-nums"
            style={{
              fontSize: "88px",
              lineHeight: 1,
              fontFamily: "Inter, sans-serif",
              textShadow: "0 4px 30px rgba(0,0,0,0.3)",
            }}
          >
            <AnimatedNumber value={Math.round(current.temperature)} />
          </span>
          <span className="text-white/60 text-3xl font-light self-start mt-2">°C</span>
        </div>

        {/* Weather description and high/low */}
        <div className="flex items-center gap-2 flex-wrap mb-2">
          <span className="text-white/70 text-sm">{info.label}</span>
          {daily && (
            <>
              <span className="text-white/30">·</span>
              <span className="text-white/50 text-sm">
                最高 {Math.round(daily.tempMax)}° / 最低 {Math.round(daily.tempMin)}°
              </span>
            </>
          )}
        </div>

        {/* Detail metrics without card styling */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 text-white/40">
              <Droplets className="w-3.5 h-3.5" />
              <span className="text-white text-sm font-medium"><AnimatedNumber value={current.humidity} suffix="%" /></span>
            </div>
            <div className="flex items-center gap-1.5 text-white/40">
              <Wind className="w-3.5 h-3.5" />
              <span className="text-white text-sm font-medium"><AnimatedNumber value={Math.round(current.windSpeed)} suffix=" km/h" /></span>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 text-white/40">
              <Gauge className="w-3.5 h-3.5" />
              <span className="text-white text-sm font-medium"><AnimatedNumber value={Math.round(current.pressure)} suffix=" hPa" /></span>
            </div>
            <div className="flex items-center gap-1.5 text-white/40">
              <Cloud className="w-3.5 h-3.5" />
              <span className="text-white text-sm font-medium"><AnimatedNumber value={current.cloudCover} suffix="%" /></span>
            </div>
          </div>
        </div>

        <div className="text-white/30 text-xs mt-4">当地时间：{localTime}</div>
      </div>
    );
  }

  // Default mobile/tablet layout: card style
  return (
    <div
      ref={cardRef}
      className="w-full rounded-3xl p-6 sm:p-8 md:p-10 transition-all duration-500"
      style={{
        background: "rgba(255,255,255,0.07)",
        backdropFilter: "blur(40px) saturate(1.45) brightness(1.05)",
        WebkitBackdropFilter: "blur(40px) saturate(1.45) brightness(1.05)",
        border: "1px solid rgba(255,255,255,0.15)",
        boxShadow: "0 8px 32px rgba(0,0,0,0.18), inset 0 1px 0 rgba(255,255,255,0.08)",
        filter: `blur(${blurAmount}px)`,
        transform: `scale(${scaleValue}) translateY(${translateY}px)`,
        opacity: opacityValue,
        pointerEvents: opacityValue < 0.15 ? "none" : "auto",
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.3)";
        (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.1)";
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.15)";
        (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.07)";
      }}
    >
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6">
        <div className="flex flex-col">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-white/50 text-sm tracking-wide">{localDate}</span>
          </div>
          <h2 className="text-white text-xl sm:text-2xl font-medium mb-1">{cityName}</h2>
          <div className="flex items-center gap-3 mb-2">
            {getWeatherIcon(info.type, current.isDay, "w-10 h-10 sm:w-12 sm:h-12 text-white")}
            <span
              className="text-white font-bold leading-none tabular-nums"
              style={{
                fontSize: "clamp(60px, 10vw, 120px)",
                fontFamily: "Inter, sans-serif",
                textShadow: "0 4px 30px rgba(0,0,0,0.3)",
              }}
            >
              <AnimatedNumber value={Math.round(current.temperature)} />
            </span>
            <span className="text-white/60 text-3xl sm:text-4xl font-light self-start mt-2">°C</span>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-white/70 text-base">{info.label}</span>
            <span className="text-white/30">·</span>
            <span className="text-white/50 text-sm">体感 <AnimatedNumber value={Math.round(current.apparentTemperature)} />°</span>
            {aqiLabel && (
              <>
                <span className="text-white/30">·</span>
                <span
                  className="text-xs px-2 py-0.5 rounded-full"
                  style={{ background: aqiLabel.color + "30", color: aqiLabel.color }}
                >
                  空气质量 {airQuality?.euAqi} {aqiLabel.label}
                </span>
              </>
            )}
          </div>
          <div className="text-white/40 text-xs mt-1">当地时间：{localTime}</div>
        </div>

        <div className="grid grid-cols-2 gap-3 sm:gap-4 min-w-[200px]">
          <DetailItem
            icon={<Droplets className="w-4 h-4" />}
            label="湿度"
            value={<><AnimatedNumber value={current.humidity} suffix="%" /></>}
          />
          <DetailItem
            icon={<Wind className="w-4 h-4" />}
            label="风速"
            value={<><AnimatedNumber value={Math.round(current.windSpeed)} suffix=" km/h" /></>}
          />
          <DetailItem
            icon={<Gauge className="w-4 h-4" />}
            label="气压"
            value={<><AnimatedNumber value={Math.round(current.pressure)} suffix=" hPa" /></>}
          />
          <DetailItem
            icon={<Eye className="w-4 h-4" />}
            label="能见度"
            value={`${(current.pressure > 0 ? (current.pressure / 1013) * 24 : 10).toFixed(1)} km`}
          />
          {airQuality && (
            <DetailItem
              icon={<CloudFog className="w-4 h-4" />}
              label="PM2.5"
              value={<><AnimatedNumber value={Math.round(airQuality.pm2_5)} suffix=" µg/m³" /></>}
            />
          )}
          <DetailItem
            icon={<Cloud className="w-4 h-4" />}
            label="云量"
            value={<><AnimatedNumber value={current.cloudCover} suffix="%" /></>}
          />
        </div>
      </div>
    </div>
  );
}

function DetailItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: React.ReactNode }) {
  return (
    <div
      className="flex flex-col gap-1 p-3 rounded-xl transition-all duration-300 hover:bg-white/8"
      style={{ background: "rgba(255,255,255,0.04)" }}
    >
      <div className="flex items-center gap-1.5 text-white/40">
        {icon}
        <span className="text-xs">{label}</span>
      </div>
      <span className="text-white text-sm font-medium">{value}</span>
    </div>
  );
}
