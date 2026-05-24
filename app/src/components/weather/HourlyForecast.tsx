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
import { Wind, CloudFog } from "lucide-react";
import { formatLocalTime, windDirectionToLabel, getAqiLabel } from "@/lib/weather/codes";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip);

interface HourlyForecastProps {
  hourly: Array<{
    time: string;
    temperature: number;
    humidity: number;
    apparentTemperature: number;
    precipitationProbability: number;
    precipitation: number;
    rain: number;
    showers: number;
    snowfall: number;
    weatherCode: number;
    cloudCover: number;
    windSpeed: number;
    windDirection: number;
    windGusts: number;
    pressure: number;
    visibility: number;
    isDay: boolean;
    uvIndex: number;
  }>;
  airQualityHourly?: Array<{
    time: string;
    pm10: number;
    pm2_5: number;
    euAqi: number;
    usAqi: number;
  }>;
  timezone?: string;
  chartColor: string;
}

export default function HourlyForecast({ hourly, airQualityHourly, timezone, chartColor }: HourlyForecastProps) {
  const cardRef = useRef<HTMLDivElement>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [tiltStyle, setTiltStyle] = useState({ transform: "", transition: "" });

  const next24Hours = hourly.slice(0, 24);

  const labels = next24Hours.map((h) => formatLocalTime(h.time, timezone));
  const temps = next24Hours.map((h) => h.temperature);
  const minTemp = Math.min(...temps) - 1;
  const maxTemp = Math.max(...temps) + 1;

  const data = {
    labels,
    datasets: [
      {
        data: temps,
        borderColor: chartColor,
        backgroundColor: (ctx: { chart: { ctx: CanvasRenderingContext2D; chartArea?: { top: number; bottom: number } } }) => {
          const chart = ctx.chart;
          const { ctx: canvasCtx, chartArea } = chart;
          if (!chartArea) return `rgba(255,255,255,0.05)`;
          const gradient = canvasCtx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, chartColor + "60");
          gradient.addColorStop(1, chartColor + "05");
          return gradient;
        },
        fill: true,
        tension: 0.4,
        pointRadius: 0,
        pointHoverRadius: 6,
        pointHoverBackgroundColor: chartColor,
        pointHoverBorderColor: "#fff",
        pointHoverBorderWidth: 2,
        borderWidth: 2,
      },
    ],
  };

  const options: ChartOptions<"line"> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "index", intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        backgroundColor: "rgba(20, 20, 35, 0.95)",
        titleColor: "#fff",
        bodyColor: "rgba(255,255,255,0.7)",
        borderColor: "rgba(255,255,255,0.1)",
        borderWidth: 1,
        cornerRadius: 12,
        padding: 12,
        titleFont: { family: "Inter", size: 13 },
        bodyFont: { family: "Inter", size: 12 },
        callbacks: {
          title: (items) => {
            if (!items.length) return "";
            const idx = items[0].dataIndex;
            return formatLocalTime(next24Hours[idx].time, timezone);
          },
          label: (item) => {
            const idx = item.dataIndex;
            const h = next24Hours[idx];
            return [
              `温度：${Math.round(h.temperature)}°C`,
              `体感：${Math.round(h.apparentTemperature)}°C`,
              `降水概率：${h.precipitationProbability}%`,
              `风速：${Math.round(h.windSpeed)} km/h ${windDirectionToLabel(h.windDirection)}`,
              `湿度：${h.humidity}%`,
              `紫外线：${h.uvIndex}`,
            ];
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: "rgba(255,255,255,0.4)",
          font: { family: "Inter", size: 10 },
          maxRotation: 0,
          autoSkip: true,
          maxTicksLimit: 8,
          callback: (_value, index) => {
            return labels[index] || "";
          },
        },
        border: { display: false },
      },
      y: {
        min: minTemp,
        max: maxTemp,
        grid: { color: "rgba(255,255,255,0.05)", lineWidth: 1 },
        ticks: {
          color: "rgba(255,255,255,0.3)",
          font: { family: "Inter", size: 10 },
          callback: (value) => `${Math.round(Number(value))}°`,
          stepSize: 2,
        },
        border: { display: false },
      },
    },
    onHover: (_event, elements) => {
      if (elements.length > 0) {
        setHoveredIndex(elements[0].index);
      } else {
        setHoveredIndex(null);
      }
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
        { threshold: 0.1 }
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

  const hoveredHour = hoveredIndex !== null ? next24Hours[hoveredIndex] : null;
  const hoveredAqi = hoveredIndex !== null && airQualityHourly
    ? airQualityHourly[hoveredIndex]
    : null;

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
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.3)";
      }}
    >
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-white text-lg font-medium">24小时预报</h3>
        {hoveredHour && (
          <div className="flex items-center gap-3 text-xs text-white/50">
            <span className="flex items-center gap-1">
              <Wind className="w-3 h-3" />
              {Math.round(hoveredHour.windSpeed)} km/h {windDirectionToLabel(hoveredHour.windDirection)}
            </span>
            {hoveredAqi && (
              <span className="flex items-center gap-1">
                <CloudFog className="w-3 h-3" />
                空气质量 {hoveredAqi.euAqi}
              </span>
            )}
          </div>
        )}
      </div>

      <div className="h-56 sm:h-64">
        <Line data={data} options={options} />
      </div>

      <div className="flex gap-3 mt-4 overflow-x-auto pb-2 scrollbar-thin scrollbar-track-transparent scrollbar-thumb-white/10">
        {next24Hours.slice(0, 12).map((h, i) => {
          const aqi = airQualityHourly?.[i];
          const aqiInfo = aqi ? getAqiLabel(aqi.euAqi) : null;
          return (
            <div
              key={h.time}
              className="flex-shrink-0 flex flex-col items-center gap-1 px-3 py-2 rounded-xl min-w-[60px] transition-all duration-200"
              style={{
                background: hoveredIndex === i ? "rgba(255,255,255,0.1)" : "transparent",
                transform: hoveredIndex === i ? "translateY(-2px)" : "translateY(0)",
              }}
            >
              <span className="text-white/40 text-[10px]">{formatLocalTime(h.time, timezone)}</span>
              <span className="text-white text-sm font-medium">{Math.round(h.temperature)}°</span>
              <span className="text-white/30 text-[10px]">{h.precipitationProbability}%</span>
              {aqiInfo && (
                <span
                  className="text-[9px] px-1.5 py-0.5 rounded-full"
                  style={{ background: aqiInfo.color + "30", color: aqiInfo.color }}
                >
                  {aqi?.euAqi}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
