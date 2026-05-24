import { useRef, useEffect, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Filler,
  Tooltip,
  type ChartOptions,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { CloudRain, Droplets } from "lucide-react";
import { formatLocalTime } from "@/lib/weather/codes";

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Filler, Tooltip);

interface PrecipitationForecastProps {
  hourly: Array<{
    time: string;
    precipitationProbability: number;
    precipitation: number;
    rain: number;
    showers: number;
    snowfall: number;
  }>;
  timezone?: string;
}

export default function PrecipitationForecast({ hourly, timezone }: PrecipitationForecastProps) {
  const cardRef = useRef<HTMLDivElement>(null);
  const [tiltStyle, setTiltStyle] = useState({ transform: "", transition: "" });

  const next24 = hourly.slice(0, 24);
  const hasPrecipitation = next24.some((h) => h.precipitationProbability > 0 || h.precipitation > 0);

  if (!hasPrecipitation) return null;

  const labels = next24.map((h) => formatLocalTime(h.time, timezone));
  const precipProb = next24.map((h) => h.precipitationProbability);
  const precipAmount = next24.map((h) => h.precipitation);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const data: any = {
    labels,
    datasets: [
      {
        type: "bar",
        data: precipProb,
        backgroundColor: "rgba(74, 144, 226, 0.5)",
        borderColor: "transparent",
        borderRadius: 4,
        barThickness: 8,
        yAxisID: "y",
        label: "概率 (%)",
      },
      {
        type: "line",
        data: precipAmount,
        borderColor: "rgba(100, 200, 255, 0.8)",
        backgroundColor: "rgba(100, 200, 255, 0.1)",
        fill: true,
        tension: 0.4,
        pointRadius: 0,
        pointHoverRadius: 4,
        borderWidth: 1.5,
        yAxisID: "y1",
        label: "降水量 (mm)",
      },
    ],
  };

  const options: ChartOptions<"bar"> = {
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
            return formatLocalTime(next24[items[0].dataIndex].time, timezone);
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: "rgba(255,255,255,0.4)", font: { family: "Inter", size: 10 }, maxRotation: 0, autoSkip: true, maxTicksLimit: 8 },
        border: { display: false },
      },
      y: {
        min: 0,
        max: 100,
        grid: { color: "rgba(255,255,255,0.05)" },
        ticks: { color: "rgba(255,255,255,0.3)", font: { family: "Inter", size: 10 }, callback: (v) => `${v}%` },
        border: { display: false },
        title: { display: false },
      },
      y1: {
        min: 0,
        display: false,
        position: "right",
        grid: { display: false },
        ticks: { display: false },
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

  const maxProb = Math.max(...precipProb);
  const totalAmount = precipAmount.reduce((a, b) => a + b, 0);
  const peakHour = precipProb.indexOf(maxProb);

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
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <CloudRain className="w-5 h-5 text-blue-400" />
          <h3 className="text-white text-lg font-medium">降水预报</h3>
        </div>
        <div className="flex items-center gap-4 text-xs text-white/50">
          <span className="flex items-center gap-1">
            <Droplets className="w-3 h-3" />
            总量：{totalAmount.toFixed(1)} mm
          </span>
          <span>峰值：{maxProb}% ({labels[peakHour]})</span>
        </div>
      </div>

      <div className="h-48 sm:h-56">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
}
