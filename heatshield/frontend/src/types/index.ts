// ============================================================
// HeatShield — ALL types defined here, imported from here only
// This prevents ALL circular import issues
// ============================================================

export type RiskTier = 'SAFE' | 'WATCH' | 'WARNING' | 'DANGER' | 'EXTREME';

export interface City {
  id:      string;
  name:    string;
  lat:     number;
  lng:     number;
  country: string;
}

export interface HeatState {
  cityId:             string;
  cityName:           string;
  heatIndex:          number;
  riskTier:           RiskTier;
  vulnerabilityScore: number;
  estimatedAtRisk:    number;
  trend:              'RISING' | 'FALLING' | 'STABLE';
  updatedAt?:         number;
}

export interface Shelter {
  id?:           string;
  name:          string;
  shelterType:   string;
  lat:           number;
  lng:           number;
  distanceKm:    number;
  exposureScore: number;
  routeAdvice:   string;
  acConfirmed:   boolean;
  source?:       string;
}

export interface HeatAlert {
  cityName:  string;
  severity:  RiskTier;
  message:   string;
  heatIndex: number;
  timestamp: number;
}

export interface RouteResponse {
  shelters:         Shelter[];
  generalAdvice:    string;
  currentHeatIndex: number;
  currentRiskTier:  string;
  source:           string;
}
