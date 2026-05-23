import { useEffect, useRef, useCallback } from 'react';
import mapboxgl from 'mapbox-gl';
import type { City, HeatState } from '../../types';
import { TIER_COLOR } from '../../constants';

mapboxgl.accessToken = import.meta.env.VITE_MAPBOX_TOKEN || '';

type OnCityClick = (city: City) => void;

export const useMapbox = (
  containerRef: React.RefObject<HTMLDivElement>,
  onCityClick:  OnCityClick,
) => {
  const mapRef     = useRef<mapboxgl.Map | null>(null);
  const markersRef = useRef<Record<string, mapboxgl.Marker>>({});
  const readyRef   = useRef(false);

  // ── Init map ───────────────────────────────────────────────
  useEffect(() => {
    if (mapRef.current || !containerRef.current) return;

    const map = new mapboxgl.Map({
      container: containerRef.current,
      style:     'mapbox://styles/mapbox/dark-v11',
      center:    [78.9629, 22.5937],
      zoom:      4,
      projection: { name: 'mercator' },
    });

    map.addControl(new mapboxgl.NavigationControl(), 'top-right');
    map.addControl(
      new mapboxgl.GeolocateControl({ positionOptions: { enableHighAccuracy: true } }),
      'top-right'
    );

    map.on('load', () => {
      // Heat source
      map.addSource('heat-pts', {
        type: 'geojson',
        data: { type: 'FeatureCollection', features: [] },
      });

      // Heatmap layer — weight driven by heat index
      map.addLayer({
        id:     'heat-layer',
        type:   'heatmap',
        source: 'heat-pts',
        paint: {
          'heatmap-weight': [
            'interpolate', ['linear'], ['get', 'hi'], 27, 0, 54, 1,
          ],
          'heatmap-intensity': [
            'interpolate', ['linear'], ['zoom'], 2, 1, 9, 3,
          ],
          'heatmap-color': [
            'interpolate', ['linear'], ['heatmap-density'],
            0,   'rgba(34,197,94,0)',
            0.2, 'rgba(234,179,8,0.5)',
            0.5, 'rgba(249,115,22,0.7)',
            0.8, 'rgba(239,68,68,0.85)',
            1.0, 'rgba(124,58,237,1)',
          ],
          'heatmap-radius': [
            'interpolate', ['linear'], ['zoom'], 2, 30, 9, 60,
          ],
          'heatmap-opacity': 0.75,
        },
      });

      readyRef.current = true;
    });

    mapRef.current = map;
    return () => { map.remove(); mapRef.current = null; readyRef.current = false; };
  }, []);

  // ── Update city markers + heatmap ──────────────────────────
  const updateCities = useCallback((
    heatData: Record<string, HeatState>,
    cities:   City[],
  ) => {
    const map = mapRef.current;
    if (!map || !readyRef.current) return;

    // Update heatmap GeoJSON
    const features = cities
      .filter(c => heatData[c.id])
      .map(c => ({
        type:       'Feature'  as const,
        geometry:   { type: 'Point' as const, coordinates: [c.lng, c.lat] },
        properties: { hi: heatData[c.id]?.heatIndex || 0 },
      }));

    (map.getSource('heat-pts') as mapboxgl.GeoJSONSource)
      ?.setData({ type: 'FeatureCollection', features });

    // Update city markers
    cities.forEach(city => {
      const tier  = heatData[city.id]?.riskTier || 'SAFE';
      const color = TIER_COLOR[tier];

      // Remove old marker
      markersRef.current[city.id]?.remove();

      // Build marker element
      const el = document.createElement('div');
      el.style.cssText = `
        width: 36px; height: 36px; border-radius: 50%;
        background: ${color}; border: 2.5px solid #1a1d2e;
        box-shadow: 0 0 14px ${color}88;
        cursor: pointer; display: flex;
        align-items: center; justify-content: center;
        font-size: 14px; transition: transform 0.15s;
        user-select: none;
      `;
      el.textContent   = '🌡️';
      el.title         = `${city.name}: ${heatData[city.id]?.heatIndex?.toFixed(1) || '?'}°C`;
      el.onmouseenter  = () => { el.style.transform = 'scale(1.3)'; };
      el.onmouseleave  = () => { el.style.transform = 'scale(1)'; };
      el.onclick       = () => onCityClick(city);

      markersRef.current[city.id] = new mapboxgl.Marker({ element: el })
        .setLngLat([city.lng, city.lat])
        .addTo(map);
    });
  }, [onCityClick]);

  // ── Fly to location ────────────────────────────────────────
  const flyTo = useCallback((lng: number, lat: number, zoom = 9) => {
    mapRef.current?.flyTo({ center: [lng, lat], zoom, duration: 1200 });
  }, []);

  return { updateCities, flyTo };
};
