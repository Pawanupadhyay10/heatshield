import { create } from 'zustand';
import type { City, HeatState, HeatAlert } from '../types';

interface AppStore {
  // ── Data state ──────────────────────────────────────────────
  heatData:      Record<string, HeatState>;
  alerts:        HeatAlert[];

  // ── UI state ────────────────────────────────────────────────
  selectedCity:  City | null;
  activeTab:     'risk' | 'shelters' | 'alerts';
  sidebarOpen:   boolean;
  isLoading:     boolean;

  // ── Actions ─────────────────────────────────────────────────
  setHeatData:   (cityId: string, data: HeatState) => void;
  setAlerts:     (alerts: HeatAlert[]) => void;
  selectCity:    (city: City | null) => void;
  setTab:        (tab: 'risk' | 'shelters' | 'alerts') => void;
  setSidebar:    (open: boolean) => void;
  setLoading:    (loading: boolean) => void;
  reset:         () => void;
}

const initialState = {
  heatData:     {},
  alerts:       [],
  selectedCity: null,
  activeTab:    'risk' as const,
  sidebarOpen:  false,
  isLoading:    false,
};

export const useStore = create<AppStore>((set) => ({
  ...initialState,

  setHeatData: (cityId, data) =>
    set(s => ({ heatData: { ...s.heatData, [cityId]: data } })),

  setAlerts: (alerts) => set({ alerts }),

  selectCity: (city) => set({
    selectedCity: city,
    sidebarOpen:  !!city,
    activeTab:    'risk',
  }),

  setTab:     (tab)  => set({ activeTab: tab }),
  setSidebar: (open) => set({ sidebarOpen: open }),
  setLoading: (loading) => set({ isLoading: loading }),
  reset:      () => set(initialState),
}));

// ── Selectors (memoised slices — prevent unnecessary re-renders) ──
export const useHeatData    = () => useStore(s => s.heatData);
export const useAlerts      = () => useStore(s => s.alerts);
export const useSelectedCity= () => useStore(s => s.selectedCity);
export const useActiveTab   = () => useStore(s => s.activeTab);
export const useSidebarOpen = () => useStore(s => s.sidebarOpen);
