import React, { useState, useEffect, useRef, useCallback, memo } from 'react';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { create } from 'zustand';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import axios from 'axios';
import toast, { Toaster } from 'react-hot-toast';

// ── Config ────────────────────────────────────────────────────
const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN || '';
const MS2 = import.meta.env.VITE_MS2_BASE || 'http://localhost:8082';
const MS3 = import.meta.env.VITE_MS3_BASE || 'http://localhost:8083';
const MS4 = import.meta.env.VITE_MS4_BASE || 'http://localhost:8084';
const MS5 = import.meta.env.VITE_MS5_BASE || 'http://localhost:8085';

mapboxgl.accessToken = MAPBOX_TOKEN;

// ── Types ─────────────────────────────────────────────────────
interface City { id: string; name: string; lat: number; lng: number; }
interface HeatState {
  cityId: string; cityName: string; heatIndex: number;
  riskTier: string; vulnerabilityScore: number;
  estimatedAtRisk: number; trend: string;
}
interface Shelter {
  name: string; shelterType: string; distanceKm: number;
  exposureScore: number; routeAdvice: string; acConfirmed: boolean;
}
interface HeatAlert { cityName: string; severity: string; message: string; }

// ── Constants ─────────────────────────────────────────────────
const CITIES: City[] = [
  { id: 'city-haridwar', name: 'Haridwar',  lat: 29.9457, lng: 78.1642 },
  { id: 'city-delhi',    name: 'New Delhi', lat: 28.6139, lng: 77.2090 },
  { id: 'city-mumbai',   name: 'Mumbai',    lat: 18.9667, lng: 72.8777 },
  { id: 'city-cairo',    name: 'Cairo',     lat: 30.0444, lng: 31.2357 },
  { id: 'city-madrid',   name: 'Madrid',    lat: 40.4168, lng: -3.7038 },
];

const TIER_COLOR: Record<string, string> = {
  SAFE: '#22c55e', WATCH: '#eab308', WARNING: '#f97316',
  DANGER: '#ef4444', EXTREME: '#7c3aed',
};
const TIER_BG: Record<string, string> = {
  SAFE: 'rgba(34,197,94,0.15)', WATCH: 'rgba(234,179,8,0.15)',
  WARNING: 'rgba(249,115,22,0.15)', DANGER: 'rgba(239,68,68,0.15)',
  EXTREME: 'rgba(124,58,237,0.15)',
};

// ── Zustand Store ─────────────────────────────────────────────
interface Store {
  heatData:      Record<string, HeatState>;
  selectedCity:  City | null;
  activeTab:     'risk' | 'shelters' | 'alerts';
  sidebarOpen:   boolean;
  alerts:        HeatAlert[];
  setHeat:       (id: string, data: HeatState) => void;
  selectCity:    (city: City | null) => void;
  setTab:        (tab: 'risk' | 'shelters' | 'alerts') => void;
  setSidebar:    (open: boolean) => void;
  setAlerts:     (alerts: HeatAlert[]) => void;
}

const useStore = create<Store>((set) => ({
  heatData: {}, selectedCity: null,
  activeTab: 'risk', sidebarOpen: false, alerts: [],
  setHeat:    (id, data) => set(s => ({ heatData: { ...s.heatData, [id]: data } })),
  selectCity: (city)     => set({ selectedCity: city, sidebarOpen: !!city, activeTab: 'risk' }),
  setTab:     (tab)      => set({ activeTab: tab }),
  setSidebar: (open)     => set({ sidebarOpen: open }),
  setAlerts:  (alerts)   => set({ alerts }),
}));

// ── API ───────────────────────────────────────────────────────
const api = {
  heat:     (id: string)  => axios.get(`${MS2}/status/city/${id}`, { timeout: 5000 }),
  shelters: (lat: number, lng: number, cityId: string) =>
    axios.get(`${MS4}/api/v1/routes/cool-shelters`, { params: { lat, lng, maxResults: 3, cityId }, timeout: 5000 }),
  alerts:   () => axios.get(`${MS5}/api/v1/notifications/feed/global`, { timeout: 3000 }),
  trigger:  (cityId: string, cityName: string) =>
    axios.post(`${MS3}/agent/trigger/${cityId}`, null, { params: { tier: 'DANGER', prevTier: 'WATCH', cityName } }),
};

