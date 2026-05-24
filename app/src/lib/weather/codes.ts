import { ASSET_VERSION } from "@/const";

export const weatherCodeMap: Record<number, { label: string; icon: string; type: WeatherType }> = {
  0: { label: "晴", icon: "sun", type: "clear" },
  1: { label: "大部晴朗", icon: "sun", type: "clear" },
  2: { label: "多云", icon: "cloud-sun", type: "cloudy" },
  3: { label: "阴天", icon: "cloud", type: "cloudy" },
  45: { label: "雾", icon: "cloud-fog", type: "fog" },
  48: { label: "雾凇", icon: "cloud-fog", type: "fog" },
  51: { label: "小毛毛雨", icon: "cloud-drizzle", type: "rain" },
  53: { label: "中毛毛雨", icon: "cloud-drizzle", type: "rain" },
  55: { label: "大毛毛雨", icon: "cloud-drizzle", type: "rain" },
  56: { label: "冻毛毛雨", icon: "cloud-drizzle", type: "rain" },
  57: { label: "冻毛毛雨", icon: "cloud-drizzle", type: "rain" },
  61: { label: "小雨", icon: "cloud-rain", type: "rain" },
  63: { label: "中雨", icon: "cloud-rain", type: "rain" },
  65: { label: "大雨", icon: "cloud-rain-wind", type: "rain" },
  66: { label: "冻雨", icon: "cloud-rain", type: "rain" },
  67: { label: "冻雨", icon: "cloud-rain", type: "rain" },
  71: { label: "小雪", icon: "snowflake", type: "snow" },
  73: { label: "中雪", icon: "snowflake", type: "snow" },
  75: { label: "大雪", icon: "snowflake", type: "snow" },
  77: { label: "米雪", icon: "snowflake", type: "snow" },
  80: { label: "小阵雨", icon: "cloud-rain", type: "rain" },
  81: { label: "中阵雨", icon: "cloud-rain", type: "rain" },
  82: { label: "大阵雨", icon: "cloud-lightning", type: "rain" },
  85: { label: "阵雪", icon: "snowflake", type: "snow" },
  86: { label: "大阵雪", icon: "snowflake", type: "snow" },
  95: { label: "雷暴", icon: "cloud-lightning", type: "storm" },
  96: { label: "雷暴冰雹", icon: "cloud-lightning", type: "storm" },
  99: { label: "雷暴冰雹", icon: "cloud-lightning", type: "storm" },
};

export type WeatherType = "clear" | "cloudy" | "rain" | "snow" | "fog" | "storm";

export function getWeatherInfo(code: number) {
  return weatherCodeMap[code] ?? { label: "未知", icon: "help-circle", type: "cloudy" as WeatherType };
}

export function getWeatherType(code: number): WeatherType {
  return getWeatherInfo(code).type;
}

export function getBackgroundImage(weatherCode: number, isDay: boolean): string {
  const type = getWeatherType(weatherCode);
  if (!isDay) return `/bg-night-clear.jpg?${ASSET_VERSION}`;
  switch (type) {
    case "clear": return `/bg-sunny.jpg?${ASSET_VERSION}`;
    case "cloudy": return `/bg-cloudy.jpg?${ASSET_VERSION}`;
    case "rain": return `/bg-rainy.jpg?${ASSET_VERSION}`;
    case "snow": return `/bg-snowy.jpg?${ASSET_VERSION}`;
    case "fog": return `/bg-foggy.jpg?${ASSET_VERSION}`;
    case "storm": return `/bg-rainy.jpg?${ASSET_VERSION}`;
    default: return `/bg-sunny.jpg?${ASSET_VERSION}`;
  }
}

export function getChartColor(weatherCode: number): string {
  const type = getWeatherType(weatherCode);
  switch (type) {
    case "clear": return "#FFD700";
    case "cloudy": return "#A0AEC0";
    case "rain": return "#4A90E2";
    case "snow": return "#B0E0E6";
    case "fog": return "#9CA3AF";
    case "storm": return "#7C3AED";
    default: return "#FFD700";
  }
}

export function windDirectionToLabel(deg: number): string {
  const dirs = ["北", "北东北", "东北", "东东北", "东", "东东南", "东南", "南东南", "南", "南西南", "西南", "西西南", "西", "西西北", "西北", "北西北"];
  return dirs[Math.round(deg / 22.5) % 16];
}

export function getAqiLabel(aqi: number): { label: string; color: string } {
  if (aqi <= 20) return { label: "优", color: "#10B981" };
  if (aqi <= 40) return { label: "良", color: "#34D399" };
  if (aqi <= 60) return { label: "中", color: "#FBBF24" };
  if (aqi <= 80) return { label: "差", color: "#F97316" };
  if (aqi <= 100) return { label: "很差", color: "#EF4444" };
  return { label: "危险", color: "#DC2626" };
}

export function formatLocalTime(isoTime: string, timezone?: string): string {
  try {
    const d = new Date(isoTime);
    return d.toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      timeZone: timezone || undefined,
    });
  } catch {
    return isoTime.split("T")[1]?.slice(0, 5) || "--:--";
  }
}

export function formatLocalDate(isoDate: string, timezone?: string): string {
  try {
    const d = new Date(isoDate + "T00:00:00");
    return d.toLocaleDateString("zh-CN", {
      weekday: "short",
      month: "short",
      day: "numeric",
      timeZone: timezone || undefined,
    });
  } catch {
    return isoDate;
  }
}

export function formatDayOfWeek(isoDate: string, timezone?: string): string {
  try {
    const d = new Date(isoDate + "T00:00:00");
    const today = new Date();
    const isToday = d.toDateString() === today.toDateString();
    if (isToday) return "今天";
    return d.toLocaleDateString("zh-CN", {
      weekday: "short",
      timeZone: timezone || undefined,
    });
  } catch {
    return isoDate;
  }
}
