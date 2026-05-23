import React, { useRef, useEffect, useCallback, memo } from 'react';
import 'mapbox-gl/dist/mapbox-gl.css';
import { useMapbox } from './useMapbox';
import { useStore, useHeatData } from '../../store';
import { CITIES } from '../../constants';
import type { City } from '../../types';

export const HeatMap = memo(() => {
  const containerRef   = useRef<HTMLDivElement>(null);
  const heatData       = useHeatData();
  const selectCity     = useStore(s => s.selectCity);
  const setSidebar     = useStore(s => s.setSidebar);
  const setTab         = useStore(s => s.setTab);

  const handleCityClick = useCallback((city: City) => {
    selectCity(city);
    setTab('risk');
    setSidebar(true);
    flyTo(city.lng, city.lat);
  }, [selectCity, setTab, setSidebar]);

  const { updateCities, flyTo } = useMapbox(containerRef, handleCityClick);

  useEffect(() => {
    updateCities(heatData, CITIES);
  }, [heatData, updateCities]);

  return (
    <div
      ref={containerRef}
      style={{ flex: 1, position: 'relative', minHeight: 0 }}
    />
  );
});
