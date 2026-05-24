export interface SavedCity {
  id: string;
  name: string;
  latitude: string;
  longitude: string;
  country: string;
  timezone: string;
  isDefault: "0" | "1";
  createdAt: string;
}

const STORAGE_KEY = "skyweather_saved_cities";

function readCities(): SavedCity[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function writeCities(cities: SavedCity[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cities));
}

export function getSavedCities(): SavedCity[] {
  return readCities();
}

export function addSavedCity(city: {
  name: string;
  latitude: number;
  longitude: number;
  country?: string;
  timezone?: string;
}): SavedCity {
  const cities = readCities();

  const exists = cities.some(
    (c) => c.latitude === city.latitude.toFixed(7) && c.longitude === city.longitude.toFixed(7)
  );
  if (exists) {
    throw new Error("Duplicate city");
  }

  const newCity: SavedCity = {
    id: crypto.randomUUID(),
    name: city.name,
    latitude: city.latitude.toFixed(7),
    longitude: city.longitude.toFixed(7),
    country: city.country ?? "",
    timezone: city.timezone ?? "",
    isDefault: cities.length === 0 ? "1" : "0",
    createdAt: new Date().toISOString(),
  };

  cities.push(newCity);
  writeCities(cities);
  return newCity;
}

export function removeSavedCity(id: string): boolean {
  const cities = readCities();
  const filtered = cities.filter((c) => c.id !== id);
  if (filtered.length === cities.length) return false;
  writeCities(filtered);
  return true;
}

export function setDefaultCity(id: string): boolean {
  const cities = readCities();
  const target = cities.find((c) => c.id === id);
  if (!target) return false;
  cities.forEach((c) => (c.isDefault = c.id === id ? "1" : "0"));
  writeCities(cities);
  return true;
}

export function getDefaultCity(): SavedCity | undefined {
  return readCities().find((c) => c.isDefault === "1");
}
