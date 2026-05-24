import SearchBar from "./SearchBar";
import SavedCities from "./SavedCities";

interface HeaderProps {
  onCitySelect: (city: { name: string; latitude: number; longitude: number; country: string; timezone: string }) => void;
  onAddCity?: () => void;
  currentCity?: { name: string; latitude: number; longitude: number; country?: string; timezone?: string };
  scrollY?: number;
}

export default function Header({ onCitySelect, onAddCity, currentCity, scrollY = 0 }: HeaderProps) {
  const handleCitySelect = (city: { name: string; latitude: number; longitude: number; country: string; timezone: string }) => {
    onCitySelect(city);
  };

  // Scroll-driven fade: 0~280px range
  const fadeProgress = Math.min(scrollY / 280, 1);
  const eased = 1 - (1 - fadeProgress) * (1 - fadeProgress);

  const headerOpacity = 1 - eased;

  // Internal glass effect also transitions smoothly
  const bgAlpha = 0.3 * eased;
  const gradientAlpha = 0.35 * (1 - eased * 0.8);
  const borderAlpha = eased * 0.08;
  const shadowAlpha = eased * 0.2;
  const logoShadowAlpha = eased * 0.25;
  // Backdrop blur stays subtle and fades out with scroll
  const backdropBlur = eased * 10 * (1 - eased * 0.5);

  return (
    <header
      className="fixed top-0 left-0 right-0 z-40 px-4 sm:px-6 py-3 transition-all duration-500"
      style={{
        opacity: headerOpacity,
        pointerEvents: headerOpacity < 0.12 ? "none" : "auto",
        background:
          fadeProgress > 0.05
            ? `rgba(0, 0, 0, ${bgAlpha})`
            : `linear-gradient(to bottom, rgba(0,0,0,${gradientAlpha}) 0%, transparent 100%)`,
        backdropFilter: fadeProgress > 0.05 ? `blur(${backdropBlur}px) saturate(${1 + eased * 0.15})` : "none",
        WebkitBackdropFilter: fadeProgress > 0.05 ? `blur(${backdropBlur}px) saturate(${1 + eased * 0.15})` : "none",
        borderBottom: borderAlpha > 0.01 ? `1px solid rgba(255,255,255,${borderAlpha})` : "none",
        boxShadow: fadeProgress > 0.1 ? `0 4px 30px rgba(0,0,0,${shadowAlpha})` : "none",
      }}
    >
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-shrink-0">
          <div
            className="w-8 h-8 rounded-lg flex items-center justify-center transition-transform duration-500"
            style={{
              background: "linear-gradient(135deg, #FFD700, #FF8C00)",
              boxShadow: fadeProgress > 0.1 ? `0 0 16px rgba(255, 165, 0, ${logoShadowAlpha})` : "none",
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="5" />
              <line x1="12" y1="1" x2="12" y2="3" />
              <line x1="12" y1="21" x2="12" y2="23" />
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
              <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
              <line x1="1" y1="12" x2="3" y2="12" />
              <line x1="21" y1="12" x2="23" y2="12" />
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
              <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
          </div>
          <span
            className="text-white text-lg font-semibold hidden sm:block transition-opacity duration-300"
            style={{ letterSpacing: "-0.02em", opacity: 0.9 + (1 - eased) * 0.1 }}
          >
            SkyWeather
          </span>
        </div>

        <div className="flex-1 max-w-md">
          <SearchBar onCitySelect={handleCitySelect} currentCityName={currentCity?.name} />
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          <SavedCities onCitySelect={handleCitySelect} currentCity={currentCity} />

          {onAddCity && currentCity && (
            <button
              onClick={onAddCity}
              className="flex items-center gap-1 px-3 py-2 rounded-full text-xs text-white/50 hover:text-white hover:bg-white/10 transition-all duration-300"
              style={{
                background: fadeProgress > 0.1 ? `rgba(255,255,255,${0.04 + eased * 0.04})` : "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
              }}
              title="收藏此城市"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span className="hidden md:inline">收藏</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