// ── QueryClient ───────────────────────────────────────────────
const qc = new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } });

// ── Hooks ─────────────────────────────────────────────────────
const useIsMobile = () => {
  const [mobile, setMobile] = useState(window.innerWidth < 768);
  useEffect(() => {
    const h = () => setMobile(window.innerWidth < 768);
    window.addEventListener('resize', h);
    return () => window.removeEventListener('resize', h);
  }, []);
  return mobile;
};

// ── Components ────────────────────────────────────────────────
const TierBadge = memo(({ tier }: { tier: string }) => (
  <span style={{
    fontSize: '10px', fontWeight: 600, padding: '2px 8px',
    borderRadius: '20px', color: TIER_COLOR[tier] || '#718096',
    background: TIER_BG[tier] || 'rgba(113,128,150,0.15)',
  }}>{tier}</span>
));

const CityCard = memo(({ city, selected }: { city: City; selected: boolean }) => {
  const state    = useStore(s => s.heatData[city.id]);
  const selectCity = useStore(s => s.selectCity);
  const tier     = state?.riskTier || 'SAFE';
  const color    = TIER_COLOR[tier];

  return (
    <div onClick={() => selectCity(city)} style={{
      padding: '10px 12px', borderRadius: '8px', cursor: 'pointer',
      background: selected ? '#2d3748' : '#242736',
      borderLeft: `4px solid ${color}`, marginBottom: '4px',
      outline: selected ? '1px solid #4a5568' : 'none',
      transition: 'background 0.15s',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
        <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}>{city.name}</span>
        <TierBadge tier={tier} />
      </div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
        <span style={{ fontSize: '20px', fontWeight: 700, color }}>
          {state?.heatIndex?.toFixed(1) ?? '—'}°C
        </span>
        {state && (
          <span style={{ fontSize: '11px', color: '#718096' }}>
            👥 {((state.estimatedAtRisk || 0) / 1000).toFixed(0)}K
            {' '}{state.trend === 'RISING' ? '↑' : state.trend === 'FALLING' ? '↓' : '→'}
          </span>
        )}
      </div>
    </div>
  );
});

const RiskPanel = memo(({ state }: { state: HeatState }) => {
  const color = TIER_COLOR[state.riskTier] || '#718096';
  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '12px' }}>
        {[
          { label: 'Heat Index', value: `${state.heatIndex?.toFixed(1)}°C`, color },
          { label: 'Risk Tier',  value: state.riskTier, color },
          { label: 'At Risk',    value: `${((state.estimatedAtRisk||0)/1000).toFixed(0)}K` },
          { label: 'Trend',      value: state.trend === 'RISING' ? '↑ Rising' : state.trend === 'FALLING' ? '↓ Falling' : '→ Stable' },
        ].map(s => (
          <div key={s.label} style={{ background: '#242736', borderRadius: '8px', padding: '10px' }}>
            <div style={{ fontSize: '10px', color: '#718096', marginBottom: '4px' }}>{s.label}</div>
            <div style={{ fontSize: '18px', fontWeight: 700, color: s.color || '#e2e8f0' }}>{s.value}</div>
          </div>
        ))}
      </div>
      <div style={{ fontSize: '11px', color: '#718096', marginBottom: '6px' }}>
        Vulnerability: {state.vulnerabilityScore?.toFixed(0)}/100
      </div>
      <div style={{ height: '6px', background: '#2d3748', borderRadius: '3px' }}>
        <div style={{
          height: '100%', borderRadius: '3px',
          width: `${state.vulnerabilityScore || 0}%`,
          background: color, transition: 'width 0.4s',
        }} />
      </div>
    </div>
  );
});

