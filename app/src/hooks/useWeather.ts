import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchForecast,
  fetchAirQuality,
  fetchGeocode,
  type WeatherData,
  type AirQualityData,
  type GeocodeResult,
} from "@/lib/weather/api";
import {
  getSavedCities,
  addSavedCity,
  removeSavedCity,
  setDefaultCity,
  type SavedCity,
} from "@/lib/storage/cities";

export function useWeatherForecast(latitude: number, longitude: number) {
  return useQuery<WeatherData>({
    queryKey: ["weather", "forecast", latitude, longitude],
    queryFn: () => fetchForecast(latitude, longitude),
    enabled: !!latitude && !!longitude,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAirQuality(latitude: number, longitude: number) {
  return useQuery<AirQualityData>({
    queryKey: ["weather", "airQuality", latitude, longitude],
    queryFn: () => fetchAirQuality(latitude, longitude),
    enabled: !!latitude && !!longitude,
    staleTime: 10 * 60 * 1000,
  });
}

export function useGeocode(query: string) {
  return useQuery<GeocodeResult[]>({
    queryKey: ["weather", "geocode", query],
    queryFn: () => fetchGeocode(query),
    enabled: query.length >= 2,
    staleTime: 60000,
  });
}

export function useSavedCities() {
  return useQuery<SavedCity[]>({
    queryKey: ["cities", "list"],
    queryFn: () => getSavedCities(),
    staleTime: 0,
  });
}

export function useAddCity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (city: { name: string; latitude: number; longitude: number; country?: string; timezone?: string }) => {
      addSavedCity(city);
      return Promise.resolve();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cities"] });
    },
  });
}

export function useRemoveCity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      removeSavedCity(id);
      return Promise.resolve();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cities"] });
    },
  });
}

export function useSetDefaultCity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      setDefaultCity(id);
      return Promise.resolve();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cities"] });
    },
  });
}
