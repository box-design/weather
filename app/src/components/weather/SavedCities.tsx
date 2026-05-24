import { useState, useEffect, useRef } from "react";
import { MapPin, Star, Trash2, X, ChevronDown, Cloud, Sun, CloudRain, Snowflake, CloudFog, CloudLightning, CloudSun, Loader2 } from "lucide-react";
import { useSavedCities, useRemoveCity, useSetDefaultCity } from "@/hooks/useWeather";
import { fetchForecast } from "@/lib/weather/api";
import { getWeatherInfo } from "@/lib/weather/codes";
import type { WeatherType } from "@/lib/weather/codes";

interface SavedCitiesProps {
  onCitySelect: (city: { name: string; latitude: number; longitude: number; country: string; timezone: string }) => void;
  currentCity?: { name: string; latitude: number; longitude: number };
}

interface CityWeather {
  temperature: number;
  weatherCode: number;
  weatherType: WeatherType;
  label: string;
  humidity: number;
  windSpeed: number;
  isDay: boolean;
}

function getMiniIcon(type: WeatherType, isDay: boolean, className: string) {
  const map: Record<WeatherType, React.ReactNode> = {
    clear: isDay ? <Sun className={className} /> : <Sun className={className} />,
    cloudy: <CloudSun className={className} />,
    rain: <CloudRain className={className} />,
    snow: <Snowflake className={className} />,
    fog: <CloudFog className={className} />,
    storm: <CloudLightning className={className} />,
  };
  return map[type] || <Cloud className={className} />;
}

