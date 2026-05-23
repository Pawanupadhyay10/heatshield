import type { City, RiskTier } from './types';

export const CITIES: City[] = [
  { id: 'city-haridwar', name: 'Haridwar',  lat: 29.9457, lng: 78.1642, country: 'IN' },
  { id: 'city-delhi',    name: 'New Delhi', lat: 28.6139, lng: 77.2090, country: 'IN' },
  { id: 'city-mumbai',   name: 'Mumbai',    lat: 18.9667, lng: 72.8777, country: 'IN' },
  { id: 'city-cairo',    name: 'Cairo',     lat: 30.0444, lng: 31.2357, country: 'EG' },
  { id: 'city-madrid',   name: 'Madrid',    lat: 40.4168, lng: -3.7038, country: 'ES' },
];

export const TIER_COLOR: Record<RiskTier, string> = {
  SAFE:    '#22c55e',
  WATCH:   '#eab308',
  WARNING: '#f97316',
  DANGER:  '#ef4444',
  EXTREME: '#7c3aed',
};

export const TIER_BG: Record<RiskTier, string> = {
  SAFE:    'rgba(34,197,94,0.15)',
  WATCH:   'rgba(234,179,8,0.15)',
  WARNING: 'rgba(249,115,22,0.15)',
  DANGER:  'rgba(239,68,68,0.15)',
  EXTREME: 'rgba(124,58,237,0.15)',
};

export const TIER_LABEL: Record<RiskTier, string> = {
  SAFE: 'Safe', WATCH: 'Watch', WARNING: 'Warning',
  DANGER: 'Danger', EXTREME: 'Extreme',
};

export const SHELTER_ICON: Record<string, string> = {
  hospital: '🏥', mall: '🏬', metro_station: '🚇',
  library: '📚', community_center: '🏛️',
};
