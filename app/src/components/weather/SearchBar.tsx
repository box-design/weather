import { useState, useRef, useCallback, useEffect } from "react";
import { Search, MapPin, Loader2 } from "lucide-react";
import { useGeocode } from "@/hooks/useWeather";

interface SearchBarProps {
  onCitySelect: (city: { name: string; latitude: number; longitude: number; country: string; timezone: string }) => void;
  currentCityName?: string;
}

export default function SearchBar({ onCitySelect, currentCityName }: SearchBarProps) {
  const [query, setQuery] = useState("");
  const [showResults, setShowResults] = useState(false);
  const [letterAnims, setLetterAnims] = useState<string[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);

  const { data: searchResults, isLoading } = useGeocode(query);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    setShowResults(val.length >= 2);
    const chars = val.split("");
    setLetterAnims(chars);
  }, []);

  const handleCityClick = (city: { name: string; latitude: number; longitude: number; country: string; timezone: string }) => {
    setQuery("");
    setShowResults(false);
    setLetterAnims([]);
    onCitySelect(city);
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (resultsRef.current && !resultsRef.current.contains(e.target as Node) &&
          inputRef.current && !inputRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className="relative w-full max-w-md">
      <div
        className="flex items-center gap-2 px-4 py-2.5 rounded-full border transition-all duration-300"
        style={{
            background: "rgba(255,255,255,0.08)",
            backdropFilter: "blur(24px) saturate(1.3)",
            WebkitBackdropFilter: "blur(24px) saturate(1.3)",
            borderColor: "rgba(255,255,255,0.15)",
            boxShadow: "0 2px 12px rgba(0,0,0,0.1)",
          }}
        onMouseEnter={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.4)";
          (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.12)";
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(255,255,255,0.15)";
          (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.08)";
        }}
      >
        <Search className="w-4 h-4 text-white/60 flex-shrink-0" />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={() => query.length >= 2 && setShowResults(true)}
          placeholder={currentCityName || "Search city..."}
          className="flex-1 bg-transparent text-white text-sm placeholder-white/40 outline-none"
          style={{ fontFamily: "Inter, sans-serif" }}
        />
        {isLoading && <Loader2 className="w-4 h-4 text-white/60 animate-spin" />}
        <button
          className="p-1.5 rounded-full hover:bg-white/10 transition-colors"
          onClick={() => {
            if (navigator.geolocation) {
              navigator.geolocation.getCurrentPosition(
                (pos) => {
                  onCitySelect({
                    name: "当前位置",
                    latitude: pos.coords.latitude,
                    longitude: pos.coords.longitude,
                    country: "",
                    timezone: "auto",
                  });
                },
                () => alert("Could not get your location. Please allow location access.")
              );
            }
          }}
          title="使用当前位置"
        >
          <MapPin className="w-4 h-4 text-white/60" />
        </button>
      </div>

      {letterAnims.length > 0 && (
        <div
          className="absolute top-1/2 left-10 -translate-y-1/2 flex pointer-events-none"
          style={{ zIndex: 5 }}
        >
          {letterAnims.map((char, i) => (
            <span
              key={`${i}-${char}`}
              className="inline-block text-white/30 text-sm"
              style={{
                animation: "letterPop 0.6s ease-out forwards",
                animationDelay: `${i * 0.02}s`,
                fontFamily: "Inter, sans-serif",
              }}
            >
              {char === " " ? "\u00A0" : char}
            </span>
          ))}
        </div>
      )}

      {showResults && searchResults && searchResults.length > 0 && (
        <div
          ref={resultsRef}
          className="absolute top-full left-0 right-0 mt-2 rounded-2xl overflow-hidden max-h-72 overflow-y-auto"
          style={{
            background: "rgba(15, 15, 30, 0.65)",
            backdropFilter: "blur(32px) saturate(1.4)",
            WebkitBackdropFilter: "blur(32px) saturate(1.4)",
            border: "1px solid rgba(255,255,255,0.12)",
            boxShadow: "0 8px 32px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.06)",
            zIndex: 50,
          }}
        >
          {searchResults.map((city) => (
            <button
              key={`${city.latitude}-${city.longitude}`}
              className="w-full text-left px-4 py-3 hover:bg-white/10 transition-colors flex flex-col gap-0.5"
              onClick={() => handleCityClick({
                name: city.name,
                latitude: city.latitude,
                longitude: city.longitude,
                country: city.country,
                timezone: city.timezone,
              })}
            >
              <span className="text-white text-sm font-medium">{city.name}</span>
              <span className="text-white/50 text-xs">
                {city.admin1 ? `${city.admin1}, ` : ""}{city.country}
              </span>
            </button>
          ))}
        </div>
      )}

      <style>{`
        @keyframes letterPop {
          0% { transform: scale(0) rotateX(-90deg); opacity: 0; }
          60% { transform: scale(1.2) rotateX(10deg); opacity: 1; }
          100% { transform: scale(1) rotateX(0deg); opacity: 0.3; }
        }
      `}</style>
    </div>
  );
}
