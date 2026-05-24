import { useState, useEffect, useCallback, useRef } from "react";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useWeatherForecast, useAirQuality, useAddCity } from "@/hooks/useWeather";
import { getWeatherType, getChartColor } from "@/lib/weather/codes";
import type { WeatherType } from "@/lib/weather/codes";
import { fetchReverseGeocode } from "@/lib/weather/api";
import { getDefaultCity } from "@/lib/storage/cities";
import { ASSET_VERSION } from "@/const";
import { useScrollProgress } from "@/hooks/useScrollProgress";

import WeatherBackground from "@/components/weather/WeatherBackground";
import Header from "@/components/weather/Header";
import CurrentWeather from "@/components/weather/CurrentWeather";
import HourlyForecast from "@/components/weather/HourlyForecast";
import DailyForecast from "@/components/weather/DailyForecast";
import PrecipitationForecast from "@/components/weather/PrecipitationForecast";

interface CityData {
  name: string;
  latitude: number;
  longitude: number;
  country: string;
  timezone: string;
}

const DEFAULT_CITY: CityData = {
  name: "Beijing",
  latitude: 39.9042,
  longitude: 116.4074,
  country: "China",
  timezone: "auto",
};

export default function Home() {
  useAuth();
  const [city, setCity] = useState<CityData>(DEFAULT_CITY);
  const [weatherType, setWeatherType] = useState<WeatherType>("clear");
  const [locating, setLocating] = useState(true);
  const locationFetched = useRef(false);
  const { scrollY, scrollProgress } = useScrollProgress();

  const { data: weatherData, isLoading: weatherLoading, error: weatherError } = useWeatherForecast(
    city.latitude, city.longitude
  );

  const { data: airQualityData } = useAirQuality(
    city.latitude, city.longitude
  );

  const addCityMutation = useAddCity();

  useEffect(() => {
    if (locationFetched.current) return;
    locationFetched.current = true;

    const initLocation = async () => {
      const defaultCity = getDefaultCity();
      if (defaultCity) {
        setCity({
          name: defaultCity.name,
          latitude: parseFloat(defaultCity.latitude),
          longitude: parseFloat(defaultCity.longitude),
          country: defaultCity.country || "",
          timezone: defaultCity.timezone || "auto",
        });
        setLocating(false);
        return;
      }

      if (!navigator.geolocation) {
        setLocating(false);
        return;
      }

      try {
        const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            timeout: 8000,
            maximumAge: 300000,
          });
        });

        const { latitude, longitude } = pos.coords;
        const geo = await fetchReverseGeocode(latitude, longitude);
        setCity({
          name: geo.name,
          latitude,
          longitude,
          country: geo.country,
          timezone: geo.timezone,
        });
      } catch {
      } finally {
        setLocating(false);
      }
    };

    initLocation();
  }, []);

  useEffect(() => {
    if (weatherData?.current) {
      const type = getWeatherType(weatherData.current.weatherCode);
      setWeatherType(type);
    }
  }, [weatherData]);

  const handleCitySelect = useCallback((newCity: CityData) => {
    setCity(newCity);
  }, []);

  const handleAddCity = useCallback(() => {
    addCityMutation.mutate({
      name: city.name,
      latitude: city.latitude,
      longitude: city.longitude,
      country: city.country,
      timezone: weatherData?.timezone || city.timezone,
    });
  }, [city, weatherData, addCityMutation]);

  const chartColor = weatherData?.current ? getChartColor(weatherData.current.weatherCode) : "#FFD700";

  if (locating || weatherLoading) {
    return (
      <div className="min-h-screen relative grain-overlay">
        <WeatherBackground weatherType={weatherType} isDay={true} />
        <div className="relative z-10 min-h-screen flex items-center justify-center">
          <div className="flex flex-col items-center gap-4">
            <Loader2 className="w-10 h-10 text-white/60 animate-spin" />
            <p className="text-white/50 text-sm">
              {locating ? "正在定位当前位置..." : "正在加载天气数据..."}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (weatherError || !weatherData) {
    return (
      <div className="min-h-screen relative">
        <WeatherBackground weatherType="cloudy" isDay={true} />
        <div className="relative z-10 min-h-screen flex items-center justify-center">
          <div
            className="rounded-3xl p-8 max-w-md text-center"
            style={{
              background: "rgba(255,255,255,0.08)",
              backdropFilter: "blur(24px)",
              WebkitBackdropFilter: "blur(24px)",
              border: "1px solid rgba(255,255,255,0.15)",
            }}
          >
            <p className="text-white text-lg mb-2">无法加载天气数据</p>
            <p className="text-white/50 text-sm mb-4">请检查网络连接后重试。</p>
            <button
              onClick={() => window.location.reload()}
              className="px-4 py-2 rounded-full text-sm text-white bg-white/10 hover:bg-white/20 transition-colors"
            >
              重试
            </button>
          </div>
        </div>
      </div>
    );
  }

  const hasPrecipitation = weatherData.hourly.some(
    (h) => h.precipitationProbability > 0 || h.precipitation > 0
  );

  return (
    <div className="min-h-screen relative">
      <WeatherBackground weatherType={weatherType} isDay={weatherData.current.isDay} scrollY={scrollY} />

      {/* City silhouette with parallax */}
      <div
        className="fixed bottom-0 left-0 right-0 z-[1] pointer-events-none transition-transform duration-100"
        style={{
          backgroundImage: `url(/city-silhouette.png?${ASSET_VERSION})`,
          backgroundSize: "1200px auto",
          backgroundRepeat: "repeat-x",
          backgroundPosition: "bottom center",
          height: "120px",
          opacity: 0.25,
          maskImage: "linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 100%)",
          WebkitMaskImage: "linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 100%)",
          transform: `translateY(${scrollY * 0.05}px)`,
        }}
      />

      <div className="relative z-10 min-h-screen">
        <Header
          onCitySelect={handleCitySelect}
          onAddCity={handleAddCity}
          currentCity={city}
          scrollY={scrollY}
        />

        <main className="pt-20 pb-12 px-4 sm:px-6 lg:px-10 xl:px-16 2xl:px-24">
          <div className="w-full">
            {/* Wide screen: two-column layout */}
            <div className="hidden lg:flex lg:gap-10 xl:gap-16 2xl:gap-20">
              {/* Left: Current weather info (no card style) */}
              <div className="w-[320px] xl:w-[360px] 2xl:w-[400px] flex-shrink-0 pt-8 lg:sticky lg:top-24 lg:self-start">
                <CurrentWeather
                  current={weatherData.current}
                  airQuality={airQualityData?.current}
                  daily={weatherData.daily.length > 0 ? { tempMax: weatherData.daily[0].tempMax, tempMin: weatherData.daily[0].tempMin } : undefined}
                  cityName={city.name}
                  timezone={weatherData.timezone}
                  scrollY={scrollY}
                  isWideLayout={true}
                />
              </div>
              {/* Right: Forecast cards */}
              <div className="flex-1 flex flex-col gap-6 min-w-0">
                <HourlyForecast
                  hourly={weatherData.hourly}
                  airQualityHourly={airQualityData?.hourly}
                  timezone={weatherData.timezone}
                  chartColor={chartColor}
                />

                {hasPrecipitation && (
                  <PrecipitationForecast
                    hourly={weatherData.hourly}
                    timezone={weatherData.timezone}
                  />
                )}

                <DailyForecast
                  daily={weatherData.daily}
                  airQualityDaily={airQualityData?.hourly?.filter((_h, i) => i % 24 === 12).map((h) => ({ time: h.time, euAqi: h.euAqi }))}
                  timezone={weatherData.timezone}
                  chartColor={chartColor}
                />
              </div>
            </div>

            {/* Mobile/Tablet: single column layout */}
            <div className="lg:hidden flex flex-col gap-6">
              <CurrentWeather
                current={weatherData.current}
                airQuality={airQualityData?.current}
                cityName={city.name}
                timezone={weatherData.timezone}
                scrollY={scrollY}
                isWideLayout={false}
              />

              <HourlyForecast
                hourly={weatherData.hourly}
                airQualityHourly={airQualityData?.hourly}
                timezone={weatherData.timezone}
                chartColor={chartColor}
              />

              {hasPrecipitation && (
                <PrecipitationForecast
                  hourly={weatherData.hourly}
                  timezone={weatherData.timezone}
                />
              )}

              <DailyForecast
                daily={weatherData.daily}
                airQualityDaily={airQualityData?.hourly?.filter((_h, i) => i % 24 === 12).map((h) => ({ time: h.time, euAqi: h.euAqi }))}
                timezone={weatherData.timezone}
                chartColor={chartColor}
              />
            </div>
          </div>
        </main>

        <footer className="relative z-10 py-6 text-center">
          <p className="text-white/20 text-xs">
            SkyWeather · 数据来自 Open-Meteo API · 每小时更新
          </p>
        </footer>
      </div>
    </div>
  );
}
