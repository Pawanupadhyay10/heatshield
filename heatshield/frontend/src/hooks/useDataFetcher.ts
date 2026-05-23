import { useEffect, useCallback, useRef } from 'react';
import { useStore } from '../store';
import { heatApi, notificationApi } from '../services/api';
import { CITIES } from '../constants';

const POLL_INTERVAL = 30_000; // 30 seconds

export const useDataFetcher = () => {
  const setHeatData = useStore(s => s.setHeatData);
  const setAlerts   = useStore(s => s.setAlerts);
  const setLoading  = useStore(s => s.setLoading);
  const timerRef    = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchAll = useCallback(async () => {
    // Fetch heat data for all cities in parallel
    await Promise.allSettled(
      CITIES.map(async city => {
        const data = await heatApi.getCity(city.id);
        if (data) setHeatData(city.id, data);
      })
    );

    // Fetch global alert feed
    const alerts = await notificationApi.getFeed();
    setAlerts(alerts);
  }, [setHeatData, setAlerts]);

  useEffect(() => {
    // Initial fetch with loading state
    setLoading(true);
    fetchAll().finally(() => setLoading(false));

    // Poll every 30s
    timerRef.current = setInterval(fetchAll, POLL_INTERVAL);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [fetchAll, setLoading]);

  return { refetch: fetchAll };
};
