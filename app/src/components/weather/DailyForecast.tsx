import { useRef, useEffect, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  type ChartOptions,
} from "chart.js";
import { Line } from "react-chartjs-2";
import { Sun, Cloud, CloudRain, Snowflake, CloudLightning, CloudFog, CloudSun } from "lucide-react";
import { formatDayOfWeek, windDirectionToLabel, getAqiLabel, getWeatherInfo } from "@/lib/weather/codes";
import type { WeatherType } from "@/lib/weather/codes";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip);

interface DailyForecastProps {
  daily: Array<{
    date: string;
    weatherCode: number;
    tempMax: number;
    tempMin: number;
    apparentTempMax: number;
    apparentTempMin: number;
    sunrise: string;
    sunset: string;
    precipitationSum: number;
    rainSum: number;
    showersSum: number;
    snowfallSum: number;
    precipitationProbabilityMax: number;
    windSpeedMax: number;
    windDirectionDominant: number;
    windGustsMax: number;
    uvIndexMax: number;
  }>;
  airQualityDaily?: Array<{ time: string; euAqi: number }>;
  timezone?: string;
  chartColor: string;
}

function getWeatherIcon(type: WeatherType, className: string) {
  const iconMap: Record<WeatherType, React.ReactNode> = {
    clear: <Sun className={className} />,
    cloudy: <CloudSun className={className} />,
    rain: <CloudRain className={className} />,
    snow: <Snowflake className={className} />,
    fog: <CloudFog className={className} />,
    storm: <CloudLightning className={className} />,
  };
  return iconMap[type] || <Cloud className={className} />;
}