const ShelterPanel = memo(({ city }: { city: City }) => {
  const { data, isLoading } = useQuery({
    queryKey: ['shelters', city.id],
    queryFn:  () => api.shelters(city.lat, city.lng, city.id).then(r => r.data),
    staleTime: 600_000,
  });
  const shelters: Shelter[] = data?.shelters || [];
  if (isLoading) return <div style={{ color: '#718096', padding: '16px', textAlign: 'center' }}>Loading shelters...</div>;
  if (!shelters.length) return <div style={{ color: '#718096', padding: '16px', textAlign: 'center' }}>No shelters found</div>;
  return (
    <div>
      {shelters.map((s, i) => (
        <div key={i} style={{ background: '#242736', borderRadius: '8px', padding: '10px', marginBottom: '8px' }}>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '6px' }}>
            <span style={{
              width: '20px', height: '20px', borderRadius: '50%',
              background: '#f97316', color: '#fff', display: 'flex',
              alignItems: 'center', justifyContent: 'center', fontSize: '11px', fontWeight: 700,
            }}>{i + 1}</span>
            <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}>{s.name}</span>
          </div>
          <div style={{ fontSize: '11px', color: '#718096', marginBottom: '4px' }}>
            📍 {s.distanceKm?.toFixed(2)}km · 🌡️ Exposure: {s.exposureScore?.toFixed(1)}
            {s.acConfirmed && ' · ❄️ AC'}
          </div>
          <div style={{ fontSize: '11px', color: '#a0aec0', lineHeight: 1.5 }}>{s.routeAdvice}</div>
        </div>
      ))}
    </div>
  );
});

