import axios from 'axios';
import type { HeatState, RouteResponse, HeatAlert, RiskTier } from '../types';

const MS2 = import.meta.env.VITE_MS2_BASE || 'http://localhost:8082';
const MS3 = import.meta.env.VITE_MS3_BASE || 'http://localhost:8083';
const MS4 = import.meta.env.VITE_MS4_BASE || 'http://localhost:8084';
const MS5 = import.meta.env.VITE_MS5_BASE || 'http://localhost:8085';

const opts = { timeout: 5000 };

export const heatApi = {
  getCity: async (cityId: string): Promise<HeatState | null> => {
    try {
      const res = await axios.get(`${MS2}/status/city/${cityId}`, opts);
      if (res.data?.status === 'ok' && res.data?.heatState) {
        return res.data.heatState as HeatState;
      }
      return null;
    } catch { return null; }
  },

  getHighRisk: async () => {
    const res = await axios.get(`${MS2}/status/high-risk`, opts);
    return res.data;
  },
};

export const routingApi = {
  getShelters: async (lat: number, lng: number, cityId: string): Promise<RouteResponse> => {
    const res = await axios.get(`${MS4}/api/v1/routes/cool-shelters`, {
      ...opts,
      params: { lat, lng, maxResults: 3, cityId },
    });
    return res.data as RouteResponse;
  },
};

export const agentApi = {
  trigger: async (cityId: string, cityName: string, tier = 'DANGER') => {
    return axios.post(`${MS3}/agent/trigger/${cityId}`, null, {
      ...opts,
      params: { tier, prevTier: 'WATCH', cityName },
    });
  },
  getAlert: async (cityId: string) => {
    const res = await axios.get(`${MS3}/agent/alert/${cityId}`, opts);
    return res.data;
  },
};

export const notificationApi = {
  getFeed: async (): Promise<HeatAlert[]> => {
    try {
      const res = await axios.get(`${MS5}/api/v1/notifications/feed/global`, opts);
      const raw: string[] = res.data?.alerts || [];
      return raw.map((a, i) => {
        const [cityName, severity] = a.split(':');
        return {
          cityName:  cityName || 'Unknown',
          severity:  (severity || 'WATCH') as RiskTier,
          message:   `Heat ${severity} conditions active in ${cityName}`,
          heatIndex: 0,
          timestamp: Date.now() - i * 60_000,
        };
      });
    } catch { return []; }
  },
};