export default function DailyForecast({ daily, airQualityDaily, timezone, chartColor: _chartColor }: DailyForecastProps) {
  void _chartColor;
  const cardRef = useRef<HTMLDivElement>(null);
  const [tiltStyle, setTiltStyle] = useState({ transform: "", transition: "" });

  const days = daily.slice(0, 15);
  const labels = days.map((d) => formatDayOfWeek(d.date, timezone));
  const maxTemps = days.map((d) => d.tempMax);
  const minTemps = days.map((d) => d.tempMin);
  const minT = Math.min(...minTemps) - 2;
  const maxT = Math.max(...maxTemps) + 2;

  const chartData = {
    labels,
    datasets: [
      {
        data: maxTemps,
        borderColor: "#FF8C42",
        backgroundColor: "transparent",
        fill: false,
        tension: 0.4,
        pointRadius: 3,
        pointBackgroundColor: "#FF8C42",
        pointBorderColor: "transparent",
        borderWidth: 2,
        label: "最高",
      },
      {
        data: minTemps,
        borderColor: "#5B9BD5",
        backgroundColor: "transparent",
        fill: false,
        tension: 0.4,
        pointRadius: 3,
        pointBackgroundColor: "#5B9BD5",
        pointBorderColor: "transparent",
        borderWidth: 2,
        label: "最低",
      },
    ],
  };

  const chartOptions: ChartOptions<"line"> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "index", intersect: false },
    plugins: {
      legend: {
        display: true,
        labels: {
          color: "rgba(255,255,255,0.5)",
          font: { family: "Inter", size: 11 },
          usePointStyle: true,
          pointStyle: "circle",
          padding: 16,
        },
        position: "top",
        align: "end",
      },
      tooltip: {
        backgroundColor: "rgba(20, 20, 35, 0.95)",
        titleColor: "#fff",
        bodyColor: "rgba(255,255,255,0.7)",
        borderColor: "rgba(255,255,255,0.1)",
        borderWidth: 1,
        cornerRadius: 12,
        padding: 12,
        callbacks: {
          title: (items) => {
            if (!items.length) return "";
            const idx = items[0].dataIndex;
            return days[idx].date;
          },
          label: (item) => {
            const idx = item.dataIndex;
            const d = days[idx];
            return [
              `${item.dataset.label}：${Math.round(item.parsed.y ?? 0)}°C`,
              `降水概率：${d.precipitationProbabilityMax}%`,
              `风速：${Math.round(d.windSpeedMax)} km/h ${windDirectionToLabel(d.windDirectionDominant ?? 0)}`,
              `紫外线：${d.uvIndexMax}`,
            ];
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: "rgba(255,255,255,0.4)", font: { family: "Inter", size: 10 }, maxRotation: 45 },
        border: { display: false },
      },
      y: {
        min: minT,
        max: maxT,
        grid: { color: "rgba(255,255,255,0.05)" },
        ticks: { color: "rgba(255,255,255,0.3)", font: { family: "Inter", size: 10 }, callback: (v) => `${Math.round(Number(v))}°` },
        border: { display: false },
      },
    },
  };

  useEffect(() => {
    if (cardRef.current) {
      cardRef.current.style.opacity = "0";
      cardRef.current.style.transform = "translateY(40px)";
      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              (entry.target as HTMLElement).style.transition = "all 0.9s cubic-bezier(0.22, 1, 0.36, 1)";
              (entry.target as HTMLElement).style.opacity = "1";
              (entry.target as HTMLElement).style.transform = "translateY(0)";
              observer.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.05 }
      );
      observer.observe(cardRef.current);
      return () => observer.disconnect();
    }
  }, []);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    setTiltStyle({
      transform: `perspective(1000px) rotateY(${x * 2}deg) rotateX(${-y * 2}deg) scale3d(1.005, 1.005, 1.005)`,
      transition: "transform 0.15s ease-out",
    });
  };

  const handleMouseLeave = () => {
    setTiltStyle({
      transform: "perspective(1000px) rotateY(0deg) rotateX(0deg) scale3d(1, 1, 1)",
      transition: "transform 0.5s cubic-bezier(0.22, 1, 0.36, 1)",
    });
  };

  return (
    <div
      ref={cardRef}
      className="w-full rounded-3xl p-5 sm:p-6 lg:p-8 glow-hover"
      style={{
        background: "rgba(255,255,255,0.07)",
        backdropFilter: "blur(40px) saturate(1.45) brightness(1.05)",
        WebkitBackdropFilter: "blur(40px) saturate(1.45) brightness(1.05)",
        border: "1px solid rgba(255,255,255,0.15)",
        boxShadow: "0 8px 32px rgba(0,0,0,0.18), inset 0 1px 0 rgba(255,255,255,0.08)",
        ...tiltStyle,
      }}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.3)"; }}
    >
      <h3 className="text-white text-lg font-medium mb-4">15天预报</h3>

      <div className="h-48 sm:h-56 mb-4">
        <Line data={chartData} options={chartOptions} />
      </div>

      <div className="flex flex-col divide-y divide-white/5">
        {days.map((day, i) => {
          const info = getWeatherInfo(day.weatherCode);
          const aqi = airQualityDaily?.[i];
          const aqiInfo = aqi ? getAqiLabel(aqi.euAqi) : null;
          return (
            <div
              key={day.date}
              className="grid grid-cols-[80px_1fr_80px_80px_60px] sm:grid-cols-[100px_1fr_100px_100px_80px] items-center gap-2 py-2.5 px-2 rounded-lg hover:bg-white/5 transition-all duration-200"
              style={{
                animationDelay: `${i * 30}ms`,
              }}
            >
              <span className="text-white/60 text-xs sm:text-sm">{formatDayOfWeek(day.date, timezone)}</span>
              <div className="flex items-center gap-2">
                {getWeatherIcon(info.type, "w-4 h-4 sm:w-5 sm:h-5 text-white/70")}
                <span className="text-white/70 text-xs sm:text-sm truncate">{info.label}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="text-white text-xs sm:text-sm font-medium">{Math.round(day.tempMax)}°</span>
                <span className="text-white/30">/</span>
                <span className="text-white/50 text-xs sm:text-sm">{Math.round(day.tempMin)}°</span>
              </div>
              <div className="flex items-center gap-1 text-white/40 text-xs">
                <span>{Math.round(day.windSpeedMax)} km/h</span>
                <span className="text-white/20">{windDirectionToLabel(day.windDirectionDominant ?? 0)}</span>
              </div>
              {aqiInfo ? (
                <span
                  className="text-[10px] sm:text-xs px-2 py-0.5 rounded-full text-center"
                  style={{ background: aqiInfo.color + "30", color: aqiInfo.color }}
                >
                  空气 {aqi?.euAqi}
                </span>
              ) : (
                <span className="text-white/20 text-xs">{day.precipitationProbabilityMax}%</span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