function CityCard({
  city,
  isActive,
  onSelect,
  onRemove,
  onSetDefault,
  weather,
  expanded,
  onToggle,
}: {
  city: {
    id: string;
    name: string;
    latitude: string;
    longitude: string;
    country: string;
    timezone: string;
    isDefault: string;
  };
  isActive: boolean;
  onSelect: () => void;
  onRemove: () => void;
  onSetDefault: () => void;
  weather: CityWeather | null;
  expanded: boolean;
  onToggle: () => void;
}) {
  const contentRef = useRef<HTMLDivElement>(null);
  const [contentHeight, setContentHeight] = useState(0);

  useEffect(() => {
    if (contentRef.current) {
      setContentHeight(contentRef.current.scrollHeight);
    }
  }, [expanded, weather]);

  return (
    <div
      className="overflow-hidden"
      style={{
        background: isActive ? "rgba(255,255,255,0.08)" : "rgba(255,255,255,0.03)",
        borderRadius: "16px",
        border: isActive ? "1px solid rgba(255,255,255,0.2)" : "1px solid rgba(255,255,255,0.06)",
        transition: "all 0.3s cubic-bezier(0.22, 1, 0.36, 1)",
      }}
      onMouseEnter={(e) => {
        if (!isActive) {
          (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.06)";
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.12)";
        }
      }}
      onMouseLeave={(e) => {
        if (!isActive) {
          (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.03)";
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.06)";
        }
      }}
    >
      <div
        className="flex items-center gap-2 px-3 py-2.5 cursor-pointer"
        onClick={onToggle}
      >
        <MapPin className={`w-3.5 h-3.5 flex-shrink-0 ${isActive ? "text-blue-400" : "text-white/30"}`} />
        <button
          className="flex-1 flex items-center gap-2 text-left"
          onClick={(e) => {
            e.stopPropagation();
            onSelect();
          }}
        >
          <div className="flex flex-col min-w-0">
            <span className={`text-sm truncate ${isActive ? "text-blue-400 font-medium" : "text-white/80"}`}>
              {city.name}
            </span>
            {city.country && (
              <span className="text-white/30 text-[10px] truncate">{city.country}</span>
            )}
          </div>
        </button>

        {weather && (
          <div className="flex items-center gap-1 flex-shrink-0">
            {getMiniIcon(weather.weatherType, weather.isDay, "w-3.5 h-3.5 text-white/50")}
            <span className="text-white/70 text-xs font-medium tabular-nums">
              {Math.round(weather.temperature)}°
            </span>
          </div>
        )}

        <ChevronDown
          className="w-3.5 h-3.5 text-white/30 flex-shrink-0 transition-transform duration-300"
          style={{ transform: expanded ? "rotate(180deg)" : "rotate(0deg)" }}
        />
      </div>

      <div
        style={{
          maxHeight: expanded ? `${contentHeight}px` : "0px",
          opacity: expanded ? 1 : 0,
          transition: "max-height 0.4s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.3s ease",
          overflow: "hidden",
        }}
      >
        <div ref={contentRef} className="px-3 pb-3">
          <div
            className="rounded-xl p-3"
            style={{ background: "rgba(255,255,255,0.04)" }}
          >
            {weather ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {getMiniIcon(weather.weatherType, weather.isDay, "w-5 h-5 text-white/70")}
                    <span className="text-white/70 text-xs">{weather.label}</span>
                  </div>
                  <span className="text-white text-lg font-semibold tabular-nums">
                    {Math.round(weather.temperature)}°C
                  </span>
                </div>
                <div className="flex items-center gap-3 text-white/40 text-[11px]">
                  <span>💧 {weather.humidity}%</span>
                  <span>💨 {Math.round(weather.windSpeed)} km/h</span>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center gap-2 py-1">
                <Loader2 className="w-3.5 h-3.5 text-white/30 animate-spin" />
                <span className="text-white/30 text-xs">加载中...</span>
              </div>
            )}

            <div className="flex items-center gap-1 mt-2 pt-2" style={{ borderTop: "1px solid rgba(255,255,255,0.06)" }}>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onSetDefault();
                }}
                className="flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] text-white/40 hover:text-white/70 hover:bg-white/5 transition-all"
              >
                <Star className={`w-3 h-3 ${city.isDefault === "1" ? "text-yellow-400 fill-yellow-400" : ""}`} />
                {city.isDefault === "1" ? "默认" : "设为默认"}
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onRemove();
                }}
                className="flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] text-white/40 hover:text-red-400 hover:bg-red-500/10 transition-all"
              >
                <Trash2 className="w-3 h-3" />
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function SavedCities({ onCitySelect, currentCity }: SavedCitiesProps) {
  const [open, setOpen] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [cityWeathers, setCityWeathers] = useState<Record<string, CityWeather | null>>({});
  const panelRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  const { data: savedCities, isLoading } = useSavedCities();
  const removeMutation = useRemoveCity();
  const setDefaultMutation = useSetDefaultCity();

  useEffect(() => {
    if (!open || !savedCities || savedCities.length === 0) return;

    const loadWeathers = async () => {
      const results: Record<string, CityWeather | null> = {};
      await Promise.all(
        savedCities.map(async (city) => {
          try {
            const data = await fetchForecast(
              parseFloat(city.latitude),
              parseFloat(city.longitude)
            );
            const info = getWeatherInfo(data.current.weatherCode);
            results[city.id] = {
              temperature: data.current.temperature,
              weatherCode: data.current.weatherCode,
              weatherType: info.type,
              label: info.label,
              humidity: data.current.humidity,
              windSpeed: data.current.windSpeed,
              isDay: data.current.isDay,
            };
          } catch {
            results[city.id] = null;
          }
        })
      );
      setCityWeathers(results);
    };

    loadWeathers();
  }, [open, savedCities]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        panelRef.current && !panelRef.current.contains(e.target as Node) &&
        buttonRef.current && !buttonRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  const handleToggleExpand = (id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  return (
    <div className="relative">
      <button
        ref={buttonRef}
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1.5 px-3 py-2 rounded-full text-white/70 hover:text-white transition-all text-sm"
        style={{
          background: open ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.06)",
          border: "1px solid rgba(255,255,255,0.1)",
          backdropFilter: "blur(12px)",
          WebkitBackdropFilter: "blur(12px)",
        }}
      >
        <MapPin className="w-4 h-4" />
        <span className="hidden sm:inline">城市</span>
        {savedCities && savedCities.length > 0 && (
          <span className="text-[10px] bg-white/20 text-white/80 px-1.5 py-0.5 rounded-full">
            {savedCities.length}
          </span>
        )}
      </button>

      <div
        ref={panelRef}
        className="absolute right-0 top-full mt-2 w-72 rounded-2xl overflow-hidden"
        style={{
          background: "rgba(15, 15, 30, 0.65)",
          backdropFilter: "blur(32px) saturate(1.4)",
          WebkitBackdropFilter: "blur(32px) saturate(1.4)",
          border: "1px solid rgba(255,255,255,0.12)",
          boxShadow: "0 8px 32px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.06)",
          zIndex: 50,
          transform: open ? "translateY(0) scale(1)" : "translateY(-8px) scale(0.96)",
          opacity: open ? 1 : 0,
          pointerEvents: open ? "auto" : "none",
          transition: "all 0.3s cubic-bezier(0.22, 1, 0.36, 1)",
        }}
      >
        <div
          className="flex items-center justify-between px-4 py-3"
          style={{ borderBottom: "1px solid rgba(255,255,255,0.06)" }}
        >
          <span className="text-white text-sm font-medium">收藏城市</span>
          <button
            onClick={() => setOpen(false)}
            className="text-white/40 hover:text-white transition-colors p-1 rounded-lg hover:bg-white/5"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {isLoading ? (
          <div className="px-4 py-8 text-center">
            <Loader2 className="w-5 h-5 text-white/30 animate-spin mx-auto mb-2" />
            <p className="text-white/30 text-xs">加载中...</p>
          </div>
        ) : !savedCities || savedCities.length === 0 ? (
          <div className="px-4 py-8 text-center">
            <MapPin className="w-8 h-8 text-white/15 mx-auto mb-2" />
            <p className="text-white/30 text-sm">暂无收藏城市</p>
            <p className="text-white/20 text-xs mt-1">搜索并添加城市到收藏列表</p>
          </div>
        ) : (
          <div className="p-2 flex flex-col gap-1.5 max-h-80 overflow-y-auto">
            {savedCities.map((city) => {
              const isActive = currentCity?.name === city.name;
              return (
                <CityCard
                  key={city.id}
                  city={city}
                  isActive={isActive}
                  onSelect={() => {
                    onCitySelect({
                      name: city.name,
                      latitude: parseFloat(city.latitude),
                      longitude: parseFloat(city.longitude),
                      country: city.country || "",
                      timezone: city.timezone || "",
                    });
                    setOpen(false);
                  }}
                  onRemove={() => removeMutation.mutate(city.id)}
                  onSetDefault={() => setDefaultMutation.mutate(city.id)}
                  weather={cityWeathers[city.id] ?? null}
                  expanded={expandedId === city.id}
                  onToggle={() => handleToggleExpand(city.id)}
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
