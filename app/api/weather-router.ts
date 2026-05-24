import { z } from "zod";
import { createRouter, publicQuery } from "./middleware";

const OPEN_METEO_BASE = "https://api.open-meteo.com/v1";
const AIR_QUALITY_BASE = "https://air-quality-api.open-meteo.com/v1";
const GEOCODING_BASE = "https://geocoding-api.open-meteo.com/v1";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function fetchJson(url: string): Promise<any> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

export const weatherRouter = createRouter({
  forecast: publicQuery
    .input(z.object({ latitude: z.number(), longitude: z.number() }))
    .query(async ({ input }) => {
      const { latitude, longitude } = input;
      const hourlyVars = [
        "temperature_2m", "relative_humidity_2m", "apparent_temperature",
        "precipitation_probability", "precipitation", "rain", "showers", "snowfall",
        "weather_code", "cloud_cover", "wind_speed_10m", "wind_direction_10m",
        "wind_gusts_10m", "pressure_msl", "visibility", "is_day", "uv_index",
      ].join(",");
      const dailyVars = [
        "weather_code", "temperature_2m_max", "temperature_2m_min",
        "apparent_temperature_max", "apparent_temperature_min", "sunrise", "sunset",
        "precipitation_sum", "rain_sum", "showers_sum", "snowfall_sum",
        "precipitation_probability_max", "wind_speed_10m_max",
        "wind_direction_10m_dominant", "wind_gusts_10m_max", "uv_index_max",
      ].join(",");
      const currentVars = [
        "temperature_2m", "relative_humidity_2m", "apparent_temperature",
        "precipitation", "rain", "showers", "snowfall", "weather_code",
        "cloud_cover", "wind_speed_10m", "wind_direction_10m", "wind_gusts_10m",
        "pressure_msl", "is_day",
      ].join(",");

      const url = `${OPEN_METEO_BASE}/forecast?latitude=${latitude}&longitude=${longitude}&hourly=${hourlyVars}&daily=${dailyVars}&current=${currentVars}&timezone=auto&forecast_days=16`;
      const data = await fetchJson(url);

      return {
        current: {
          temperature: data.current.temperature_2m ?? 0,
          humidity: data.current.relative_humidity_2m ?? 0,
          apparentTemperature: data.current.apparent_temperature ?? 0,
          weatherCode: data.current.weather_code ?? 0,
          windSpeed: data.current.wind_speed_10m ?? 0,
          windDirection: data.current.wind_direction_10m ?? 0,
          windGusts: data.current.wind_gusts_10m ?? 0,
          pressure: data.current.pressure_msl ?? 0,
          cloudCover: data.current.cloud_cover ?? 0,
          isDay: !!data.current.is_day,
          precipitation: data.current.precipitation ?? 0,
          rain: data.current.rain ?? 0,
          showers: data.current.showers ?? 0,
          snowfall: data.current.snowfall ?? 0,
        },
        hourly: data.hourly.time.map((t: string, i: number) => ({
          time: t,
          temperature: data.hourly.temperature_2m[i] ?? 0,
          humidity: data.hourly.relative_humidity_2m[i] ?? 0,
          apparentTemperature: data.hourly.apparent_temperature[i] ?? 0,
          precipitationProbability: data.hourly.precipitation_probability[i] ?? 0,
          precipitation: data.hourly.precipitation[i] ?? 0,
          rain: data.hourly.rain[i] ?? 0,
          showers: data.hourly.showers[i] ?? 0,
          snowfall: data.hourly.snowfall[i] ?? 0,
          weatherCode: data.hourly.weather_code[i] ?? 0,
          cloudCover: data.hourly.cloud_cover[i] ?? 0,
          windSpeed: data.hourly.wind_speed_10m[i] ?? 0,
          windDirection: data.hourly.wind_direction_10m[i] ?? 0,
          windGusts: data.hourly.wind_gusts_10m[i] ?? 0,
          pressure: data.hourly.pressure_msl[i] ?? 0,
          visibility: data.hourly.visibility[i] ?? 0,
          isDay: !!data.hourly.is_day[i],
          uvIndex: data.hourly.uv_index[i] ?? 0,
        })),
        daily: data.daily.time.map((t: string, i: number) => ({
          date: t,
          weatherCode: data.daily.weather_code[i] ?? 0,
          tempMax: data.daily.temperature_2m_max[i] ?? 0,
          tempMin: data.daily.temperature_2m_min[i] ?? 0,
          apparentTempMax: data.daily.apparent_temperature_max[i] ?? 0,
          apparentTempMin: data.daily.apparent_temperature_min[i] ?? 0,
          sunrise: data.daily.sunrise[i] ?? "",
          sunset: data.daily.sunset[i] ?? "",
          precipitationSum: data.daily.precipitation_sum[i] ?? 0,
          rainSum: data.daily.rain_sum[i] ?? 0,
          showersSum: data.daily.showers_sum[i] ?? 0,
          snowfallSum: data.daily.snowfall_sum[i] ?? 0,
          precipitationProbabilityMax: data.daily.precipitation_probability_max[i] ?? 0,
          windSpeedMax: data.daily.wind_speed_10m_max[i] ?? 0,
          windDirectionDominant: data.daily.wind_direction_10m_dominant[i] ?? 0,
          windGustsMax: data.daily.wind_gusts_10m_max[i] ?? 0,
          uvIndexMax: data.daily.uv_index_max[i] ?? 0,
        })),
        timezone: data.timezone ?? "UTC",
        utcOffset: data.utc_offset_seconds ?? 0,
        latitude: data.latitude,
        longitude: data.longitude,
      };
    }),

  airQuality: publicQuery
    .input(z.object({ latitude: z.number(), longitude: z.number() }))
    .query(async ({ input }) => {
      const { latitude, longitude } = input;
      const hourlyVars = [
        "pm10", "pm2_5", "carbon_monoxide", "nitrogen_dioxide",
        "sulphur_dioxide", "ozone", "european_aqi", "us_aqi",
      ].join(",");

      const url = `${AIR_QUALITY_BASE}/air-quality?latitude=${latitude}&longitude=${longitude}&hourly=${hourlyVars}&timezone=auto`;
      const data = await fetchJson(url);

      const currentIdx = data.hourly?.time?.length > 0
        ? Math.min(new Date().getHours(), data.hourly.time.length - 1)
        : 0;

      return {
        hourly: (data.hourly?.time ?? []).map((t: string, i: number) => ({
          time: t,
          pm10: data.hourly.pm10[i] ?? 0,
          pm2_5: data.hourly.pm2_5[i] ?? 0,
          co: data.hourly.carbon_monoxide[i] ?? 0,
          no2: data.hourly.nitrogen_dioxide[i] ?? 0,
          so2: data.hourly.sulphur_dioxide[i] ?? 0,
          o3: data.hourly.ozone[i] ?? 0,
          euAqi: data.hourly.european_aqi[i] ?? 0,
          usAqi: data.hourly.us_aqi[i] ?? 0,
        })),
        current: {
          pm10: data.hourly?.pm10?.[currentIdx] ?? 0,
          pm2_5: data.hourly?.pm2_5?.[currentIdx] ?? 0,
          euAqi: data.hourly?.european_aqi?.[currentIdx] ?? 0,
          usAqi: data.hourly?.us_aqi?.[currentIdx] ?? 0,
        },
      };
    }),

  geocode: publicQuery
    .input(z.object({ query: z.string().min(1) }))
    .query(async ({ input }) => {
      const { query } = input;
      const url = `${GEOCODING_BASE}/search?name=${encodeURIComponent(query)}&count=10&language=en&format=json`;
      const data = await fetchJson(url);

      return (data.results ?? []).map((r: Record<string, unknown>) => ({
        id: r.id ?? 0,
        name: r.name ?? "",
        latitude: r.latitude ?? 0,
        longitude: r.longitude ?? 0,
        country: r.country ?? "",
        countryCode: r.country_code ?? "",
        admin1: r.admin1 ?? "",
        elevation: r.elevation ?? 0,
        timezone: r.timezone ?? "",
        population: r.population ?? 0,
      }));
    }),
});