const AlertsPanel = memo(() => {
  const alerts = useStore(s => s.alerts);
  if (!alerts.length) return <div style={{ color: '#718096', padding: '16px', textAlign: 'center' }}>No active alerts</div>;
  return (
    <div>
      {alerts.slice(0, 10).map((a, i) => (
        <div key={i} style={{
          background: '#242736', borderRadius: '8px', padding: '10px',
          marginBottom: '8px', borderLeft: `3px solid ${TIER_COLOR[a.severity] || '#eab308'}`,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
            <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}>{a.cityName}</span>
            <span style={{ fontSize: '11px', fontWeight: 700, color: TIER_COLOR[a.severity] || '#eab308' }}>{a.severity}</span>
          </div>
          <div style={{ fontSize: '11px', color: '#a0aec0' }}>{a.message}</div>
        </div>
      ))}
    </div>
  );
});

// ── Map Component ─────────────────────────────────────────────
const HeatMap = memo(() => {
  const ref      = useRef<HTMLDivElement>(null);
  const map      = useRef<mapboxgl.Map | null>(null);
  const markers  = useRef<Record<string, mapboxgl.Marker>>({});
  const heatData = useStore(s => s.heatData);
  const selectCity = useStore(s => s.selectCity);

  useEffect(() => {
    if (map.current || !ref.current) return;
    map.current = new mapboxgl.Map({
      container: ref.current,
      style: 'mapbox://styles/mapbox/dark-v11',
      center: [78.9629, 22.5937], zoom: 4,
    });
    map.current.addControl(new mapboxgl.NavigationControl(), 'top-right');
    map.current.on('load', () => {
      map.current!.addSource('heat-pts', { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
      map.current!.addLayer({
        id: 'heat-layer', type: 'heatmap', source: 'heat-pts',
        paint: {
          'heatmap-weight': ['interpolate',['linear'],['get','hi'],27,0,54,1],
          'heatmap-intensity': ['interpolate',['linear'],['zoom'],2,1,9,3],
          'heatmap-color': ['interpolate',['linear'],['heatmap-density'],
            0,'rgba(34,197,94,0)', 0.3,'rgba(234,179,8,0.6)',
            0.6,'rgba(249,115,22,0.8)', 1,'rgba(124,58,237,1)'],
          'heatmap-radius': ['interpolate',['linear'],['zoom'],2,30,9,60],
          'heatmap-opacity': 0.75,
        }
      });
    });
    return () => { map.current?.remove(); map.current = null; };
  }, []);

  useEffect(() => {
    if (!map.current?.loaded()) return;
    const features = CITIES.filter(c => heatData[c.id]).map(c => ({
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [c.lng, c.lat] },
      properties: { hi: heatData[c.id]?.heatIndex || 0 }
    }));
    (map.current.getSource('heat-pts') as mapboxgl.GeoJSONSource)?.setData({ type: 'FeatureCollection', features });

    CITIES.forEach(city => {
      const tier  = heatData[city.id]?.riskTier || 'SAFE';
      const color = TIER_COLOR[tier];
      markers.current[city.id]?.remove();
      const el = document.createElement('div');
      el.style.cssText = `width:36px;height:36px;border-radius:50%;background:${color};border:2.5px solid #1a1d2e;box-shadow:0 0 14px ${color}99;cursor:pointer;display:flex;align-items:center;justify-content:center;font-size:14px;transition:transform 0.15s;`;
      el.textContent = '🌡️';
      el.onmouseenter = () => el.style.transform = 'scale(1.25)';
      el.onmouseleave = () => el.style.transform = 'scale(1)';
      el.onclick = () => {
        selectCity(city);
        map.current?.flyTo({ center: [city.lng, city.lat], zoom: 9, duration: 1200 });
      };
      markers.current[city.id] = new mapboxgl.Marker({ element: el })
        .setLngLat([city.lng, city.lat]).addTo(map.current!);
    });
  }, [heatData, selectCity]);

  return <div ref={ref} style={{ flex: 1, position: 'relative' }} />;
});

// ── Sidebar ───────────────────────────────────────────────────
const Sidebar = memo(() => {
  const heatData     = useStore(s => s.heatData);
  const selectedCity = useStore(s => s.selectedCity);
  const selectCity   = useStore(s => s.selectCity);
  const activeTab    = useStore(s => s.activeTab);
  const setTab       = useStore(s => s.setTab);
  const sidebarOpen  = useStore(s => s.sidebarOpen);
  const setSidebar   = useStore(s => s.setSidebar);
  const isMobile     = useIsMobile();

  const handleAgent = async () => {
    if (!selectedCity) return;
    try {
      await api.trigger(selectedCity.id, selectedCity.name);
      toast.success(`Agent triggered for ${selectedCity.name}`);
    } catch { toast.error('Agent trigger failed'); }
  };

  const style: React.CSSProperties = isMobile ? {
    position: 'fixed', bottom: 0, left: 0, right: 0,
    height: sidebarOpen ? '72vh' : '0',
    background: '#1a1d2e',
    borderTopLeftRadius: '16px', borderTopRightRadius: '16px',
    borderTop: '1px solid #2d3748',
    overflow: 'hidden',
    transition: 'height 0.3s cubic-bezier(0.4,0,0.2,1)',
    zIndex: 200, display: 'flex', flexDirection: 'column',
  } : {
    width: '340px', flexShrink: 0,
    background: '#1a1d2e', borderLeft: '1px solid #2d3748',
    display: 'flex', flexDirection: 'column', overflowY: 'auto',
  };

  return (
    <div style={style}>
      {isMobile && sidebarOpen && (
        <div style={{ padding: '8px', display: 'flex', justifyContent: 'center', cursor: 'pointer' }}
          onClick={() => setSidebar(false)}>
          <div style={{ width: '36px', height: '4px', borderRadius: '2px', background: '#4a5568' }} />
        </div>
      )}

      <div style={{ padding: '0 8px 8px', overflowY: 'auto', flex: 1 }}>
        {/* City list */}
        {!selectedCity && (
          <>
            <div style={{ fontSize: '11px', fontWeight: 600, color: '#718096', textTransform: 'uppercase', letterSpacing: '0.05em', padding: '14px 8px 8px' }}>
              🌍 Live Heat Monitor
            </div>
            {CITIES.map(city => (
              <CityCard key={city.id} city={city} selected={selectedCity?.id === city.id} />
            ))}
          </>
        )}

        {/* Detail panel */}
        {selectedCity && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 4px', borderBottom: '1px solid #2d3748', marginBottom: '8px' }}>
              <div>
                <div style={{ fontSize: '15px', fontWeight: 600, color: '#e2e8f0' }}>{selectedCity.name}</div>
                <button onClick={() => selectCity(null)} style={{ fontSize: '11px', color: '#718096', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
                  ← Back to all cities
                </button>
              </div>
              <button onClick={handleAgent} style={{ padding: '5px 10px', borderRadius: '6px', border: 'none', background: '#7c3aed', color: '#fff', cursor: 'pointer', fontSize: '11px' }}>
                🤖 Agent
              </button>
            </div>

            {/* Tabs */}
            <div style={{ display: 'flex', borderBottom: '1px solid #2d3748', marginBottom: '12px' }}>
              {(['risk', 'shelters', 'alerts'] as const).map(t => (
                <button key={t} onClick={() => setTab(t)} style={{
                  flex: 1, padding: '8px 4px', border: 'none', background: 'none',
                  color: activeTab === t ? '#f97316' : '#718096',
                  borderBottom: activeTab === t ? '2px solid #f97316' : '2px solid transparent',
                  cursor: 'pointer', fontSize: '12px', fontWeight: 500,
                }}>
                  {t === 'risk' ? '🌡️ Risk' : t === 'shelters' ? '🏥 Shelters' : '🔔 Alerts'}
                </button>
              ))}
            </div>

            {activeTab === 'risk' && heatData[selectedCity.id] && <RiskPanel state={heatData[selectedCity.id]} />}
            {activeTab === 'shelters' && <ShelterPanel city={selectedCity} />}
            {activeTab === 'alerts' && <AlertsPanel />}
          </>
        )}
      </div>
    </div>
  );
});

// ── Data fetcher ──────────────────────────────────────────────
const DataFetcher: React.FC = () => {
  const setHeat   = useStore(s => s.setHeat);
  const setAlerts = useStore(s => s.setAlerts);

  const fetchAll = useCallback(async () => {
    await Promise.allSettled(CITIES.map(async city => {
      try {
        const res = await api.heat(city.id);
        if (res.data.status === 'ok' && res.data.heatState)
          setHeat(city.id, res.data.heatState);
      } catch { /* silent */ }
    }));
    try {
      const res = await api.alerts();
      const parsed: HeatAlert[] = (res.data.alerts || []).map((a: string) => {
        const [cityName, severity] = a.split(':');
        return { cityName, severity, message: `Heat ${severity} conditions active` };
      });
      setAlerts(parsed);
    } catch { /* silent */ }
  }, [setHeat, setAlerts]);

  useEffect(() => {
    fetchAll();
    const id = setInterval(fetchAll, 30_000);
    return () => clearInterval(id);
  }, [fetchAll]);

  return null;
};

// ── App ───────────────────────────────────────────────────────
const AppInner: React.FC = () => {
  const isMobile     = useIsMobile();
  const selectedCity = useStore(s => s.selectedCity);
  const sidebarOpen  = useStore(s => s.sidebarOpen);
  const setSidebar   = useStore(s => s.setSidebar);
  const [updated, setUpdated] = useState(new Date());

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100dvh', background: '#0f1117', color: '#e2e8f0', overflow: 'hidden' }}>
      <Toaster position="top-center" toastOptions={{ duration: 3000 }} />
      <DataFetcher />

      {/* Header */}
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 16px', background: '#1a1d2e', borderBottom: '1px solid #2d3748', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '18px', fontWeight: 700, color: '#f97316' }}>🌡️ HeatShield</span>
          <span style={{ fontSize: '11px', color: '#718096' }}>Global Urban Heat Intelligence</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '11px', color: '#718096' }}>{updated.toLocaleTimeString()}</span>
          {isMobile && selectedCity && (
            <button onClick={() => setSidebar(!sidebarOpen)} style={{ padding: '5px 10px', borderRadius: '6px', border: '1px solid #4a5568', background: '#2d3748', color: '#e2e8f0', cursor: 'pointer', fontSize: '11px' }}>
              {sidebarOpen ? '✕' : `📊 ${selectedCity.name}`}
            </button>
          )}
        </div>
      </header>

      {/* Main */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>
        <HeatMap />
        {!isMobile && <Sidebar />}
      </div>

      {/* Mobile bottom sheet */}
      {isMobile && <Sidebar />}
    </div>
  );
};

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AppInner />
    </QueryClientProvider>
  );
}
